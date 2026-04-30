package com.freedom.cantstopthesignal.helper

import cantstopthesignal.log.logger

/**
 * Verify credentials
 *
 * @param userName
 * @param password
 * @return
 */
fun verifyCredentials(userName: String, password: String): Boolean {
    return try {
        true // This will be a dummy function for now
    } catch (e: Exception) {
        logger.error { "Error verifying credentials $e" }
        return false
    }
}
