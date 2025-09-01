#include "nms.h"
#include <algorithm>

static float iouBox(const Detection& a, const Detection& b){
    float x1 = std::max(a.x1, b.x1);
    float y1 = std::max(a.y1, b.y1);
    float x2 = std::min(a.x2, b.x2);
    float y2 = std::min(a.y2, b.y2);
    float w = std::max(0.f, x2 - x1);
    float h = std::max(0.f, y2 - y1);
    float inter = w*h;
    float areaA = (a.x2 - a.x1)*(a.y2 - a.y1);
    float areaB = (b.x2 - b.x1)*(b.y2 - b.y1);
    return inter / (areaA + areaB - inter + 1e-6f);
}

std::vector<Detection> nms(const std::vector<Detection>& dets, float iouThr, int topK){
    std::vector<Detection> order = dets;
    std::sort(order.begin(), order.end(), [](const Detection&a,const Detection&b){return a.score>b.score;});
    if((int)order.size()>topK) order.resize(topK);
    std::vector<Detection> kept;
    kept.reserve(order.size());
    for(auto &d : order){
        bool drop=false;
        for(auto &k: kept){
            if(d.label==k.label && iouBox(d,k) > iouThr){ drop=true; break; }
        }
        if(!drop) kept.push_back(d);
    }
    return kept;
}
