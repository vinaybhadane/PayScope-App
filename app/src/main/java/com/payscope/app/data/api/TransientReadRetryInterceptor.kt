package com.payscope.app.data.api

import okhttp3.Interceptor
import okhttp3.Response

/** Retry reads briefly when a waking or busy host returns a transient status. */
class TransientReadRetryInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response = chain.proceed(request)
        if (request.method != "GET") return response
        repeat(2) { attempt ->
            if (response.code !in setOf(429, 502, 503, 504) || chain.call().isCanceled()) return response
            response.close()
            Thread.sleep(500L * (attempt + 1))
            response = chain.proceed(request)
        }
        return response
    }
}
