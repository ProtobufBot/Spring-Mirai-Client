package net.lz1998.mirai.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.lz1998.mirai.alias.BMessage
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.getGroupOrNull
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.uploadAsGroupVoice
import net.mamoe.mirai.message.uploadAsImage
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory

val MSG_EMPTY = PlainText("")


suspend fun protoMessageToMiraiMessage(msgList: List<BMessage>, bot: Bot, contact: Contact, notConvert: Boolean): List<Message> {
    val messageChain = mutableListOf<Message>()
    msgList.forEach {
        when (it.type) {
            "text" -> {
                if (notConvert) {
                    messageChain.add(protoTextToMiraiText(it.dataMap))
                } else {
                    val text = it.dataMap["text"] ?: return@forEach
                    messageChain.addAll(rawMessageToMiraiMessage(text, bot, contact))
                }
            }
            "face" -> messageChain.add(protoFaceToMiraiFace(it.dataMap))
            "image" -> messageChain.add(protoImageToMiraiImage(it.dataMap, contact))
            "at" -> messageChain.add(protoAtToMiraiAt(it.dataMap, bot, contact))
            "record" -> messageChain.add(protoVoiceToMiraiVoice(it.dataMap, contact))
            "voice" -> messageChain.add(protoVoiceToMiraiVoice(it.dataMap, contact))
            else -> MSG_EMPTY
        }
    }
    return messageChain
}


fun protoTextToMiraiText(dataMap: Map<String, String>): Message {
    return PlainText(dataMap["text"] ?: "")
}

suspend fun protoImageToMiraiImage(dataMap: Map<String, String>, contact: Contact): Message {
    return try {
        withContext(Dispatchers.IO) {
            val img = URL(dataMap["url"] ?: dataMap["file"]
            ?: "").openConnection().getInputStream().uploadAsImage(contact)
            if (dataMap["type"] == "flash") img.flash() else img
        }
    } catch (e: Exception) {
        MSG_EMPTY
    }
}

fun protoAtToMiraiAt(dataMap: Map<String, String>, bot: Bot, contact: Contact): Message {
    return if (dataMap["qq"] == "all")
        AtAll
    else
        dataMap["qq"]?.toLong()?.let { userId -> bot.getGroupOrNull(contact.id)?.getOrNull(userId)?.let { At(it) } }
                ?: MSG_EMPTY
}

fun protoFaceToMiraiFace(dataMap: Map<String, String>): Message {
    return dataMap["id"]?.toInt()?.let { Face(it) } ?: MSG_EMPTY
}

suspend fun protoVoiceToMiraiVoice(dataMap: Map<String, String>, contact: Contact): Message {
    when (contact) {
        is Group -> {
            val url = dataMap["url"] ?: return MSG_EMPTY
            return try {
                withContext(Dispatchers.IO) {
                    URL(url).openStream().uploadAsGroupVoice(contact)
                }
            } catch (e: Exception) {
                MSG_EMPTY
            }
        }
        else -> return MSG_EMPTY
    }

}

suspend fun rawMessageToMiraiMessage(str: String, bot: Bot, contact: Contact): List<Message> {
    val messageList = mutableListOf<Message>()
    var str = str
    val re = Regex("<[\\s\\S]+?/>")
    val textList = re.split(str).toMutableList()
    val codeList = re.findAll(str).map { it.value }.toMutableList()
    while (textList.isNotEmpty() || codeList.isNotEmpty()) {
        if (textList.isNotEmpty() && str.startsWith(textList.first())) {
            val text = textList.first()
            textList.removeFirst()
            str = str.substring(text.length)
            messageList.add(PlainText(text))
        }
        if (codeList.isNotEmpty() && str.startsWith(codeList.first())) {
            val code = codeList.first()
            codeList.removeFirst()
            str = str.substring(code.length)
            // decode xml
            val builderFactory = DocumentBuilderFactory.newInstance()
            val docBuilder = builderFactory.newDocumentBuilder()
            val node = docBuilder.parse(code.byteInputStream()).firstChild

            val dataMap = mutableMapOf<String, String>()
            while (node.attributes.length > 0) {
                val item = node.attributes.item(0)
                dataMap[item.nodeName] = item.nodeValue
                node.attributes.removeNamedItem(item.nodeName)
            }
            when (node.nodeName) {
                "at" -> messageList.add(protoAtToMiraiAt(dataMap, bot, contact))
                "image" -> messageList.add(protoImageToMiraiImage(dataMap, contact))
                "face" -> messageList.add(protoFaceToMiraiFace(dataMap))
                "text" -> messageList.add(protoTextToMiraiText(dataMap))
                "record" -> messageList.add(protoVoiceToMiraiVoice(dataMap, contact))
                "voice" -> messageList.add(protoVoiceToMiraiVoice(dataMap, contact))
            }
        }

    }
    return messageList
}


suspend fun MessageChain.toRawMessage(): String {
    return this.map {
        when (it) {
            is PlainText -> it.content
            is At -> """<at qq="${it.target}"/>"""
            is Image -> """<image url="${it.queryUrl()}"/>"""
            is Voice -> """<voice url="${it.url}"/>"""
            is Face -> """<face id="${it.id}"/>"""
            else -> ""
        }
    }.joinToString("")
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
