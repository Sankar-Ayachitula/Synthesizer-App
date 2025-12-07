package edu.northeastern.synthesizer.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.northeastern.synthesizer.R;
import edu.northeastern.synthesizer.adapters.OnWaveTypeSelected;
import edu.northeastern.synthesizer.adapters.WaveAdapter;
import edu.northeastern.synthesizer.adapters.WaveType;
import edu.northeastern.synthesizer.models.WaveModel;
import edu.northeastern.synthesizer.utils.NativeSynth;

public class MainActivity extends AppCompatActivity implements OnWaveTypeSelected {

    private ArrayList<WaveModel> waves = new ArrayList<>();
    RecyclerView recyclerView;
    WaveAdapter adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });

        recyclerView = findViewById(R.id.rv_waveform);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Example list (replace with your actual model)
        List<String> list = Arrays.asList("Wave 1", "Wave 2", "Wave 3");

        adapter = new WaveAdapter(waves, this);
        recyclerView.setAdapter(adapter);

        NativeSynth.start();
        // waveType: 0=sine, 1=square, 2=tri, 3=saw

//        int waveId1 = NativeSynth.addWave(2, 440f, 1f); // sine

        findViewById(R.id.addWaveForm).setOnClickListener(v -> {
            addNewWave();
        });

    }

    @Override
    public void onWaveSelected(int pos, WaveType type) {
        WaveModel model = waves.get(pos);

        if (model.waveId == -1) {
            // FIRST TIME → create in native
            int id = NativeSynth.addWave(type.ordinal(), model.frequency, model.amplitude);
            model.waveId = id;
        } else {
            // Modify type only
            NativeSynth.setWaveType(model.waveId, type.ordinal());
        }
    }

    @Override
    public void onWaveFrequencyChanged(int pos, WaveModel model) {
        if (model.waveId != -1) {
            NativeSynth.setWaveFrequency(model.waveId, model.frequency);
        }
    }

    @Override
    public void onWaveLfoChanged(int pos, WaveModel model) {
        if (model.waveId != -1) {
            NativeSynth.setWaveLfoFrequency(model.waveId, model.lfoFrequency);
        }
    }

    private void addNewWave() {
        // 1. Create WaveModel with defaults
        WaveModel model = new WaveModel();

        // 2. Add wave to native engine
        int id = NativeSynth.addWave(
                model.waveType,
                model.frequency,
                model.amplitude
        );
        model.waveId = id;

        // 3. Add to list
        waves.add(model);

        // 4. Notify adapter
        adapter.notifyItemInserted(waves.size() - 1);

        // 5. Scroll to last item
        recyclerView.smoothScrollToPosition(waves.size() - 1);
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Stop audio thread cleanly
        NativeSynth.stop();
    }


}