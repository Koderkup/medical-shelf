package com.example.medical_shelf.domain.licensing

import android.content.Context
import android.provider.Settings
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object LicenseManager {

    private const val ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
    private const val SECRET_KEY = "MED_SHELF_PRO_SECRET_KEY_2026_OFFLINE_RUSTORE"

    fun getDeviceCode(context: Context): String {
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "DEFAULT_MED_SHELF_DEVICE"

        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(androidId.toByteArray(Charsets.UTF_8))
        return formatCode(hash)
    }

    fun generateLicenseKey(deviceCode: String): String {
        val normalizedCode = deviceCode.replace("-", "").uppercase()
        val mac = Mac.getInstance("HmacSHA256")
        val secretKeySpec = SecretKeySpec(SECRET_KEY.toByteArray(Charsets.UTF_8), "HmacSHA256")
        mac.init(secretKeySpec)
        val hmacBytes = mac.doFinal(normalizedCode.toByteArray(Charsets.UTF_8))
        val raw8Chars = formatCode(hmacBytes).replace("-", "")
        return "MP-${raw8Chars.substring(0, 4)}-${raw8Chars.substring(4, 8)}"
    }

    fun verifyLicenseKey(deviceCode: String, key: String): Boolean {
        val cleanedInput = key.trim().uppercase()
        val expectedKey = generateLicenseKey(deviceCode)
        return cleanedInput == expectedKey
    }

    private fun formatCode(bytes: ByteArray): String {
        val chars = CharArray(8)
        val alphabetLen = ALPHABET.length
        for (i in 0 until 8) {
            val byteVal = (bytes[i].toInt() and 0xFF)
            chars[i] = ALPHABET[byteVal % alphabetLen]
        }
        return "${String(chars, 0, 4)}-${String(chars, 4, 4)}"
    }
}