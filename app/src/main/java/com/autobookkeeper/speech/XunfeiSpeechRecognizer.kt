package com.autobookkeeper.speech

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.iflytek.cloud.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * 讯飞语音听写 SDK 封装 v2
 * 支持：暂停/恢复、连续语音、重试、音量动画、方言
 * APPID: 06bfde78
 */
class XunfeiSpeechRecognizer(context: Context) {

    private val appContext = context.applicationContext
    private var mIat: SpeechRecognizer? = null

    // ── 状态流 ──
    private val _recognitionResult = MutableStateFlow<String?>(null)
    val recognitionResult: StateFlow<String?> = _recognitionResult

    private val _partialResult = MutableStateFlow<String>("")
    val partialResult: StateFlow<String> = _partialResult

    private val _recognitionError = MutableStateFlow<String?>(null)
    val recognitionError: StateFlow<String?> = _recognitionError

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    /** 是否暂停中（暂停止听但UI保持） */
    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused

    /** 音量级别 0-10 */
    private val _volumeLevel = MutableStateFlow(0)
    val volumeLevel: StateFlow<Int> = _volumeLevel

    /** 是否在连续模式 */
    private var continuousMode = false

    /** 累积的识别文本（用于暂停/恢复） */
    private var accumulatedText = StringBuilder()

    /** 重试计数 */
    private var retryCount = 0
    private var maxRetries = 0
    private var currentDialect = "普通话"

    companion object {
        private const val TAG = "XunfeiSpeech"
        private var isInitialized = false
        private var initError: String? = null

        val DIALECTS = listOf(
            "普通话", "粤语", "四川话", "东北话", "河南话", "湖南话",
            "山东话", "陕西话", "客家话", "闽南语", "云南话", "贵州话"
        )

        fun initialize(context: Context) {
            if (isInitialized) return
            try {
                val ret = SpeechUtility.createUtility(
                    context.applicationContext,
                    SpeechConstant.APPID + "=06bfde78"
                )
                if (ret != null) {
                    Log.i(TAG, "讯飞SDK初始化成功")
                    isInitialized = true
                    initError = null
                } else {
                    Log.e(TAG, "讯飞SDK初始化失败")
                    initError = "初始化失败"
                }
            } catch (e: Exception) {
                Log.e(TAG, "讯飞SDK初始化异常: ${e.message}", e)
                initError = "初始化异常: ${e.message}"
            }
        }

        fun isSdkAvailable(): Boolean = isInitialized
        fun getInitError(): String? = initError
    }

    init {
        initialize(appContext)
        mIat = SpeechRecognizer.createRecognizer(appContext) { code ->
            if (code != ErrorCode.SUCCESS) {
                Log.e(TAG, "初始化失败，错误码：$code")
                initError = "初始化失败，错误码：$code"
            }
        }
    }

    /**
     * 开始识别
     * @param dialect 方言
     * @param retries 失败重试次数（0=不重试）
     * @param continuous 连续模式（识别完一段后自动重新开始）
     */
    fun startListening(
        dialect: String = "普通话",
        retries: Int = 2,
        continuous: Boolean = false
    ) {
        currentDialect = dialect
        maxRetries = retries
        retryCount = 0
        continuousMode = continuous

        if (!isInitialized) {
            Log.e(TAG, "SDK未初始化")
            _recognitionError.value = "语音识别服务未初始化"
            return
        }
        ensureRecognizer()
        resetState()
        applyParams(dialect)
        doStart()
    }

    /** 暂停（保持连接但停止采集音频——实际上是 stop + 后续 resume 重启） */
    fun pauseListening() {
        if (!_isListening.value || _isPaused.value) return
        _isPaused.value = true
        // 讯飞SDK没有pause，只能stop
        mIat?.stopListening()
        // 保持 isListening = true 让 UI 显示等待状态
    }

    /** 恢复（重新开始，追加到已有文本） */
    fun resumeListening() {
        if (!_isPaused.value) return
        _isPaused.value = false
        // 重新开始识别，累积文本保留
        _partialResult.value = ""
        _recognitionError.value = null
        applyParams(currentDialect)
        doStart()
    }

    /** 停止识别并返回最终结果 */
    fun stopListening() {
        mIat?.stopListening()
        _isListening.value = false
        _isPaused.value = false
        _volumeLevel.value = 0
        // 如果有累积文本未提交，提交它
        val accumulated = accumulatedText.toString().trim()
        if (accumulated.isNotEmpty() && _recognitionResult.value == null) {
            _recognitionResult.value = accumulated
        }
    }

    /** 取消本次识别（丢弃结果） */
    fun cancelListening() {
        mIat?.cancel()
        _isListening.value = false
        _isPaused.value = false
        _volumeLevel.value = 0
        accumulatedText.clear()
        _recognitionResult.value = null
        _partialResult.value = ""
    }

    // ── 内部方法 ──

    private fun ensureRecognizer() {
        if (mIat == null) {
            mIat = SpeechRecognizer.createRecognizer(appContext) { code ->
                Log.d(TAG, "识别器创建回调: $code")
            }
        }
    }

