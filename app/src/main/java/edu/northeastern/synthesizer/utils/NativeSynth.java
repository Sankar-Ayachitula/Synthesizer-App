package edu.northeastern.synthesizer.utils;

public class NativeSynth {

    static {
        System.loadLibrary("synthesizer");
    }

    public static native void start();
    public static native void stop();

    public static native int  addWave(int waveType, float freq, float amp);
    public static native void removeWave(int waveId);

    public static native void setWaveFrequency(int waveId, float freq);
    public static native void setWaveAmplitude(int waveId, float amp);

    // NEW: per-wave LFO
    public static native void setWaveLfoFrequency(int waveId, float freq);
    public static native void setWaveLfoDepth(int waveId, float depth);

    public static native void setWaveType(int waveId, int waveType);
}

