package cantstopthesignal.cryptography

import cantstopthesignal.log.logger
import kotlinx.io.IOException
import org.pgpainless.PGPainless

/*
    This doesn't actually verify the contents of the key just the format , if the format is good it's likely a good key. Someone could put a nonsense key there but that
    will be easily recognizable when someone tries to import the key from their profile
 */
fun isValidOpenPGPPublicKey(publicKey: String): Boolean {


    val trimmedKey = publicKey.trim()
    val convertedKey = convertPgpMessageOrKey(trimmedKey)
    val lines = convertedKey.split("\n")

    for (line in lines) {
        if (line.length > 76 && !line.equals("\n")) {
            return false
        }
    }

    // Check if the key is neither null nor blank

    if (trimmedKey.isBlank()) return false

    val header = "-----BEGIN PGP PUBLIC KEY BLOCK-----"
    val footer = "-----END PGP PUBLIC KEY BLOCK-----"

    // Check the presence and position of the header and footer
    val hasValidHeader = trimmedKey.startsWith(header)

    val hasValidFooter = trimmedKey.endsWith(footer)

    if (!(hasValidHeader && hasValidFooter)) return false

    try {
        val parsedKey = PGPainless.getInstance().readKey().parseCertificate(convertedKey)
    } catch (e: IOException) {
        logger.info { "${e.message} Could not parse PGP public key: $convertedKey" }
        return false
    }

    return true
}

/*
    This function is needed because the basic bitch post form clobbers the base64 PGP key so must restore it to its former glory. This is mostly replacing spaces with line breaks with a bit of extra stuff for header and footer
 */
/*
 We will support people sending pgp keys through chat as well so they dont HAVE to put on on their profile if they don't want to or want to share multiple keys in chats
 */
fun isPgpMessageOrPgpKey(message: String): Boolean {
    return (message.startsWith("-----BEGIN PGP"))
}

/*
    This function is needed because the basic bitch post form clobbers the base64 PGP key so must restore it to its former glory. This is mostly replacing spaces with line breaks with a bit of extra stuff for header and footer
 */
fun convertPgpMessageOrKey(message: String): String {
    val lines = message.trim().split(" ")

    val result = StringBuilder()
    var inHeader = false
    var inFooter = false
    val headerBuffer = StringBuilder()

    for (token in lines) {
        when {
            token.startsWith("-----BEGIN") -> {
                inHeader = true
                headerBuffer.append(token)
            }

            inHeader && token.endsWith("-----") -> {
                headerBuffer.append(" $token")
                result.appendLine(headerBuffer.toString())
                result.appendLine() // blank line after header
                inHeader = false
                headerBuffer.clear()
            }

            inHeader -> headerBuffer.append(" $token")

            token.startsWith("-----END") -> {
                inFooter = true
                headerBuffer.append(token)
            }

            inFooter && token.endsWith("-----") -> {
                headerBuffer.append(" $token")
                result.appendLine(headerBuffer.toString())
                inFooter = false
                headerBuffer.clear()
            }

            inFooter -> headerBuffer.append(" $token")

            else -> result.appendLine(token)
        }
    }

    return result.toString().trim()
}
