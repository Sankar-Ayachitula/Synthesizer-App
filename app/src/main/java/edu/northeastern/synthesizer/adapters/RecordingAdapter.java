package edu.northeastern.synthesizer.adapters;

import android.media.MediaPlayer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;

import edu.northeastern.synthesizer.R;
import edu.northeastern.synthesizer.activities.RecordingHistoryActivity;

public class RecordingAdapter extends RecyclerView.Adapter<RecordingAdapter.RecordingVH> {

    private final File[] recordings;

    public RecordingAdapter(File[] recordings) {
        this.recordings = recordings;
    }

    @NonNull
    @Override
    public RecordingVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recording_item, parent, false);
        return new RecordingVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecordingVH holder, int position) {
        File file = recordings[position];
        holder.fileName.setText(file.getName());
        holder.dateText.setText("Created: " + new java.util.Date(file.lastModified()).toString());

        holder.playBtn.setText("Play");

        holder.playBtn.setOnClickListener(v -> {

            // STOP currently active playback
            if (RecordingHistoryActivity.globalPlayer != null) {
                RecordingHistoryActivity.globalPlayer.stop();
                RecordingHistoryActivity.globalPlayer.release();
                RecordingHistoryActivity.globalPlayer = null;
                RecordingHistoryActivity.currentlyPlayingPath = null;
            }

            // PLAY selected file
            try {
                MediaPlayer player = new MediaPlayer();
                player.setDataSource(file.getAbsolutePath());
                player.prepare();
                player.start();

                RecordingHistoryActivity.globalPlayer = player;
                RecordingHistoryActivity.currentlyPlayingPath = file.getAbsolutePath();

                holder.playBtn.setText("Stop");

                // When finished, reset UI
                player.setOnCompletionListener(mp -> {
                    holder.playBtn.setText("Play");
                    mp.release();
                    RecordingHistoryActivity.globalPlayer = null;
                    RecordingHistoryActivity.currentlyPlayingPath = null;
                });

                // If pressed again → Stop
                holder.playBtn.setOnClickListener(x -> {
                    if (RecordingHistoryActivity.globalPlayer != null &&
                            file.getAbsolutePath().equals(RecordingHistoryActivity.currentlyPlayingPath)) {

                        RecordingHistoryActivity.globalPlayer.stop();
                        RecordingHistoryActivity.globalPlayer.release();
                        RecordingHistoryActivity.globalPlayer = null;
                        RecordingHistoryActivity.currentlyPlayingPath = null;

                        holder.playBtn.setText("Play");
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public int getItemCount() {
        return recordings.length;
    }

    static class RecordingVH extends RecyclerView.ViewHolder {
        TextView fileName, dateText;
        Button playBtn;

        RecordingVH(@NonNull View itemView) {
            super(itemView);
            fileName = itemView.findViewById(R.id.recordingFileName);
            dateText = itemView.findViewById(R.id.recordingDate);
            playBtn = itemView.findViewById(R.id.btnPlayRecording);
        }
    }
}
