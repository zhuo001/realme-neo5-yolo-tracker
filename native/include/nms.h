#pragma once
#include <vector>
#include "detector.h"

// 简单 IoU NMS (按 score 降序)
std::vector<Detection> nms(const std::vector<Detection>& dets, float iouThr, int topK = 300);
