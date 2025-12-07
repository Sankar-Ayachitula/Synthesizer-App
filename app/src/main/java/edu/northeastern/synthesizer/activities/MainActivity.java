package edu.northeastern.synthesizer.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import edu.northeastern.synthesizer.R;
import edu.northeastern.synthesizer.adapters.OnWaveTypeSelected;
import edu.northeastern.synthesizer.adapters.WaveAdapter;
import edu.northeastern.synthesizer.adapters.WaveType;
import edu.northeastern.synthesizer.models.WaveModel;
import edu.northeastern.synthesizer.utils.NativeSynth;
import edu.northeastern.synthesizer.utils.WaveformGenerator;

public class MainActivity extends AppCompatActivity implements OnWaveTypeSelected {

    private ArrayList<WaveModel> waves = new ArrayList<>();
    RecyclerView recyclerView;
    WaveAdapter adapter;

    private Button recordButton;
    private boolean isRecording = false;
    private File lastRecordingFile = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        recordButton = findViewById(R.id.btnRecord);

        recyclerView = findViewById(R.id.rv_waveform);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new WaveAdapter(waves, this);
        recyclerView.setAdapter(adapter);

        NativeSynth.start();

        findViewById(R.id.addWaveForm).setOnClickListener(v -> addNewWave());

        recordButton.setOnClickListener(v -> toggleRecording());

        findViewById(R.id.btnHistory).setOnClickListener(v -> {
            NativeSynth.stop();
            Intent i = new Intent(MainActivity.this, RecordingHistoryActivity.class);
            startActivity(i);
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

            isRecording = true;
            recordButton.setText("Stop");
            Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show();

        } else {
            NativeSynth.stopRecording();
            isRecording = false;
            recordButton.setText("Record");

            if (lastRecordingFile != null) {
                Toast.makeText(this,
                        "Recording saved: " + lastRecordingFile.getName(),
                        Toast.LENGTH_LONG).show();
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
            int id = NativeSynth.addWave(type.ordinal(), model.frequency, model.amplitude);
            model.waveId = id;
        } else {
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

        WaveModel model = new WaveModel();

        model.previewWave = WaveformGenerator.generate(
                model.waveType,   // 0 = sine
                model.amplitude
        );

        int id = NativeSynth.addWave(
                model.waveType,
                model.frequency,
                model.amplitude
        );
        model.waveId = id;

        waves.add(model);
        adapter.notifyItemInserted(waves.size() - 1);
        recyclerView.smoothScrollToPosition(waves.size() - 1);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        NativeSynth.stop();
    }
}
