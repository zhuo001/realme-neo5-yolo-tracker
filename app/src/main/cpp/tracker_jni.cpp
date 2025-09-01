#include <jni.h>
#include <android/log.h>

// 转发到我们的 native 实现
extern "C" {
    JNIEXPORT jboolean JNICALL Java_com_example_tracker_NativeBridge_init(JNIEnv* env, jclass clazz, jstring jparam, jstring jbin, jint inputSize, jint numClasses);
    JNIEXPORT void JNICALL Java_com_example_tracker_NativeBridge_setImageSize(JNIEnv* env, jclass clazz, jint w, jint h);
    JNIEXPORT jint JNICALL Java_com_example_tracker_NativeBridge_inferRGBA(JNIEnv* env, jclass clazz, jobject buf, jint w, jint h, jbyteArray outFrame);
    JNIEXPORT jfloat JNICALL Java_com_example_tracker_NativeBridge_inferRGBAWithTiming(JNIEnv* env, jclass clazz, jobject buf, jint w, jint h, jbyteArray outFrame);
    JNIEXPORT void JNICALL Java_com_example_tracker_NativeBridge_updateThresholds(JNIEnv* env, jclass clazz, jfloat confThresh, jfloat nmsThresh);
    JNIEXPORT void JNICALL Java_com_example_tracker_NativeBridge_reset(JNIEnv* env, jclass clazz);
    JNIEXPORT void JNICALL Java_com_example_tracker_NativeBridge_release(JNIEnv* env, jclass clazz);
    JNIEXPORT jstring JNICALL Java_com_example_tracker_NativeBridge_version(JNIEnv* env, jclass clazz);
}

#define LOG_TAG "YoloTracker"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
