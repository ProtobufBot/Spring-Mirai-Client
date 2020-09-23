package net.lz1998.mirai.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.getGroupOrNull
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.uploadAsImage
import onebot.OnebotBase
import java.io.File
import java.net.URL

val MSG_EMPTY = PlainText("")

suspend fun OnebotBase.Message.toMiraiMessage(bot: Bot, contact: Contact): Message {
    return when (this.type) {
        "text" -> PlainText(dataMap["text"] ?: "")
        "face" -> dataMap["id"]?.toInt()?.let { Face(it) } ?: MSG_EMPTY
        "image" -> {
            return try {
                withContext(Dispatchers.IO) {
                    URL(dataMap["file"] ?: "").openConnection().getInputStream().uploadAsImage(contact)
                }
            } catch (e: Exception) {
                MSG_EMPTY
            }
        }
        "at" -> dataMap["qq"]?.toLong()?.let { userId -> bot.getGroupOrNull(contact.id)?.getOrNull(userId)?.let { At(it) } }
                ?: MSG_EMPTY
        else -> MSG_EMPTY
    }
}


fun MessageChain.toRawMessage(): String {
    var rawMessage = ""
    this.forEachContent { rawMessage += it.contentToString() }
    return rawMessage
}

fun MessageChain.toOnebotMessage(): List<OnebotBase.Message> {
    val messageChain = mutableListOf<OnebotBase.Message>()
    this.forEachContent { content ->
        val message = when (content) {
            is At -> OnebotBase.Message.newBuilder().setType("at").putAllData(mapOf("qq" to content.target.toString())).build()
            is PlainText -> OnebotBase.Message.newBuilder().setType("text").putAllData(mapOf("text" to content.content)).build()
            is Face -> OnebotBase.Message.newBuilder().setType("face").putAllData(mapOf("id" to content.id.toString())).build()
            is Image -> OnebotBase.Message.newBuilder().setType("image").putAllData(mapOf("file" to content.imageId)).build()
            is Voice -> OnebotBase.Message.newBuilder().setType("record").putAllData(mapOf("file" to content.fileName)).build()
            else -> OnebotBase.Message.newBuilder().setType("unknown").build()
        }
        messageChain.add(message)
    }
    return messageChain
}
