package com.alarm.meluko.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.UUID

class TribeException(message: String) : Exception(message)

/**
 * Every tribe membership write, shaped to pass firestore.rules (see the header
 * comment there). Writes are batches, never transactions that read docs the
 * caller can't see yet.
 */
object TribeActions {

    private const val INVITE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private const val DEFAULT_ALARM_TIME = "07:30"

    private fun newInviteCode() = buildString { repeat(6) { append(INVITE_CHARS.random()) } }

    private fun freshMember(displayName: String, photoUrl: String?, role: String, joinedWith: String?) =
        buildMap<String, Any?> {
            put(FirestoreField.DISPLAY_NAME, displayName)
            put(FirestoreField.PHOTO_URL, photoUrl)
            put(FirestoreField.ROLE, role)
            put(FirestoreField.TODAY_STATUS, FirestoreField.STATUS_SLEEPING)
            put(FirestoreField.TODAY_WAKE_TIME, null)
            put(FirestoreField.STREAK_DAYS, 0)
            put(FirestoreField.BEST_STREAK, 0)
            put(FirestoreField.TOTAL_WAKES, 0)
            put(FirestoreField.VETOES_USED, 0)
            put(FirestoreField.RANK, 1)
            put(FirestoreField.JOINED_AT, FieldValue.serverTimestamp())
            if (joinedWith != null) put(FirestoreField.JOINED_WITH, joinedWith)
        }

    private suspend fun currentTribeId(db: FirebaseFirestore, uid: String): String? =
        db.collection(FirestoreCollection.USER_ALARMS).document(uid).get().await()
            .getString(FirestoreField.PRIMARY_TRIBE_ID)

    private suspend fun pointUserAt(db: FirebaseFirestore, uid: String, tribeId: String?, alarmTime: String?) {
        val data = mutableMapOf<String, Any?>(
            FirestoreField.PRIMARY_TRIBE_ID to tribeId,
            FirestoreField.ACTIVE to (tribeId != null)
        )
        if (alarmTime != null) data[FirestoreField.FOLLOWED_ALARM_TIME] = alarmTime
        db.collection(FirestoreCollection.USER_ALARMS).document(uid).set(data, SetOptions.merge()).await()
    }

    /** Creates a tribe with the caller as admin. Leaves any current tribe first. Returns the tribe id. */
    suspend fun createTribe(
        db: FirebaseFirestore,
        uid: String,
        displayName: String,
        photoUrl: String?,
        name: String,
        description: String,
    ): String {
        currentTribeId(db, uid)?.let { leaveTribe(db, uid, it) }

        val tribeId = UUID.randomUUID().toString()
        val tribeRef = db.collection(FirestoreCollection.TRIBES).document(tribeId)
        // The invite doc's ID is the code, so a collision makes the batch fail; retry a few times.
        repeat(5) {
            val code = newInviteCode()
            try {
                db.batch().apply {
                    set(tribeRef, mapOf(
                        FirestoreField.NAME to name,
                        FirestoreField.DESCRIPTION to description,
                        FirestoreField.INVITE_CODE to code,
                        FirestoreField.CREATED_BY to uid,
                        FirestoreField.ALARM_TIME to DEFAULT_ALARM_TIME,
                        FirestoreField.ALARM_DAYS to listOf(1, 2, 3, 4, 5),
                        FirestoreField.MEMBER_COUNT to 1,
                        FirestoreField.PHOTO_URL to null,
                        FirestoreField.ACTIVE_PROPOSAL_ID to null,
                        FirestoreField.CREATED_AT to FieldValue.serverTimestamp()
                    ))
                    set(tribeRef.collection(FirestoreCollection.MEMBERS).document(uid),
                        freshMember(displayName, photoUrl, FirestoreField.ROLE_ADMIN, joinedWith = null))
                    set(db.collection(FirestoreCollection.INVITES).document(code), mapOf(
                        FirestoreField.TRIBE_ID to tribeId,
                        FirestoreField.CREATED_AT to FieldValue.serverTimestamp()
                    ))
                }.commit().await()
                pointUserAt(db, uid, tribeId, DEFAULT_ALARM_TIME)
                return tribeId
            } catch (e: FirebaseFirestoreException) {
                if (e.code != FirebaseFirestoreException.Code.PERMISSION_DENIED) throw e
                // Most likely the code was taken; try another one.
            }
        }
        throw TribeException("Couldn't create the tribe. Please try again.")
    }

    /** Joins by invite code. Leaves any other tribe first. Returns the tribe id. */
    suspend fun joinTribe(
        db: FirebaseFirestore,
        uid: String,
        displayName: String,
        photoUrl: String?,
        rawCode: String,
    ): String {
        val code = rawCode.trim().uppercase()
        val invite = try {
            db.collection(FirestoreCollection.INVITES).document(code).get().await()
        } catch (e: FirebaseFirestoreException) {
            throw TribeException("Couldn't check that code. Are you online?")
        }
        val tribeId = invite.getString(FirestoreField.TRIBE_ID)
            ?: throw TribeException("That invite code doesn't exist. Check with your tribe.")

        val tribeRef = db.collection(FirestoreCollection.TRIBES).document(tribeId)
        val tribe = tribeRef.get().await()
        if (!tribe.exists()) throw TribeException("That tribe no longer exists.")
        val alarmTime = tribe.getString(FirestoreField.ALARM_TIME) ?: DEFAULT_ALARM_TIME

        // Already a member? (Readable only if we are one.)
        val alreadyMember = try {
            tribeRef.collection(FirestoreCollection.MEMBERS).document(uid).get().await().exists()
        } catch (e: FirebaseFirestoreException) {
            false
        }

        if (!alreadyMember) {
            currentTribeId(db, uid)?.takeIf { it != tribeId }?.let { leaveTribe(db, uid, it) }
            db.batch().apply {
                set(tribeRef.collection(FirestoreCollection.MEMBERS).document(uid),
                    freshMember(displayName, photoUrl, FirestoreField.ROLE_MEMBER, joinedWith = code))
                update(tribeRef, FirestoreField.MEMBER_COUNT, FieldValue.increment(1))
            }.commit().await()
        }
        pointUserAt(db, uid, tribeId, alarmTime)
        return tribeId
    }

