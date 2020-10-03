package net.lz1998.mirai.utils

import net.lz1998.mirai.alias.*
import net.lz1998.mirai.ext.friendRequestLru
import net.lz1998.mirai.ext.groupRequestLru
import net.lz1998.mirai.ext.messageSourceLru
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.MemberPermission
import net.mamoe.mirai.getFriendOrNull
import net.mamoe.mirai.getGroupOrNull
import net.mamoe.mirai.message.data.asMessageChain


suspend fun handleSendPrivateMsg(bot: Bot, req: BSendPrivateMsgReq): BSendPrivateMsgResp? {
    val contact = bot.getFriendOrNull(req.userId) ?: return null
    val messageChain = req.messageList.map { it.toMiraiMessage(bot, contact) }.asMessageChain()
    val messageSource = contact.sendMessage(messageChain).source
    bot.messageSourceLru.put(messageSource.id, messageSource)
    return BSendPrivateMsgResp.newBuilder().setMessageId(messageSource.id).build()
}

suspend fun handleSendGroupMsg(bot: Bot, req: BSendGroupMsgReq): BSendGroupMsgResp? {
    val contact = bot.getGroupOrNull(req.groupId) ?: return null
    val messageChain = req.messageList.map { it.toMiraiMessage(bot, contact) }.asMessageChain()
    val messageSource = contact.sendMessage(messageChain).source
    bot.messageSourceLru.put(messageSource.id, messageSource)
    return BSendGroupMsgResp.newBuilder().setMessageId(messageSource.id).build()
}

suspend fun handleSendMsgReq(bot: Bot, req: BSendMsgReq): BSendMsgResp? {
    val contact = when (req.messageType) {
        "group" -> {
            bot.getGroupOrNull(req.groupId)
        }
        "private" -> {
            bot.getFriendOrNull(req.userId)
        }
        else -> {
            bot.getGroupOrNull(req.groupId) ?: bot.getFriendOrNull(req.userId)
        }
    } ?: return null
    val messageChain = req.messageList.map { it.toMiraiMessage(bot, contact) }.asMessageChain()
    val messageSource = contact.sendMessage(messageChain).source
    bot.messageSourceLru.put(messageSource.id, messageSource)
    return BSendMsgResp.newBuilder().setMessageId(messageSource.id).build()
}

suspend fun handleDeleteMsg(bot: Bot, req: BDeleteMsgReq): BDeleteMsgResp? {
    val messageSource = bot.messageSourceLru[req.messageId] ?: return null
    bot.recall(messageSource)
    return BDeleteMsgResp.newBuilder().build()
}

suspend fun handleSetGroupKick(bot: Bot, req: BSetGroupKickReq): BSetGroupKickResp? {
    val group = bot.getGroupOrNull(req.groupId) ?: return null
    val member = group.getOrNull(req.userId) ?: return null
    member.kick()
    return BSetGroupKickResp.newBuilder().build()
}

suspend fun handleSetGroupBan(bot: Bot, req: BSetGroupBanReq): BSetGroupBanResp? {
    val group = bot.getGroupOrNull(req.groupId) ?: return null
    val member = group.getOrNull(req.userId) ?: return null
    if (req.duration == 0) {
        member.unmute()
    } else {
        member.mute(req.duration)
    }
    return BSetGroupBanResp.newBuilder().build()
}

suspend fun handleSetGroupWholeBan(bot: Bot, req: BSetGroupWholeBanReq): BSetGroupWholeBanResp? {
    val group = bot.getGroupOrNull(req.groupId) ?: return null
    group.settings.isMuteAll = req.enable
    return BSetGroupWholeBanResp.newBuilder().build()
}

suspend fun handleSetGroupCard(bot: Bot, req: BSetGroupCardReq): BSetGroupCardResp? {
    val group = bot.getGroupOrNull(req.groupId) ?: return null
    val member = group.getOrNull(req.userId) ?: return null
    member.nameCard = req.card
    return BSetGroupCardResp.newBuilder().build()
}

suspend fun handleSetGroupName(bot: Bot, req: BSetGroupNameReq): BSetGroupNameResp? {
    val group = bot.getGroupOrNull(req.groupId) ?: return null
    group.name = req.groupName
    return BSetGroupNameResp.newBuilder().build()
}

