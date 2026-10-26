package panattoni.simo.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Utility per la crittografia hardware-backed tramite Android KeyStore.
 *
 * Caratteristiche:
 * 1. Chiave AES a 256 bit generata e custodita nell'AndroidKeyStore hardware (TEE/StrongBox).
 * 2. Cifratura autenticata AES/GCM/NoPadding (con tag di integrità a 128 bit).
 * 3. La chiave crittografica è hardware-bound al singolo dispositivo: non può essere esportata
 *    né estratta. Se i dati o le SharedPreferences venissero copiate su un altro telefono,
 *    la chiave rimarrebbe inaccessibile e inutilizzabile.
 * 4. La chiave API non viene mai inclusa nei backup JSON o nei backup cloud/device-transfer.
 */
object CryptoUtils{

    private const val ANDROID_KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "simo_gemini_master_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val IV_SEPARATOR = "]"

    private val keyStore: KeyStore by lazy{
        KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER).apply{
            load(null)
        }
    }

    @Synchronized
    private fun getOrCreateSecretKey(): SecretKey{
        if (keyStore.containsAlias(KEY_ALIAS)){
            val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (entry != null){
                return entry.secretKey
            }
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE_PROVIDER
        )

        val keyGenSpecBuilder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)

        keyGenerator.init(keyGenSpecBuilder.build())
        return keyGenerator.generateKey()
    }

    /**
     * Cifra una stringa in chiaro (es. Gemini API Key) restituendo una stringa
     * Base64 composta da: Base64(IV) + "]" + Base64(Ciphertext+Tag).
     */
    fun encrypt(plainText: String): String{
        if (plainText.isBlank()) return ""
        return try{
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            val cipherTextBase64 = Base64.encodeToString(cipherText, Base64.NO_WRAP)
            "$ivBase64$IV_SEPARATOR$cipherTextBase64"
        } catch (e: Exception){
            ""
        }
    }

    /**
     * Decifra la stringa cifrata. Se la decifratura fallisce (es. file corrotto o spostato
     * su altro dispositivo con KeyStore differente), restituisce stringa vuota.
     */
    fun decrypt(encryptedPayload: String): String{
        if (encryptedPayload.isBlank()) return ""
        return try{
            val parts = encryptedPayload.split(IV_SEPARATOR)
            if (parts.size != 2) return ""

            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val cipherText = Base64.decode(parts[1], Base64.NO_WRAP)

            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val decryptedBytes = cipher.doFinal(cipherText)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception){
            ""
        }
    }
}
