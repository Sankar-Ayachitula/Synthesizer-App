//
// Created by Sankar Ayachitula on 11/6/25.
//

#include "jni_bridge.h"
#include <jni.h>
#include <string>

extern "C"
JNIEXPORT jstring JNICALL
Java_edu_northeastern_synthesizer_activities_MainActivity_startSynth(JNIEnv* env, jobject /* this */) {
    std::string msg = "Synth started successfully!";
    return env->NewStringUTF(msg.c_str());
}

