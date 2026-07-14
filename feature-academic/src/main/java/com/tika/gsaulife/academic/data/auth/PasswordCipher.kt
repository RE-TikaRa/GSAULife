package com.tika.gsaulife.academic.data.auth

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

internal object PasswordCipher {
    private const val CHARS = "ABCDEFGHJKMNPQRSTWXYZabcdefhijkmnprstwxyz2345678"
    private val random = SecureRandom()

    fun encrypt(plain: String, salt: String): String {
        val data = (randomString(64) + plain).toByteArray()
        val key = SecretKeySpec(salt.toByteArray(), "AES")
        val iv = IvParameterSpec(randomString(16).toByteArray())
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, key, iv)
        return Base64.encodeToString(cipher.doFinal(data), Base64.NO_WRAP)
    }

    private fun randomString(length: Int): String =
        buildString(length) { repeat(length) { append(CHARS[random.nextInt(CHARS.length)]) } }
}
