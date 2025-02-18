package com.swasher.productus.data.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName

data class Photo(
    val id: String = "",
    val imageUrl: String = "",
    val comment: String = "",
    val tags: List<String> = emptyList(),
    val folder: String = "Unsorted",
    val createdAt: Long = System.currentTimeMillis(),

    // ✅ Новые поля
    val name: String = "",
    val country: String = "",
    val store: String = "",
    val price: Float = 0.0f,
    val rating: Int = 0, // значение от 0 до 5

    // подавить предупреждения о доступе к полю. Эта аннотация указывает компилятору, что поле должно быть доступно напрямую, без геттера.
    // @JvmField
    // @get:PropertyName("isUploading") @set:PropertyName("isUploading")
    // @get:JvmName("isUploading")
    // @set:JvmName("setIsUploading")
    // @get:PropertyName("isUploading")
    @Exclude @JvmField
    val uploadProcessing: Boolean = false
    //val isUploading: Boolean = false
)
