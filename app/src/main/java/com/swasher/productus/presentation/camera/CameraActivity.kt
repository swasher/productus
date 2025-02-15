package com.swasher.productus.presentation.camera

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.Intent
import android.Manifest
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swasher.productus.R
import com.swasher.productus.presentation.viewmodel.PhotoViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


@AndroidEntryPoint
class CameraActivity : ComponentActivity() {

    private val viewModel: PhotoViewModel by viewModels()

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var previewView: PreviewView
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var flashEnabled = false // ✅ Вспышка вкл/выкл

    private val currentFolder: String by lazy {
        intent.getStringExtra("FOLDER_NAME") ?: "Unsorted" // ✅ Получаем имя папки
    }

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

    private fun playShutterSound() {
        val shutterSound = R.raw.kwahmah_02_camera2
        // val shutterSound = R.raw.benboncan_dslr_click
        // val shutterSound = R.raw.the_egyptian_gamedev_camera_shutter // этот не завелся
        val mediaPlayer = MediaPlayer.create(this, shutterSound) // 📌 Подключаем звук
        mediaPlayer.setOnCompletionListener { it.release() } // ✅ Освобождаем ресурс после воспроизведения
        mediaPlayer.start()
    }


    // НОВАЯ ФУНКЦИЯ БЕЗ ИСПОЛЬЗОВАНИЯ CLOUDINARY НАПРЯМУЮ
    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val photoFile = File(externalCacheDir, "${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    // val intent = Intent().apply {
                    //     putExtra("photo_path", photoFile.absolutePath)
                    // }
                    // setResult(RESULT_OK, intent)

                    onPhotoCaptured(photoFile.absolutePath)

                    playShutterSound()

                    finish() // ✅ Закрываем камеру сразу после съемки

                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraActivity", "Ошибка сохранения фото", exception)
                }
            })
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


    private fun savePhotoToCache(photoFile: File): String {
        val cacheDir = File(cacheDir, "photos")
        if (!cacheDir.exists()) cacheDir.mkdirs()

        val cachedFile = File(cacheDir, "${UUID.randomUUID()}.jpg")
        photoFile.copyTo(cachedFile, overwrite = true)

        return cachedFile.absolutePath
    }

    private fun onPhotoCaptured(photoPath: String) {
        val cachedPhotoPath = savePhotoToCache(File(photoPath)) // ✅ Сохраняем фото в кеш
        viewModel.uploadPhoto(cachedPhotoPath, currentFolder) // ✅ Передаём в ViewModel
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
    }

}
