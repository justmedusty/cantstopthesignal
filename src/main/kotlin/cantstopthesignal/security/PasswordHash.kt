package cantstopthesignal.security

import at.favre.lib.crypto.bcrypt.BCrypt

fun hashPassword(password: String): String {
    return BCrypt.hashpw(password, BCrypt.gensalt())
}