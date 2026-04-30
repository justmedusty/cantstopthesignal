package cantstopthesignal.cryptography

fun isValidOpenPGPPublicKey(publicKey: String): Boolean {

    val trimmedKey = publicKey.trim() // Trim once and reuse

    // Check if the key is neither null nor blank

    if (trimmedKey.isBlank()) return false

    val header = "-----BEGIN PGP PUBLIC KEY BLOCK-----"
    val footer = "-----END PGP PUBLIC KEY BLOCK-----"

    // Check the presence and position of the header and footer
    val hasValidHeader = trimmedKey.startsWith(header)

    val hasValidFooter = trimmedKey.endsWith(footer)

    if (!(hasValidHeader && hasValidFooter)) return false

    // Check for actual content between header and footer
    val content = trimmedKey.substringAfter(header).substringBeforeLast(footer).trim()
    return content.isNotEmpty()
}