#include "usb_protocol.h"
#include <algorithm>

uint16_t crc16_ccitt(const uint8_t* data, size_t len) {
    uint16_t crc = 0xFFFF;
    for (size_t i = 0; i < len; ++i) {
        crc ^= (uint16_t)data[i] << 8;
        for (int b = 0; b < 8; ++b) {
            if (crc & 0x8000) crc = (crc << 1) ^ 0x1021; else crc <<= 1;
        }
    }
    return crc;
}

std::vector<uint8_t> buildTrackFrame(const std::vector<Track>& tracks,
                                     uint16_t frame_id,
                                     int imgWidth,
                                     int imgHeight,
                                     int maxTracksToSend) {
    // 选取得分最高的前 N 个轨迹
    std::vector<const Track*> sorted;
    sorted.reserve(tracks.size());
    for (auto &t : tracks) if (t.lost == 0) sorted.push_back(&t);
    std::sort(sorted.begin(), sorted.end(), [](const Track* a, const Track* b){return a->score > b->score;});
    if ((int)sorted.size() > maxTracksToSend) sorted.resize(maxTracksToSend);

    size_t trackCnt = sorted.size();
    size_t payloadLen = 2 /*frame_id*/ + 1 /*track_cnt*/ + trackCnt * (1+1+2+2+2+2+1);
    size_t totalLenNoCrc = 2 /*header*/ + 1 /*len*/ + payloadLen;
    std::vector<uint8_t> buf;
    buf.reserve(totalLenNoCrc + 2);

    // Header
    buf.push_back(0xAA);
    buf.push_back(0x55);
    buf.push_back(static_cast<uint8_t>(payloadLen));
    // frame_id little endian
    buf.push_back(frame_id & 0xFF);
    buf.push_back((frame_id >> 8) & 0xFF);
    buf.push_back(static_cast<uint8_t>(trackCnt));

    auto clamp16 = [](int v){ if (v < -32768) return (int)-32768; if (v > 32767) return (int)32767; return v; };

    for (auto *tp : sorted) {
        int cx = (int)std::lround(tp->cx);
        int cy = (int)std::lround(tp->cy);
        int w  = (int)std::lround(tp->w);
        int h  = (int)std::lround(tp->h);
        // 边界裁剪
        cx = std::max(0, std::min(cx, imgWidth - 1));
        cy = std::max(0, std::min(cy, imgHeight - 1));
        w  = std::max(0, std::min(w, imgWidth));
        h  = std::max(0, std::min(h, imgHeight));
        auto push_int16 = [&](int v){ v = clamp16(v); buf.push_back(v & 0xFF); buf.push_back((v >> 8) & 0xFF); };
        buf.push_back((uint8_t)tp->id);
        buf.push_back((uint8_t)tp->label);
        push_int16(cx); push_int16(cy); push_int16(w); push_int16(h);
        uint8_t qScore = (uint8_t)std::clamp<int>((int)std::lround(tp->score * 255.f), 0, 255);
        buf.push_back(qScore);
    }

    uint16_t crc = crc16_ccitt(buf.data(), buf.size());
    buf.push_back(crc & 0xFF);
    buf.push_back((crc >> 8) & 0xFF);
    return buf;
}
