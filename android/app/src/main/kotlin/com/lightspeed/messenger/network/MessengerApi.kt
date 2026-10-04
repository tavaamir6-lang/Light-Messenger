package com.lightspeed.messenger.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class MessengerApi(private val baseUrl: String = ApiConfig.DEFAULT_BASE_URL) {
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; encodeDefaults = true })
        }
    }

    fun baseUrl(): String = baseUrl
    fun close() = client.close()
}
