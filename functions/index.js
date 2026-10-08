/**
 * Meluko Firebase Cloud Functions
 *
 * These functions handle operations that must be reliable and server-side:
 *  1. Auto-accept expired alarm proposals (guaranteed even if no client is online).
 *  2. Join a tribe by invite code (validates the code server-side).
 *
 * Deploy with:
 *   firebase deploy --only functions
 */

const {onDocumentCreated, onDocumentUpdated} = require("firebase-functions/v2/firestore");
const {onCall} = require("firebase-functions/v2/https");
const {getFirestore, FieldValue} = require("firebase-admin/firestore");
const {initializeApp} = require("firebase-admin/app");

initializeApp();
const db = getFirestore();

const PROPOSALS = "proposals";
const TRIBES = "tribes";
const MEMBERS = "members";
const USER_ALARMS = "user_alarms";

const STATUS = "status";
const EXPIRES_AT = "expiresAt";
const PROPOSED_TIME = "proposedTime";
const PROPOSED_BY = "proposedBy";
const ALARM_TIME = "alarmTime";
const FOLLOWED_ALARM_TIME = "followedAlarmTime";
const PRIMARY_TRIBE_ID = "primaryTribeId";

/**
 * Auto-accept a proposal when its expiresAt timestamp has passed.
 *
 * Triggered on every proposal create/update across all tribes.
 * Idempotent: if status is already accepted/vetoed, it does nothing.
 */
async function maybeAcceptProposal(tribeId, proposalId) {
  const proposalRef = db
      .collection(TRIBES).doc(tribeId)
      .collection(PROPOSALS).doc(proposalId);

  return db.runTransaction(async (transaction) => {
    const proposalSnap = await transaction.get(proposalRef);
    if (!proposalSnap.exists) return null;

    const proposal = proposalSnap.data();
    if (proposal[STATUS] !== "pending") return null;

    const now = new Date();
    const expiresAt = proposal[EXPIRES_AT]?.toDate?.() || proposal[EXPIRES_AT];
    if (!expiresAt || now <= expiresAt) return null;

    const proposedTime = proposal[PROPOSED_TIME];
    if (!proposedTime) return null;

    const tribeRef = db.collection(TRIBES).doc(tribeId);
    const membersSnap = await transaction.get(tribeRef.collection(MEMBERS));

    // Update proposal status
    transaction.update(proposalRef, {[STATUS]: "accepted"});

    // Update tribe alarm time
    transaction.update(tribeRef, {[ALARM_TIME]: proposedTime});

    // Update every member's followed alarm time
    membersSnap.forEach((memberDoc) => {
      const uid = memberDoc.id;
      const userAlarmRef = db.collection(USER_ALARMS).doc(uid);
      transaction.update(userAlarmRef, {[FOLLOWED_ALARM_TIME]: proposedTime});
    });

    return {tribeId, proposalId, proposedTime, memberCount: membersSnap.size};
  });
}

exports.onProposalCreated = onDocumentCreated(
    {
      document: `${TRIBES}/{tribeId}/${PROPOSALS}/{proposalId}`,
      region: "asia-south1",
    },
    async (event) => {
      const {tribeId, proposalId} = event.params;
      await maybeAcceptProposal(tribeId, proposalId);
    }
);

exports.onProposalUpdated = onDocumentUpdated(
    {
      document: `${TRIBES}/{tribeId}/${PROPOSALS}/{proposalId}`,
      region: "asia-south1",
    },
    async (event) => {
      const {tribeId, proposalId} = event.params;
      await maybeAcceptProposal(tribeId, proposalId);
    }
);

/**
 * Callable function: join a tribe by invite code.
 *
 * Validates the invite code server-side, atomically creates the member doc,
 * increments member count, and sets the user's primary tribe / followed alarm.
 */
exports.joinTribeByInviteCode = onCall(
    {
      region: "asia-south1",
      enforceAppCheck: false,
    },
    async (request) => {
      const {inviteCode} = request.data || {};
      const uid = request.auth?.uid;

      if (!uid) {
        throw new Error("Unauthenticated");
      }
      if (!inviteCode || typeof inviteCode !== "string") {
        throw new Error("Invalid invite code");
      }

      const normalizedCode = inviteCode.trim().toUpperCase();

      // Look up tribe by invite code
      const tribesQuery = await db.collection(TRIBES)
          .where("inviteCode", "==", normalizedCode)
          .limit(1)
          .get();

      if (tribesQuery.empty) {
        throw new Error("Invalid invite code. Check with your tribe admin.");
      }

      const tribeDoc = tribesQuery.docs[0];
      const tribeId = tribeDoc.id;
      const alarmTime = tribeDoc.get(ALARM_TIME) || "07:30";

      const userDoc = await db.collection("users").doc(uid).get();
      const displayName = userDoc.get("displayName") || "Unknown";
      const photoUrl = userDoc.get("photoUrl") || null;

      const tribeRef = db.collection(TRIBES).doc(tribeId);
      const memberRef = tribeRef.collection(MEMBERS).doc(uid);
      const userAlarmRef = db.collection(USER_ALARMS).doc(uid);

      await db.runTransaction(async (transaction) => {
        const existing = await transaction.get(memberRef);
        if (existing.exists) {
          throw new Error("You're already in this tribe!");
        }

        transaction.set(memberRef, {
          displayName,
          photoUrl,
          role: "member",
          todayStatus: "sleeping",
          todayWakeTime: null,
          streakDays: 0,
          bestStreak: 0,
          totalWakes: 0,
          vetoesUsed: 0,
          rank: 1,
          joinedAt: FieldValue.serverTimestamp(),
        });

        transaction.update(tribeRef, {
          memberCount: FieldValue.increment(1),
        });

        transaction.set(userAlarmRef, {
          primaryTribeId: tribeId,
          followedAlarmTime: alarmTime,
          active: true,
        }, {merge: true});
      });

      return {success: true, tribeId, tribeName: tribeDoc.get("name")};
    }
);


