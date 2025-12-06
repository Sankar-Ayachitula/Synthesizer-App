#include <jni.h>
#include <oboe/Oboe.h>
#include <atomic>
#include <array>
#include <vector>
#include <cstdint>
#include <cstdio>
#include <cstring>

#include "SynthEngine.h"
#include "MessageQueue.h"

// ------------------------------------------------------
// Global engine state
// ------------------------------------------------------
static SynthEngine      gEngine;
static MessageQueue     gMsgQueue;
static oboe::AudioStream *gStream = nullptr;

// ------------------------------------------------------
// Waveform buffer for oscilloscope
// ------------------------------------------------------
static constexpr int WAVEFORM_SIZE = 512;
static std::array<float, WAVEFORM_SIZE> gWaveformBuffer{};
static std::atomic<int> gWaveformWriteIndex{0};

// ------------------------------------------------------
// Recording state
// ------------------------------------------------------
static std::atomic<bool> gIsRecording{false};
static FILE* gRecordFile = nullptr;
static std::atomic<uint64_t> gRecordedSamples{0};
static int32_t gSampleRateGlobal = 48000;

// ------------------------------------------------------
// WAV writing helpers
// ------------------------------------------------------
static void writeWavHeaderPlaceholder(FILE* f, int sampleRate, int channels, int bitsPerSample) {
    uint32_t byteRate = sampleRate * channels * bitsPerSample / 8;
    uint16_t blockAlign = channels * bitsPerSample / 8;

    fwrite("RIFF", 1, 4, f);
    uint32_t chunkSize = 0;
    fwrite(&chunkSize, 4, 1, f);
    fwrite("WAVE", 1, 4, f);

    fwrite("fmt ", 1, 4, f);
    uint32_t subchunk1Size = 16;
    fwrite(&subchunk1Size, 4, 1, f);
    uint16_t audioFormat = 1;
    fwrite(&audioFormat, 2, 1, f);
    uint16_t numChannels = channels;
    fwrite(&numChannels, 2, 1, f);
    fwrite(&sampleRate, 4, 1, f);
    fwrite(&byteRate, 4, 1, f);
    fwrite(&blockAlign, 2, 1, f);
    fwrite(&bitsPerSample, 2, 1, f);

    fwrite("data", 1, 4, f);
    uint32_t dataSize = 0;
    fwrite(&dataSize, 4, 1, f);
}

static void finalizeWavHeader(FILE* f, uint64_t totalSamples, int channels, int bitsPerSample) {
    if (!f) return;

    uint32_t subchunk2Size = (uint32_t)(totalSamples * channels * (bitsPerSample / 8));
    uint32_t chunkSize = 36 + subchunk2Size;

    fseek(f, 4, SEEK_SET);
    fwrite(&chunkSize, 4, 1, f);

    fseek(f, 40, SEEK_SET);
    fwrite(&subchunk2Size, 4, 1, f);

    fseek(f, 0, SEEK_END);
}

