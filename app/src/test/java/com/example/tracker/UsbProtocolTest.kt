package com.example.tracker

import org.junit.Test
import org.junit.Assert.*

/**
 * USB 协议帧测试
 * 验证 CRC16 算法与 C++ 一致性，以及帧解析正确性
 */
class UsbProtocolTest {

    private fun crc16Ccitt(data: ByteArray, start: Int = 0, endExclusive: Int = data.size): Int {
        var crc = 0xFFFF
        for (i in start until endExclusive) {
            crc = crc xor ((data[i].toInt() and 0xFF) shl 8)
            repeat(8) { crc = if (crc and 0x8000 != 0) (crc shl 1) xor 0x1021 else crc shl 1 }
            crc = crc and 0xFFFF
        }
        return crc
    }

    @Test
    fun testCrc16_knownValues() {
        // 测试已知 CRC16-CCITT 值
        val testData1 = byteArrayOf(0x48, 0x65, 0x6C, 0x6C, 0x6F) // "Hello"
        val expectedCrc1 = 0x4A81
        assertEquals("CRC16 for 'Hello' should match", expectedCrc1, crc16Ccitt(testData1))

        val testData2 = byteArrayOf(0x00, 0x01, 0x02, 0x03)
        val crc2 = crc16Ccitt(testData2)
        assertTrue("CRC16 should be non-zero for non-empty data", crc2 != 0)
    }

    @Test
    fun testCrc16_emptyData() {
        val emptyData = byteArrayOf()
        val crc = crc16Ccitt(emptyData)
        assertEquals("CRC16 for empty data should be 0xFFFF", 0xFFFF, crc)
    }

    @Test
    fun testFrameGeneration_and_parsing() {
        // 模拟生成一个简单的帧
        val frameId = 0x1234.toShort()
        val trackCount = 2
        
        // 手工构建帧：header + len + frame_id + track_cnt + 2个track + crc
        val frame = mutableListOf<Byte>()
        frame.add(0xAA.toByte())
        frame.add(0x55.toByte())
        
        // 计算 payload 长度：frame_id(2) + track_cnt(1) + 2*track_block(11 bytes each)
        val payloadLen = 2 + 1 + 2 * 11
        frame.add(payloadLen.toByte())
        
        // frame_id (little endian)
        frame.add((frameId.toInt() and 0xFF).toByte())
        frame.add(((frameId.toInt() shr 8) and 0xFF).toByte())
        
        // track_cnt
        frame.add(trackCount.toByte())
        
        // Track 1: id=1, label=0, cx=100, cy=200, w=50, h=80, score=0.8
        frame.add(1) // id
        frame.add(0) // label
        // cx=100 (little endian int16)
        frame.add(100 and 0xFF); frame.add((100 shr 8) and 0xFF)
        // cy=200
        frame.add(200 and 0xFF); frame.add((200 shr 8) and 0xFF)
        // w=50
        frame.add(50 and 0xFF); frame.add((50 shr 8) and 0xFF)
        // h=80
        frame.add(80 and 0xFF); frame.add((80 shr 8) and 0xFF)
        // score=0.8 -> 204 (0.8 * 255)
        frame.add(204)
        
        // Track 2: id=2, label=1, cx=300, cy=150, w=60, h=90, score=0.9
        frame.add(2) // id
        frame.add(1) // label
        // cx=300
        frame.add(300 and 0xFF); frame.add((300 shr 8) and 0xFF)
        // cy=150
        frame.add(150 and 0xFF); frame.add((150 shr 8) and 0xFF)
        // w=60
        frame.add(60 and 0xFF); frame.add((60 shr 8) and 0xFF)
        // h=90
        frame.add(90 and 0xFF); frame.add((90 shr 8) and 0xFF)
        // score=0.9 -> 229 (0.9 * 255)
        frame.add(229)
        
        // 计算 CRC
        val frameBytes = frame.toByteArray()
        val crc = crc16Ccitt(frameBytes)
        frame.add((crc and 0xFF).toByte())
        frame.add(((crc shr 8) and 0xFF).toByte())
        
        val finalFrame = frame.toByteArray()
        
        // 现在测试解析
        val tracks = parseTrackFrame(finalFrame, finalFrame.size)
        
        assertEquals("Should parse 2 tracks", 2, tracks.size)
        
        val track1 = tracks[0]
        assertEquals("Track 1 ID", 1, track1.id)
        assertEquals("Track 1 label", 0, track1.label)
        assertEquals("Track 1 cx", 100, track1.cx)
        assertEquals("Track 1 cy", 200, track1.cy)
        assertEquals("Track 1 w", 50, track1.w)
        assertEquals("Track 1 h", 80, track1.h)
        assertEquals("Track 1 score", 0.8f, track1.score, 0.01f)
        
        val track2 = tracks[1]
        assertEquals("Track 2 ID", 2, track2.id)
        assertEquals("Track 2 label", 1, track2.label)
        assertEquals("Track 2 cx", 300, track2.cx)
        assertEquals("Track 2 cy", 150, track2.cy)
        assertEquals("Track 2 w", 60, track2.w)
        assertEquals("Track 2 h", 90, track2.h)
        assertEquals("Track 2 score", 0.9f, track2.score, 0.01f)
    }

