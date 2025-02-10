package com.swasher.productus

import android.util.Log
import android.app.Application
import android.content.Context
import android.widget.Toast
import com.cloudinary.android.MediaManager
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import kotlin.system.exitProcess


@HiltAndroidApp
class ProductusApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Инициализируем Firebase
        FirebaseApp.initializeApp(this)

        // Проверяем credentials Cloudinary и инициализируем, если они есть
        if (areCloudinaryCredentialsValid()) {
            CloudinaryManager.init(this)
        } else {
            // Показываем сообщение об ошибке и завершаем работу приложения
            showErrorAndExit("Cloudinary credentials are missing. Please check your local.properties file.")
        }
    }

    /**
     * Проверяет, что все необходимые credentials для Cloudinary указаны.
     */
    private fun areCloudinaryCredentialsValid(): Boolean {
        return BuildConfig.CLOUDINARY_CLOUD_NAME.isNotEmpty() &&
                BuildConfig.CLOUDINARY_API_KEY.isNotEmpty() &&
                BuildConfig.CLOUDINARY_API_SECRET.isNotEmpty()
    }

    /**
     * Показывает сообщение об ошибке и завершает работу приложения.
     */
    private fun showErrorAndExit(errorMessage: String) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        Log.e("ProductusApp", errorMessage)
        exitProcess(1)   // Завершаем процесс
    }

}

class CloudinaryManager {
    companion object {
        fun init(context: Context) {
            Log.d("CloudinaryManager", "Cloudinary инициализируется")
            val config = HashMap<String, String>().apply {
                put("cloud_name", BuildConfig.CLOUDINARY_CLOUD_NAME)
                put("api_key", BuildConfig.CLOUDINARY_API_KEY)
                put("api_secret", BuildConfig.CLOUDINARY_API_SECRET)
            }
            Log.d("CloudinaryManager", "Конфигурация Cloudinary: $config")
            MediaManager.init(context, config)
        }
    }
}
