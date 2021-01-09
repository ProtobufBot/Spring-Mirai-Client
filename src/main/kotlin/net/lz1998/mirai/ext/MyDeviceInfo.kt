package net.lz1998.mirai.ext

import kotlinx.io.core.toByteArray
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.mamoe.mirai.utils.*
import java.io.File


import kotlin.jvm.JvmSynthetic
import kotlin.random.Random
import kotlin.random.nextInt


/**
 * 加载一个设备信息. 若文件不存在或为空则随机并创建一个设备信息保存.
 */
public fun File.loadAsMyDeviceInfo(json: Json): MyDeviceInfo {
    if (!this.exists() || this.length() == 0L) {
        return MyDeviceInfo.random().also {
            this.writeText(json.encodeToString(MyDeviceInfo.serializer(), it))
        }
    }
    return json.decodeFromString(MyDeviceInfo.serializer(), this.readText())
}

@Serializable
public class MyDeviceInfo(
        public val display: String,
        public val product: String,
        public val device: String,
        public val board: String,
        public val brand: String,
        public val model: String,
        public val bootloader: String,
        public val fingerprint: String,
        public val bootId: String,
        public val procVersion: String,
        public val baseBand: String,
        public val version: Version,
        public val simInfo: String,
        public val osType: String,
        public val macAddress: String,
        public val wifiBSSID: String,
        public val wifiSSID: String,
        public val imsiMd5: ByteArray,
        public val imei: String,
        public val apn: String,
        public val protocol: BotConfiguration.MiraiProtocol
) {
    public val androidId: String get() = display
    public val ipAddress: ByteArray get() = byteArrayOf(192.toByte(), 168.toByte(), 1, 123)

    @Serializable
    public class Version(
            public val incremental: String = "5891938",
            public val release: String = "10",
            public val codename: String = "REL",
            public val sdk: Int = 29
    )

    public companion object {
        @JvmStatic
        public fun random(): MyDeviceInfo {
            return MyDeviceInfo(
                    display = "SMC.${getRandomString(6, '0'..'9')}.001",
                    product = "smc",
                    device = "smc",
                    board = "smc",
                    brand = "pbbot",
                    model = "smc",
                    bootloader = "unknown",
                    fingerprint = "pbbot/smc/smc:10/SMC.200122.001/${getRandomIntString(7)}:user/release-keys",
                    bootId = generateUUID(getRandomByteArray(16).md5()),
                    procVersion = "Linux version 3.0.31-${getRandomString(8)} (android-build@gmail.com)",
                    baseBand = "",
                    version = Version(),
                    simInfo = "T-Mobile",
                    osType = "android",
                    macAddress = "02:00:00:00:00:00",
                    wifiBSSID = "02:00:00:00:00:00",
                    wifiSSID = "KFC FREE WIFI",
                    imsiMd5 = getRandomByteArray(16).md5(),
                    imei = getRandomIntString(15),
                    apn = "wifi",
                    protocol = BotConfiguration.MiraiProtocol.ANDROID_PAD
            )
        }
    }
}

public fun MyDeviceInfo.generateDeviceInfoData(): DeviceInfo {
    return DeviceInfo(
            display = display.toByteArray(),
            product = product.toByteArray(),
            device = device.toByteArray(),
            board = board.toByteArray(),
            brand = brand.toByteArray(),
            model = model.toByteArray(),
            bootloader = bootloader.toByteArray(),
            fingerprint = fingerprint.toByteArray(),
            bootId = bootId.toByteArray(),
            procVersion = procVersion.toByteArray(),
            baseBand = baseBand.toByteArray(),
            version = DeviceInfo.Version(
                    incremental = version.incremental.toByteArray(),
                    release = version.release.toByteArray(),
                    codename = version.codename.toByteArray(),
                    sdk = version.sdk
            ),
            simInfo = simInfo.toByteArray(),
            osType = osType.toByteArray(),
            macAddress = macAddress.toByteArray(),
            wifiBSSID = wifiBSSID.toByteArray(),
            wifiSSID = wifiSSID.toByteArray(),
            imsiMd5 = imsiMd5,
            imei = imei,
            apn = apn.toByteArray()
    )

}


/**
 * 生成长度为 [length], 元素为随机 `0..255` 的 [ByteArray]
 */
@JvmSynthetic
internal fun getRandomByteArray(length: Int): ByteArray = ByteArray(length) { Random.nextInt(0..255).toByte() }

/**
 * 随机生成长度为 [length] 的 [String].
 */
@JvmSynthetic
internal fun getRandomString(length: Int): String =
        getRandomString(length, *defaultRanges)

private val defaultRanges: Array<CharRange> = arrayOf('a'..'z', 'A'..'Z', '0'..'9')
private val intCharRanges: Array<CharRange> = arrayOf('0'..'9')

/**
 * 根据所给 [charRange] 随机生成长度为 [length] 的 [String].
 */
@JvmSynthetic
internal fun getRandomString(length: Int, charRange: CharRange): String =
        CharArray(length) { charRange.random() }.concatToString()

/**
 * 根据所给 [charRanges] 随机生成长度为 [length] 的 [String].
 */
@JvmSynthetic
internal fun getRandomString(length: Int, vararg charRanges: CharRange): String =
        CharArray(length) { charRanges[Random.Default.nextInt(0..charRanges.lastIndex)].random() }.concatToString()


@JvmSynthetic
internal fun getRandomIntString(length: Int): String =
        getRandomString(length, *intCharRanges)
