package com.example.pretimer

import android.content.Context
import android.media.SoundPool
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.ceil
import kotlin.math.floor

class MainActivity : AppCompatActivity() {
    /* 設定 */
    val timePreset: Long = 30000 // ms
    var preTime: Long = 5 // s

    /* 関数初期化 */
    var timeMillis: Long = timePreset
    var timer: Timer? = null // null == 停止中 / null != 稼働中

    /* 残り時間 (ms)→(分：秒) 表示用テキスト変換 */
    private fun formatText(time: Long): String {
        val m: Double = floor(ceil(time.toDouble() / 1000) / 60)
        val s: Double = ceil(time.toDouble() / 1000) % 60
        return "time %02.0f:%02.0f".format(m,s)
    }

    /* 全体 */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        timeText.setText(formatText(timePreset))

        // 時間変動（暫定）
        buttonAdd1m.setOnClickListener {
            timeMillis += 60000
            timeText.setText(formatText(timeMillis))
        }
        buttonAdd10s.setOnClickListener {
            timeMillis += 10000
            timeText.setText(formatText(timeMillis))
        }
        buttonSub1m.setOnClickListener {
            if (timeMillis > 60000) {
                timeMillis -= 60000
                timeText.setText(formatText(timeMillis))
            }
        }
        buttonSub10s.setOnClickListener {
            if (timeMillis > 10000) {
                timeMillis -= 10000
                timeText.setText(formatText(timeMillis))
            }
        }

        // 背景押下
        backGround.setOnClickListener {
            if (timer == null) {
                if (timeMillis > 0) {
                    startTimer(timeMillis)
                }
            } else {
                timer?.stopVib()
                timer?.cancel()
                timer = null
            }

        }

        // リセットボタン押下
        buttonReset.setOnClickListener {
            timer?.stopVib()
            timer?.cancel()
            timer = null
            timeMillis = timePreset
            timeText.setText(formatText(timeMillis))
        }
    }

    /* タイマー開始 */
    private fun startTimer(time :Long = timePreset) {
        timer = Timer(time)
        timer?.start()
    }

    /* タイマー */
    inner class Timer(millis: Long): CountDownTimer(millis,1000) {
        private val vibrator: Vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
//        private val vibrationEffect: VibrationEffect = VibrationEffect.createWaveform(longArrayOf(200, 200), 1)

        override fun onFinish() {
            timeMillis = 0
            timeText.setText("End")
            timer = null
            vibrator.vibrate(longArrayOf(200,200 ,200,200 ,200,200 ,200,200 ,200,200), -1)

//            vibrator.vibrate(vibrationEffect)
        }

        override fun onTick(millisUntilFinished: Long) {
            timeText.setText(formatText(millisUntilFinished))
            timeMillis = millisUntilFinished
            if (timeMillis/1000 == preTime) {
                vibrator.vibrate(200)
            }
        }

        fun stopVib() {
            vibrator.cancel()
        }
    }
}
