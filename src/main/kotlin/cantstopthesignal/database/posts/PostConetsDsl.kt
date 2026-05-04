package cantstopthesignal.database.posts

import cantstopthesignal.log.logger
import com.freedom.cantstopthesignal.database.dsl.table_definitions.PostContents
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

fun addPostContents(postContent: String, id: Long, postTitle: String): Boolean {
    return try {
        transaction {
            PostContents.insert {
                it[content] = postContent
                it[postId] = id
                it[title] = postTitle
            }
            true
        }
    } catch (e: Exception) {
        logger.error { e.message }
        false
    }

}


fun updatePostContents(newTitle: String?, newContent: String?, postId: Long): Boolean {
    if (newTitle == null && newContent == null) {
        return false
    }
    if (newTitle != null && newContent != null) {
        try {
            transaction {
                PostContents.update({ PostContents.id eq postId }) {
                    it[title] = newTitle
                    it[content] = newContent
                }
            }
            return true

        } catch (e: Exception) {
            logger.error { e.message }
            return false
        }
    } else if (newTitle != null) {
        try {
            transaction {
                PostContents.update({ PostContents.id eq postId }) {
                    it[title] = newTitle
                }
            }
            return true

        } catch (e: Exception) {
            logger.error { e.message }
            return false
        }

    } else if (newContent != null) {
        try {
            transaction {
                PostContents.update({ PostContents.id eq postId }) {
                    it[content] = newContent
                }
            }
            return true

        } catch (e: Exception) {
            logger.error { e.message }
            return false
        }


    }
    return false
}