package cantstopthesignal.routing.login

import cantstopthesignal.cryptography.*
import cantstopthesignal.database.sitewide_permissions.isInviteOnlyEnabled
import cantstopthesignal.database.users.getPublicKey
import cantstopthesignal.database.users.getUserId
import cantstopthesignal.database.users.userNameAlreadyExists
import cantstopthesignal.database.users.verifyCredentials
import cantstopthesignal.enums.Length
import cantstopthesignal.enums.ThymeLeafMapKeys
import cantstopthesignal.log.logger
import cantstopthesignal.security.JWTConfig
import cantstopthesignal.security.createJWT
import cantstopthesignal.security.jwtSecret
import cantstopthesignal.siteConfig
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.*

fun Application.configureLoginRoutes() {
    routing {

        get("/login") {

            val error = call.request.queryParameters["error"]
            val success = call.request.queryParameters["success"]
            if (siteConfig?.pgpLoginOnly == true) {
                return@get call.respondRedirect("/login/pgpchallenge${if (success == null) "" else "?success=$success"}${if (error == null) "" else "?error=$error"}")
            }

            val map = buildMap {
                put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                put(ThymeLeafMapKeys.SITE_INVITE_ONLY.value, isInviteOnlyEnabled())
                /* These values can be passed as query params to avoid doing a ton of setup in other call routines, its easier to redirect with a query param instead of duplicating code everywhere */
                if (error != null) {
                    put(ThymeLeafMapKeys.ERROR.value, error)
                }

                if (success != null) {
                    put(ThymeLeafMapKeys.SUCCESS.value, success)
                }
            }
            return@get call.respond(
                ThymeleafContent("login", map)
            )
        }
        get("/login/pgpchallenge") {
            val error = call.request.queryParameters["error"]
            val success = call.request.queryParameters["success"]

            val map = buildMap {
                put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                /* These values can be passed as query params to avoid doing a ton of setup in other call routines, its easier to redirect with a query param instead of duplicating code everywhere */
                if (error != null) {
                    put(ThymeLeafMapKeys.ERROR.value, error)
                }
                if (success != null) {
                    put(ThymeLeafMapKeys.SUCCESS.value, success)
                }
            }
            return@get call.respond(
                ThymeleafContent("pgp_login", map)
            )
        }
        post("/login/pgpchallenge") {
            val error = call.request.queryParameters["error"]
            val success = call.request.queryParameters["success"]
            val parameters = call.receiveParameters()
            val username = parameters["username"]
                ?: return@post call.respondRedirect("/login/pgpchallenge?error=You must provide a username")

            if (!userNameAlreadyExists(username)) {
                val error = "No user with this username: $username was found"
                call.respondRedirect("/login/pgpchallenge?error=$error")
            }

            if (getPublicKey(getUserId(username)!! /* We can assert null because the username exists from the check above, it will have an ID */) == null) {
                val error = "This account does not have a PGP key uploaded, so a challenge is not possible"
                call.respondRedirect("/login/pgpchallenge?error=$error")
            }

            logger.debug { "registering a new challenge" }

            val challengeString =
                if (pgpChallengeHashSet[username] == null) registerNewChallenge(username) else pgpChallengeHashSet[username]?.challengeString

            logger.debug { "preparing PGP challenge" }
            if (challengeString == null) {
                val error = "An unexpected error occurred while generating or fetching your challenge"
                call.respondRedirect("/login/pgpchallenge?error=$error")
            }

            val map = buildMap {
                put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                put(ThymeLeafMapKeys.PGP_CHALLENGE_STRING.value, challengeString)
                put(ThymeLeafMapKeys.PGP_CHALLENGE_USERNAME.value, username)
                /* These values can be passed as query params to avoid doing a ton of setup in other call routines, its easier to redirect with a query param instead of duplicating code everywhere */
                if (error != null) {
                    put(ThymeLeafMapKeys.ERROR.value, error)
                }
                if (success != null) {
                    put(ThymeLeafMapKeys.SUCCESS.value, success)
                }
            }


            return@post call.respond(
                ThymeleafContent("pgp_challenge", map)
            )
        }

        post("/login/attemptchallenge") {
            val error = call.request.queryParameters["error"]
            val success = call.request.queryParameters["success"]
            val params = call.receiveParameters()
            val username = params["username"]
                ?: return@post call.respondRedirect("/login/pgpchallenge?error=You must provide a username")
            val signedChallenge = params["challenge"]
                ?: return@post call.respondRedirect("/login/pgpchallenge?error=You must provide a signed challenge")

            if (!verifySignature(username, signedChallenge)) {
                val error = "Your challenge is either incorrectly signed, expired, or does not exist."
                call.respondRedirect("/login/pgpchallenge?error=$error")
            }


            val token = (createJWT(
                JWTConfig(
                    siteConfig?.audience ?: "someoneisbadanddidntsetthis",
                    siteConfig?.issuer ?: "someoneisbadanddidntsetthis",
                    jwtSecret,
                    getUserId(username)!!, // We can force assert this as not null due to the verifiy credentials call above, it cannot get here if the user info is bogus
                    (siteConfig?.tokenLifetimeMinutes?.times(60)?.times(1000) ?: Length.JWT_TOKEN_LIFETIME_MS.value),
                ),
            ))
            call.response.cookies.append(
                Cookie(
                    name = "jwt",
                    value = token,
                    httpOnly = true,
                    secure = false /* THIS IS FOR I2P ONLY YOU MUST CHANGE THIS IF YOU RUN IT THROUGH TOR OR CLEARNET */,
                    path = "/"
                ),
            )
            //Redirect user to the home page
            return@post call.respondRedirect("/feed")
        }

        authenticate("jwt") {

            get("/logout") {
                val error = call.request.queryParameters["error"]
                val success = call.request.queryParameters["success"]
                /*
                 This will not work properly if both are defined but that shouldnt happen
                 "Ive got some good news and some bad news" type shit
                 */

                call.respondRedirect("/login${if (error == null) "" else "?error=$error"}${if (success == null) "" else "?success=$success"}")
            }
        }


        post("/login") {
            if (siteConfig?.pgpLoginOnly == true) {
                return@post call.respondRedirect("/login/pgpchallenge")
            }

            val params = call.receiveParameters()

            //These two shouldn't be able to happen under normal conditons, but they are thrown a page with the proper error just in case
            val username = params["username"] ?: return@post call.respondRedirect("/login?error=You must provide a username")

            val password = params["password"] ?: return@post call.respondRedirect("/login?error=You must provide a password")

            if (!verifyCredentials(username, password)) {
                val error = "Invalid credentials, make sure your user and password are correct."
                return@post call.respondRedirect("/login?error=$error")
            }


            val token = (createJWT(
                JWTConfig(
                    siteConfig.audience ?: "someoneisbadanddidntsetthis",
                    siteConfig.issuer ?: "someoneisbadanddidntsetthis",
                    jwtSecret,
                    getUserId(username)!!, // We can force assert this as not null due to the verifiy credentials call above, it cannot get here if the user info is bogus
                    (siteConfig.tokenLifetimeMinutes?.times(60)?.times(1000) ?: Length.JWT_TOKEN_LIFETIME_MS.value),
                ),
            ))
            call.response.cookies.append(
                Cookie(
                    name = "jwt",
                    value = token,
                    httpOnly = true,
                    secure = false /* THIS IS FOR I2P ONLY YOU MUST CHANGE THIS IF YOU RUN IT THROUGH TOR OR CLEARNET */,
                    path = "/"
                ),
            )
            //Redirect user to the home page
            return@post call.respondRedirect("/feed")


        }
    }


}