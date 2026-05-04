package cantstopthesignal.database.posts
import cantstopthesignal.log.logger
import com.freedom.cantstopthesignal.database.dsl.table_definitions.PostDislikes
import com.freedom.cantstopthesignal.database.dsl.table_definitions.PostLikes
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
fun getDislikesForPost(postId: Long): Long {
    return try {
        transaction {
            PostDislikes.select {
                (PostDislikes.postId eq postId)
            }.count()
        }
    } catch (e: Exception) {
        println("Error getting likes for post: $e")
        -1
    }
}

fun isPostDislikedByUser(postId: Long, userId: Long): Boolean {
    return try {
        transaction {
            PostDislikes.select((PostDislikes.postId eq postId) and (PostDislikes.dislikedById eq userId)).count() > 0
        }
    } catch (e: Exception) {
        logger.error { e.message }
        false
    }


}
fun dislikePost(likedById: Long, postId: Long): Boolean {

    return try {

        transaction {

            if(isPostLikedByUser(postId, likedById)){
                unlikePost(likedById,postId)
            }
            PostDislikes.insert {
                it[PostDislikes.postId] = postId
                it[PostDislikes.dislikedById] = likedById
            }
            true
        }
    } catch (e: Exception) {
        logger.error { e.message }
        false
    }
}

fun isRequesterPostDislikeOwner(userId: Long, postId: Long): Boolean {
    return try {
        transaction {
            val match = PostLikes.select { (PostDislikes.postId eq postId) and (PostDislikes.dislikedById eq userId) }
            match.count() > 0
        }
    } catch (e: Exception) {
        logger.error { "Error checking who is comment poster" }
        false
    }
}

fun unDislikePost(userId: Long, postId: Long): Boolean {
    try {
        return transaction {
            val success = PostDislikes.deleteWhere { (PostDislikes.postId eq postId) and (PostDislikes.dislikedById eq userId) }
            success > 0
        }
    } catch (e: Exception) {
        logger.error { e.message }
        return false
    }
}