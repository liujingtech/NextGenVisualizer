package io.github.jeffshee.visualizerdemo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.github.jeffshee.visualizer.painters.fft.*
import io.github.jeffshee.visualizer.painters.misc.Gradient
import io.github.jeffshee.visualizer.painters.misc.Icon
import io.github.jeffshee.visualizer.painters.modifier.*
import io.github.jeffshee.visualizer.painters.waveform.WfmAnalog
import io.github.jeffshee.visualizer.utils.Preset
import io.github.jeffshee.visualizer.utils.VisualizerHelper
import io.github.jeffshee.visualizer.views.VisualizerView
import java.security.AccessController.getContext

class MainActivity : AppCompatActivity() {
    private var helper: VisualizerHelper? = null
    private lateinit var background: Bitmap
    private lateinit var bitmap: Bitmap
    private lateinit var circleBitmap: Bitmap
    private var current = 0
    private var mPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        hideSystemUI()

        setContentView(R.layout.activity_main)

        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 0)
        } else {
//            requestAudioFocus()
//            findViewById<VisualizerView>(R.id.visual).postDelayed({
                init()
//            }, 300)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        if (requestCode == 0 && grantResults[0] == 0) init()
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
//        hideSystemUI()
    }

    private fun hideSystemUI() {
        val decorView = window.decorView
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            decorView.systemUiVisibility =
                (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        }
    }

    var delayedFocusRequest: AudioFocusRequest? = null
    fun requestAudioFocus(): Boolean {
        val builder = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setFlags(AudioAttributes.FLAG_LOW_LATENCY)
            .build()

        delayedFocusRequest = AudioFocusRequest
            .Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(builder)
            //            .setWillPauseWhenDucked(true)
            .setOnAudioFocusChangeListener { it ->
                if (it == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
//                    stopMusic()
//                    requestAudioFocus()
                    //                    mPlayer?.pause()
                } else if (it == AudioManager.AUDIOFOCUS_GAIN) {
                    //                    mPlayer?.start()
                }
            }
            .build();
        val manager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        return manager?.requestAudioFocus(delayedFocusRequest!!)
            .let {
                it == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            }
    }

    private fun init() {
        background = BitmapFactory.decodeResource(resources, R.drawable.background)
        bitmap = BitmapFactory.decodeResource(resources, R.drawable.chino512)
        circleBitmap = Icon.getCircledBitmap(bitmap)

        mPlayer = MediaPlayer.create(this, R.raw.c).apply {
            isLooping = true
            start()
        }
        findViewById<VisualizerView>(R.id.visual).postDelayed({
            mPlayer!!.stop()
            finish()
        }, 15000)
        Log.e("aa"," mPlayer!!.audioSessionId:${ mPlayer!!.audioSessionId}")
        helper = VisualizerHelper(applicationContext, mPlayer!!.audioSessionId)

//        var count = 0
//        do {
//            try {
//                helper = VisualizerHelper(applicationContext, 57)
//            } catch (exception: Exception) {
//                exception.printStackTrace()
//                Log.e("a","initVisualizer Exception:${exception.message}")
//            }
//        } while (count++ < 1)

        findViewById<VisualizerView>(R.id.visual).apply {
            setup(
                helper!!, Compose(
                    Move(
                        CameraRotate(
                            Rotate(
                                FftCLine(
                                    startHz = 0,
                                    endHz = 2000,
                                    endOffset = 5f,
                                    radiusR = .37f,
                                    ampR = .5f,
                                    mirror = false,
                                    power = true,
                                    interpolator = "li",
                                    num = 128,
                                ),
                                pxR = .5f,
                                pyR = .5f,
                            ), rotateX = 76f
                        ), CameraRotate(
                            Rotate(
                                FftCSpaceLine(
                                    startHz = 0,
                                    endHz = 2000,
                                    radiusR = .37f,
                                    ampR = 1.5f,
                                    mirror = false,
                                    power = true,
                                    startOffset = 7f,
                                    endOffset = 3f,
                                    interpolator = "li",
                                    num = 128,
                                ),
                                pxR = .5f,
                                pyR = .5f,
                            ), rotateX = 76f
                        ), Beat(
                            onBeat = {},
                        ), yR = 0.03f
                    ),
                )
            )
        }

        Toast.makeText(this, "Try long-click \ud83d\ude09", Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        helper?.forceFlush()
        helper?.release()
        mPlayer?.release()
        super.onDestroy()
    }
}
