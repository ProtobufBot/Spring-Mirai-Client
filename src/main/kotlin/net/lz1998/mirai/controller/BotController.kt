package net.lz1998.mirai.controller

import dto.HttpDto
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.lz1998.mirai.service.BotService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/bot")
class BotController {

    @Autowired
    lateinit var botService: BotService


    // 创建一个机器人并登陆
    @RequestMapping("/create/v1", produces = ["application/x-protobuf"], consumes = ["application/x-protobuf"])
    fun createBot(@RequestBody param: HttpDto.CreateBotReq): HttpDto.CreateBotResp {
        botService.createBot(param.botId, param.password)
        return HttpDto.CreateBotResp.newBuilder().build()
    }

    @RequestMapping("/list/v1", produces = ["application/x-protobuf"], consumes = ["application/x-protobuf"])
    fun listBot(@RequestBody param: HttpDto.ListBotReq): HttpDto.ListBotResp {
        val botList = botService.listBot()
        return HttpDto.ListBotResp.newBuilder().addAllBotList(botList).build()
    }

    @RequestMapping("/login/v1", produces = ["application/x-protobuf"], consumes = ["application/x-protobuf"])
    fun botLoginAsync(@RequestBody param: HttpDto.BotLoginAsyncReq): HttpDto.BotLoginAsyncResp {
        GlobalScope.launch {
            botService.botLogin(param.botId)
        }
        return HttpDto.BotLoginAsyncResp.newBuilder().build()
    }

}