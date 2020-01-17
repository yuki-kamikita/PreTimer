package com.example.pretimer

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.ceil
import kotlin.math.floor


class MainActivity : AppCompatActivity() {
    /* 設定 */
    private val timePreset: Long = 30000 // ms
    var preTime: Long = 5 // s

    /* 関数初期化 */
    var timeMillis: Long = timePreset
    var timer: Timer? = null // null == 停止中 / null != 稼働中
    private lateinit var soundPool: SoundPool
    private var alert = 0

    /* 残り時間 (ms)→(分：秒) 表示用テキスト変換 */
    private fun formatText(time: Long): String {
        val m: Double = floor(ceil(time.toDouble() / 1000) / 60)
        val s: Double = ceil(time.toDouble() / 1000) % 60
        return "time %02.0f:%02.0f".format(m,s)
    }

    /* 時間変更 */
    private fun addTime(add: Long) {
        timeMillis += add
        timeText.text = formatText(timeMillis)
    }

    /* 全体 */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 初期表示
        timeText.text = formatText(timePreset)
        progressBar.max = timePreset.toInt()
        progressBar.secondaryProgress = (timePreset - preTime * 1000).toInt()

        // 音声ファイル指定
        val audioAttributes= AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
        soundPool = SoundPool.Builder().setAudioAttributes(audioAttributes).setMaxStreams(2).build()
        alert = soundPool.load(this, R.raw.decision2, 1)

        // 時間変動（暫定）
        buttonAdd1m.setOnClickListener {
            addTime(60000)
        }
        buttonAdd10s.setOnClickListener {
            addTime(10000)
        }
        buttonSub1m.setOnClickListener {
            if (timeMillis > 60000) {
                addTime(-60000)
            }
        }
        buttonSub10s.setOnClickListener {
            if (timeMillis > 10000) {
                addTime(-10000)
            }
        }

        // 背景押下
        backGround.setOnClickListener {
            if (timer == null) {
                if (timeMillis > 0) {
                    startTimer(timeMillis)
                }
            } else {
                timer?.cancel()
                timer = null
            }

        }

        // リセットボタン押下
        buttonReset.setOnClickListener {
            timer?.cancel()
            timer = null
            timeMillis = timePreset
            timeText.text = formatText(timeMillis)
        }
    }

    /* タイマー開始 */
    private fun startTimer(time :Long = timePreset) {
        timer = Timer(time)
        timer?.start()

        // 進捗管理
        progressBar.max = time.toInt()
        progressBar.secondaryProgress = (time - preTime * 1000).toInt()
    }

    /* タイマー */
    inner class Timer(millis: Long): CountDownTimer(millis,1000) {
        private var millis = millis
        private val vibrator: Vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        private val vibEnd = longArrayOf(200,200 ,200,200 ,200,200 ,200,200 ,200,200)

        override fun onFinish() {
            timeMillis = 0
            timeText.text = "End"
            timer = null
            progressBar.progress = 0

            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) { // ~ API25 OS7.1.2
                vibrator.vibrate(vibEnd, -1)
            } else { // API26 OS8.0 ~
                val vibrationEffect: VibrationEffect = VibrationEffect.createWaveform(vibEnd, -1)
                vibrator.vibrate(vibrationEffect)
            }
            soundPool.play(alert,1.0f,1.0f,0,4,2.0f)
        }

        override fun onTick(millisUntilFinished: Long) {
            timeText.text = formatText(millisUntilFinished)
            timeMillis = millisUntilFinished
            progressBar.progress = (millis - timeMillis).toInt()

            if (timeMillis/1000 == preTime-1) { // 1回振動
                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) { // ~ API25 OS7.1.2
                    vibrator.vibrate(200)
                } else { // API26 OS8.0 ~
                    val vibrationEffect: VibrationEffect = VibrationEffect.createWaveform(longArrayOf(200,200), -1)
                    vibrator.vibrate(vibrationEffect)
                }
                soundPool.play(alert,1.0f,1.0f,0,0,2.0f)
            }
        }

    }
}
