package com.swasher.productus.data.model

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
    val price: Float = 0.0f
)
