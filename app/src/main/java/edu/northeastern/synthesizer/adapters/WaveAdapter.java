package edu.northeastern.synthesizer.adapters;

import static edu.northeastern.synthesizer.adapters.WaveType.SAWTOOTH;
import static edu.northeastern.synthesizer.adapters.WaveType.SINE;
import static edu.northeastern.synthesizer.adapters.WaveType.SQUARE;
import static edu.northeastern.synthesizer.adapters.WaveType.TRIANGLE;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.slider.Slider;

import java.util.List;

import edu.northeastern.synthesizer.R;
import edu.northeastern.synthesizer.models.WaveModel;
import edu.northeastern.synthesizer.utils.NativeSynth;
import edu.northeastern.synthesizer.views.WaveformView;
import edu.northeastern.synthesizer.utils.WaveformGenerator;

public class WaveAdapter extends RecyclerView.Adapter<WaveAdapter.WaveViewHolder> {

    private final List<WaveModel> waves;
    private final OnWaveTypeSelected listener;

    public WaveAdapter(List<WaveModel> waves, OnWaveTypeSelected listener) {
        this.waves = waves;
        this.listener = listener;
    }

    @NonNull
    @Override
    public WaveViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.rv_wave_item, parent, false);
        return new WaveViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WaveViewHolder holder, int position) {
        holder.bind(waves.get(position), position);
    }

    @Override
    public int getItemCount() {
        return waves.size();
    }

    class WaveViewHolder extends RecyclerView.ViewHolder {

        WaveformView waveformView;
        TextView waveIdTv;

        Button btnSine, btnTriangle, btnSquare, btnSaw;
        Button[] buttons;
        Slider freqSlider, lfoSlider, volumeSlider;

        ImageButton deleteButton;
        Context context;

        public WaveViewHolder(@NonNull View itemView) {
            super(itemView);
            context = itemView.getContext();

            btnSine = itemView.findViewById(R.id.btnSine);
            btnTriangle = itemView.findViewById(R.id.btnTriangle);
            btnSquare = itemView.findViewById(R.id.btnSquare);
            btnSaw = itemView.findViewById(R.id.btnSawtooth);

            freqSlider = itemView.findViewById(R.id.freq_slider);
            lfoSlider = itemView.findViewById(R.id.lfo_slider);
            volumeSlider = itemView.findViewById(R.id.volume_slider);

            waveformView = itemView.findViewById(R.id.waveformView);
            waveIdTv = itemView.findViewById(R.id.waveId_tv);

            deleteButton = itemView.findViewById(R.id.deleteButton);

            buttons = new Button[]{btnSine, btnTriangle, btnSquare, btnSaw};
        }

        public void bind(WaveModel model, int position) {


            freqSlider.setValue(model.frequency);
            lfoSlider.setValue(model.lfoFrequency);
            volumeSlider.setValue(model.amplitude);

            highlightWaveType(WaveType.values()[model.waveType]);

            waveIdTv.setText("Wave Id: " + (model.waveId + 1));

            if (model.previewWave == null) {
                model.previewWave = WaveformGenerator.generate(model.waveType, model.amplitude);
            }
            waveformView.updateWave(model.previewWave);

            btnSine.setOnClickListener(v -> {
                updateType(model, WaveType.SINE, position);
            });

            btnTriangle.setOnClickListener(v -> {
                updateType(model, WaveType.TRIANGLE, position);
            });

            btnSquare.setOnClickListener(v -> {
                updateType(model, WaveType.SQUARE, position);
            });

            btnSaw.setOnClickListener(v -> {
                updateType(model, WaveType.SAWTOOTH, position);
            });


            freqSlider.addOnChangeListener((slider, value, fromUser) -> {
                model.frequency = value;
                listener.onWaveFrequencyChanged(position, model);
            });

            lfoSlider.addOnChangeListener((slider, value, fromUser) -> {
                model.lfoFrequency = value;
                listener.onWaveLfoChanged(position, model);
            });


            volumeSlider.addOnChangeListener((slider, value, fromUser) -> {
                model.amplitude = value;
                NativeSynth.setWaveAmplitude(model.waveId, value);

                model.previewWave = WaveformGenerator.generate(model.waveType, model.amplitude);
                waveformView.updateWave(model.previewWave);
            });

            deleteButton.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;

                WaveModel m = waves.get(pos);
                NativeSynth.removeWave(m.waveId);

                waves.remove(pos);
                notifyItemRemoved(pos);
                notifyItemRangeChanged(pos, waves.size());
            });
        }

        private void updateType(WaveModel model, WaveType type, int position) {
            model.waveType = type.ordinal();
            highlightWaveType(type);
            listener.onWaveSelected(position, type);

            model.previewWave = WaveformGenerator.generate(model.waveType, model.amplitude);
            waveformView.updateWave(model.previewWave);
        }

        private void highlightWaveType(WaveType type) {
            for (Button btn : buttons) {
                btn.setSelected(false);
                btn.setTextColor(context.getColor(R.color.orange));
            }

            switch (type) {
                case SINE: select(btnSine); break;
                case TRIANGLE: select(btnTriangle); break;
                case SQUARE: select(btnSquare); break;
                case SAWTOOTH: select(btnSaw); break;
            }
        }

        private void select(Button btn) {
            btn.setSelected(true);
            btn.setTextColor(Color.WHITE);
        }
    }
}
