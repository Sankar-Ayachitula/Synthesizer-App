package edu.northeastern.synthesizer.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Arrays;
import java.util.List;

import edu.northeastern.synthesizer.R;
import edu.northeastern.synthesizer.adapters.OnWaveTypeSelected;
import edu.northeastern.synthesizer.adapters.WaveAdapter;
import edu.northeastern.synthesizer.adapters.WaveType;
import edu.northeastern.synthesizer.models.WaveModel;
import edu.northeastern.synthesizer.utils.NativeSynth;

import edu.northeastern.synthesizer.adapters.OnWaveTypeSelected;
import edu.northeastern.synthesizer.adapters.WaveType;
// import edu.northeastern.synthesizer.views.OscilloscopeView;

public class MainActivity extends AppCompatActivity implements OnWaveTypeSelected {

    private ArrayList<WaveModel> waves = new ArrayList<>();
    RecyclerView recyclerView;
    WaveAdapter adapter;


    // NEW: oscilloscope + polling
  //  private OscilloscopeView oscilloscopeView;
    private static final int WAVEFORM_SIZE = 512;
    private final float[] waveformBuffer = new float[WAVEFORM_SIZE];

    private final Handler waveformHandler = new Handler(Looper.getMainLooper());
    private final int WAVEFORM_REFRESH_MS = 16; // ~60 FPS
//    private final Runnable waveformUpdater = new Runnable() {
//        @Override
//         public void run() {
//            try {
//                NativeSynth.getWaveform(waveformBuffer);
//               // if (oscilloscopeView != null) {
//                    oscilloscopeView.updateWaveform(waveformBuffer);
//                }
//            } catch (Throwable t) {
//                // ignore; keep UI safe
//            }
//            waveformHandler.postDelayed(this, WAVEFORM_REFRESH_MS);
//        }
 //   };

    // NEW: recording
    private Button recordButton;
    private boolean isRecording = false;
    private File lastRecordingFile = null;

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
       // oscilloscopeView = findViewById(R.id.oscilloscopeView);
        recordButton = findViewById(R.id.btnRecord);

        recyclerView = findViewById(R.id.rv_waveform);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        findViewById(R.id.btnHistory).setOnClickListener(v -> {
            NativeSynth.stop();
            Intent i = new Intent(MainActivity.this, RecordingHistoryActivity.class);
            startActivity(i);
        });
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

        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleRecording();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        NativeSynth.start();
    }

    private void toggleRecording() {
        if (!isRecording) {
            File f = createNewRecordingFile();
            if (f == null) return;
            lastRecordingFile = f;
            NativeSynth.startRecording(f.getAbsolutePath());
            NativeSynth.startRecording(f.getAbsolutePath());
            isRecording = true;
            recordButton.setText("Stop");
            Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show();
        } else {
            NativeSynth.stopRecording();
            NativeSynth.stopRecording();
            isRecording = false;
            recordButton.setText("Record");
            if (lastRecordingFile != null) {
                Toast.makeText(this,
                        "Recording saved: " + lastRecordingFile.getName(),
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private File createNewRecordingFile() {
        File recordingsDir = new File(getFilesDir(), "recordings");
        if (!recordingsDir.exists()) {
            boolean ok = recordingsDir.mkdirs();
            if (!ok) {
                Toast.makeText(this, "Failed to create recordings directory", Toast.LENGTH_SHORT).show();
                return null;
            }
        }
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        return new File(recordingsDir, "recording_" + timestamp + ".wav");
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