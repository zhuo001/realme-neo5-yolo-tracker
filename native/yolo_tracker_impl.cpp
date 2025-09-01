#include <jni.h>
#include <android/log.h>
#include <string>

#define LOG_TAG "YoloTrackerNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// 函数实现
extern "C" {

JNIEXPORT jboolean JNICALL Java_com_example_tracker_NativeBridge_init(JNIEnv* env, jclass clazz, jstring jparam, jstring jbin, jint inputSize, jint numClasses) {
    LOGI("Native init called - stub implementation");
    // 临时返回 true，实际实现中需要加载 YOLO 模型
    return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_com_example_tracker_NativeBridge_setImageSize(JNIEnv* env, jclass clazz, jint w, jint h) {
    LOGI("Native setImageSize called: %dx%d", w, h);
}

JNIEXPORT jint JNICALL Java_com_example_tracker_NativeBridge_inferRGBA(JNIEnv* env, jclass clazz, jobject buf, jint w, jint h, jbyteArray outFrame) {
    LOGI("Native inferRGBA called - stub implementation");
    // 临时返回 0，实际实现中需要进行 YOLO 推理
    return 0;
}

JNIEXPORT jfloat JNICALL Java_com_example_tracker_NativeBridge_inferRGBAWithTiming(JNIEnv* env, jclass clazz, jobject buf, jint w, jint h, jbyteArray outFrame) {
    LOGI("Native inferRGBAWithTiming called - stub implementation");
    // 临时返回 0.0，实际实现中需要进行 YOLO 推理并计时
    return 0.0f;
}

JNIEXPORT void JNICALL Java_com_example_tracker_NativeBridge_updateThresholds(JNIEnv* env, jclass clazz, jfloat confThresh, jfloat nmsThresh) {
    LOGI("Native updateThresholds called: conf=%.2f, nms=%.2f", confThresh, nmsThresh);
}

JNIEXPORT void JNICALL Java_com_example_tracker_NativeBridge_reset(JNIEnv* env, jclass clazz) {
    LOGI("Native reset called");
}

JNIEXPORT void JNICALL Java_com_example_tracker_NativeBridge_release(JNIEnv* env, jclass clazz) {
    LOGI("Native release called");
}

JNIEXPORT jstring JNICALL Java_com_example_tracker_NativeBridge_version(JNIEnv* env, jclass clazz) {
    return env->NewStringUTF("YOLO Tracker v1.0 (stub)");
}

}