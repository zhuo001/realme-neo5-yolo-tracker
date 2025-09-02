#pragma once
#include <string>
#include <vector>
#include <cstdint>
#include <functional>

// 若未集成 ncnn，可不定义 USE_NCNN，代码会退化为空实现
#ifdef USE_NCNN
#include <net.h>
#endif

struct Detection {
    float x1, y1, x2, y2; // corner format
    float score;
    int   label;
};

struct YoloConfig {
    int inputSize = 640;          // 正方形输入，letterbox
    int numClasses = 1;           // 类别数
    std::vector<int> strides {8,16,32}; // anchor-free strides
    float confThreshold = 0.25f;
    float nmsThreshold  = 0.45f;
    bool useFP16 = true;          // ncnn fp16 (需 runtime 支持)
    std::string outName = "output"; // 导出后 blob 名称（需根据实际 param 调整）
};

class YoloDetector {
public:
    bool load(const std::string& paramPath, const std::string& binPath, const YoloConfig& cfg);
    // 输入：RGBA8888 图像指针
    std::vector<Detection> infer(const uint8_t* rgba, int width, int height);
    // 带计时的推理，返回耗时毫秒数
    std::vector<Detection> inferWithTiming(const uint8_t* rgba, int width, int height, float& inferTimeMs);
    // 运行时更新阈值
    void updateThresholds(float confThresh, float nmsThresh);
    void release();
private:
    YoloConfig cfg_;
#ifdef USE_NCNN
    ncnn::Net net_;
#endif
    // 预计算网格 - 对于 anchor-free 解码
    struct GridCell { int sx; int sy; int stride; };
    std::vector<GridCell> gridCache_;
    int lastGridInput_ = -1;
    void buildGrid();
    void letterbox(const uint8_t* rgba, int w, int h, std::vector<uint8_t>& out, float& scale, int& padX, int& padY);
    std::vector<Detection> decode(const float* feats, int featLen, float scale, int padX, int padY, int srcW, int srcH);
};
