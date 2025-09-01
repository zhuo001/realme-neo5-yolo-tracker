#include "detector.h"
#include <algorithm>

// 这是一个占位实现：尚未集成 ncnn，仅返回空结果。

bool YoloDetector::load(const std::string& paramPath, const std::string& binPath, int inputSize) {
    inputSize_ = inputSize;
    // TODO: 集成 ncnn::Net load_param / load_model
    (void)paramPath; (void)binPath;
    return true;
}

std::vector<Detection> YoloDetector::infer(const uint8_t* rgba, int width, int height) {
    (void)rgba; (void)width; (void)height;
    // TODO: 预处理 + net forward + 解码 + NMS
    return {};
}

void YoloDetector::release() {
    // TODO: net_.clear();
}
