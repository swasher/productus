package com.swasher.productus.presentation.camera

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.UploadCallback
import com.cloudinary.android.callback.ErrorInfo
import com.swasher.productus.BuildConfig
import com.swasher.productus.R
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

//import com.cloudinary.android.callback.UploadCallback
//import com.cloudinary.UploadCallback

class CameraActivity : ComponentActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var previewView: PreviewView
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var flashEnabled = false // ✅ Вспышка вкл/выкл

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkCameraPermission()

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

            // замена
            //            val preview = Preview.Builder().build().also {
            //                it.setSurfaceProvider(previewView.surfaceProvider)
            //            }
            val preview = Preview.Builder().build()
            preview.surfaceProvider = previewView.surfaceProvider // ✅ Обязательно устанавливаем SurfaceProvider

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
        val imageCapture = imageCapture ?: run {
            Log.e("CameraActivity", "Ошибка: imageCapture = null")
            return
        }

        Log.d("CameraActivity", "📸 Нажата кнопка 'Сделать фото'")

        // ✅ Создаём файл для сохранения фото
        val photoFile = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "${System.currentTimeMillis()}.jpg")

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    Log.d("CameraActivity", "✅ Фото сохранено: $savedUri")

                    // ✅ Загружаем в Cloudinary
                    uploadToCloudinary(savedUri)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraActivity", "❌ Ошибка сохранения фото", exception)
                }
            }
        )
    }

    private fun uploadToCloudinary(uri: Uri) {
        val uploadFolder = BuildConfig.CLOUDINARY_UPLOAD_DIR // ✅ Папка из `BuildConfig`
        Log.d("CameraActivity", "🌍 Начинаем загрузку фото в Cloudinary: $uri, в папку $uploadFolder")

        MediaManager.get().upload(uri)
            .option("resource_type", "image")
            .option("folder", uploadFolder)
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}

                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val imageUrl = resultData["secure_url"] as String
                    Log.d("CameraActivity", "✅ Фото загружено в Cloudinary: $imageUrl")

                    // ✅ После загрузки можно закрыть камеру
                    finish()
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    Log.e("CameraActivity", "❌ Ошибка загрузки в Cloudinary: ${error.description}")
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) {}
            })
            .dispatch()
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


    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
        } else {
            startCamera() // ✅ Разрешение уже есть, запускаем камеру
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera() // ✅ Если разрешение получено, запускаем камеру
        } else {
            // Разрешение не предоставлено, показываем сообщение пользователю
            Toast.makeText(this, "Camera permission is required to use the camera", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
    }

}
