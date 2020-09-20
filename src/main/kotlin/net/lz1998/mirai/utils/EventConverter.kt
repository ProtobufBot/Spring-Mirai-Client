package net.lz1998.mirai.utils

import com.google.protobuf.Message
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.message.*
import net.mamoe.mirai.message.data.*
import onebot.OnebotBase
import onebot.OnebotEvent

fun BotEvent.toProtoMessage(): Message {
    return when (this) {
        is GroupMessageEvent -> this.toProtoMessage()
        is FriendMessageEvent -> this.toProtoMessage()
        is MemberJoinEvent -> this.toProtoMessage()
        is MemberLeaveEvent -> this.toProtoMessage()
        is MemberMuteEvent -> this.toProtoMessage()
        is MemberUnmuteEvent -> this.toProtoMessage()
        else -> OnebotEvent.BaseEvent.newBuilder().setTime(System.currentTimeMillis()).setSelfId(this.bot.id).setPostType("UNKNOWN").build()
    }
}

fun GroupMessageEvent.toProtoMessage(): Message {
    val sender = OnebotEvent.GroupMessageEvent.Sender.newBuilder()
            .setUserId(this.sender.id)
            .setNickname(this.sender.nick)
            .setCard(this.sender.nameCard)
            .setRole(this.sender.permission.name)
            .setTitle(this.sender.specialTitle)

    // 构造消息链
    val onebotMessage = this.message.toOnebotMessage()

    // 构造rawMessage
    val rawMessage = this.message.toRawMessage()

    // 构造GroupMessageEvent
    return OnebotEvent.GroupMessageEvent.newBuilder()
            .setTime(System.currentTimeMillis())
            .setSelfId(bot.id)
            .setPostType("message")
            .setMessageType("group")
            .setSubType("normal")
            .setMessageId(message.id)
            .setGroupId(group.id)
            .setUserId(this.sender.id)
            .addAllMessage(onebotMessage)
            .setRawMessage(rawMessage)
            .setSender(sender)
            .build()
}

fun FriendMessageEvent.toProtoMessage(): Message {
    val sender = OnebotEvent.PrivateMessageEvent.Sender.newBuilder()
            .setUserId(this.sender.id)
            .setNickname(this.sender.nick)

    // 构造消息链
    val onebotMessage = this.message.toOnebotMessage()

    // 构造rawMessage
    val rawMessage = this.message.toRawMessage()

    // 构造GroupMessageEvent
    return OnebotEvent.PrivateMessageEvent.newBuilder()
            .setTime(System.currentTimeMillis())
            .setSelfId(bot.id)
            .setPostType("message")
            .setMessageType("private")
            .setSubType("normal")
            .setMessageId(message.id)
            .setUserId(this.sender.id)
            .addAllMessage(onebotMessage)
            .setRawMessage(rawMessage)
            .setSender(sender)
            .build()
}

fun MemberJoinEvent.toProtoMessage(): Message {
    return OnebotEvent.GroupIncreaseNoticeEvent.newBuilder()
            .setTime(System.currentTimeMillis())
            .setSelfId(bot.id)
            .setPostType("notice")
            .setNoticeType("group_increase")
            .setGroupId(this.group.id)
            .setUserId(this.member.id)
            .build()
}

fun MemberLeaveEvent.toProtoMessage(): Message {
    val operatorId: Long
    val subType = when (this) {
        is MemberLeaveEvent.Kick -> {
            operatorId = this.operator?.id ?: 0
            "kick"
        }
        is MemberLeaveEvent.Quit -> {
            operatorId = this.member.id
            "leave"
        }
    }
    return OnebotEvent.GroupDecreaseNoticeEvent.newBuilder()
            .setTime(System.currentTimeMillis())
            .setSelfId(bot.id)
            .setPostType("notice")
            .setNoticeType("group_decrease")
            .setSubType(subType)
            .setGroupId(this.group.id)
            .setUserId(this.member.id)
            .setOperatorId(operatorId)
            .build()
}

fun MemberMuteEvent.toProtoMessage(): Message {

}

fun MemberUnmuteEvent.toProtoMessage(): Message {

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
            else -> OnebotBase.Message.newBuilder().setType("unknown").build()
        }
        messageChain.add(message)
    }
    return messageChain
}
