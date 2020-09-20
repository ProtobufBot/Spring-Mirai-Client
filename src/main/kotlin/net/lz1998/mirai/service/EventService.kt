package net.lz1998.mirai.service

import com.google.protobuf.Message
import com.google.protobuf.util.JsonFormat
import org.springframework.stereotype.Service

// 处理 Bot 收到的 Event，发送给server
@Service
class EventService {
    fun handleEvent(botId: Long, event: Message) {
        val eventStr = JsonFormat.printer().print(event)
        println(eventStr)
    }

}