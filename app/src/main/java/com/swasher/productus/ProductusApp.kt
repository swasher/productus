package com.swasher.productus


import android.util.Log
import android.app.Application
import android.content.Context
import com.cloudinary.android.MediaManager
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp


@HiltAndroidApp
class ProductusApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this) // Запускаем Firebase
        CloudinaryManager.init(this)   // Инициализируем Cloudinary
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
            Log.d("CloudinaryManager", BuildConfig.CLOUDINARY_API_KEY)
            MediaManager.init(context, config)
        }
    }
}
