package edu.northeastern.synthesizer.models;

public class WaveModel {

    public float[] previewWave = new float[300];  // static waveform


    public int waveId = -1;

    // Wave properties
    public int waveType;        // 0=sine, 1=square, 2=triangle, 3=saw
    public float frequency;     // Hz
    public float amplitude;     // 0..1

    // LFO settings
    public float lfoFrequency;  // Hz
    public float lfoDepth;      // 0..1

    // DEFAULT VALUES
    // waveType = sine
    // frequency = 440 Hz
    // amplitude = 1.0
    // lfoFreq = 0.5 Hz
    // lfoDepth = 1.0

    public WaveModel() {
        this.waveType = 0;     // sine
        this.frequency = 440f;
        this.amplitude = 0.8f;
        this.lfoFrequency = 0.5f;
        this.lfoDepth = 1.0f;
    }

    public WaveModel(int waveType, float frequency, float amplitude) {
        this.waveType = waveType;
        this.frequency = frequency;
        this.amplitude = amplitude;
        this.lfoFrequency = 0.5f;
        this.lfoDepth = 1.0f;
    }
}
