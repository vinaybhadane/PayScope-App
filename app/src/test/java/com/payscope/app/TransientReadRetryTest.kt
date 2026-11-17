package com.payscope.app

import com.payscope.app.data.api.TransientReadRetryInterceptor
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert.assertEquals
import org.junit.Test

class TransientReadRetryTest {
    private fun checkRequest(method: String, expectedStatus: Int, expectedCalls: Int) {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(503).setBody("busy"))
        server.enqueue(MockResponse().setResponseCode(200).setBody("ready"))
        server.start()
        val client = OkHttpClient.Builder().addInterceptor(TransientReadRetryInterceptor()).build()
        try {
            val request = Request.Builder().url(server.url("/"))
                .method(method, if (method == "POST") "upload".toRequestBody() else null).build()
            client.newCall(request).execute().use { assertEquals(expectedStatus, it.code) }
            assertEquals(expectedCalls, server.requestCount)
        } finally {
            client.connectionPool.evictAll()
            client.dispatcher.executorService.shutdown()
            server.shutdown()
        }
    }

    @Test fun busyReadRecoversAutomatically() = checkRequest("GET", 200, 2)
    @Test fun uploadsAreNotRetriedImplicitly() = checkRequest("POST", 503, 1)
}
