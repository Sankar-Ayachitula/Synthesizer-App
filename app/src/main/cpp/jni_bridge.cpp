#include <jni.h>
#include <oboe/Oboe.h>

#include "SynthEngine.h"
#include "MessageQueue.h"


static SynthEngine      gEngine;
static MessageQueue     gMsgQueue;
static oboe::AudioStream *gStream = nullptr;

class MyCallback : public oboe::AudioStreamCallback {
public:
    oboe::DataCallbackResult onAudioReady(
            oboe::AudioStream *audioStream,
            void *audioData,
            int32_t numFrames) override {

        float *out = static_cast<float*>(audioData);


        Message msg;
        while (gMsgQueue.pop(msg)) {
            switch (msg.type) {
                case MSG_ADD_WAVE: {
                    WaveType type = static_cast<WaveType>(msg.id); // id = waveType
                    float freq = msg.v1;
                    float amp  = msg.v2;
                    gEngine.addWave(type, freq, amp);
                    break;
                }
                case MSG_REMOVE_WAVE: {
                    gEngine.removeWave(msg.id);
                    break;
                }
                case MSG_SET_WAVE_FREQ: {
                    gEngine.setWaveTargetFreq(msg.id, msg.v1);
                    break;
                }
                case MSG_SET_WAVE_AMP: {
                    gEngine.setWaveTargetAmp(msg.id, msg.v1);
                    break;
                }
                case MSG_SET_WAVE_LFO_FREQ: {
                    gEngine.setWaveLfoFreq(msg.id, msg.v1);
                    break;
                }
                case MSG_SET_WAVE_LFO_DEPTH: {
                    gEngine.setWaveLfoDepth(msg.id, msg.v1);
                    break;
                }
                case MSG_SET_WAVE_TYPE: {
                    gEngine.setWaveType(msg.id, static_cast<WaveType>(msg.v1));
                    break;
                }

            }
        }

        for (int i = 0; i < numFrames; i++) {
            float sample = gEngine.nextSample();
            out[i] = sample;
        }

        return oboe::DataCallbackResult::Continue;
    }
};

static MyCallback gCallback;


extern "C" {
    //Start Audio
JNIEXPORT void JNICALL
Java_edu_northeastern_synthesizer_utils_NativeSynth_start(JNIEnv*, jclass) {
    if (gStream != nullptr) return; // already started

    oboe::AudioStreamBuilder builder;

    builder.setFormat(oboe::AudioFormat::Float);
    builder.setChannelCount(oboe::ChannelCount::Mono);
    builder.setPerformanceMode(oboe::PerformanceMode::LowLatency);
    builder.setSharingMode(oboe::SharingMode::Exclusive);
    builder.setCallback(&gCallback);

    oboe::Result result = builder.openStream(&gStream);
    if (result == oboe::Result::OK && gStream) {
        int32_t sr = gStream->getSampleRate();
        if (sr <= 0) sr = 48000;

        gEngine.setSampleRate((float)sr);
        gStream->requestStart();
    }
}

// Stop audio
JNIEXPORT void JNICALL
Java_edu_northeastern_synthesizer_utils_NativeSynth_stop(JNIEnv*, jclass) {
    if (gStream) {
        gStream->requestStop();
        gStream->close();
        gStream = nullptr;
    }
}

// waveType: 0 = sine, 1 = square, 2 = triangle, 3 = saw
JNIEXPORT jint JNICALL
Java_edu_northeastern_synthesizer_utils_NativeSynth_addWave(
        JNIEnv*, jclass,
        jint waveType,
        jfloat freq,
        jfloat amp) {


    int id = gEngine.peekNextId();

    Message m;
    m.type = MSG_ADD_WAVE;
    m.id   = waveType; // type
    m.v1   = freq;
    m.v2   = amp;

    gMsgQueue.push(m);

    return id;
}


// Remove wave by ID
JNIEXPORT void JNICALL
Java_edu_northeastern_synthesizer_utils_NativeSynth_removeWave(
        JNIEnv*, jclass,
        jint waveId) {

    Message m;
    m.type = MSG_REMOVE_WAVE;
    m.id   = waveId;
    m.v1   = 0;
    m.v2   = 0;
    gMsgQueue.push(m);
}

// Set wave frequency
JNIEXPORT void JNICALL
Java_edu_northeastern_synthesizer_utils_NativeSynth_setWaveFrequency(
        JNIEnv*, jclass,
        jint waveId,
        jfloat freq) {

    Message m;
    m.type = MSG_SET_WAVE_FREQ;
    m.id   = waveId;
    m.v1   = freq;
    m.v2   = 0;
    gMsgQueue.push(m);
}

// Set wave amplitude
JNIEXPORT void JNICALL
Java_edu_northeastern_synthesizer_utils_NativeSynth_setWaveAmplitude(
        JNIEnv*, jclass,
        jint waveId,
        jfloat amp) {

    Message m;
    m.type = MSG_SET_WAVE_AMP;
    m.id   = waveId;
    m.v1   = amp;
    m.v2   = 0;
    gMsgQueue.push(m);
}


// Set per-wave LFO frequency (Hz)
JNIEXPORT void JNICALL
Java_edu_northeastern_synthesizer_utils_NativeSynth_setWaveLfoFrequency(
        JNIEnv*, jclass,
        jint waveId,
        jfloat freq) {

    Message m;
    m.type = MSG_SET_WAVE_LFO_FREQ;
    m.id   = waveId;
    m.v1   = freq;
    m.v2   = 0;
    gMsgQueue.push(m);
}

// Set per-wave LFO depth (0..1)
JNIEXPORT void JNICALL
Java_edu_northeastern_synthesizer_utils_NativeSynth_setWaveLfoDepth(
        JNIEnv*, jclass,
        jint waveId,
        jfloat depth) {

    Message m;
    m.type = MSG_SET_WAVE_LFO_DEPTH;
    m.id   = waveId;
    m.v1   = depth;
    m.v2   = 0;
    gMsgQueue.push(m);
}

JNIEXPORT void JNICALL
Java_edu_northeastern_synthesizer_utils_NativeSynth_setWaveType(
        JNIEnv*, jclass,
        jint waveId,
        jint waveType) {

    Message m;
    m.type = MSG_SET_WAVE_TYPE;
    m.id   = waveId;
    m.v1   = waveType;   // waveType = 0..3
    m.v2   = 0;

    gMsgQueue.push(m);
}


} // extern "C"
