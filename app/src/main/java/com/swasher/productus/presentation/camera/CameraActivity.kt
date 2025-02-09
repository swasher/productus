package com.swasher.productus.presentation.camera

import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.camera.core.Preview
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.UploadCallback
import com.cloudinary.android.callback.ErrorInfo
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.swasher.productus.R
import androidx.camera.core.CameraXConfig
import androidx.camera.camera2.Camera2Config

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import com.swasher.productus.BuildConfig
import com.swasher.productus.data.repository.PhotoRepository
import com.swasher.productus.presentation.viewmodel.PhotoViewModel

@AndroidEntryPoint
class CameraActivity : AppCompatActivity(), CameraXConfig.Provider {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var outputFile: File

    private var imageCapture: ImageCapture? = null

    override fun getCameraXConfig(): CameraXConfig {
        return CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig()).build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        checkCameraPermission() // Сначала проверяем разрешение

        cameraExecutor = Executors.newSingleThreadExecutor()

        findViewById<Button>(R.id.capture_button).setOnClickListener {
            takePhoto() // Снимок фото
        }
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        outputFile = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "${System.currentTimeMillis()}.jpg")

        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val uri = Uri.fromFile(outputFile)
                Log.d("CameraActivity", "Фото отправлено на сохранение: $uri")
                uploadToCloudinary(uri)
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("CameraActivity", "Фото не удалось сохранить", exception)
            }
        })
    }

    private fun uploadToCloudinary(uri: Uri) {
        val uploadDir = BuildConfig.CLOUDINARY_UPLOAD_DIR
        val folderName = intent.getStringExtra("folderName") ?: "Unsorted"
        val photoRepository = PhotoRepository()

        MediaManager.get().upload(uri)
            .option("resource_type", "image")
            .option("folder", uploadDir)
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val imageUrl = resultData["secure_url"] as String
                    Log.d("CameraActivity", "Фото загружено: $imageUrl")

                    // 📌 Сохраняем в Firestore
                    photoRepository.savePhoto(
                        folderName, imageUrl,
                        onSuccess = {
                            Log.d("CameraActivity", "Фото сохранено в Firestore")

                            // Уведомляем ViewModel о новом фото
                            val viewModel = ViewModelProvider(this@CameraActivity).get(PhotoViewModel::class.java)
                            viewModel.loadPhotos(folderName) // Перезагружаем фото для текущей папки

                            finish()
                        },
                        onFailure = { e ->
                            Log.e("CameraActivity", "Ошибка сохранения в Firestore", e)
                        }
                    )
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    Log.e("CameraActivity", "Ошибка загрузки: ${error.description}")
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) {}
            })
            .dispatch()
    }


    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        } else {
            startCamera()
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                startCamera()
            } else {
                Toast.makeText(this, "Камера не разрешена", Toast.LENGTH_SHORT).show()
            }
        }
    }



    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build()

            val previewView = findViewById<PreviewView>(R.id.camera_preview)
            preview.setSurfaceProvider(previewView.surfaceProvider)

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
                Log.d("CameraActivity", "Камера запущена")
            } catch (exc: Exception) {
                Log.e("CameraActivity", "Ошибка запуска камеры", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }


    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
