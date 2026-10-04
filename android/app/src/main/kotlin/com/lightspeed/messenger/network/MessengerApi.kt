package com.lightspeed.messenger.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.headers
import io.ktor.client.call.body
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class AuthRequest(val name: String, val code: String)

@Serializable
data class AuthUser(val id: String, val name: String)

@Serializable
data class AuthResponse(val user: AuthUser, val accessToken: String)

class MessengerApi(private val baseUrl: String = ApiConfig.DEFAULT_BASE_URL) {
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; encodeDefaults = true })
        }
    }

    suspend fun register(name: String, code: String): AuthResponse =
        client.post("$baseUrl/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(AuthRequest(name, code))
        }.body()

    suspend fun login(name: String, code: String): AuthResponse =
        client.post("$baseUrl/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(AuthRequest(name, code))
        }.body()

    fun baseUrl(): String = baseUrl
    fun close() = client.close()
}
