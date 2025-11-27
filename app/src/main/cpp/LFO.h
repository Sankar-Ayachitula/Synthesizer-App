#pragma once
#include <math.h>

class LFO {
public:
    float freq = 0.5f;
    float targetFreq = 0.5f;

    float depth = 1.0f;
    float targetDepth = 1.0f;

    float phase = 0.0f;
    float sampleRate = 48000.0f;

    float freqSmooth = 0.001f;
    float depthSmooth = 0.002f;

    void setSampleRate(float sr) {
        sampleRate = sr;
    }

    void setTargetFreq(float f) {
        targetFreq = f;
    }

    void setTargetDepth(float d) {
        targetDepth = d;
    }

    inline void update() {
        freq  += freqSmooth  * (targetFreq  - freq);
        depth += depthSmooth * (targetDepth - depth);

        if (depth < 0) depth = 0;
        if (depth > 1) depth = 1;
    }

    inline float nextValue() {
        update();
        float phaseInc = (2.0f * M_PI * freq) / sampleRate;
        phase += phaseInc;
        if (phase >= 2.0f * M_PI) phase -= 2.0f * M_PI;

        float raw = sinf(phase);       // -1..1
        float uni = 0.5f + 0.5f * raw; // 0..1
        return uni * depth;            // 0..depth
    }
};
