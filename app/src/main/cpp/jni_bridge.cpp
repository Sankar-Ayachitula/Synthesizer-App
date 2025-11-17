#include "jni_bridge.h"
#include <jni.h>
#include <string>
#include <oboe/Oboe.h>
#include "Oscillator.h"


static AudioOscillator audioOsc;
static LFOOscillator   lfo;
static oboe::AudioStream *stream = nullptr;


class MyCallback : public oboe::AudioStreamCallback {
public:
    oboe::DataCallbackResult onAudioReady(
            oboe::AudioStream *oboeStream,
            void *audioData,
            int32_t numFrames) override {

        float *out = static_cast<float*>(audioData);

        for (int i = 0; i < numFrames; i++) {
            float audioSample = audioOsc.nextSample();   // 440Hz audio
            float lfoValue    = lfo.nextValue();         // slow 0..1 wave
            out[i] = audioSample * lfoValue;             // LFO modulated
        }

        return oboe::DataCallbackResult::Continue;
    }
};

static MyCallback callback;

extern "C"
JNIEXPORT jstring JNICALL
Java_edu_northeastern_synthesizer_activities_MainActivity_startSynth(
        JNIEnv* env,
        jobject /* this */) {

    std::string msg = "Synth started successfully!";
    return env->NewStringUTF(msg.c_str());
}

extern "C"
JNIEXPORT void JNICALL
Java_edu_northeastern_synthesizer_activities_MainActivity_startLfoSynth(
        JNIEnv *env,
        jobject /* this */) {

    oboe::AudioStreamBuilder builder;

    builder.setFormat(oboe::AudioFormat::Float);
    builder.setChannelCount(oboe::ChannelCount::Mono);
    builder.setSampleRate(48000);
    builder.setPerformanceMode(oboe::PerformanceMode::LowLatency);
    builder.setSharingMode(oboe::SharingMode::Exclusive);
    builder.setCallback(&callback);

    builder.openStream(&stream);

    if (stream) {
        stream->requestStart();
    }
}

// --------------------------------------------------
// JNI STOP LFO SYNTH
// --------------------------------------------------
extern "C"
JNIEXPORT void JNICALL
Java_edu_northeastern_synthesizer_activities_MainActivity_stopLfoSynth(
        JNIEnv *env,
        jobject /* this */) {

    if (stream) {
        stream->requestStop();
        stream->close();
        stream = nullptr;
    }
}
