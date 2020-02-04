package com.PreTimer.pretimer

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.*
import android.util.Log
import android.view.View.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.io.BufferedReader
import java.io.File
import kotlin.math.ceil
import kotlin.math.floor


class MainActivity : AppCompatActivity() {
    /* 設定 */
    private val timePreset: Long = 180000 // ms
    private var preTime: Long = 30000 // ms

    /* 関数初期化 */
    private val lastTime = "lastTime.txt"
    private val lastPretime = "lastPretime.txt"
    private var timeMillis: Long = timePreset
    private var timer: Timer? = null // null == 停止中 / null != 稼働中
    private lateinit var soundPool: SoundPool
    private var alert = 0
    private var pause: Long = 0
    private var timeMax: Long = timePreset

    /* 残り時間 (ms)→(分：秒) 表示用テキスト変換 */
    private fun formatText(time: Long): String {
        val m: Double = floor(ceil(time.toDouble() / 1000) / 60)
        val s: Double = ceil(time.toDouble() / 1000) % 60
        return "%03.0f:%02.0f".format(m,s)
    }

    /* progressBar描画 */
    private fun drawBar(timeMillis: Long ,preTime: Long) {
        progressBar.max = timeMillis.toInt()
        progressBar.secondaryProgress = (timeMillis - preTime).toInt()
    }

    /* 数字 表示/非表示 */
    private fun displayNumber(view: Int) {
        timeText.visibility = view
        buttonReset.visibility = view
        numberPickerMin.visibility = view
        numberPickerSec.visibility = view
        textColon.visibility = view
        numberPickerPremin.visibility = view
        numberPickerPresec.visibility = view
        textColonPre.visibility = view
    }

    /* ファイル保存 */
    private fun saveFile(file: String, str: String) {
        applicationContext.openFileOutput(file, Context.MODE_PRIVATE).use {
            it.write(str.toByteArray())
        }
//        File(applicationContext.filesDir, file).writer().use {
//            it.write(str)
//        }
    }

    /* ファイル読み込み */
    private fun readFiles(file: String): String? {
        // to check whether file exists or not
        val readFile = File(applicationContext.filesDir, file)

        if(!readFile.exists()) {
            Log.d("debug","No file exists")
            return null
        } else {
            return readFile.bufferedReader().use(BufferedReader::readText)
        }
    }

    /* 全体 */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar!!.hide() // タイトルバー非表示

        // 起動時に最後に開始した時間を読み込み
        if(readFiles(lastTime) != null) timeMillis = readFiles(lastTime)!!.toLong()
        if(readFiles(lastPretime) != null) preTime = readFiles(lastPretime)!!.toLong()

        // 初期表示
        timeText.text = formatText(timeMillis)
        drawBar(timeMillis ,preTime)
        numberPickerMin.minValue = 0
        numberPickerMin.maxValue = 199
        numberPickerMin.value = floor(ceil(timeMillis.toDouble() / 1000) / 60).toInt()
        numberPickerSec.minValue = 0
        numberPickerSec.maxValue = 59
        numberPickerSec.value = (ceil(timeMillis.toDouble() / 1000) % 60).toInt()
        numberPickerPremin.minValue = 0
        numberPickerPremin.maxValue = 199
        numberPickerPremin.value = floor(ceil(preTime.toDouble() / 1000) / 60).toInt()
        numberPickerPresec.minValue = 0
        numberPickerPresec.maxValue = 59
        numberPickerPresec.value = (ceil(preTime.toDouble() / 1000) % 60).toInt()

        // 音声ファイル指定
        val audioAttributes= AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
        soundPool = SoundPool.Builder().setAudioAttributes(audioAttributes).setMaxStreams(2).build()
        alert = soundPool.load(this, R.raw.decision2, 1)

        // リセットボタン押下
        buttonReset.setOnClickListener {
            timer?.cancel()
            timer = null
            pause = 0
            timeMillis = (numberPickerMin.value*60000 + numberPickerSec.value*1000).toLong()
            preTime = (numberPickerPremin.value*60000 + numberPickerPresec.value*1000).toLong()
            timeText.text = formatText(timeMillis)
            drawBar(timeMillis ,preTime)
            progressBar.progress = 0
        }

        // 背景押下 Start/Pause
        backGround.setOnClickListener {
            if (timer == null) { // Start

                if (pause.toInt() == 0 || timeMax.toInt() != (numberPickerMin.value*60000 + numberPickerSec.value*1000)) { // 再開でない
                    timeMillis = (numberPickerMin.value*60000 + numberPickerSec.value*1000).toLong()
                    preTime = (numberPickerPremin.value*60000 + numberPickerPresec.value*1000).toLong()
                    drawBar(timeMillis ,preTime)
                    timeMax = timeMillis
                    saveFile(lastTime, timeMillis.toString()) // 記録
                    saveFile(lastPretime, preTime.toString()) // 記録
                }
                if (timeMillis > 0) {
                    startTimer(timeMillis)
                }
            } else { // Pause
                pause = timeMillis
                timer?.cancel()
                timer = null
                displayNumber(VISIBLE)
            }
        }
    }

    /* タイマー開始 */
    private fun startTimer(time :Long = timePreset) {
        timer = Timer(time)
        timer?.start()

        displayNumber(INVISIBLE)
    }

    /* タイマー */
    inner class Timer(millis: Long): CountDownTimer(millis,1000) {
        private val vibrator: Vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        private val vibEnd = longArrayOf(200,200 ,200,200 ,200,200 ,200,200 ,200,200)

        override fun onFinish() {
            timeMillis = 0
            timeText.text = "End"
            timer?.cancel()
            timer = null
            progressBar.progress = 0
            displayNumber(VISIBLE)

            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) { // ~ API25 OS7.1.2
                vibrator.vibrate(vibEnd, -1)
            } else { // API26 OS8.0 ~
                val vibrationEffect: VibrationEffect = VibrationEffect.createWaveform(vibEnd, -1)
                vibrator.vibrate(vibrationEffect)
            }
            soundPool.play(alert,1.0f,1.0f,0,2,2.0f)
        }

        override fun onTick(millisUntilFinished: Long) {
            timeText.text = formatText(millisUntilFinished)
            timeMillis = millisUntilFinished
            progressBar.progress = (timeMax - timeMillis).toInt()

            if (timeMillis/1000 == preTime/1000-1) { // 1回振動
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