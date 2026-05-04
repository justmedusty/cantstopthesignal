package com.freedom.cantstopthesignal.database.dsl.table_definitions

import org.jetbrains.exposed.v1.javatime.datetime
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.statements.api.ExposedBlob
import org.jetbrains.exposed.v1.javatime.CurrentDateTime
import java.time.LocalDateTime

/****************************************************************************************************************************************************************
 * This file contains ALL the DSL table definitions, I prefer this project to have the DSL in one place to make it easier to add or check things
 ****************************************************************************************************************************************************************/
object Users : Table(name = "Users") {
    val id: Column<Long> = long("id").autoIncrement()
    val userName: Column<String> = varchar("user_name", 45).uniqueIndex()
    val passwordHash = text("password_hash")
    val isAdmin = bool("is_admin").default(false)
    val isModerator = bool("is_moderator").default(false)
    val isSuspended = bool("is_suspended").default(false)

    override val primaryKey = PrimaryKey(id)
}

object ProfileData : Table(name = "ProfileData") {
    val id: Column<Long> = long("id").autoIncrement()
    val userId: Column<Long> = long("user_id").references(Users.id, ReferenceOption.CASCADE)
    val bio = text("bio").nullable().default(null)
    val publicKey = text("public_key").nullable().default(null)
    val profilePhoto: Column<ExposedBlob?> = blob("profile_photo").nullable().default(null)
    val autoEncrypt: Column<Boolean> = bool("auth_encrypt").default(false)

    override val primaryKey = PrimaryKey(id)
}

object Messages : Table(name = "Messages") {
    val id: Column<Long> = long("id").autoIncrement()
    val senderId: Column<Long> = long("sender_id").references(Users.id)
    val receiverId: Column<Long> = long("receiver_id").references(Users.id)
    val message: Column<String> = text("message")
    val timeSent: Column<LocalDateTime> = datetime("time_sent").defaultExpression(CurrentDateTime)

    override val primaryKey = PrimaryKey(id)
}

object Notifications : Table(name = "Notifications") {
    val id: Column<Long> = long("id").autoIncrement()
    val read: Column<Boolean> = bool("read").default(false)
    val eventId: Column<Long> = long("event_id").references(Posts.id, onDelete = ReferenceOption.CASCADE)
    val userId: Column<Long> = long("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val type: Column<Long> = long("type")

    override val primaryKey = PrimaryKey(id)
}


object MessageNotifications : Table(name = "MessageNotifications") {
    val id: Column<Long> = long("id").autoIncrement()
    val read: Column<Boolean> = bool("read").default(false)
    val messageId: Column<Long> = long("message_id").references(Messages.id, onDelete = ReferenceOption.CASCADE)
    val userId: Column<Long> = long("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE)

    override val primaryKey = PrimaryKey(id)
}

object Posts : Table(name = "Posts") {
    val id: Column<Long> = long("id").autoIncrement()
    val posterId: Column<Long> = long("posterId").references(Users.id)
    val topic: Column<String> = varchar("topic", 60)
    val timestamp: Column<LocalDateTime> = datetime("timestamp").defaultExpression(CurrentDateTime)


    override val primaryKey = PrimaryKey(id)
}

object PostLikes : Table(name = "Likes") {
    val id: Column<Long> = long("id").autoIncrement()
    val postId: Column<Long> = long("post_id").references(Posts.id, ReferenceOption.CASCADE)
    val likedById: Column<Long> = long("likedById").references(Users.id)


    override val primaryKey = PrimaryKey(id)
}

object PostEdits : Table(name = "PostEdits") {
    val id: Column<Long> = long("id").autoIncrement()
    val postId: Column<Long> = long("commentId").references(Posts.id)
    val lastEdited: Column<LocalDateTime> = datetime("lastEdited")


    override val primaryKey = PrimaryKey(id)
}

object PostDislikes : Table(name = "Dislikes") {
    val id: Column<Long> = long("id").autoIncrement()
    val postId: Column<Long> = long("post").references(Posts.id, ReferenceOption.CASCADE)
    val dislikedById: Column<Long> = long("dislikedBy").references(Users.id)

    init {
        index(true, postId, dislikedById)
    }

    override val primaryKey = PrimaryKey(id)
}

object PostContents : Table(name = "PostContents") {
    val id: Column<Long> = long("id").autoIncrement()
    var title: Column<String> = text("title")
    val content: Column<String> = text("postContent")
    val postId: Column<Long> = long("post").references(Posts.id, onDelete = ReferenceOption.CASCADE)


    override val primaryKey = PrimaryKey(id)
}

object Comments : Table(name = "Comments") {
    val id: Column<Long> = long("id").autoIncrement()
    val content: Column<String> = text("commentContent")
    val postId: Column<Long> = long("post").references(Posts.id, ReferenceOption.CASCADE)
    val commenterId: Column<Long> = long("commenterId").references(Users.id)
    val isReply: Column<Boolean> = bool("isReply").default(false)
    val parentCommentId: Column<Long?> =
        long("parentCommentId").references(id, onDelete = ReferenceOption.CASCADE).nullable().default(null)
    val timeStamp: Column<LocalDateTime> = datetime("time_posted").defaultExpression(CurrentDateTime)


    override val primaryKey = PrimaryKey(id)
}

object CommentLikes : Table(name = "CommentLikes") {
    val id: Column<Long> = long("id").autoIncrement()
    val commentId: Column<Long> = long("commentId").references(Comments.id, ReferenceOption.CASCADE)
    val likedById: Column<Long> = long("likedById").references(Users.id)

    init {
        index(true, commentId, likedById)
    }

    override val primaryKey = PrimaryKey(id)
}

object CommentEdits : Table(name = "CommentEdits") {
    val id: Column<Long> = long("id").autoIncrement()
    val commentId: Column<Long> = long("commentId").references(Comments.id)
    val posterId: Column<Long> = long("posterId").references(Users.id)
    val lastEdited: Column<LocalDateTime> = datetime("lastEdited")


    override val primaryKey = PrimaryKey(id)
}


object CommentDislikes : Table(name = "CommentDislikes") {
    val id: Column<Long> = long("id").autoIncrement()
    val commentId: Column<Long> = long("commentId").references(
        Comments.id, ReferenceOption.CASCADE
    )
    val dislikedById: Column<Long> = long("dislikedById").references(Users.id)


    init {
        index(true, commentId, dislikedById)
    }


    override val primaryKey = PrimaryKey(id)
}

object AdminLogs : Table(name = "AdminLogs") {
    val id: Column<Long> = long("id").autoIncrement()
    val timestamp: Column<LocalDateTime> = datetime("timestamp")
    val userId: Column<Long> = long("message_id").references(Users.id)
    val doneById: Column<Long> = long("done_by_id")

    //true for added, false for removed
    val added: Column<Boolean> = bool("added")
    val reason: Column<String> = text("reason")

    override val primaryKey = PrimaryKey(id)
}

object SuspendLog : Table(name = "SuspendLog") {
    val id: Column<Long> = long("id").autoIncrement()
    val timestamp: Column<LocalDateTime> = datetime("suspend_time")
    val adminId: Column<Long> = long("admin_id").references(Users.id)
    val suspendedUserId: Column<Long> = long("suspended_user_id")

    //true for suspend, false for unsuspend
    val suspend: Column<Boolean> = bool("suspend")
    val reason: Column<String> = text("reason")

    override val primaryKey = PrimaryKey(id)
}
