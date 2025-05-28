package com.workhubui.model

data class ChatMessage(
    val sender: String,
    val receiver: String,
    val content: String,
    val timestamp: Long
)

// Extension để chuyển từ Entity → Model
fun com.workhubui.data.local.entity.ChatMessageEntity.toModel(): ChatMessage =
    ChatMessage(sender, receiver, content, timestamp)
