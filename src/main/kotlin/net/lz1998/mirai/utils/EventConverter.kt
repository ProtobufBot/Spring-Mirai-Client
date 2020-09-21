package net.lz1998.mirai.utils

import com.google.protobuf.Message
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.message.*
import net.mamoe.mirai.message.data.*
import onebot.OnebotEvent
import onebot.OnebotFrame

fun BotEvent.toFrame(): Message? = when (this) {
    is GroupMessageEvent -> this.toProtoMessage().toProtoFrame(this.bot.id)
    is FriendMessageEvent -> this.toProtoMessage().toProtoFrame(this.bot.id)
    is MemberJoinEvent -> this.toProtoMessage().toProtoFrame(this.bot.id)
    is MemberLeaveEvent -> this.toProtoMessage().toProtoFrame(this.bot.id)
    else -> null
}

fun GroupMessageEvent.toProtoMessage(): OnebotEvent.GroupMessageEvent {
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

fun FriendMessageEvent.toProtoMessage(): OnebotEvent.PrivateMessageEvent {
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

fun MemberJoinEvent.toProtoMessage(): OnebotEvent.GroupIncreaseNoticeEvent {
    val subType = when (this) {
        is MemberJoinEvent.Invite -> "invite"
        is MemberJoinEvent.Active -> "approve"
        is MemberJoinEvent.Retrieve -> "retrieve"
    }
    return OnebotEvent.GroupIncreaseNoticeEvent.newBuilder()
            .setTime(System.currentTimeMillis())
            .setSelfId(bot.id)
            .setPostType("notice")
            .setNoticeType("group_increase")
            .setSubType(subType)
            .setGroupId(this.group.id)
            .setUserId(this.member.id)
            .build()
}

fun MemberLeaveEvent.toProtoMessage(): OnebotEvent.GroupDecreaseNoticeEvent {
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

fun OnebotEvent.GroupMessageEvent.toProtoFrame(botId: Long): OnebotFrame.Frame = OnebotFrame.Frame.newBuilder().setBotId(botId).setMessageType(OnebotFrame.Frame.MessageType.GroupMessageEvent).setGroupMessageEvent(this).build()
fun OnebotEvent.PrivateMessageEvent.toProtoFrame(botId: Long): OnebotFrame.Frame = OnebotFrame.Frame.newBuilder().setBotId(botId).setMessageType(OnebotFrame.Frame.MessageType.PrivateMessageEvent).setPrivateMessageEvent(this).build()
fun OnebotEvent.GroupIncreaseNoticeEvent.toProtoFrame(botId: Long): OnebotFrame.Frame = OnebotFrame.Frame.newBuilder().setBotId(botId).setMessageType(OnebotFrame.Frame.MessageType.GroupIncreaseNoticeEvent).setGroupIncreaseNoticeEvent(this).build()
fun OnebotEvent.GroupDecreaseNoticeEvent.toProtoFrame(botId: Long): OnebotFrame.Frame = OnebotFrame.Frame.newBuilder().setBotId(botId).setMessageType(OnebotFrame.Frame.MessageType.GroupDecreaseNoticeEvent).setGroupDecreaseNoticeEvent(this).build()



