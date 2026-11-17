package com.payscope.app.data.api

import org.json.JSONObject
import retrofit2.Response

fun <T> Response<T>.requireBody(): T {
    requireSuccess()
    return body() ?: throw IllegalStateException("The server returned an empty response. Please retry.")
}

fun Response<*>.requireSuccess() {
    if (isSuccessful) return
    val detail = runCatching { JSONObject(errorBody()?.string().orEmpty()).optString("detail") }.getOrNull()
    throw IllegalStateException(detail?.takeIf { it.isNotBlank() && !it.startsWith("[") }
        ?: when (code()) {
            401 -> "Your session expired. Please sign in again."
            413 -> "The statement exceeds the 10 MB limit."
            else -> "PayScope could not complete this request (HTTP ${code()}). Please retry."
        })
}
