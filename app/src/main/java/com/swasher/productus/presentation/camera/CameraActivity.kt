package com.swasher.productus.presentation.camera

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.SeekBar
import androidx.activity.ComponentActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.swasher.productus.R
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : ComponentActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var previewView: PreviewView
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var flashEnabled = false // ✅ Вспышка вкл/выкл

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        previewView = findViewById(R.id.camera_preview)
        val captureButton: ImageButton = findViewById(R.id.capture_button)
        val flashButton: ImageButton = findViewById(R.id.flash_button)
        val zoomSeekBar: SeekBar = findViewById(R.id.zoom_seekbar)

        cameraExecutor = Executors.newSingleThreadExecutor()

        startCamera()

        // ✅ Захват фото
        captureButton.setOnClickListener { takePhoto() }

        // ✅ Переключение вспышки
        flashButton.setOnClickListener {
            flashEnabled = !flashEnabled
            camera?.cameraControl?.enableTorch(flashEnabled)
            flashButton.setImageResource(if (flashEnabled) R.drawable.ic_flash_on else R.drawable.ic_flash_off)
        }

        // ✅ Зум: обработка жестов
        val scaleGestureDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val zoomRatio = camera?.cameraInfo?.zoomState?.value?.zoomRatio ?: 1f
                val scaleFactor = detector.scaleFactor
                camera?.cameraControl?.setZoomRatio(zoomRatio * scaleFactor)
                return true
            }
        })

/*
        previewView.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_DOWN) {
                focusOnPoint(event.x, event.y)
            }
            return@setOnTouchListener true
        }
*/
        // Исправленная ошибка от дикпик
        previewView.setOnTouchListener { view, event ->
            scaleGestureDetector.onTouchEvent(event)

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Обработка нажатия (например, фокусировка)
                    focusOnPoint(event.x, event.y)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // Уведомляем систему о клике
                    view.performClick()
                    true
                }
                else -> false
            }
        }

        // ✅ Зум: обработка слайдера
        zoomSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val linearZoom = progress / 100f
                camera?.cameraControl?.setLinearZoom(linearZoom)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e("CameraActivity", "Ошибка запуска камеры", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        // ✅ Логика захвата фото
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun focusOnPoint(x: Float, y: Float) {
        val meteringPointFactory = SurfaceOrientedMeteringPointFactory(previewView.width.toFloat(), previewView.height.toFloat())
        val meteringPoint = meteringPointFactory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(meteringPoint).build()

        camera?.cameraControl?.startFocusAndMetering(action)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
