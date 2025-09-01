#pragma once
#include <vector>
#include <cstdint>
#include <optional>
#include <unordered_map>

struct Track {
    int id;
    float cx, cy, w, h; // current bbox center + size
    float vx = 0.f, vy = 0.f; // velocity (for simple linear prediction)
    float score;
    int label;
    int age = 0;        // total frames
    int lost = 0;       // consecutive lost frames
};

struct AssocPair { int trackIdx; int detIdx; };

class ByteTrackLite {
public:
    // det_high: 高质量框阈值; det_low: 低质量扩展匹配阈值; max_lost: 最大丢失帧
    ByteTrackLite(float det_high=0.5f, float det_low=0.1f, int max_lost=30);
    // 输入：检测结果 (corner bbox)
    std::vector<Track> update(const std::vector<Detection>& dets);
    void reset();
private:
    int next_id_ = 1;
    float det_high_;
    float det_low_;
    int max_lost_;
    std::vector<Track> tracks_;

    float iou(const Track& t, const Detection& d) const;
    void linearPredict(Track& t) const; // 简化的恒速度预测
};
