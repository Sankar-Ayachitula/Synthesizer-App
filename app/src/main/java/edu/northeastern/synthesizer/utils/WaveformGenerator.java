package edu.northeastern.synthesizer.utils;

public class WaveformGenerator {

    public static float[] generate(int waveType, float amplitude) {
        int size = 300;
        float[] buffer = new float[size];

        for (int i = 0; i < size; i++) {
            float t = (float) i / size;
            float phase = t * (float)(2 * Math.PI);

            float sample = 0;

            switch (waveType) {
                case 0: // Sine
                    sample = (float)Math.sin(phase);
                    break;

                case 1: // Square
                    sample = Math.sin(phase) >= 0 ? 1f : -1f;
                    break;

                case 2: // Triangle
                    sample = 2f * Math.abs(2f * (t - (float)Math.floor(t + 0.5))) - 1f;
                    break;

                case 3: // Sawtooth
                    sample = 2f * (t - (float)Math.floor(t + 0.5));
                    break;
            }

            buffer[i] = sample * amplitude;
        }

        return buffer;
    }
}
