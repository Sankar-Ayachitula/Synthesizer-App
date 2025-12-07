package edu.northeastern.synthesizer.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import edu.northeastern.synthesizer.R;
import edu.northeastern.synthesizer.adapters.RecordingAdapter;

public class RecordingHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView emptyText;

    public static MediaPlayer globalPlayer = null;     // SINGLE player for whole app
    public static String currentlyPlayingPath = null;  // Track active audio

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (globalPlayer != null) {
            globalPlayer.stop();
            globalPlayer.release();
            globalPlayer = null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording_history);

        recyclerView = findViewById(R.id.recordingRecyclerView);
        emptyText = findViewById(R.id.emptyText);

        File recordingDir = new File(getFilesDir(), "recordings");

        if (!recordingDir.exists() || recordingDir.listFiles() == null || recordingDir.listFiles().length == 0) {
            emptyText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            return;
        }

        File[] files = recordingDir.listFiles((dir, name) -> name.endsWith(".wav"));

        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new RecordingAdapter(files));
    }
}