    private fun resetState() {
        _recognitionResult.value = null
        _partialResult.value = ""
        _recognitionError.value = null
        _isListening.value = true
        _isPaused.value = false
        _volumeLevel.value = 0
        accumulatedText.clear()
    }

    private fun applyParams(dialect: String) {
        mIat?.apply {
            setParameter(SpeechConstant.PARAMS, null)
            setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD)
            setParameter(SpeechConstant.RESULT_TYPE, "json")
            setParameter(SpeechConstant.LANGUAGE, "zh_cn")

            val accent = when (dialect) {
                "粤语" -> "cantonese"
                "四川话" -> "lmz"
                "东北话" -> "dongbei"
                "河南话" -> "henan"
                "湖南话" -> "hunan"
                "山东话" -> "shandong"
                "陕西话" -> "shanxi"
                "客家话" -> "kejia"
                "闽南语" -> "minnan"
                "云南话" -> "yunnan"
                "贵州话" -> "guizhou"
                else -> "mandarin"
            }
            setParameter(SpeechConstant.ACCENT, accent)

            setParameter(SpeechConstant.VAD_BOS, "5000")  // 前端点超时5s
            setParameter(SpeechConstant.VAD_EOS, "1500")  // 后端点1.5s
            setParameter(SpeechConstant.ASR_PTT, "1")     // 标点

            // 实时返回
            setParameter(SpeechConstant.ASR_DWA, "0")

            // 保存音频
            setParameter(SpeechConstant.AUDIO_FORMAT, "wav")
            setParameter(
                SpeechConstant.ASR_AUDIO_PATH,
                appContext.getExternalFilesDir(null)?.absolutePath + "/iat.wav"
            )
        }
    }

    private fun doStart() {
        val ret = mIat?.startListening(mRecognizerListener)
        Log.d(TAG, "startListening返回: $ret")

        if (ret != ErrorCode.SUCCESS) {
            handleStartError(ret)
        }
    }

    private fun handleStartError(ret: Int?) {
        val errorMsg = when (ret) {
            20001 -> "网络连接失败，请检查网络"
            20002 -> "网络超时"
            20003 -> "麦克风权限被拒绝"
            20004 -> "参数错误"
            20005 -> "引擎不支持"
            20006 -> "引擎初始化失败"
            else -> "启动识别失败，错误码：$ret"
        }
        Log.e(TAG, errorMsg)
        _recognitionError.value = errorMsg
        _isListening.value = false
    }

    private val mRecognizerListener = object : RecognizerListener {
        override fun onBeginOfSpeech() {
            Log.d(TAG, "开始说话")
        }

        override fun onError(error: SpeechError?) {
            error?.let {
                Log.e(TAG, "识别错误: ${it.errorCode} - ${it.errorDescription}")

                if (_isPaused.value) return

                if (continuousMode && retryCount < maxRetries) {
                    retryCount++
                    Log.d(TAG, "自动重试第${retryCount}次...")
                    // 使用后台线程延迟后重试
                    android.os.Handler(appContext.mainLooper).postDelayed({
                        applyParams(currentDialect)
                        doStart()
                    }, 500)
                    return
                }

                _recognitionError.value = it.errorDescription
                _isListening.value = false
            }
        }

        override fun onEndOfSpeech() {
            Log.d(TAG, "结束说话")
            // 连续模式：等结果出来后自动重启
        }

        override fun onResult(results: RecognizerResult?, isLast: Boolean) {
            results?.let {
                val text = parseResult(it.resultString)
                if (text.isNotEmpty()) {
                    if (isLast) {
                        accumulatedText.append(text)
                        val fullText = accumulatedText.toString()
                        _recognitionResult.value = fullText
                        _isListening.value = false
                        _partialResult.value = ""

                        // 连续模式：自动重启
                        if (continuousMode) {
                            Log.d(TAG, "连续模式，自动重启识别")
                            // 保留累积文本
                            _recognitionResult.value = null  // 清除最终结果标记
                            _isListening.value = true
                            applyParams(currentDialect)
                            doStart()
                        }
                    } else {
                        // 暂停状态下也更新partial
                        _partialResult.value = if (_isPaused.value) {
                            accumulatedText.toString() + text
                        } else {
                            accumulatedText.toString() + text
                        }
                    }
                }
            }
        }

        override fun onVolumeChanged(volume: Int, data: ByteArray?) {
            // 音量 0-30，映射到 0-10
            _volumeLevel.value = (volume * 10 / 30).coerceIn(0, 10)
        }

        override fun onEvent(eventType: Int, arg1: Int, arg2: Int, obj: Bundle?) {
            // 扩展事件
        }
    }

    private fun parseResult(json: String): String {
        val sb = StringBuilder()
        try {
            val jsonObject = JSONObject(json)
            val ws = jsonObject.getJSONArray("ws")
            for (i in 0 until ws.length()) {
                val wsItem = ws.getJSONObject(i)
                val cw = wsItem.getJSONArray("cw")
                for (j in 0 until cw.length()) {
                    val cwItem = cw.getJSONObject(j)
                    sb.append(cwItem.getString("w"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析结果失败: ${e.message}")
        }
        return sb.toString()
    }

    fun destroy() {
        mIat?.destroy()
        mIat = null
    }
}
