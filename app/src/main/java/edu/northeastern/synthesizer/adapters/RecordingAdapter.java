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

    // Track currently playing view holder so UI updates correctly
    private RecordingVH currentlyPlayingVH = null;

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
        holder.dateText.setText("Created: " + new java.util.Date(file.lastModified()));

        holder.playBtn.setText("Play");

        holder.playBtn.setOnClickListener(v -> {

            String filePath = file.getAbsolutePath();

            // CASE 1: CLICKING THE SAME ITEM THAT IS CURRENTLY PLAYING → STOP IT
            if (RecordingHistoryActivity.globalPlayer != null &&
                    filePath.equals(RecordingHistoryActivity.currentlyPlayingPath)) {

                RecordingHistoryActivity.globalPlayer.stop();
                RecordingHistoryActivity.globalPlayer.release();
                RecordingHistoryActivity.globalPlayer = null;
                RecordingHistoryActivity.currentlyPlayingPath = null;

                holder.playBtn.setText("Play");
                currentlyPlayingVH = null;
                return;
            }


            // CASE 2: STOP OLD PLAYBACK (IF ANY)
            if (RecordingHistoryActivity.globalPlayer != null) {
                RecordingHistoryActivity.globalPlayer.stop();
                RecordingHistoryActivity.globalPlayer.release();
                RecordingHistoryActivity.globalPlayer = null;
            }

            // Reset previous playing row button
            if (currentlyPlayingVH != null && currentlyPlayingVH != holder) {
                currentlyPlayingVH.playBtn.setText("Play");
            }

            try {
                MediaPlayer player = new MediaPlayer();
                player.setDataSource(filePath);
                player.prepare();
                player.start();

                RecordingHistoryActivity.globalPlayer = player;
                RecordingHistoryActivity.currentlyPlayingPath = filePath;
                holder.playBtn.setText("Stop");

                currentlyPlayingVH = holder;

                player.setOnCompletionListener(mp -> {
                    holder.playBtn.setText("Play");
                    RecordingHistoryActivity.globalPlayer = null;
                    RecordingHistoryActivity.currentlyPlayingPath = null;
                    currentlyPlayingVH = null;
                    mp.release();
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

    public static class RecordingVH extends RecyclerView.ViewHolder {
        TextView fileName, dateText;
        Button playBtn;

        public RecordingVH(@NonNull View itemView) {
            super(itemView);
            fileName = itemView.findViewById(R.id.recordingFileName);
            dateText  = itemView.findViewById(R.id.recordingDate);
            playBtn   = itemView.findViewById(R.id.btnPlayRecording);
        }
    }
}
