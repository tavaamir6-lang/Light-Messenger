package com.lightspeed.messenger.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val text: String,
    val createdAt: Long,
    val status: MessageStatus = MessageStatus.SENT
)

@Serializable
enum class MessageStatus { QUEUED, SENT, DELIVERED, READ, FAILED }
