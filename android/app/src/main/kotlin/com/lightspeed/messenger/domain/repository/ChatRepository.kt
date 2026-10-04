package com.lightspeed.messenger.domain.repository

import com.lightspeed.messenger.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeMessages(conversationId: String): Flow<List<Message>>
    suspend fun sendMessage(conversationId: String, text: String)
    suspend fun retryPending()
}
