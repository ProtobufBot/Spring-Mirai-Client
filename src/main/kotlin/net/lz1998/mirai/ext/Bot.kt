package net.lz1998.mirai.ext

import com.fasterxml.jackson.databind.util.LRUMap
import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.events.BotInvitedJoinGroupRequestEvent
import net.mamoe.mirai.event.events.MemberJoinRequestEvent
import net.mamoe.mirai.event.events.NewFriendRequestEvent
import net.mamoe.mirai.message.data.OnlineMessageSource

// 消息记录，用于撤回
val messageLru = LRUMap<Int, OnlineMessageSource>(128, 2048)
val Bot.messageSourceLru
    get() = messageLru

// 别人加群请求
val memberJoinRequestEventLru = LRUMap<Long, MemberJoinRequestEvent>(16, 128)
val Bot.groupRequestLru
    get() = memberJoinRequestEventLru

// 机器人自己被邀请进群请求
val botInvitedJoinGroupRequestEventLru = LRUMap<Long, BotInvitedJoinGroupRequestEvent>(8, 32)
val Bot.botInvitedGroupRequestLru
    get() = botInvitedJoinGroupRequestEventLru

// 添加好友请求
val newFriendRequestEventLru = LRUMap<Long, NewFriendRequestEvent>(16, 128)
val Bot.friendRequestLru
    get() = newFriendRequestEventLru
