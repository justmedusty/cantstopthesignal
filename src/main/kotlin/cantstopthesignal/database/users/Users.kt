package cantstopthesignal.database.users

import cantstopthesignal.cryptography.isValidOpenPGPPublicKey
import cantstopthesignal.database.admin.insertAdminLogEntry
import cantstopthesignal.database.admin.insertSuspendEntry
import cantstopthesignal.database.admin.insertUnsuspendEntry
import cantstopthesignal.enums.Length
import cantstopthesignal.log.logger
import cantstopthesignal.security.hashPassword
import cantstopthesignal.table_definitions.ProfileData
import cantstopthesignal.table_definitions.Users
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.time.LocalDateTime
import java.time.ZoneOffset


/**
 * User
 *
 * @property userName
 * @property publicKey
 * @property passwordHash
 * @constructor Create empty User
 */
data class User(
    val userName: String,
    val publicKey: String?,
    val passwordHash: String?,
    val isAdmin: Boolean,
    val isModerator: Boolean,
    val isSuspended: Boolean
)


/**
 * Username already exists
 *
 * @param userName
 * @return
 */
fun userNameAlreadyExists(userName: String): Boolean {
    return try {
        transaction {
            Users
                .selectAll()
                .where { Users.userName eq userName }
                .count() > 0
        }
    } catch (e: Exception) {
        logger.error { "Error checking username $e" }
        true
    }
}

fun updateLastLogin(userId: Long): Boolean {
    return try {
        transaction {
            ProfileData.update({ ProfileData.userId eq userId }) {
                it[lastLogin] = LocalDateTime.now(ZoneOffset.UTC)
            }
        }
        true
    } catch (e: Exception) {
        logger.error { e.message }
        false
    }
}

/**
 * Verify credentials
 *
 * @param userName
 * @param password
 * @return
 */
fun verifyCredentials(userName: String, password: String): Boolean {
    return try {
        var success = false
        transaction {
            val user = Users
                .selectAll()
                .where { Users.userName eq userName }.singleOrNull()
            val userpassword = user?.get(Users.passwordHash).toString()
            if (user != null && BCrypt.checkpw(password, userpassword) && updateLastLogin(
                    getUserId(
                        userName
                    )!!
                )   /* This can be asserted not null because user != null at this point*/) {
                success = true
            }
        }

        success
    } catch (e: Exception) {
        logger.error { "Error verifying credentials $e" }
        return false
    }
}


/**
 * User and password validation
 *
 * @param userName
 * @param password
 * @return
 */
fun userAndPasswordValidation(userName: String, password: String): Boolean {
    return try {
        when {
            password.isEmpty() && userName.isNotEmpty() -> {
                if (userNameAlreadyExists(userName)) {
                    false
                } else {
                    userName.length in Length.MIN_USERNAME_LENGTH.value..Length.MAX_USERNAME_LENGTH.value
                }
            }

            password.isNotEmpty() && userName.isEmpty() -> {
                password.length >= Length.MIN_USERNAME_LENGTH.value
            }

            else -> {
                throw IllegalArgumentException("Unknown error")
            }
        }
    } catch (e: Exception) {
        logger.error { "Error with user/pass validation $e" }
        return false
    }
}

/**
 * Create user
 *
 * @param user
 */ // Functions to perform CRUD operations on Users table
fun createUser(user: User): Boolean {
    logger.debug { "creating user" }
    if (user.passwordHash == null) return false
    return try {
        transaction {
            logger.debug { "in user creation tx" }
            if (userAndPasswordValidation(user.userName, "") && userAndPasswordValidation("", user.passwordHash)) {
                val id: Long = Users.insert {
                    it[userName] = user.userName
                    it[passwordHash] = hashPassword(user.passwordHash)
                    it[isAdmin] = user.isAdmin
                    it[isModerator] = user.isModerator
                    it[isSuspended] = user.isSuspended
                } get Users.id

                ProfileData.insert {
                    it[userId] = id
                    it[publicKey] = user.publicKey

                }
                return@transaction true
            } else {
                return@transaction false
            }
        }
    } catch (e: Exception) {
        logger.error { "Error creating user $e" }
        return false
    }
}


/**
 * Create user
 *
 * @param user
 */ // Functions to perform CRUD operations on Users table WITHOUT password login
