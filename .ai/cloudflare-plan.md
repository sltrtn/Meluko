# Cloudflare Workers backend plan (free, no Blaze)

> Status: **plan only**, not built. Written 2026-10-11 after the rules lockdown.
> Goal: get the server-side jobs Meluko needs without upgrading Firebase to Blaze.

---

## 1. The shape of it

Keep Firebase on the free **Spark** plan for what it's good at, and add one
**Cloudflare Worker** for the few jobs that need a trusted server.

```
 ┌────────────── Android app ──────────────┐
 │ Firebase Auth (Google / anonymous)       │
 │ Firestore SDK  ── reads/writes ──────────┼──► Firestore  (security rules = the guard)
 │ FCM SDK        ◄── push ─────────────────┼─── FCM (free)
 │ HTTPS + ID token ──► Worker endpoints     │
 └──────────────────────────────────────────┘
                         │
                         ▼
 ┌──────────── Cloudflare Worker (free) ────────────┐
 │ Cron (every 1 min) ─► accept expired proposals     │
 │                     ─► daily reset + missed check   │
 │ HTTP  POST /events  ─► fan-out push notifications  │
 │ HTTP  POST /wake    ─► (phase B) trusted wake log   │
 │ Talks to Firestore REST + FCM HTTP v1 as a         │
 │ service account (bypasses rules — trusted path)    │
 └────────────────────────────────────────────────────┘
```

What stays exactly as it is: sign-in, every screen's Firestore listeners, and
the security rules shipped in `firestore.rules`. The app works without the
Worker. The Worker makes things *timely* and *trustworthy*, it isn't a hard
dependency. The app's own auto-accept stays as a fallback; the rules make two
accepters racing each other harmless.

---

## 2. How the pieces authenticate

**Worker → Google (Firestore, FCM).**
- A Firebase service-account JSON is stored as a Worker **secret** (never in git or the APK).
- The Worker signs a JWT with WebCrypto (RS256) and swaps it for a short-lived OAuth token (scopes `datastore`, `firebase.messaging`).
- The token is cached about 55 minutes in the isolate.
- The Firebase Admin SDK doesn't run on Workers, so everything uses REST: Firestore `documents:commit` / `runQuery` / `batchGet`, and FCM `v1/projects/meluko-routine/messages:send`.

**App → Worker.**
- The app sends its Firebase **ID token** (`Authorization: Bearer …`).
- The Worker verifies it against Google's public signing keys: signature, `aud = meluko-routine`, `iss`, `exp`. Keys are cached by `Cache-Control`.
- No ID token, no service.

**Cron → Worker.** Cron Triggers are internal to Cloudflare; there's no public URL to abuse.

---

## 3. Jobs

### A. Proposal auto-accept (cron, every minute)
- **What it fixes:** proposals only resolve when some member happens to have the app open.
- **Query:** collection-group `proposals` where `status == 'pending'` and `createdAt <= now − 2h` (needs a collection-group index in `firestore.indexes.json`). Usually returns nothing, which costs 1 read.
- **For each result, one Firestore `commit`:**
  - proposal → `accepted`, with an `updateTime` precondition so a client that accepted first wins cleanly
  - tribe `alarmTime` = proposed time, `activeProposalId` = null
  - every member's `user_alarms.followedAlarmTime` (the client can't write other users' docs; the Worker can)
- **Then:** an FCM **data** message `alarm_changed {time}` to every member.