    @Test
    fun testFrameParsing_invalidHeader() {
        val invalidFrame = byteArrayOf(0xAB.toByte(), 0x55, 10, 0, 0, 0)
        val tracks = parseTrackFrame(invalidFrame, invalidFrame.size)
        assertTrue("Invalid header should return empty list", tracks.isEmpty())
    }

    @Test
    fun testFrameParsing_invalidCrc() {
        val frameWithBadCrc = byteArrayOf(
            0xAA.toByte(), 0x55, 3, // header + len
            0x34, 0x12, 0, // frame_id + track_cnt=0
            0xFF.toByte(), 0xFF.toByte() // wrong CRC
        )
        val tracks = parseTrackFrame(frameWithBadCrc, frameWithBadCrc.size)
        assertTrue("Invalid CRC should return empty list", tracks.isEmpty())
    }

    private data class UsbTrackRaw(
        val id: Int,
        val label: Int,
        val cx: Int,
        val cy: Int,
        val w: Int,
        val h: Int,
        val score: Float
    )

    private fun parseTrackFrame(buf: ByteArray, size: Int): List<UsbTrackRaw> {
        if (size < 8) return emptyList()
        if (buf[0].toInt() != 0xAA || buf[1].toInt() != 0x55) return emptyList()
        val payloadLen = buf[2].toInt() and 0xFF
        if (payloadLen + 4 > size) return emptyList()
        val crcCalc = crc16Ccitt(buf, 0, size - 2)
        val crcFrame = (buf[size - 2].toInt() and 0xFF) or ((buf[size - 1].toInt() and 0xFF) shl 8)
        if (crcCalc != crcFrame) return emptyList()
        val trackCnt = buf[5].toInt() and 0xFF
        var offset = 6
        val tracks = ArrayList<UsbTrackRaw>(trackCnt)
        for (i in 0 until trackCnt) {
            if (offset + 11 > size - 2) break
            val id = buf[offset].toInt() and 0xFF; offset++
            val label = buf[offset].toInt() and 0xFF; offset++
            fun rd16(): Int { val v = (buf[offset].toInt() and 0xFF) or ((buf[offset+1].toInt() and 0xFF) shl 8); offset += 2; return if (v and 0x8000 != 0) v - 0x10000 else v }
            val cx = rd16(); val cy = rd16(); val w = rd16(); val h = rd16()
            val qScore = buf[offset].toInt() and 0xFF; offset++
            tracks.add(UsbTrackRaw(id,label,cx,cy,w,h,qScore/255f))
        }
        return tracks
    }
}
