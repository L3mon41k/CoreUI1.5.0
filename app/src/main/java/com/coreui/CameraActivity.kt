package com.coreui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.util.Locale

class CameraActivity : Activity() {
    private lateinit var previewView: PreviewView
    private lateinit var zoomLabel: TextView
    private lateinit var zoomBar: SeekBar
    private var camera: Camera? = null
    private var maxZoom = 1f
    private var requestedZoom = 1f

    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) startCamera() else finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else permission.launch(Manifest.permission.CAMERA)
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }
        root.addView(Button(this).apply {
            text = "Назад"
            setOnClickListener { finish() }
        }, ViewGroup.LayoutParams(-1, 52))

        previewView = PreviewView(this).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
        root.addView(previewView, LinearLayout.LayoutParams(-1, 0, 1f))

        zoomLabel = TextView(this).apply {
            textSize = 18f
            gravity = Gravity.CENTER
            text = "1.0x"
        }
        root.addView(zoomLabel, ViewGroup.LayoutParams(-1, 48))

        zoomBar = SeekBar(this).apply {
            max = 140
            progress = 0
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    requestedZoom = 1f + progress / 10f
                    applyZoom()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        root.addView(zoomBar, ViewGroup.LayoutParams(-1, 56))
        setContentView(root)
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            try {
                val provider = future.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                provider.unbindAll()
                camera = provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview)
                camera?.cameraInfo?.zoomState?.observe(this) { state ->
                    maxZoom = minOf(15f, state.maxZoomRatio).coerceAtLeast(1f)
                    requestedZoom = requestedZoom.coerceIn(1f, maxZoom)
                    applyZoom()
                }
            } catch (_: Exception) {
                zoomLabel.text = "Не удалось запустить камеру"
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun applyZoom() {
        val actual = requestedZoom.coerceIn(1f, maxZoom)
        camera?.cameraControl?.setZoomRatio(actual)
        zoomLabel.text = String.format(Locale.US, "%.1fx", actual)
        if (::zoomBar.isInitialized) {
            zoomBar.isEnabled = maxZoom > 1f
            zoomBar.max = ((maxZoom - 1f) * 10f).toInt().coerceAtLeast(0)
            zoomBar.progress = ((actual - 1f) * 10f).toInt()
        }
    }
}