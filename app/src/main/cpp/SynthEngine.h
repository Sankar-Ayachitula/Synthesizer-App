#pragma once
#include <vector>
#include "Oscillator.h"

class SynthEngine {
public:
    std::vector<Oscillator*> waves;
    float sampleRate = 48000.0f;
    float mixGain = 0.25f;

    float sampleWave(int waveId) {
        if (waveId < 0 || waveId >= waves.size()) return 0.0f;

        Oscillator* osc = waves[waveId];
        if (!osc) return 0.0f;

        return osc->nextSample();
    }



    void setSampleRate(float sr) {
        sampleRate = sr;
        for (auto *osc : waves)
            if (osc) osc->setSampleRate(sr);
    }

    int addWave(WaveType type, float freq, float amp) {
        Oscillator* osc = new Oscillator(type, freq, amp, sampleRate);
        waves.push_back(osc);
        return waves.size() - 1;
    }

    void removeWave(int id) {
        if (id < 0 || id >= waves.size()) return;
        delete waves[id];
        waves[id] = nullptr;
    }

    void setWaveTargetFreq(int id, float f) {
        if (id < 0 || id >= waves.size()) return;
        if (waves[id]) waves[id]->setTargetFreq(f);
    }

    void setWaveTargetAmp(int id, float a) {
        if (id < 0 || id >= waves.size()) return;
        if (waves[id]) waves[id]->setTargetAmp(a);
    }

    // NEW: per-wave LFO settings
    void setWaveLfoFreq(int id, float f) {
        if (id < 0 || id >= waves.size()) return;
        if (waves[id]) waves[id]->setLfoTargetFreq(f);
    }

    void setWaveLfoDepth(int id, float d) {
        if (id < 0 || id >= waves.size()) return;
        if (waves[id]) waves[id]->setLfoTargetDepth(d);
    }

    float nextSample() {
        float sum = 0.0f;
        for (auto *w : waves)
            if (w) sum += w->nextSample();
        return sum * mixGain;
    }

    int peekNextId() const {
        return waves.size();
    }

    void setWaveType(int id, WaveType type) {
        if (id < 0 || id >= waves.size()) return;
        if (waves[id]) waves[id]->type = type;
    }


};