fun createUserWithoutPassword(user: User): Boolean {
    try {
        /*
            This will be checked at a higher level but just in case because the user cannot log in if the key is wrong
         */
        if (user.publicKey == null) return false
        if (!isValidOpenPGPPublicKey(user.publicKey)) return false
        transaction {

            val id: Long = Users.insert {
                it[userName] = user.userName
            } get Users.id

            ProfileData.insert {
                it[userId] = id
                it[publicKey] = user.publicKey
            }
        }
        return true
    } catch (e: Exception) {
        logger.error { "Error creating user $e" }
        return false
    }
}

/**
 * Get user id
 *
 * @param userName
 * @return
 */
fun getUserId(userName: String): Long? {
    return try {
        transaction {
            Users
                .selectAll()
                .where { Users.userName eq userName }.singleOrNull()?.get(Users.id)
        }
    } catch (e: Exception) {
        logger.error { "Error getting userID $e" }
        -1
    }
}


/**
 * Get username
 *
 * @param id
 * @return
 */
fun getUserName(id: Long): String? {

    return try {
        val result = transaction {
            Users
                .selectAll()
                .where { Users.id eq id }.singleOrNull()?.get(Users.userName)
        }
        return result

    } catch (e: Exception) {
        logger.error { "Error grabbing username $e" }
        null
    }
}


/**
 * Update user credentials
 *
 * @param userName
 * @param password
 * @param newValue
 */
fun updateUserCredentials(userId: Long, password: Boolean, newValue: String): Boolean {
    return try {
        transaction {
            when {
                !password && newValue.isNotEmpty() -> {
                    Users.update({ Users.id eq userId }) {
                        it[Users.userName] = newValue
                    }
                    return@transaction true
                }

                password && newValue.isNotEmpty() -> {
                    Users.update({ Users.id eq userId }) {
                        it[passwordHash] = hashPassword(newValue)
                    }
                    return@transaction true
                }


                else -> {
                    false
                }
            }

        }
    } catch (e: Exception) {
        logger.error { "Error updating user credentials $e" }
        false
    }
}


/**
 * Delete user
 *
 * @param id
 */
fun deleteUser(id: Long) {
    try {
        transaction {
            Users.deleteWhere { Users.id eq id }
        }
    } catch (e: Exception) {
        logger.error { "Error deleting user $e" }
    }
}

fun fetchAllUsers(page: Int, limit: Int): List<String> {
    val offset = ((page - 1) * limit).toLong()

    return try {
        transaction {
            Users
                .select(Users.userName)
                .limit(limit)
                .offset(offset)
                .map { it[Users.userName] }
        }
    } catch (e: Exception) {
        logger.error { "Error occurred fetching users $e" }
        emptyList()
    }
}

fun searchAllUsers(query: String, page: Int, limit: Int): List<String> {
    val offset = ((page - 1) * limit).toLong()

    return try {
        transaction {
            Users
                .select(Users.userName)
                .where { Users.userName like "%$query%" }
                .limit(limit)
                .offset(offset)
                .map { it[Users.userName] }
        }
    } catch (e: Exception) {
        logger.error { "Error during search for users $e" }
        emptyList()
    }
}

fun isUserSuspended(userId: Long): Boolean {
    return try {
        transaction {
            userId.let { userId ->
                Users
                    .selectAll()
                    .where { Users.id eq userId }.singleOrNull()?.get(Users.isSuspended) ?: false
            }


        }
    } catch (e: ExposedSQLException) {
        logger.error { e.message }
        return false
    }
}

fun isUserAdmin(userId: Long): Boolean {
    return try {
        transaction {
            userId.let { userId ->
                Users
                    .selectAll()
                    .where { Users.id eq userId }.singleOrNull()?.get(Users.isAdmin) ?: false
            }


        }
    } catch (e: ExposedSQLException) {
        logger.error { e.message }
        return false
    }
}

fun isUserAdminOrModerator(userId: Long): Boolean {
    return try {
        transaction {

            val admin = Users
                .selectAll()
                .where { Users.id eq userId }.singleOrNull()?.get(Users.isAdmin) ?: false

            if (admin) {
                return@transaction true
            }

            val moderator = Users
                .selectAll()
                .where { Users.id eq userId }.singleOrNull()?.get(Users.isModerator) ?: false

            if (moderator) {
                return@transaction true
            }
            false


        }
    } catch (e: ExposedSQLException) {
        logger.error { e.message }
        return false
    }
}

