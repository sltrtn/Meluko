package com.alarm.meluko.data

object FirestoreCollection {
    const val USERS = "users"
    const val USER_ALARMS = "user_alarms"
    const val TRIBES = "tribes"
    const val MEMBERS = "members"
    const val PROPOSALS = "proposals"
}

object FirestoreField {
    const val DISPLAY_NAME = "displayName"
    const val USERNAME = "username"
    const val BIO = "bio"
    const val EMAIL = "email"
    const val PHOTO_URL = "photoUrl"
    const val CREATED_AT = "createdAt"
    const val UPDATED_AT = "updatedAt"

    const val PRIMARY_TRIBE_ID = "primaryTribeId"
    const val FOLLOWED_ALARM_TIME = "followedAlarmTime"
    const val ALARM_TIME = "alarmTime"
    const val LAST_UPDATED = "lastUpdated"

    const val NAME = "name"
    const val INVITE_CODE = "inviteCode"
    const val MEMBER_COUNT = "memberCount"
    const val CREATED_BY = "createdBy"

    const val STATUS = "status"
    const val AWAKE_STREAK = "awakeStreak"
    const val BEST_STREAK = "bestStreak"
    const val TOTAL_WAKES = "totalWakes"
    const val STREAK_DAYS = "streakDays"
    const val RANK = "rank"
    const val JOINED_AT = "joinedAt"
    const val VETOES_USED = "vetoesUsed"
    const val TODAY_STATUS = "todayStatus"
    const val TODAY_WAKE_TIME = "todayWakeTime"

    const val PROPOSED_BY = "proposedBy"
    const val PROPOSED_BY_NAME = "proposedByName"
    const val PROPOSED_TIME = "proposedTime"
    const val VETOED_BY = "vetoedBy"
    const val VETO_COUNT = "vetoCount"
    const val EXPIRES_AT = "expiresAt"

    const val ALARM_TONE_URI = "alarmToneUri"
    const val TONE_URI = "toneUri"
    const val TONE_SOURCE = "toneSource"

    const val ROLE = "role"
    const val ACTIVE = "active"
    const val DESCRIPTION = "description"
    const val ALARM_DAYS = "alarmDays"
    const val LAST_WAKE_DATE = "lastWakeDate"
    const val NOTIFICATIONS_ENABLED = "notificationsEnabled"

    const val STATUS_AWAKE = "awake"
    const val STATUS_SLEEPING = "sleeping"
    const val STATUS_MISSED = "missed"
    const val STATUS_PENDING = "pending"
    const val STATUS_VETOED = "vetoed"
    const val STATUS_ACCEPTED = "accepted"

    const val ROLE_ADMIN = "admin"
    const val ROLE_MEMBER = "member"
}
