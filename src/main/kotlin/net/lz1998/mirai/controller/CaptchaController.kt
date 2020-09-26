package net.lz1998.mirai.controller

import dto.HttpDto
import net.lz1998.mirai.service.MyLoginSolver
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/captcha")
class CaptchaController {

    @Autowired
    lateinit var myLoginSolver: MyLoginSolver

    @RequestMapping("/list/v1", produces = ["application/x-protobuf"], consumes = ["application/x-protobuf"])
    fun getCaptchaList(): HttpDto.GetCaptchaListResp {
        val captchaList = myLoginSolver.getCaptchaList()
        return HttpDto.GetCaptchaListResp.newBuilder().addAllCaptchaList(captchaList).build()
    }

    @RequestMapping("/solve/v1", produces = ["application/x-protobuf"], consumes = ["application/x-protobuf"])
    fun solveCaptcha(@RequestBody param: HttpDto.SolveCaptchaReq): HttpDto.SolveCaptchaResp {
        myLoginSolver.solveLogin(param.botId, param.result)
        return HttpDto.SolveCaptchaResp.newBuilder().build()
    }
}