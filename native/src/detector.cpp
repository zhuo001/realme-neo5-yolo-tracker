#include "detector.h"
#include "nms.h"
#include <cstring>
#include <cmath>
#include <algorithm>
#include <chrono>

void YoloDetector::buildGrid(){
    gridCache_.clear();
    int S = cfg_.inputSize;
    for(int stride : cfg_.strides){
        int fs = S / stride;
        for(int y=0;y<fs;++y){
            for(int x=0;x<fs;++x){
                gridCache_.push_back({x,y,stride});
            }
        }
    }
    lastGridInput_ = cfg_.inputSize;
}

void YoloDetector::letterbox(const uint8_t* rgba, int w, int h, std::vector<uint8_t>& out, float& scale, int& padX, int& padY){
    int S = cfg_.inputSize;
    out.assign(S*S*3, 0); // RGB
    float r = std::min( (float)S / w, (float)S / h );
    int newW = (int)std::round(w * r);
    int newH = (int)std::round(h * r);
    padX = (S - newW) / 2;
    padY = (S - newH) / 2;
    scale = r;
    // 简单最近邻缩放 (可替换为更优)
    for(int y=0;y<newH;++y){
        int sy = (int)(y / r);
        for(int x=0;x<newW;++x){
            int sx = (int)(x / r);
            const uint8_t* src = rgba + (sy*w + sx)*4; // RGBA
            uint8_t* dst = out.data() + ((y+padY)*S + (x+padX))*3;
            dst[0] = src[0]; dst[1] = src[1]; dst[2] = src[2];
        }
    }
}

std::vector<Detection> YoloDetector::decode(const float* feats, int featLen, float scale, int padX, int padY, int srcW, int srcH){
    // 假设输出排列：每个网格  (cx, cy, w, h, obj, cls0..clsN-1)
    int strideCount = (int)gridCache_.size();
    int step = 4 + cfg_.numClasses;
    std::vector<Detection> dets;
    dets.reserve(256);
    for(int i=0;i<8400;++i){ // YOLOv8/v10 固定 8400 个候选
        const float* p = feats + i*step;
        // 取最大类别
        int cls=-1; float clsScore=0.f;
        const float* scores = p + 4;
        for(int c=0;c<cfg_.numClasses;++c){
            if(scores[c]>clsScore){clsScore=scores[c]; cls=c;}
        }
        if(clsScore < cfg_.confThreshold) continue;

        // 解码
        float cx = p[0];
        float cy = p[1];
        float bw = p[2];
        float bh = p[3];
        
        // 反 letterbox
        float x1 = (cx - bw/2 - padX) / scale;
        float y1 = (cy - bh/2 - padY) / scale;
        float x2 = (cx + bw/2 - padX) / scale;
        float y2 = (cy + bh/2 - padY) / scale;
        // 裁剪
        x1 = std::max(0.f, std::min(x1, (float)srcW-1));
        y1 = std::max(0.f, std::min(y1, (float)srcH-1));
        x2 = std::max(0.f, std::min(x2, (float)srcW-1));
        y2 = std::max(0.f, std::min(y2, (float)srcH-1));
        if(x2<=x1 || y2<=y1) continue;
        dets.push_back({x1,y1,x2,y2,clsScore,cls});
    }
    return nms(dets, cfg_.nmsThreshold, 300);
}

bool YoloDetector::load(const std::string& paramPath, const std::string& binPath, const YoloConfig& cfg){
    cfg_ = cfg;
    buildGrid();
#ifdef USE_NCNN
    net_.clear();
    ncnn::Option opt;
    opt.num_threads = 4;
    opt.use_fp16_arithmetic = cfg_.useFP16;
    net_.opt = opt;
    if(net_.load_param(paramPath.c_str())) return false;
    if(net_.load_model(binPath.c_str())) return false;
#else
    (void)paramPath; (void)binPath; // stub
#endif
    return true;
}

std::vector<Detection> YoloDetector::infer(const uint8_t* rgba, int width, int height){
    if(!rgba) return {};
    int S = cfg_.inputSize;
    // letterbox 缩放参数
    float scale = std::min((float)S / width, (float)S / height);
    int padX = (S - (int)std::round(width * scale)) / 2;
    int padY = (S - (int)std::round(height * scale)) / 2;

#ifdef USE_NCNN
    const float norm_vals[3] = {1/255.f, 1/255.f, 1/255.f};
    // ncnn 自带 letterbox 和归一化，效率更高
    ncnn::Mat in = ncnn::Mat::from_pixels_resize(rgba, ncnn::Mat::PIXEL_RGBA2RGB, width, height, S, S);
    in.substract_mean_normalize(0, norm_vals);

    ncnn::Extractor ex = net_.create_extractor();
    ex.input("images", in); // 需与实际输入 blob 名称对应
    
    ncnn::Mat out;
    if(ex.extract(cfg_.outName.c_str(), out)) return {};

    // YOLOv8/v10 导出 ONNX 后的输出是 [1, 84, 8400] (cx,cy,w,h,cls0..clsN-1)
    // 我们需要转置为 [8400, 84] 以便逐行解码
    ncnn::Mat transposed;
    ncnn::transpose(out, transposed);
    const float* feats = (const float*)transposed.data;
    
    return decode(feats, transposed.total(), scale, padX, padY, width, height);
#else
    (void)scale; (void)padX; (void)padY; (void)S; (void)width; (void)height;
    return {};
#endif
}

std::vector<Detection> YoloDetector::inferWithTiming(const uint8_t* rgba, int width, int height, float& inferTimeMs) {
    auto start = std::chrono::high_resolution_clock::now();
    auto result = infer(rgba, width, height);
    auto end = std::chrono::high_resolution_clock::now();
    inferTimeMs = std::chrono::duration<float, std::milli>(end - start).count();
    return result;
}

void YoloDetector::updateThresholds(float confThresh, float nmsThresh) {
    cfg_.confThreshold = confThresh;
    cfg_.nmsThreshold = nmsThresh;
}

void YoloDetector::release(){
#ifdef USE_NCNN
    net_.clear();
#endif
    gridCache_.clear();
}
