package net.lz1998.mirai.ext

import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import net.mamoe.mirai.utils.BotConfigurationBase
import net.mamoe.mirai.utils.Context
import net.mamoe.mirai.utils.ExternalImage
import net.mamoe.mirai.utils.BotConfiguration
import net.mamoe.mirai.utils.ContextImpl
import net.mamoe.mirai.utils.DeviceInfo
import java.io.File
import java.security.MessageDigest
import kotlin.random.Random
import kotlin.random.nextInt


@Serializable
open class MyDeviceInfo() : DeviceInfo() {
    constructor(context: Context) : this() {
        this.context = context
    }

    @Transient
    final override lateinit var context: Context
    override val androidId: ByteArray
        get() = androidIdStr.toByteArray()

    override val apn: ByteArray
        get() = apnStr.toByteArray()

    override val baseBand: ByteArray
        get() = byteArrayOf()

    override val board: ByteArray
        get() = boardStr.toByteArray()

    override val bootId: ByteArray
        get() = bootIdStr.toByteArray()

    override val bootloader: ByteArray
        get() = bootloaderStr.toByteArray()

    override val brand: ByteArray
        get() = brandStr.toByteArray()

    override val device: ByteArray
        get() = deviceStr.toByteArray()

    override val display: ByteArray
        get() = displayStr.toByteArray()

    override val fingerprint: ByteArray
        get() = fingerprintStr.toByteArray()

    override val imei: String
        get() = imeiStr

    override val macAddress: ByteArray
        get() = macAddressStr.toByteArray()

    override val model: ByteArray
        get() = modelStr.toByteArray()

    override val osType: ByteArray
        get() = osTypeStr.toByteArray()

    override val procVersion: ByteArray
        get() = procVersionStr.toByteArray()

    override val product: ByteArray
        get() = productStr.toByteArray()

    override val simInfo: ByteArray
        get() = simInfoStr.toByteArray()

    override val wifiBSSID: ByteArray?
        get() = wifiBSSIDStr.toByteArray()

    override val wifiSSID: ByteArray?
        get() = wifiSSIDStr.toByteArray()
    override val version: Version
        get() = Version


    @Serializable
    object Version : DeviceInfo.Version {
        override val incremental: ByteArray = "5891938".toByteArray()
        override val release: ByteArray = "10".toByteArray()
        override val codename: ByteArray = "REL".toByteArray()
        override val sdk: Int = 29
    }

    @Required
    @SerialName("androidId")
    var androidIdStr: String = "SMC.${getRandomString(6, '0'..'9')}.001"

    @Required
    @SerialName("apn")
    var apnStr: String = "wifi"

    @Required
    @SerialName("board")
    var boardStr: String = "smc"

    @SerialName("bootId")
    @Required
    var bootIdStr: String = ExternalImage.generateUUID(getRandomByteArray(16).md5(0, 16))

    @Required
    @SerialName("bootloader")
    var bootloaderStr: String = "unknown"

    @Required
    @SerialName("brand")
    var brandStr: String = "pbbot"

    @Required
    @SerialName("device")
    var deviceStr: String = "smc"

    @Required
    @SerialName("display")
    var displayStr: String = androidIdStr

    @Required
    @SerialName("fingerprint")
    var fingerprintStr: String = "pbbot/smc/smc:10/SMC.200122.001/${getRandomString(7, '0'..'9')}:user/release-keys"

    @Required
    @SerialName("macAddress")
    var macAddressStr: String = "02:00:00:00:00:00"

    @Required
    @SerialName("model")
    var modelStr: String = "smc"

    @Required
    @SerialName("osType")
    var osTypeStr: String = "android"

    @Required
    @SerialName("procVersion")
    var procVersionStr: String = "Linux version 3.0.31-${getRandomString(8, 'a'..'z')} (android-build@xxx.xxx.xxx.xxx.com)"

    @Required
    @SerialName("product")
    var productStr: String = "smc"

    @Required
    @SerialName("simInfo")
    var simInfoStr: String = "T-Mobile"

    @Required
    @SerialName("wifiBSSID")
    var wifiBSSIDStr: String = "02:00:00:00:00:00"

    @Required
    @SerialName("wifiSSID")
    var wifiSSIDStr: String = "<unknown ssid>"

    @Required
    @SerialName("imei")
    var imeiStr: String = getRandomString(15, '0'..'9')

    @Required
    @SerialName("imsiMd5")
    override val imsiMd5: ByteArray = getRandomByteArray(16).md5(0, 16)

}

@JvmOverloads
@BotConfigurationBase.ConfigurationDsl
fun BotConfiguration.fileStrBasedDeviceInfo(filepath: String = "device.json") {
    deviceInfo = getFileStrBasedDeviceInfoSupplier(filepath)
}

fun BotConfiguration.getFileStrBasedDeviceInfoSupplier(filename: String): ((Context) -> DeviceInfo)? {
    return {
        File(filename).loadStrAsDeviceInfo(json, it)
    }
}

/**
 * 加载一个设备信息. 若文件不存在或为空则随机并创建一个设备信息保存.
 */
fun File.loadStrAsDeviceInfo(json: Json, context: Context = ContextImpl()): DeviceInfo {
    if (!this.exists() || this.length() == 0L) {
        return MyDeviceInfo(context).also {
            if (!this.parentFile.exists()) {
                this.parentFile.mkdirs()
            }
            this.writeText(json.encodeToString(MyDeviceInfo.serializer(), it))
        }
    }
    return json.decodeFromString(MyDeviceInfo.serializer(), this.readText()).also {
        it.context = context
    }
}


fun getRandomString(length: Int, charRange: CharRange): String =
        String(CharArray(length) { charRange.random() })

fun getRandomByteArray(length: Int): ByteArray = ByteArray(length) { Random.nextInt(0..255).toByte() }

fun ByteArray.md5(offset: Int, length: Int): ByteArray {
    this.checkOffsetAndLength(offset, length)
    return MessageDigest.getInstance("MD5").apply { update(this@md5, offset, length) }.digest()
}

fun ByteArray.checkOffsetAndLength(offset: Int, length: Int) {
    require(offset >= 0) { "offset shouldn't be negative: $offset" }
    require(length >= 0) { "length shouldn't be negative: $length" }
    require(offset + length <= this.size) { "offset ($offset) + length ($length) > array.size (${this.size})" }
}
