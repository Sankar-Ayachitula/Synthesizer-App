package edu.northeastern.synthesizer.adapters;

import edu.northeastern.synthesizer.models.WaveModel;

public interface OnWaveTypeSelected {
    void onWaveSelected(int position, WaveType type);   // wave type
    void onWaveFrequencyChanged(int position, WaveModel model);
    void onWaveLfoChanged(int position, WaveModel model);
}

