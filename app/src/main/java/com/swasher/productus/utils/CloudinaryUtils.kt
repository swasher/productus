package com.swasher.productus.utils

import java.net.URI

object CloudinaryUtils {
    fun extractPublicId(url: String): String? {
        val path = URI(url).path
        val regex = """/upload/(?:v\d+/)?(.+?)\.[a-z]+$""".toRegex()
        val match = regex.find(path)
        val publicId = match?.groupValues?.get(1)

        return publicId?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("Invalid Cloudinary URL: publicId is empty")
    }
}