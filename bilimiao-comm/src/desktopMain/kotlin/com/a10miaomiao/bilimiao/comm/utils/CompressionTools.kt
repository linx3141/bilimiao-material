package com.a10miaomiao.bilimiao.comm.utils

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.DataFormatException
import java.util.zip.Deflater
import java.util.zip.Inflater

actual object CompressionTools {

    actual fun compress(value: ByteArray, offset: Int, length: Int, compressionLevel: Int): ByteArray {
        val bos = ByteArrayOutputStream(length)
        val compressor = Deflater()
        try {
            compressor.setLevel(compressionLevel)
            compressor.setInput(value, offset, length)
            compressor.finish()
            val buf = ByteArray(1024)
            while (!compressor.finished()) {
                val count = compressor.deflate(buf)
                bos.write(buf, 0, count)
            }
        } finally {
            compressor.end()
        }
        return bos.toByteArray()
    }

    actual fun compress(value: ByteArray, offset: Int, length: Int): ByteArray {
        return compress(value, offset, length, Deflater.BEST_COMPRESSION)
    }

    actual fun compress(value: ByteArray): ByteArray {
        return compress(value, 0, value.size, Deflater.BEST_COMPRESSION)
    }

    actual fun decompress(value: ByteArray): ByteArray {
        val bos = ByteArrayOutputStream(value.size)
        val decompressor = Inflater()
        try {
            decompressor.setInput(value)
            val buf = ByteArray(1024)
            while (!decompressor.finished()) {
                val count = decompressor.inflate(buf)
                bos.write(buf, 0, count)
            }
        } finally {
            decompressor.end()
        }
        return bos.toByteArray()
    }

    actual fun decompressXML(data: ByteArray): ByteArray {
        // 明文 XML 直接返回
        if (data.isNotEmpty() && data[0] == '<'.code.toByte()) {
            return data
        }
        // 尝试 zlib 格式（带头）
        tryInflate(data, nowrap = false)?.let { return it }
        // 尝试 raw deflate（无头，B站弹幕接口实际返回格式）
        tryInflate(data, nowrap = true)?.let { return it }
        // 全部失败，返回原始数据
        return data
    }

    private fun tryInflate(data: ByteArray, nowrap: Boolean): ByteArray? {
        val decompresser = Inflater(nowrap)
        return try {
            decompresser.setInput(data)
            val baos = ByteArrayOutputStream(data.size)
            val bufferArray = ByteArray(1024)
            while (!decompresser.finished()) {
                val count = decompresser.inflate(bufferArray)
                if (count == 0) {
                    break
                }
                baos.write(bufferArray, 0, count)
            }
            val result = baos.toByteArray()
            if (result.isEmpty()) null else result
        } catch (e: DataFormatException) {
            null
        } finally {
            decompresser.end()
        }
    }
}
