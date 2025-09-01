#include "bytetrack.h"
#include "detector.h"
#include <algorithm>
#include <cmath>

ByteTrackLite::ByteTrackLite(float det_high, float det_low, int max_lost)
: det_high_(det_high), det_low_(det_low), max_lost_(max_lost) {}

float ByteTrackLite::iou(const Track& t, const Detection& d) const {
    float x1 = std::max(t.cx - t.w*0.5f, d.x1);
    float y1 = std::max(t.cy - t.h*0.5f, d.y1);
    float x2 = std::min(t.cx + t.w*0.5f, d.x2);
    float y2 = std::min(t.cy + t.h*0.5f, d.y2);
    float iw = std::max(0.f, x2 - x1);
    float ih = std::max(0.f, y2 - y1);
    float inter = iw * ih;
    float area_t = t.w * t.h;
    float area_d = (d.x2 - d.x1) * (d.y2 - d.y1);
    float uni = area_t + area_d - inter + 1e-6f;
    return inter / uni;
}

void ByteTrackLite::linearPredict(Track& t) const {
    t.cx += t.vx;
    t.cy += t.vy;
    // 简易约束
    if(!std::isfinite(t.cx) || !std::isfinite(t.cy)) return;
}

std::vector<Track> ByteTrackLite::update(const std::vector<Detection>& dets) {
    // 1. 预测现有轨迹位置
    for (auto& tr : tracks_) linearPredict(tr);

    // 2. 分离高质量和低质量检测
    std::vector<int> highIdx, lowIdx;
    for (int i=0;i<(int)dets.size();++i) {
        if (dets[i].score >= det_high_) highIdx.push_back(i);
        else if (dets[i].score >= det_low_) lowIdx.push_back(i);
    }

    // 3. 关联函数（贪心基于 IoU）
    auto associate = [&](const std::vector<int>& detIndices){
        std::vector<int> detAssigned(detIndices.size(), -1);
        std::vector<int> trackAssigned(tracks_.size(), -1);

        struct Cand { int t; int d; float iou; };
        std::vector<Cand> cands;
        for (int ti=0; ti<(int)tracks_.size(); ++ti) {
            for (int di=0; di<(int)detIndices.size(); ++di) {
                float val = iou(tracks_[ti], dets[detIndices[di]]);
                if (val > 0.1f) cands.push_back({ti, di, val});
            }
        }
        std::sort(cands.begin(), cands.end(), [](const Cand&a, const Cand&b){return a.iou>b.iou;});
        for (auto &c : cands) {
            if (trackAssigned[c.t] == -1 && detAssigned[c.d] == -1) {
                trackAssigned[c.t] = detIndices[c.d];
                detAssigned[c.d] = c.t;
            }
        }
        return trackAssigned; // size = tracks_.size(); value = det global idx or -1
    };

    // 3a. 高质量关联
    auto highAssign = associate(highIdx);

    // 3b. 对未匹配轨迹，再用低质量尝试
    std::vector<int> remainingTracks;
    for (int ti=0; ti<(int)tracks_.size(); ++ti) if (highAssign[ti] == -1) remainingTracks.push_back(ti);
    std::vector<int> lowAssign(tracks_.size(), -1);
    if (!remainingTracks.empty() && !lowIdx.empty()) {
        // 局部临时复制轨迹子集再匹配（简单复用同逻辑）
        // 复用 associate 需要所有轨迹，因此我们手动匹配
        struct Cand { int t; int d; float iou; };
        std::vector<Cand> cands;
        for (int ti : remainingTracks) {
            for (int di=0; di<(int)lowIdx.size(); ++di) {
                float val = iou(tracks_[ti], dets[lowIdx[di]]);
                if (val > 0.1f) cands.push_back({ti, di, val});
            }
        }
        std::sort(cands.begin(), cands.end(), [](const Cand&a,const Cand&b){return a.iou>b.iou;});
        std::vector<int> detUsed(lowIdx.size(), -1);
        for (auto &c : cands) {
            if (lowAssign[c.t] == -1 && detUsed[c.d] == -1) {
                lowAssign[c.t] = lowIdx[c.d];
                detUsed[c.d] = c.t;
            }
        }
    }

    // 4. 更新轨迹（位置、速度、评分）
    for (int ti=0; ti<(int)tracks_.size(); ++ti) {
        int detIndex = -1;
        if (highAssign[ti] != -1) detIndex = highAssign[ti];
        else if (lowAssign[ti] != -1) detIndex = lowAssign[ti];
        if (detIndex != -1) {
            const auto &d = dets[detIndex];
            float newCx = 0.5f*(d.x1 + d.x2);
            float newCy = 0.5f*(d.y1 + d.y2);
            float newW  = d.x2 - d.x1;
            float newH  = d.y2 - d.y1;
            tracks_[ti].vx = newCx - tracks_[ti].cx;
            tracks_[ti].vy = newCy - tracks_[ti].cy;
            tracks_[ti].cx = newCx;
            tracks_[ti].cy = newCy;
            tracks_[ti].w = newW;
            tracks_[ti].h = newH;
            tracks_[ti].score = d.score;
            tracks_[ti].label = d.label;
            tracks_[ti].lost = 0;
        } else {
            tracks_[ti].lost++;
        }
        tracks_[ti].age++;
    }

    // 5. 创建新轨迹（来自高质量集合中未匹配的）
    std::vector<int> used(highIdx.size(), 0);
    for (int ti=0; ti<(int)tracks_.size(); ++ti) {
        if (highAssign[ti] != -1) {
            // 标记哪个高质量被用了
            for (int hi=0; hi<(int)highIdx.size(); ++hi) if (highIdx[hi]==highAssign[ti]) used[hi]=1;
        }
    }
    for (int hi=0; hi<(int)highIdx.size(); ++hi) if (!used[hi]) {
        const auto &d = dets[highIdx[hi]];
        Track t;
        t.id = next_id_++;
        t.cx = 0.5f*(d.x1 + d.x2);
        t.cy = 0.5f*(d.y1 + d.y2);
        t.w = d.x2 - d.x1;
        t.h = d.y2 - d.y1;
        t.score = d.score;
        t.label = d.label;
        tracks_.push_back(t);
    }

    // 6. 移除长时间丢失
    tracks_.erase(std::remove_if(tracks_.begin(), tracks_.end(), [&](const Track& t){ return t.lost > max_lost_; }), tracks_.end());

    return tracks_;
}

void ByteTrackLite::reset() {
    tracks_.clear();
    next_id_ = 1;
}
