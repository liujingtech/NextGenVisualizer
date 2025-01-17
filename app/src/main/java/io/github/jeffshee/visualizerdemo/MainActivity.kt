package io.github.jeffshee.visualizerdemo

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
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

class MainActivity : AppCompatActivity() {

    private lateinit var helper: VisualizerHelper
    private lateinit var background: Bitmap
    private lateinit var bitmap: Bitmap
    private lateinit var circleBitmap: Bitmap
    private var current = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI()

        setContentView(R.layout.activity_main)
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 0)
        } else init()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 0 && grantResults[0] == 0) init()
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        hideSystemUI()
    }

    private fun hideSystemUI() {
        val decorView = window.decorView
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    )
        }
    }

    private fun init() {
        background = BitmapFactory.decodeResource(resources, R.drawable.background)
        bitmap = BitmapFactory.decodeResource(resources, R.drawable.chino512)
        circleBitmap = Icon.getCircledBitmap(bitmap)

        helper = VisualizerHelper(0)
        val list = listOf(
            // Basic components
            Compose(
                Move(WfmAnalog(), yR = -.3f),
                Move(FftBar(), yR = -.1f),
                Move(FftLine(), yR = .1f),
                Move(FftWave(), yR = .3f),
                Move(FftWaveRgb(), yR = .5f)
            ),
            Compose(
                Move(FftBar(side = "b"), yR = -.3f),
                Move(FftLine(side = "b"), yR = -.1f),
                Move(FftWave(side = "b"), yR = .1f),
                Move(FftWaveRgb(side = "b"), yR = .3f)
            ),
            Compose(
                Move(FftBar(side = "ab"), yR = -.3f),
                Move(FftLine(side = "ab"), yR = -.1f),
                Move(FftWave(side = "ab"), yR = .1f),
                Move(FftWaveRgb(side = "ab"), yR = .3f)
            ),
            // Basic components (Circle)
            Compose(Move(FftCLine(), xR = -.3f), FftCWave(), Move(FftCWaveRgb(), xR = .3f)),
            Compose(
                Move(FftCLine(side = "b"), xR = -.3f),
                FftCWave(side = "b"),
                Move(FftCWaveRgb(side = "b"), xR = .3f)
            ),
            Compose(
                Move(FftCLine(side = "ab"), xR = -.3f),
                FftCWave(side = "ab"),
                Move(FftCWaveRgb(side = "ab"), xR = .3f)
            ),
            //Blend
            Blend(
                FftLine().apply { paint.strokeWidth = 8f;paint.strokeCap = Paint.Cap.ROUND },
                Gradient(preset = Gradient.LINEAR_HORIZONTAL)
            ),
            Blend(
                FftLine().apply { paint.strokeWidth = 8f;paint.strokeCap = Paint.Cap.ROUND },
                Gradient(preset = Gradient.LINEAR_VERTICAL, hsv = true)
            ),
            Blend(
                FftLine().apply { paint.strokeWidth = 8f;paint.strokeCap = Paint.Cap.ROUND },
                Gradient(preset = Gradient.LINEAR_VERTICAL_MIRROR, hsv = true)
            ),
            Blend(
                FftCLine().apply { paint.strokeWidth = 8f;paint.strokeCap = Paint.Cap.ROUND },
                Gradient(preset = Gradient.RADIAL)
            ),
            Blend(
                FftCBar(side = "ab", gapX = 8f).apply { paint.style = Paint.Style.FILL },
                Gradient(preset = Gradient.SWEEP, hsv = true)
            ),
            // Composition
            Glitch(Beat(Preset.getPresetWithBitmap("cIcon", circleBitmap))),
            Compose(
                WfmAnalog().apply { paint.alpha = 150 },
                Shake(Preset.getPresetWithBitmap("cWaveRgbIcon", circleBitmap)).apply {
                    animX.duration = 1000
                    animY.duration = 2000
                }),
            Compose(
                Preset.getPresetWithBitmap("liveBg", background),
                FftCLine().apply { paint.strokeWidth = 8f;paint.strokeCap = Paint.Cap.ROUND }
            )
        )

        findViewById<VisualizerView>(R.id.visual).apply {
            setup(helper, list[current])
            setOnLongClickListener {
                if (current < list.lastIndex) current++ else current = 0
                setup(helper, list[current])
                true
            }
        }

        Toast.makeText(this, "Try long-click \ud83d\ude09", Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        helper.release()
        super.onDestroy()
    }
}
