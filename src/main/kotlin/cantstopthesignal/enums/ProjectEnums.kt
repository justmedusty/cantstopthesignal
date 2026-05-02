package com.freedom.cantstopthesignal.enums

enum class Length(val value: Long) {
    MAX_CONTENT_LENGTH(20_000),
    MAX_TITLE_LENGTH(300),
    MAX_COMMENT_LENGTH(10_000),
    MAX_TOPIC_LENGTH(100),
    MAX_PHOTO_SIZE_BYTES(1_048_576),
    MAX_DM_MESSAGE_LENGTH(5_000),
    MAX_PAGE_LIMIT(50)
}

enum class Notif(val value: Long) {
    POST(1),
    COMMENT(2)
}