// ------------------------------------------------------
// Audio Callback
// ------------------------------------------------------
class MyCallback : public oboe::AudioStreamCallback {
public:
    oboe::DataCallbackResult onAudioReady(
            oboe::AudioStream *audioStream,
            void *audioData,
            int32_t numFrames) override {

        float *out = static_cast<float*>(audioData);

        // 1) Apply UI messages
        Message msg;
        while (gMsgQueue.pop(msg)) {
            switch (msg.type) {
                case MSG_ADD_WAVE: {
                    WaveType type = static_cast<WaveType>(msg.id);
                    gEngine.addWave(type, msg.v1, msg.v2);
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

        // 2) Generate & process samples
        for (int i = 0; i < numFrames; i++) {
            float sample = gEngine.nextSample();

            // ---- Oscilloscope buffer ----
            int writeIndex = gWaveformWriteIndex.fetch_add(1, std::memory_order_relaxed);
            gWaveformBuffer[writeIndex % WAVEFORM_SIZE] = sample;

            // ---- Recording (PCM16) ----
            if (gIsRecording.load(std::memory_order_relaxed) && gRecordFile) {
                float s = sample;
                if (s > 1.0f) s = 1.0f;
                if (s < -1.0f) s = -1.0f;
                int16_t pcm = static_cast<int16_t>(s * 32767.0f);

                fwrite(&pcm, sizeof(int16_t), 1, gRecordFile);
                gRecordedSamples.fetch_add(1, std::memory_order_relaxed);
            }

            out[i] = sample;
        }

        return oboe::DataCallbackResult::Continue;
    }
};

static MyCallback gCallback;

// ------------------------------------------------------
// JNI
// ------------------------------------------------------
extern "C" {

// Start audio
JNIEXPORT void JNICALL
Java_edu_northeastern_synthesizer_utils_NativeSynth_start(JNIEnv*, jclass) {
    if (gStream != nullptr) return;

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
        gSampleRateGlobal = sr;   // store sample rate for recorder
        gStream->requestStart();
    }
}

// Stop audio
JNIEXPORT void JNICALL
Java_edu_northeastern_synthesizer_utils_NativeSynth_stop(JNIEnv*, jclass) {

    // Stop recording if active
    if (gIsRecording.load()) {
        gIsRecording.store(false);
        if (gRecordFile) {
            finalizeWavHeader(gRecordFile, gRecordedSamples.load(), 1, 16);
            fclose(gRecordFile);
            gRecordFile = nullptr;
        }
    }

    if (gStream) {
        gStream->requestStop();
        gStream->close();
        gStream = nullptr;
    }
}

// ------------------------------------------------------
// Wave Management
// ------------------------------------------------------
JNIEXPORT jint JNICALL
Java_edu_northeastern_synthesizer_utils_NativeSynth_addWave(
        JNIEnv*, jclass,
        jint waveType,
        jfloat freq,
        jfloat amp) {

    int id = gEngine.peekNextId();

    Message m;
    m.type = MSG_ADD_WAVE;
    m.id   = waveType;
    m.v1   = freq;
    m.v2   = amp;

    gMsgQueue.push(m);
    return id;
}

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
    m.v1   = waveType;
    m.v2   = 0;
    gMsgQueue.push(m);
}

// ------------------------------------------------------
// NEW: Provide the oscilloscope waveform to Java
// ------------------------------------------------------
JNIEXPORT void JNICALL
Java_edu_northeastern_synthesizer_utils_NativeSynth_getWaveform(
        JNIEnv* env,
        jclass,
        jfloatArray jBuffer) {

    if (!jBuffer) return;

    jsize len = env->GetArrayLength(jBuffer);
    if (len <= 0) return;

    int size = len;
    if (size > WAVEFORM_SIZE) size = WAVEFORM_SIZE;

    std::vector<float> temp(size);

    int writeIndex = gWaveformWriteIndex.load(std::memory_order_acquire);

    for (int i = 0; i < size; i++) {
        int idx = writeIndex - size + i;
        while (idx < 0) idx += WAVEFORM_SIZE;
        temp[i] = gWaveformBuffer[idx % WAVEFORM_SIZE];
    }

    env->SetFloatArrayRegion(jBuffer, 0, size, temp.data());
}

// ------------------------------------------------------
// NEW: Start recording to WAV
// ------------------------------------------------------
JNIEXPORT void JNICALL
Java_edu_northeastern_synthesizer_utils_NativeSynth_startRecording(
        JNIEnv* env,
        jclass,
        jstring jPath) {

    if (!jPath) return;

    // Stop previous recording if any
    if (gIsRecording.load()) {
        gIsRecording.store(false);
        if (gRecordFile) {
            finalizeWavHeader(gRecordFile, gRecordedSamples.load(), 1, 16);
            fclose(gRecordFile);
            gRecordFile = nullptr;
        }
    }

    const char* path = env->GetStringUTFChars(jPath, nullptr);
    gRecordFile = fopen(path, "wb");
    env->ReleaseStringUTFChars(jPath, path);

    if (!gRecordFile) {
        gIsRecording.store(false);
        return;
    }

    gRecordedSamples.store(0);
    writeWavHeaderPlaceholder(gRecordFile, gSampleRateGlobal, 1, 16);

    gIsRecording.store(true);
}

// ------------------------------------------------------
// NEW: Stop recording
// ------------------------------------------------------
JNIEXPORT void JNICALL
Java_edu_northeastern_synthesizer_utils_NativeSynth_stopRecording(
        JNIEnv*, jclass) {

    if (!gIsRecording.load()) return;

    gIsRecording.store(false);

    if (gRecordFile) {
        finalizeWavHeader(gRecordFile, gRecordedSamples.load(), 1, 16);
        fclose(gRecordFile);
        gRecordFile = nullptr;
    }
}

} // extern "C"
