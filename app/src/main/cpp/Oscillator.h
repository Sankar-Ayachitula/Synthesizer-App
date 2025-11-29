#pragma once
#include <math.h>
#include "LFO.h"

enum WaveType {
    WAVE_SINE = 0,
    WAVE_SQUARE = 1,
    WAVE_TRI = 2,
    WAVE_SAW = 3
};

class Oscillator {
public:
    WaveType type;

    float freq = 440.0f;
    float targetFreq = 440.0f;

    float amp = 1.0f;
    float targetAmp = 1.0f;

    float phase = 0.0f;
    float sampleRate = 48000.0f;

    float freqSmooth = 0.001f;
    float ampSmooth = 0.002f;


    LFO lfo;

    Oscillator(WaveType t, float f, float a, float sr)
            : type(t), freq(f), targetFreq(f),
              amp(a), targetAmp(a), sampleRate(sr)
    {
        lfo.setSampleRate(sr);
    }

    void setTargetFreq(float f) { targetFreq = f; }
    void setTargetAmp(float a)  { targetAmp = a; }

    // NEW: per-wave LFO controls
    void setLfoTargetFreq(float f) { lfo.setTargetFreq(f); }
    void setLfoTargetDepth(float d) { lfo.setTargetDepth(d); }

    void setSampleRate(float sr) {
        sampleRate = sr;
        lfo.setSampleRate(sr);
    }

    inline void update() {
        freq += freqSmooth * (targetFreq - freq);
        amp  += ampSmooth  * (targetAmp  - amp);
    }

    inline float nextSample() {
        update();

        float phaseInc = (2.0f * M_PI * freq) / sampleRate;
        phase += phaseInc;
        if (phase >= 2.0f * M_PI) phase -= 2.0f * M_PI;

        float base = 0.0f;
        switch (type) {
            case WAVE_SINE:   base = sinf(phase); break;
            case WAVE_SQUARE: base = sinf(phase) >= 0 ? 1.0f : -1.0f; break;
            case WAVE_TRI:    base = (2.0f / M_PI) * asinf(sinf(phase)); break;
            case WAVE_SAW: {
                float p = phase / (2.0f * M_PI);
                if (p > 1) p -= 1;
                base = 2.0f * p - 1.0f;
                break;
            }
        }

        float mod = lfo.nextValue();
        float finalAmp = amp * mod;

        return base * finalAmp;
    }
};
