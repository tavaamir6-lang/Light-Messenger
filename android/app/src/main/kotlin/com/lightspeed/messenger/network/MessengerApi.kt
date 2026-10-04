package com.lightspeed.messenger.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.headers
import io.ktor.client.request.setBody
import io.ktor.client.call.body
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.websocket.WebSocketSession
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable data class AuthRequest(val name: String, val code: String)
@Serializable data class AuthUser(val id: String, val name: String)
@Serializable data class AuthResponse(val user: AuthUser, val accessToken: String)
@Serializable data class UserItem(val id: String, val name: String)
@Serializable data class UsersResponse(val users: List<UserItem> = emptyList())
@Serializable data class ConversationItem(val conversationId: String, val userId: String, val name: String)
@Serializable data class ConversationsResponse(val conversations: List<ConversationItem> = emptyList())
@Serializable data class ChatMessage(val id: String, val conversationId: String, val senderId: String, val body: String, val createdAt: String)
@Serializable data class MessagesResponse(val messages: List<ChatMessage> = emptyList())
@Serializable data class CreatePrivateRequest(val userId: String)
@Serializable data class CreatePrivateResponse(val conversationId: String)

class MessengerApi(private val baseUrl: String = ApiConfig.DEFAULT_BASE_URL) {
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; encodeDefaults = true }) }
        install(io.ktor.client.plugins.websocket.WebSockets)
    }

    private fun auth(token: String) = headers { append("Authorization", "Bearer $token") }

    suspend fun register(name: String, code: String): AuthResponse =
        client.post("$"+"baseUrl/api/v1/auth/register") {
            contentType(ContentType.Application.Json); setBody(AuthRequest(name, code))
        }.body()

    suspend fun login(name: String, code: String): AuthResponse =
        client.post("$"+"baseUrl/api/v1/auth/login") {
            contentType(ContentType.Application.Json); setBody(AuthRequest(name, code))
        }.body()

    suspend fun searchUsers(token: String, query: String): List<UserItem> =
        client.get("$"+"baseUrl/api/v1/users/search?q="+java.net.URLEncoder.encode(query, "UTF-8")) { auth(token) }.body<UsersResponse>().users

    suspend fun createPrivate(token: String, userId: String): String =
        client.post("$"+"baseUrl/api/v1/conversations/private") {
            auth(token); contentType(ContentType.Application.Json); setBody(CreatePrivateRequest(userId))
        }.body<CreatePrivateResponse>().conversationId

    suspend fun conversations(token: String): List<ConversationItem> =
        client.get("$"+"baseUrl/api/v1/conversations") { auth(token) }.body<ConversationsResponse>().conversations

    suspend fun messages(token: String, conversationId: String): List<ChatMessage> =
        client.get("$"+"baseUrl/api/v1/conversations/$"+"conversationId/messages") { auth(token) }.body<MessagesResponse>().messages

    suspend fun socket(token: String): WebSocketSession {
        val scheme = if (baseUrl.startsWith("https://")) "wss://" else "ws://"
        val host = baseUrl.removePrefix("https://").removePrefix("http://").trimEnd('/')
        return client.webSocketSession("$"+"scheme$"+"host$"+"{ApiConfig.WS_PATH}?token="+java.net.URLEncoder.encode(token, "UTF-8"))
    }

    fun close() = client.close()
}
