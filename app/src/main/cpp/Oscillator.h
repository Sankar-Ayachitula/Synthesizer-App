#pragma once
#include <math.h>

enum WaveType {
    WAVE_SINE   = 0,
    WAVE_SQUARE = 1,
    WAVE_TRI    = 2,
    WAVE_SAW    = 3
};

class Oscillator {
public:
    WaveType type;

    // Smoothed parameters
    float freq        = 440.0f;
    float targetFreq  = 440.0f;

    float amp         = 0.5f;
    float targetAmp   = 0.5f;

    float phase       = 0.0f;
    float sampleRate  = 48000.0f;

    // Smoothing factors
    float freqSmooth  = 0.001f;
    float ampSmooth   = 0.0015f;

    Oscillator(WaveType t, float f, float a, float sr)
            : type(t), freq(f), targetFreq(f),
              amp(a), targetAmp(a), sampleRate(sr) {}

    void setTargetFreq(float f)  { targetFreq = f; }
    void setTargetAmp(float a)   { targetAmp  = a; }
    void setSampleRate(float sr) { sampleRate = sr; }

    inline void updateParams() {
        // Exponential smoothing
        freq += freqSmooth * (targetFreq - freq);
        amp  += ampSmooth  * (targetAmp  - amp);
    }

    inline float nextSample() {
        updateParams();

        float phaseInc = (2.0f * M_PI * freq) / sampleRate;
        phase += phaseInc;
        if (phase >= 2.0f * M_PI) phase -= 2.0f * M_PI;

        float s = 0.0f;

        switch (type) {
            case WAVE_SINE:
                s = sinf(phase);
                break;

            case WAVE_SQUARE:
                s = (sinf(phase) >= 0.0f ? 1.0f : -1.0f);
                break;

            case WAVE_TRI:
                // Triangle via asin(sin)
                s = (2.0f / M_PI) * asinf(sinf(phase));
                break;

            case WAVE_SAW: {
                // Normalize phase to 0..1 then map to -1..1
                float p = phase / (2.0f * M_PI);   // 0..1
                if (p > 1.0f) p -= 1.0f;
                s = 2.0f * p - 1.0f;               // -1..1
                break;
            }
        }

        return s * amp;
    }
};
