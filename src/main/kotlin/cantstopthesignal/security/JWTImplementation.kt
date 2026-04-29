package cantstopthesignal.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.*

/**
 * J w t config
 *
 * @property audience
 * @property domain
 * @property secret
 * @property id
 * @property expiresInMS
 * @constructor Create empty J w t config
 */
data class JWTConfig(
    val audience: String,
    val domain: String,
    val secret: String,
    val id: Long,
    val expiresInMS: Long,
)

/**
 * Create j w t token
 *
 * @param jwtConfig
 * @return
 */
fun createJWT(jwtConfig: JWTConfig): String {
    return JWT.create()
        .withAudience(jwtConfig.audience)
        .withIssuer(jwtConfig.domain)
        .withExpiresAt(Date(System.currentTimeMillis() + jwtConfig.expiresInMS))
        .withSubject(jwtConfig.id.toString())
        .sign(Algorithm.HMAC256(System.getenv("JWT_SECRET")))
}