#include <jni.h>
#include <string>
#include <mutex>
#include "detector.h"
#include "bytetrack.h"
#include "usb_protocol.h"

static std::mutex gMutex;
static YoloDetector gDetector;
static ByteTrackLite gTracker(0.5f, 0.1f, 30);
static bool gInited = false;
static int gImgW = 0, gImgH = 0;
static uint16_t gFrameId = 0;

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_tracker_NativeBridge_init(JNIEnv* env, jclass, jstring jparam, jstring jbin, jint inputSize, jint numClasses){
    std::lock_guard<std::mutex> lk(gMutex);
    const char* p = env->GetStringUTFChars(jparam, nullptr);
    const char* b = env->GetStringUTFChars(jbin, nullptr);
    YoloConfig cfg; cfg.inputSize = inputSize; cfg.numClasses = numClasses; cfg.outName = "output"; // 调整为真实 blob 名
    bool ok = gDetector.load(p, b, cfg);
    env->ReleaseStringUTFChars(jparam, p);
    env->ReleaseStringUTFChars(jbin, b);
    gInited = ok;
    return ok ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_tracker_NativeBridge_setImageSize(JNIEnv*, jclass, jint w, jint h){
    std::lock_guard<std::mutex> lk(gMutex);
    gImgW = w; gImgH = h;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_example_tracker_NativeBridge_inferRGBA(JNIEnv* env, jclass, jobject buf, jint w, jint h, jbyteArray outFrame){
    std::lock_guard<std::mutex> lk(gMutex);
    if(!gInited) return -1;
    uint8_t* data = (uint8_t*)env->GetDirectBufferAddress(buf);
    if(!data) return -2;
    auto dets = gDetector.infer(data, w, h);
    auto tracks = gTracker.update(dets);
    auto frameBytes = buildTrackFrame(tracks, gFrameId++, w, h, 16);
    jsize outLen = env->GetArrayLength(outFrame);
    if(outLen < (jsize)frameBytes.size()) return -3;
    env->SetByteArrayRegion(outFrame, 0, frameBytes.size(), (const jbyte*)frameBytes.data());
    return (jint)frameBytes.size();
}

extern "C" JNIEXPORT jfloat JNICALL
Java_com_example_tracker_NativeBridge_inferRGBAWithTiming(JNIEnv* env, jclass, jobject buf, jint w, jint h, jbyteArray outFrame){
    std::lock_guard<std::mutex> lk(gMutex);
    if(!gInited) return -1.0f;
    uint8_t* data = (uint8_t*)env->GetDirectBufferAddress(buf);
    if(!data) return -2.0f;
    float inferTimeMs = 0.0f;
    auto dets = gDetector.inferWithTiming(data, w, h, inferTimeMs);
    auto tracks = gTracker.update(dets);
    auto frameBytes = buildTrackFrame(tracks, gFrameId++, w, h, 16);
    jsize outLen = env->GetArrayLength(outFrame);
    if(outLen < (jsize)frameBytes.size()) return -3.0f;
    env->SetByteArrayRegion(outFrame, 0, frameBytes.size(), (const jbyte*)frameBytes.data());
    return inferTimeMs;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_tracker_NativeBridge_updateThresholds(JNIEnv*, jclass, jfloat confThresh, jfloat nmsThresh){
    std::lock_guard<std::mutex> lk(gMutex);
    if(gInited) gDetector.updateThresholds(confThresh, nmsThresh);
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_tracker_NativeBridge_reset(JNIEnv*, jclass){
    std::lock_guard<std::mutex> lk(gMutex);
    gTracker.reset(); gFrameId = 0;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_tracker_NativeBridge_release(JNIEnv*, jclass){
    std::lock_guard<std::mutex> lk(gMutex);
    gDetector.release(); gInited = false; gTracker.reset();
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_tracker_NativeBridge_version(JNIEnv* env, jclass){
    return env->NewStringUTF("tracker-native-0.1.0");
}
