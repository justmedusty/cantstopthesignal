package cantstopthesignal.cryptography

import cantstopthesignal.database.users.getPublicKey
import cantstopthesignal.database.users.getUserId
import cantstopthesignal.log.logger
import cantstopthesignal.applicationScope
import io.ktor.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bouncycastle.openpgp.api.OpenPGPCertificate
import org.bouncycastle.util.io.Streams
import org.pgpainless.PGPainless
import org.pgpainless.decryption_verification.ConsumerOptions
import org.pgpainless.decryption_verification.DecryptionStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.time.Duration
import java.time.ZoneOffset
import java.util.*
import kotlin.time.Duration.Companion.milliseconds


/*
    This will be for PGP challenge based login or password recovery. I will make PGP login a configurable option
 */

data class PgpSignatureLoginChallenge(
    val userPublicKey: OpenPGPCertificate,
    val challengeString: String,
)

val pgpChallengeHashSet: MutableMap<String, PgpSignatureLoginChallenge> = HashMap()

fun verifySignature(username: String, message: String): Boolean {
    try {
        val api = PGPainless.getInstance()

        val inputStream: InputStream = ByteArrayInputStream(message.toByteArray())
        val outputStream = ByteArrayOutputStream()
        val publicKey = pgpChallengeHashSet[username]?.userPublicKey ?: return false

        val decryptionStream: DecryptionStream =
            PGPainless.getInstance().processMessage().onInputStream(inputStream).withOptions(
                ConsumerOptions.get(api)
                    .addVerificationCert(publicKey)
            )

        Streams.pipeAll(decryptionStream, outputStream)
        decryptionStream.close()

        val metadata = decryptionStream.metadata

        if (!metadata.isVerifiedSigned()) {
            return false
        }
        logger.debug { "Signature verification complete, valid signature" }

        logger.debug { "Signature verification against public key on file ..." }
        logger.debug {
            "signing key : ${
                metadata.verifiedSignatures[0].signingKey.toString().trim()
                    .split(" ")[0].toUpperCasePreservingASCIIRules()
            } public key fingerprint ${publicKey.fingerprint.toHexString().toUpperCasePreservingASCIIRules()}"
        }

        val publicFingerprint = publicKey.fingerprint.toHexString().toUpperCasePreservingASCIIRules()

        val privateFingerPrint =
            metadata.verifiedSignatures[0].signingKey.toString().trim().split(" ")[0].toUpperCasePreservingASCIIRules()

        if (!publicFingerprint.equals(privateFingerPrint)) {
            return false
        }
        var recovered = outputStream.toString(Charsets.UTF_8.name())
        logger.debug { "Signature verification against expected challenge string..." }
        val expectedMessage = pgpChallengeHashSet[username]?.challengeString ?: return false
        logger.debug { "Signature verification success : ${expectedMessage == recovered}" }

        recovered = when {
            recovered.endsWith("\r\n") -> recovered.dropLast(2)
            recovered.endsWith("\n") -> recovered.dropLast(1)
            recovered.endsWith("\r") -> recovered.dropLast(1)
            else -> recovered
        }

        return (recovered == expectedMessage)
    } catch (e: Exception) {
        logger.info(e) { "Signature verification failed" }
        return false
    }
}

/*
    Register a challenge, generates a random UUID
 */
fun registerNewChallenge(user: String): String? /* Successful creation or not */ {

    val challengeString = (UUID.randomUUID().toString() + UUID.randomUUID().toString() + java.time.LocalDateTime.now()
        .toEpochSecond(ZoneOffset.UTC))
    val userId = getUserId(user) ?: return null
    val publicKey = getPublicKey(userId) ?: return null

    val signatureLoginChallenge = PgpSignatureLoginChallenge(
        PGPainless.getInstance().readKey().parseCertificate(publicKey),
        challengeString,
    )

    pgpChallengeHashSet[user] = signatureLoginChallenge

    // Schedule expiry
    applicationScope.launch {
        delay(Duration.ofMinutes(5).toMillis().milliseconds)
        // Only remove if it's still the same challenge (not replaced/consumed)
        pgpChallengeHashSet.remove(user, signatureLoginChallenge)
    }

    return challengeString
}