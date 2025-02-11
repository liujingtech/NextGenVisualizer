package io.github.jeffshee.visualizer.utils

import android.content.Context
import android.media.audiofx.Visualizer
import android.os.Handler
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException

class VisualizerHelper(val context: Context, sessionId: Int) {

    private val visualizer: Visualizer = Visualizer(sessionId)
    private val fftBuff: ByteArray
    private val fftMF: FloatArray
    private val fftM: DoubleArray
    private val waveBuff: ByteArray
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable

    init {
        visualizer.captureSize = Visualizer.getCaptureSizeRange()[1]
        fftBuff = ByteArray(visualizer.captureSize)
        waveBuff = ByteArray(visualizer.captureSize)
        fftMF = FloatArray(fftBuff.size / 2 - 1)
        fftM = DoubleArray(fftBuff.size / 2 - 1)
        visualizer.enabled = true
    }

    fun getFft(): ByteArray {
        if (visualizer.enabled) visualizer.getFft(fftBuff)
        return fftBuff
    }

    fun getWave(): ByteArray {
        if (visualizer.enabled) visualizer.getWaveForm(waveBuff)
        return waveBuff
    }

    fun getFftMagnitude(): DoubleArray {
        getFft()
        for (k in 0 until fftMF.size) {
            val i = (k + 1) * 2
            fftM[k] = Math.hypot(fftBuff[i].toDouble(), fftBuff[i + 1].toDouble())
        }
        // 将 fftM 的内容写入文件
        writeFftMagnitudeToFile(fftM)
        return fftM
    }

    private val bufferSizeInBytes = .1 * 1024 // 缓冲区大小
    private val buffer = StringBuilder()
    private var currentBufferSize = 0
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    fun writeFftMagnitudeToFile(fftM: DoubleArray) {
        scope.launch {
            val directory = context.getExternalFilesDir("1")
            val file = File(directory, "2.txt")
            try {
                // 如果文件不存在，则创建新文件
                if (!file.exists()) {
                    file.createNewFile()
                }
                // 将 fftM 数组内容转换为字符串
                val data = fftM.joinToString(separator = ", ") { it.toString() } + "\n"
                buffer.append(data)
                flushBuffer(file)
            } catch (e: IOException) {
                Log.e("VisualizerHelper", "${directory?.absoluteFile} 写入文件失败: ${e.message}")
            }
        }
    }

    private suspend fun flushBuffer(file: File) {
        try {
            // 使用 BufferedWriter 追加模式写入文件
            withContext(Dispatchers.IO) {
                BufferedWriter(FileWriter(file, true)).buffered().use { writer ->
                    writer.append(buffer)
                }
            }
            // 清空缓冲区和重置当前缓冲区大小
            buffer.setLength(0)
            currentBufferSize = 0
        } catch (e: IOException) {
            Log.e("VisualizerHelper", "${file.absoluteFile} 写入文件失败: ${e.message}")
        }
    }

    // 在对象销毁或需要强制写入时调用此方法
    fun forceFlush() {
        scope.launch {
            if (buffer.isNotEmpty()) {
                val directory = context.getExternalFilesDir("1")
                val file = File(directory, "2.txt")
                flushBuffer(file)
            }
        }
    }

    // 取消协程作用域，防止内存泄漏
    fun cancelScope() {
        job.cancel()
    }

    /**
     * Get Fft values from startHz to endHz
     */
    fun getFftMagnitudeRange(startHz: Int, endHz: Int): DoubleArray {
        val sIndex = hzToFftIndex(startHz)
        val eIndex = hzToFftIndex(endHz)
        return getFftMagnitude().copyOfRange(sIndex, eIndex)
    }

    /**
     * Equation from documentation, kth frequency = k*Fs/(n/2)
     */
    fun hzToFftIndex(Hz: Int): Int {
        return Math.min(Math.max(Hz * 1024 / (44100 * 2), 0), 255)
    }

    /**
     * Log WfmAnalog and Fft values every 1s
     */
    fun startDebug() {
        handler = Handler()
        runnable = object : Runnable {
            override fun run() {
                Log.d("WfmAnalog", getWave().contentToString())
                Log.d("Fft", getFftMagnitude().contentToString())
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(runnable)
    }

    /**
     * Stop logging
     */
    fun stopDebug() {
        handler.removeCallbacks(runnable)
    }

    /**
     * Release visualizer when not using anymore
     */
    fun release() {
        visualizer.enabled = false
        visualizer.release()
    }

}
