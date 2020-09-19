package net.lz1998.mirai.service

import kotlinx.coroutines.CompletableDeferred
import net.mamoe.mirai.Bot
import net.mamoe.mirai.utils.LoginSolver
import org.springframework.stereotype.Service

@Service
class LoginService : LoginSolver() {
    // TODO 通过轮询查询 loginMap
    val loginMap = mutableMapOf<Long, LoginData>()

    override suspend fun onSolvePicCaptcha(bot: Bot, data: ByteArray): String? {
        val def = CompletableDeferred<String>()
        loginMap[bot.id] = LoginData(LoginDataType.PIC_CAPTCHA, def, data, null)
        return def.await().trim()
    }

    override suspend fun onSolveSliderCaptcha(bot: Bot, url: String): String? {
        val def = CompletableDeferred<String>()
        loginMap[bot.id] = LoginData(LoginDataType.SLIDER_CAPTCHA, def, null, url)
        return def.await().trim()
    }

    override suspend fun onSolveUnsafeDeviceLoginVerify(bot: Bot, url: String): String? {
        val def = CompletableDeferred<String>()
        loginMap[bot.id] = LoginData(LoginDataType.UNSAFE_DEVICE_LOGIN_VERIFY, def, null, url)
        return def.await().trim()
    }

    fun solveLogin(botId: Long, result: String) {
        loginMap[botId]?.def?.complete(result)
    }

    fun getLoginData(botId: Long): LoginData? {
        return loginMap[botId]
    }
}

data class LoginData(
        val type: LoginDataType,
        val def: CompletableDeferred<String>,
        var data: ByteArray?,
        val url: String?
)

enum class LoginDataType(val type: String) {
    PIC_CAPTCHA("pic_captcha"),
    SLIDER_CAPTCHA("slider_captcha"),
    UNSAFE_DEVICE_LOGIN_VERIFY("unsafe_device_login)verify"),
}