suspend fun handleSetGroupLeave(bot: Bot, req: BSetGroupLeaveReq): BSetGroupLeaveResp? {
    val group = bot.getGroupOrNull(req.groupId) ?: return null
    group.quit()
    return BSetGroupLeaveResp.newBuilder().build()
}

suspend fun handleSetGroupSpecialTitle(bot: Bot, req: BSetGroupSpecialTitleReq): BSetGroupSpecialTitleResp? {
    val group = bot.getGroupOrNull(req.groupId) ?: return null
    val member = group.getOrNull(req.userId) ?: return null
    member.specialTitle = req.specialTitle

    return BSetGroupSpecialTitleResp.newBuilder().build()
}

suspend fun handleSetFriendAddRequest(bot: Bot, req: BSetFriendAddRequestReq): BSetFriendAddRequestResp? {
    val approve = req.approve
    val flag = req.flag
    val request = bot.friendRequestLru[flag.toLongOrNull()] ?: return null
    if (approve) request.accept() else request.reject()
    return BSetFriendAddRequestResp.newBuilder().build()
}

suspend fun handleSetGroupAddRequest(bot: Bot, req: BSetGroupAddRequestReq): BSetGroupAddRequestResp? {
    val approve = req.approve
    val flag = req.flag
    val request = bot.groupRequestLru[flag.toLongOrNull()] ?: return null
    if (approve) request.accept() else request.reject()
    return BSetGroupAddRequestResp.newBuilder().build()
}

suspend fun handleGetLoginInfo(bot: Bot, req: BGetLoginInfoReq): BGetLoginInfoResp {
    return BGetLoginInfoResp.newBuilder().setUserId(bot.id).setNickname(bot.nick).build()
}

suspend fun handleGetFriendList(bot: Bot, req: BGetFriendListReq): BGetFriendListResp {
    val friendList = bot.friends.map { friend ->
        BGetFriendListFriend.newBuilder()
                .setUserId(friend.id)
                .setNickname(friend.nick)
                .build()
    }
    return BGetFriendListResp.newBuilder().addAllFriendList(friendList).build()
}

suspend fun handleGetGroupInfo(bot: Bot, req: BGetGroupInfoReq): BGetGroupInfoResp? {
    val group = bot.getGroupOrNull(req.groupId) ?: return null
    return BGetGroupInfoResp.newBuilder().setGroupId(group.id).setGroupName(group.name).setMemberCount(group.members.size + 1).build()
}

suspend fun handleGetGroupList(bot: Bot, req: BGetGroupListReq): BGetGroupListResp {
    val groupList = bot.groups.map { group ->
        BGetGroupListGroup.newBuilder()
                .setGroupId(group.id)
                .setGroupName(group.name)
                .setMemberCount(group.members.size + 1)
                .build()
    }
    return BGetGroupListResp.newBuilder().addAllGroupList(groupList).build()
}

suspend fun handleGetGroupMemberInfo(bot: Bot, req: BGetGroupMemberInfoReq): BGetGroupMemberInfoResp? {
    val group = bot.getGroupOrNull(req.groupId) ?: return null
    val member = group.getOrNull(req.userId) ?: return null
    return BGetGroupMemberInfoResp.newBuilder()
            .setGroupId(group.id)
            .setUserId(member.id)
            .setNickname(member.nick)
            .setCard(member.nameCard)
            .setRole(member.permission.name)
            .setTitle(member.specialTitle)
            .setCardChangeable(group.botPermission.level > MemberPermission.MEMBER.level)
            .build()
}

suspend fun handleGetGroupMemberList(bot: Bot, req: BGetGroupMemberListReq): BGetGroupMemberListResp? {
    val group = bot.getGroupOrNull(req.groupId) ?: return null
    val groupMemberList = group.members.map { member ->
        BGetGroupMemberListGroupMember.newBuilder()
                .setGroupId(group.id)
                .setUserId(member.id)
                .setNickname(member.nick)
                .setCard(member.nameCard)
                .setRole(member.permission.name)
                .setTitle(member.specialTitle)
                .setCardChangeable(group.botPermission.level > MemberPermission.MEMBER.level)
                .build()
    }
    return BGetGroupMemberListResp.newBuilder().addAllGroupMemberList(groupMemberList).build()
}

