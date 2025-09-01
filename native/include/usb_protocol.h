#pragma once
#include <vector>
#include <cstdint>
#include "bytetrack.h"

// 构建发送给 STM32 的帧：
// 帧格式: 0xAA 0x55 | len(uint8) | frame_id(uint16) | track_cnt(uint8) | track blocks... | crc16(LSB,MSB)
// track block: id(uint8) class(uint8) cx(int16) cy(int16) w(int16) h(int16) score(uint8)
// 坐标与尺寸采用图像像素整数（四舍五入），score = 0~255 映射 (float_score*255)

std::vector<uint8_t> buildTrackFrame(const std::vector<Track>& tracks,
                                     uint16_t frame_id,
                                     int imgWidth,
                                     int imgHeight,
                                     int maxTracksToSend = 16);

// 计算 CRC16-CCITT (0x1021, init 0xFFFF, no xor out)
uint16_t crc16_ccitt(const uint8_t* data, size_t len);