    /**
     * Leaves [tribeId]. If the caller is the last member the tribe is deleted;
     * if they're the only admin, the longest-standing member is promoted first
     * so the tribe is never left without an admin.
     */
    suspend fun leaveTribe(db: FirebaseFirestore, uid: String, tribeId: String) {
        val tribeRef = db.collection(FirestoreCollection.TRIBES).document(tribeId)
        val tribe = tribeRef.get().await()
        if (!tribe.exists()) {
            pointUserAt(db, uid, null, null)
            return
        }
        val members = try {
            tribeRef.collection(FirestoreCollection.MEMBERS).get().await().documents
        } catch (e: FirebaseFirestoreException) {
            // Not a member any more (removed); just drop our pointer.
            pointUserAt(db, uid, null, null)
            return
        }
        val me = members.firstOrNull { it.id == uid }
        if (me == null) {
            pointUserAt(db, uid, null, null)
            return
        }
        val others = members.filter { it.id != uid }

        if (others.isEmpty()) {
            deleteTribe(db, uid, tribeId, tribe)
            return
        }

        db.batch().apply {
            val iAmAdmin = me.getString(FirestoreField.ROLE) == FirestoreField.ROLE_ADMIN
            val anotherAdmin = others.any { it.getString(FirestoreField.ROLE) == FirestoreField.ROLE_ADMIN }
            if (iAmAdmin && !anotherAdmin) {
                successor(others)?.let { update(it.reference, FirestoreField.ROLE, FirestoreField.ROLE_ADMIN) }
            }
            update(tribeRef, FirestoreField.MEMBER_COUNT, FieldValue.increment(-1))
            delete(me.reference)
        }.commit().await()
        pointUserAt(db, uid, null, null)
    }

    private fun successor(others: List<DocumentSnapshot>): DocumentSnapshot? =
        others.minByOrNull { it.getTimestamp(FirestoreField.JOINED_AT)?.seconds ?: Long.MAX_VALUE }

    /** Admin removes someone else. */
    suspend fun removeMember(db: FirebaseFirestore, tribeId: String, memberUid: String) {
        val tribeRef = db.collection(FirestoreCollection.TRIBES).document(tribeId)
        db.batch().apply {
            update(tribeRef, FirestoreField.MEMBER_COUNT, FieldValue.increment(-1))
            delete(tribeRef.collection(FirestoreCollection.MEMBERS).document(memberUid))
        }.commit().await()
    }

    /** Admin promotes another member (admin transfer). */
    suspend fun makeAdmin(db: FirebaseFirestore, tribeId: String, memberUid: String) {
        db.collection(FirestoreCollection.TRIBES).document(tribeId)
            .collection(FirestoreCollection.MEMBERS).document(memberUid)
            .update(FirestoreField.ROLE, FirestoreField.ROLE_ADMIN).await()
    }

    /** Last member deletes the tribe, its invite code and its proposal history. */
    suspend fun deleteTribe(
        db: FirebaseFirestore,
        uid: String,
        tribeId: String,
        knownTribe: DocumentSnapshot? = null,
    ) {
        val tribeRef = db.collection(FirestoreCollection.TRIBES).document(tribeId)
        val tribe = knownTribe ?: tribeRef.get().await()
        val code = tribe.getString(FirestoreField.INVITE_CODE)
        val proposals = try {
            tribeRef.collection(FirestoreCollection.PROPOSALS).get().await().documents
        } catch (e: FirebaseFirestoreException) {
            emptyList()
        }
        // Proposals can only be deleted by an admin; a non-admin last member leaves them behind.
        val iAmAdmin = try {
            tribeRef.collection(FirestoreCollection.MEMBERS).document(uid).get().await()
                .getString(FirestoreField.ROLE) == FirestoreField.ROLE_ADMIN
        } catch (e: FirebaseFirestoreException) {
            false
        }
        db.batch().apply {
            if (iAmAdmin) {
                proposals.take(400).forEach { delete(it.reference) }
                if (code != null) delete(db.collection(FirestoreCollection.INVITES).document(code))
            }
            delete(tribeRef.collection(FirestoreCollection.MEMBERS).document(uid))
            delete(tribeRef)
        }.commit().await()
        pointUserAt(db, uid, null, null)
    }

    /**
     * Legacy tribes (created before invites/{code} existed) have no invite doc,
     * so nobody could join them. An admin's device quietly creates it.
     */
    suspend fun ensureInviteDoc(db: FirebaseFirestore, tribeId: String, inviteCode: String) {
        val inviteRef = db.collection(FirestoreCollection.INVITES).document(inviteCode)
        val existing = try { inviteRef.get().await() } catch (e: FirebaseFirestoreException) { return }
        if (existing.exists()) return
        try {
            inviteRef.set(mapOf(
                FirestoreField.TRIBE_ID to tribeId,
                FirestoreField.CREATED_AT to FieldValue.serverTimestamp()
            )).await()
        } catch (_: FirebaseFirestoreException) {
            // Not an admin, or the code is taken by another tribe — nothing to do here.
        }
    }
}