fun isUserModerator(userId: Long): Boolean {
    return try {
        transaction {
            userId.let { userId ->
                Users
                    .selectAll()
                    .where { Users.id eq userId }.singleOrNull()?.get(Users.isModerator) ?: false
            }


        }
    } catch (e: ExposedSQLException) {
        logger.error { e.message }
        return false
    }
}

fun suspendUser(userId: Long, requesterId: Long, reason: String): Boolean {

    if (isUserAdmin(requesterId)) {
        val success = try {
            transaction {
                Users.update({ Users.id eq userId }) {
                    it[isSuspended] = true
                }
                true
            }
        } catch (e: Exception) {
            logger.error { e.message }
            false
        }

        if (!success) {
            logger.error { "An error occurred while trying to suspend user $userId" }
            return false
        }
        return insertSuspendEntry(userId, reason, userId)
    }
    return false

}

fun unSuspendUser(userId: Long, requesterId: Long, reason: String): Boolean {

    if (isUserAdmin(requesterId)) {
        val success = try {
            transaction {
                Users.update({ Users.id eq userId }) {
                    it[isSuspended] = false
                }
                true
            }
        } catch (e: Exception) {
            logger.error { e.message }
            false
        }
        if (!success) {
            logger.error { "An error occurred while trying to unsuspend user $userId" }
            return false
        }

        return insertUnsuspendEntry(userId, reason, userId)
    }
    return false

}

fun giveModerator(userId: Long, requesterId: Long): Boolean {

    if (isUserAdmin(requesterId)) {
        val success = try {
            transaction {
                Users.update({ Users.id eq userId }) {
                    it[isModerator] = true
                }
                true
            }
        } catch (e: Exception) {
            logger.error { e.message }
            false
        }

        if (!success) {
            logger.error { "An error occurred updating user $userId trying to give them admin" }
            return false
        }

        return insertAdminLogEntry(
            requesterId,
            "Admin ${getUserName(requesterId)} (ID ${requesterId}) has given moderator to ${getUserName(userId)} (ID ${userId})",
            "Gave admin to ${getUserName(userId)}"
        )
    }
    return false

}

/*
    Taking it away requires more of an explanation than giving it so we will require an explicit admin given string for this
 */
fun takeModerator(userId: Long, requesterId: Long, reason: String): Boolean {

    if (isUserAdmin(requesterId)) {
        val success = try {
            transaction {
                Users.update({ Users.id eq userId }) {
                    it[isModerator] = false
                }
                true
            }
        } catch (e: Exception) {
            logger.error { e.message }
            false
        }

        if (!success) {
            logger.error { "An error occurred updating user $userId trying to take moderator status" }
            return false
        }

        return insertAdminLogEntry(
            requesterId,
            reason,
            "Took moderator status from ${getUserName(userId)} (ID ${userId})"
        )
    }
    return false
}

fun giveAdmin(userId: Long, requesterId: Long): Boolean {

    if (isUserAdmin(requesterId)) {
        val success = try {
            transaction {
                Users.update({ Users.id eq userId }) {
                    it[isAdmin] = true
                }
                true
            }
        } catch (e: Exception) {
            logger.error { e.message }
            false
        }

        if (!success) {
            logger.error { "An error occurred updating user $userId trying to give them admin" }
            return false
        }

        return insertAdminLogEntry(
            requesterId,
            "Admin ${getUserName(requesterId)} (ID ${requesterId}) has given admin to ${getUserName(userId)} (ID ${userId})",
            "Gave admin to ${getUserName(userId)}"
        )
    }
    return false

}

/*
    Taking it away requires more of an explanation than giving it so we will require an explicit admin given string for this
 */
fun takeAdmin(userId: Long, requesterId: Long, reason: String): Boolean {

    if (isUserAdmin(requesterId)) {
        val success = try {
            transaction {
                Users.update({ Users.id eq userId }) {
                    it[isAdmin] = false
                }
                true
            }
        } catch (e: Exception) {
            logger.error { e.message }
            false
        }

        if (!success) {
            logger.error { "An error occurred updating user $userId trying to take their admin status" }
            return false
        }

        return insertAdminLogEntry(
            requesterId,
            reason,
            "Took admin status from ${getUserName(userId)} (ID ${userId})"
        )
    }
    return false
}