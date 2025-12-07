package edu.northeastern.synthesizer.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
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
import edu.northeastern.synthesizer.adapters.WaveAdapter;
import edu.northeastern.synthesizer.models.WaveModel;
import edu.northeastern.synthesizer.utils.NativeSynth;
//import edu.northeastern.synthesizer.utils.OnWaveTypeSelected;
//import edu.northeastern.synthesizer.utils.WaveType;
import edu.northeastern.synthesizer.adapters.OnWaveTypeSelected;
import edu.northeastern.synthesizer.adapters.WaveType;

import edu.northeastern.synthesizer.views.OscilloscopeView;

public class MainActivity extends AppCompatActivity implements OnWaveTypeSelected {

    private final ArrayList<WaveModel> waves = new ArrayList<>();
    private RecyclerView recyclerView;
    private WaveAdapter adapter;

    // NEW: oscilloscope + polling
    private OscilloscopeView oscilloscopeView;
    private static final int WAVEFORM_SIZE = 512;
    private final float[] waveformBuffer = new float[WAVEFORM_SIZE];

    private final Handler waveformHandler = new Handler(Looper.getMainLooper());
    private final int WAVEFORM_REFRESH_MS = 16; // ~60 FPS
    private final Runnable waveformUpdater = new Runnable() {
        @Override
        public void run() {
            try {
                NativeSynth.getWaveform(waveformBuffer);
                if (oscilloscopeView != null) {
                    oscilloscopeView.updateWaveform(waveformBuffer);
                }
            } catch (Throwable t) {
                // ignore; keep UI safe
            }
            waveformHandler.postDelayed(this, WAVEFORM_REFRESH_MS);
        }
    };

    // NEW: recording
    private Button recordButton;
    private boolean isRecording = false;
    private File lastRecordingFile = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Start native audio engine
        NativeSynth.start();

        // Find views

        oscilloscopeView = findViewById(R.id.oscilloscopeView);
        recordButton = findViewById(R.id.btnRecord);
        recyclerView = findViewById(R.id.rv_waveform);

        findViewById(R.id.btnHistory).setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, RecordingHistoryActivity.class);
            startActivity(i);
        });

        // RecyclerView setup
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WaveAdapter(waves, this);
        recyclerView.setAdapter(adapter);

        // Add wave button
        findViewById(R.id.addWaveForm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNewWave();
            }
        });

        // NEW: Record button handler
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleRecording();
            }
        });

        // Start waveform polling
        waveformHandler.post(waveformUpdater);


    }

    private void addNewWave() {
        WaveModel model = new WaveModel();
        waves.add(model);
        adapter.notifyItemInserted(waves.size() - 1);
        recyclerView.smoothScrollToPosition(waves.size() - 1);
    }

    // NEW: create recording path in internal storage
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

    // NEW: toggling recording
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
            } else {
                Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Callbacks from WaveAdapter via OnWaveTypeSelected

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop waveform polling
        waveformHandler.removeCallbacks(waveformUpdater);

        // Stop any active recording
        if (isRecording) {
            NativeSynth.stopRecording();
            isRecording = false;
        }

        // Stop audio engine
        NativeSynth.stop();
    }


}
