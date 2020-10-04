package net.lz1998.mirai.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.lz1998.mirai.alias.BMessage
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.getGroupOrNull
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.uploadAsImage
import java.net.URL

val MSG_EMPTY = PlainText("")

suspend fun BMessage.toMiraiMessage(bot: Bot, contact: Contact): Message {
    return when (this.type) {
        "text" -> PlainText(dataMap["text"] ?: "")
        "face" -> dataMap["id"]?.toInt()?.let { Face(it) } ?: MSG_EMPTY
        "image" -> try {
            withContext(Dispatchers.IO) {
                val img = URL(dataMap["url"] ?: dataMap["file"]
                ?: "").openConnection().getInputStream().uploadAsImage(contact)
                if (dataMap["type"] == "flash") img.flash() else img
            }
        } catch (e: Exception) {
            MSG_EMPTY
        }
        "at" -> {
            if (dataMap["qq"] == "all")
                AtAll
            else
                dataMap["qq"]?.toLong()?.let { userId -> bot.getGroupOrNull(contact.id)?.getOrNull(userId)?.let { At(it) } }
                        ?: MSG_EMPTY
        }
        else -> MSG_EMPTY
    }
}


fun MessageChain.toRawMessage(): String {
    var rawMessage = ""
    this.forEachContent { rawMessage += it.contentToString() }
    return rawMessage
}

suspend fun MessageChain.toOnebotMessage(): List<BMessage> {
    val messageChain = mutableListOf<BMessage>()
    this.forEachContent { content ->
        val message = when (content) {
            is At -> BMessage.newBuilder().setType("at").putAllData(mapOf("qq" to content.target.toString())).build()
            is PlainText -> BMessage.newBuilder().setType("text").putAllData(mapOf("text" to content.content)).build()
            is Face -> BMessage.newBuilder().setType("face").putAllData(mapOf("id" to content.id.toString())).build()
            is Image -> BMessage.newBuilder().setType("image").putAllData(mapOf("file" to content.queryUrl())).build()
            is Voice -> BMessage.newBuilder().setType("record").putAllData(mapOf("file" to content.fileName)).build()
            else -> BMessage.newBuilder().setType("unknown").build()
        }
        messageChain.add(message)
    }
    return messageChain
}