### B. Daily status reset + missed detection (cron, every minute, only touches "due" tribes)
- **What it fixes:**
  - Missed detection currently depends on the sleeper's own phone, and is broken (audit #6).
  - Other members see yesterday's "Awake" as today's (audit #7).
- **Needs:** each member's time zone. The app writes `user_alarms.timeZone` (an IANA id like `Asia/Kolkata`).
- **Per tribe, the Worker keeps a `nextDueAt` timestamp,** so each run only queries tribes where `nextDueAt <= now`. That's cheap, and it's what keeps us inside the free read quota (see §5).
  - **At alarm time:** set every member to `sleeping` / no wake time for the new day.
  - **At alarm time + 10 min:** anyone still `sleeping` whose `lastWakeDate` isn't today → `missed`, streak 0. Then push "Sam slept through it" to the tribe if wanted.

### C. Push notifications (HTTP, called by the app right after its own write)
- **Why the app calls it:** there are no Firestore → Worker triggers on Spark.
- **How:** after a successful write, the app POSTs `/events` with `{type, tribeId, proposalId?}`. The Worker re-reads the state, so it never trusts the payload. It checks the caller is a member, then sends FCM to the other members' tokens.
- **Events:**
  - new proposal ("Ria wants 6:30, veto within 2h")
  - veto
  - accepted (also sent by job A)
  - someone woke up ("Ria is up, 6-day streak")
- **Tokens:** `users/{uid}.fcmTokens` (an array, multi-device), owner-writable under the existing rules.

### D. Housekeeping (cron, daily)
- Delete proposals older than 30 days.
- Delete orphaned invite docs (tribe gone).
- Delete anonymous **debug-preview** users older than a day (Identity Toolkit REST), plus their tribes.

### E. Phase B (optional): trusted wake recording `/wake`
- **Problem:** the rules already make faking stats hard (+1/day, recent date), but a determined user can still mark themselves awake without waking.
- **How:** the app POSTs `/wake` instead of writing the member doc. The Worker checks the server time is within, say, [alarm − 5 min, alarm + 3 h] in the member's time zone, then writes the stats itself.
- **Then:** the rules drop client write access to `streakDays`/`totalWakes`/`lastWakeDate` entirely.

---

## 4. App-side changes this needs

1. **Push:** add `firebase-messaging` (free on Spark).
   - Register and refresh the token into `users/{uid}.fcmTokens`.
   - A `FirebaseMessagingService` handles `alarm_changed` by rescheduling `AlarmManager` from the payload. This fixes "members who don't open the app keep the old alarm" (audit #8).
2. **Time zone:** write `user_alarms.timeZone` on start and on `TIMEZONE_CHANGED`.
3. **Events:** after propose / veto / wake, fire-and-forget `POST /events`. Failure is harmless: job A and the clients still resolve proposals, the push is just skipped.
4. **Keep the client fallbacks** (auto-accept timer, self-heal) so the app works even if the Worker is down.

---

## 5. Free-tier budget (the real constraint)

| Resource | Free limit | Expected use (≈100 users, ≈15 tribes) |
|---|---|---|
| Worker requests | 100,000/day | ~1,440 cron runs + a few hundred `/events` |
| Worker CPU | 10 ms per invocation | JWT signing + JSON: fine; network waits don't count |
| Cron Triggers | a few per account (5 on Free at time of writing; verify) | 2 (every-minute, daily) |
| Firestore reads | 50,000/day | ~1,440 (empty proposal queries) + ~1,440 (due-tribe queries) + 2 reads/member/day for job B |
| Firestore writes | 20,000/day | 2 per member/day (reset + missed/awake) + proposals |
| FCM | effectively unlimited | — |

The every-minute scans cost a read even when empty (~2.9k reads/day fixed).
Fine at this size, but don't add more per-minute queries casually. The
`nextDueAt` design matters: a naive "scan all members every 5 minutes" would
burn ~29k reads/day at only 100 users.

---

## 6. Disadvantages and risks

1. **Two platforms.** Two deploys (`firebase deploy` + `wrangler deploy`), two dashboards, two sets of logs, two secret stores. More to forget.
2. **The service-account key is a master key.** It bypasses every security rule. If the Worker secret leaks, the whole database is open. It must stay a Worker secret and be rotated if exposed. The Worker's code becomes security-critical.
3. **No real triggers, only polling.**
   - Acceptance can lag up to about a minute (the client fallback usually beats it anyway).
   - Pushes depend on the app pinging `/events`. If that ping fails, that notification is lost, though the state stays correct.
4. **Hand-written plumbing.** No Admin SDK means writing JWT signing, ID-token verification and Firestore REST's verbose JSON by hand. More code, more places for bugs, and it needs its own tests.
5. **Hard free-tier walls, no overage.** Over 100k requests/day the Worker returns errors; over Firestore's Spark quota, writes fail until midnight (US Pacific). No surprise bill, but also no graceful scaling. Firestore's own quota will bite first as the user base grows (that's a Firebase limit, not a Cloudflare one).
6. **Cron is best-effort.** Runs can be delayed or occasionally skipped. Every job has to be idempotent and safe to re-run, which this design is.
7. **Time zones become the server's problem.** Missed detection and reset must be computed in each member's zone. That's more logic, and a tribe spread across zones needs a product decision (each member's local 7:30?).
8. **Push delivery isn't guaranteed.** FCM data messages can be delayed under Doze unless sent high-priority, which has its own quotas. The alarm-time sync should be treated as "usually instant, eventually correct" (the app still re-syncs on open).
9. **Local dev is fiddlier.** `wrangler dev` plus the Firestore emulator, with the Worker pointed at the emulator's REST endpoint.

**Versus Blaze:** Cloud Functions would give native Firestore triggers, the Admin SDK, no hand-rolled auth, and one platform, at ₹0 at this scale. The only real cost is putting a card on file (set a budget alert). If the card isn't a hard blocker, Blaze is simpler. The Worker route is the right call only if "no billing account" is a firm requirement.

---

## 7. Build order (each step ships on its own)

| Step | What | Rough size |
|---|---|---|
| 1 | Worker skeleton: secrets, Google OAuth token, ID-token verify, Firestore REST helper, tests against the emulator | ~1 day |
| 2 | Job A (auto-accept) + `alarm_changed` push + app FCM + reschedule on push | ~1 day |
| 3 | Time zone field + job B (reset + missed), `nextDueAt` scheduling | ~1–1.5 days |
| 4 | `/events` pushes (proposal, veto, wake) | ~0.5 day |
| 5 | Job D housekeeping | ~0.5 day |
| 6 | (Optional) `/wake` trusted stats + tighten rules | ~1 day |
