// Firestore security rules tests for Meluko.
// Run: firebase emulators:exec --only firestore --project demo-meluko "npm --prefix firestore-tests test"
import { readFileSync } from 'node:fs';
import { test, before, beforeEach, after } from 'node:test';
import {
  initializeTestEnvironment, assertSucceeds, assertFails,
} from '@firebase/rules-unit-testing';
import {
  doc, getDoc, setDoc, updateDoc, deleteDoc, collection, getDocs, query, where, limit,
  writeBatch, increment, serverTimestamp, arrayUnion, Timestamp,
} from 'firebase/firestore';

let env;
const T = 'tribe1';
const today = new Date().toISOString().slice(0, 10);
const yesterday = new Date(Date.now() - 86400000).toISOString().slice(0, 10);

before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-meluko',
    firestore: { rules: readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8') },
  });
});
after(async () => { await env.cleanup(); });

const member = (role, extra = {}) => ({
  displayName: role, photoUrl: null, role, todayStatus: 'sleeping', todayWakeTime: null,
  streakDays: 3, bestStreak: 5, totalWakes: 10, vetoesUsed: 0, rank: 1,
  lastWakeDate: yesterday, joinedAt: Timestamp.now(), ...extra,
});

// Seed: tribe1 with admin "alice" + member "bob", invite ABC123.
beforeEach(async () => {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await setDoc(doc(db, 'tribes', T), {
      name: 'Hostel', description: '', inviteCode: 'ABC123', createdBy: 'alice',
      alarmTime: '07:30', alarmDays: [1, 2, 3, 4, 5], memberCount: 2, photoUrl: null,
      activeProposalId: null, createdAt: Timestamp.now(),
    });
    await setDoc(doc(db, 'invites', 'ABC123'), { tribeId: T, createdAt: Timestamp.now() });
    await setDoc(doc(db, 'tribes', T, 'members', 'alice'), member('admin'));
    await setDoc(doc(db, 'tribes', T, 'members', 'bob'), member('member'));
  });
});

const as = (uid) => env.authenticatedContext(uid).firestore();

async function seedProposal(pid, { by = 'bob', time = '06:00', hoursAgo = 0, active = true } = {}) {
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await setDoc(doc(db, 'tribes', T, 'proposals', pid), {
      proposedBy: by, proposedByName: by, proposedTime: time, status: 'pending',
      vetoedBy: [], vetoCount: 0,
      createdAt: Timestamp.fromMillis(Date.now() - hoursAgo * 3600000),
    });
    if (active) await updateDoc(doc(db, 'tribes', T), { activeProposalId: pid });
  });
}

// ════════════════════════════ Allowed flows ════════════════════════════════

test('create a tribe (tribe + admin member + invite in one batch)', async () => {
  const db = as('carol');
  const b = writeBatch(db);
  b.set(doc(db, 'tribes', 't2'), {
    name: 'New', description: '', inviteCode: 'NEW234', createdBy: 'carol', alarmTime: '06:45',
    alarmDays: [1, 2, 3, 4, 5], memberCount: 1, photoUrl: null, activeProposalId: null,
    createdAt: serverTimestamp(),
  });
  b.set(doc(db, 'tribes', 't2', 'members', 'carol'), {
    displayName: 'c', photoUrl: null, role: 'admin', todayStatus: 'sleeping', todayWakeTime: null,
    streakDays: 0, bestStreak: 0, totalWakes: 0, vetoesUsed: 0, rank: 1, joinedAt: serverTimestamp(),
  });
  b.set(doc(db, 'invites', 'NEW234'), { tribeId: 't2', createdAt: serverTimestamp() });
  await assertSucceeds(b.commit());
});

test('join with a valid invite code', async () => {
  const db = as('dave');
  const inv = await assertSucceeds(getDoc(doc(db, 'invites', 'ABC123')));
  const tribeId = inv.data().tribeId;
  await assertSucceeds(getDoc(doc(db, 'tribes', tribeId)));
  const b = writeBatch(db);
  b.set(doc(db, 'tribes', tribeId, 'members', 'dave'), {
    displayName: 'd', photoUrl: null, role: 'member', todayStatus: 'sleeping', todayWakeTime: null,
    streakDays: 0, bestStreak: 0, totalWakes: 0, vetoesUsed: 0, rank: 1,
    joinedAt: serverTimestamp(), joinedWith: 'ABC123',
  });
  b.update(doc(db, 'tribes', tribeId), { memberCount: increment(1) });
  await assertSucceeds(b.commit());
});

test('member leaves', async () => {
  const db = as('bob');
  const b = writeBatch(db);
  b.update(doc(db, 'tribes', T), { memberCount: increment(-1) });
  b.delete(doc(db, 'tribes', T, 'members', 'bob'));
  await assertSucceeds(b.commit());
});

test('admin removes a member', async () => {
  const db = as('alice');
  const b = writeBatch(db);
  b.update(doc(db, 'tribes', T), { memberCount: increment(-1) });
  b.delete(doc(db, 'tribes', T, 'members', 'bob'));
  await assertSucceeds(b.commit());
});

test('admin leaves and hands admin to someone else', async () => {
  const db = as('alice');
  const b = writeBatch(db);
  b.update(doc(db, 'tribes', T, 'members', 'bob'), { role: 'admin' });
  b.update(doc(db, 'tribes', T), { memberCount: increment(-1) });
  b.delete(doc(db, 'tribes', T, 'members', 'alice'));
  await assertSucceeds(b.commit());
});

test('last member deletes the tribe with its invite and proposals', async () => {
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await deleteDoc(doc(db, 'tribes', T, 'members', 'bob'));
    await updateDoc(doc(db, 'tribes', T), { memberCount: 1 });
    await setDoc(doc(db, 'tribes', T, 'proposals', 'old'), { status: 'vetoed', createdAt: Timestamp.now() });
  });
  const db = as('alice');
  const b = writeBatch(db);
  b.delete(doc(db, 'tribes', T, 'proposals', 'old'));
  b.delete(doc(db, 'invites', 'ABC123'));
  b.delete(doc(db, 'tribes', T, 'members', 'alice'));
  b.delete(doc(db, 'tribes', T));
  await assertSucceeds(b.commit());
});

test('propose a new time', async () => {
  const db = as('bob');
  const b = writeBatch(db);
  b.set(doc(db, 'tribes', T, 'proposals', 'p1'), {
    proposedBy: 'bob', proposedByName: 'Bob', proposedTime: '06:00', status: 'pending',
    vetoedBy: [], vetoCount: 0, createdAt: serverTimestamp(),
  });
  b.update(doc(db, 'tribes', T), { activeProposalId: 'p1' });
  await assertSucceeds(b.commit());
});

test('veto within the window', async () => {
  await seedProposal('p1');
  const db = as('alice');
  const b = writeBatch(db);
  b.update(doc(db, 'tribes', T, 'proposals', 'p1'), {
    status: 'vetoed', vetoedBy: arrayUnion('alice'), vetoCount: increment(1),
  });
  b.update(doc(db, 'tribes', T, 'members', 'alice'), { vetoesUsed: increment(1) });
  b.update(doc(db, 'tribes', T), { activeProposalId: null });
  await assertSucceeds(b.commit());
});

test('accept after the window passes', async () => {
  await seedProposal('p1', { hoursAgo: 3 });
  const db = as('alice');
  const b = writeBatch(db);
  b.update(doc(db, 'tribes', T, 'proposals', 'p1'), { status: 'accepted' });
  b.update(doc(db, 'tribes', T), { alarmTime: '06:00', activeProposalId: null });
  await assertSucceeds(b.commit());
});

test('record a wake: status, +1 streak, best, total, today', async () => {
  await assertSucceeds(updateDoc(doc(as('bob'), 'tribes', T, 'members', 'bob'), {
    todayStatus: 'awake', todayWakeTime: '7:02 AM', streakDays: 4, bestStreak: 5,
    totalWakes: 11, lastWakeDate: today,
  }));
});

test('daily reset and missed', async () => {
  const db = as('bob');
  await assertSucceeds(updateDoc(doc(db, 'tribes', T, 'members', 'bob'), { todayStatus: 'sleeping', todayWakeTime: null }));
  await assertSucceeds(updateDoc(doc(db, 'tribes', T, 'members', 'bob'), { todayStatus: 'missed', streakDays: 0 }));
});

test('profile name/photo propagates to own member doc', async () => {
  await assertSucceeds(updateDoc(doc(as('bob'), 'tribes', T, 'members', 'bob'), { displayName: 'Bobby', photoUrl: 'x' }));
});

test('solo admin can set the time directly', async () => {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await deleteDoc(doc(ctx.firestore(), 'tribes', T, 'members', 'bob'));
    await updateDoc(doc(ctx.firestore(), 'tribes', T), { memberCount: 1 });
  });
  await assertSucceeds(updateDoc(doc(as('alice'), 'tribes', T), { alarmTime: '06:15' }));
});

test('admin backfills an invite doc for a legacy tribe', async () => {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await deleteDoc(doc(ctx.firestore(), 'invites', 'ABC123'));
  });
  await assertSucceeds(setDoc(doc(as('alice'), 'invites', 'ABC123'), { tribeId: T, createdAt: serverTimestamp() }));
});

test('members can read the member list and proposals', async () => {
  const db = as('bob');
  await assertSucceeds(getDocs(collection(db, 'tribes', T, 'members')));
  await assertSucceeds(getDocs(collection(db, 'tribes', T, 'proposals')));
});

// ════════════════════════════ Attacks ═════════════════════════════════════

test('DENY: member promotes self to admin', async () => {
  await assertFails(updateDoc(doc(as('bob'), 'tribes', T, 'members', 'bob'), { role: 'admin' }));
});

test('DENY: member inflates streak', async () => {
  await assertFails(updateDoc(doc(as('bob'), 'tribes', T, 'members', 'bob'), { streakDays: 99, lastWakeDate: today }));
});

test('DENY: +1 streak without a new wake date', async () => {
  await assertFails(updateDoc(doc(as('bob'), 'tribes', T, 'members', 'bob'), { streakDays: 4 }));
});

test('DENY: wake date far in the future', async () => {
  await assertFails(updateDoc(doc(as('bob'), 'tribes', T, 'members', 'bob'), {
    streakDays: 4, lastWakeDate: '2099-01-01',
  }));
});

test('DENY: resetting vetoesUsed', async () => {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await updateDoc(doc(ctx.firestore(), 'tribes', T, 'members', 'bob'), { vetoesUsed: 2 });
  });
  await assertFails(updateDoc(doc(as('bob'), 'tribes', T, 'members', 'bob'), { vetoesUsed: 0 }));
});

test('DENY: member edits another member', async () => {
  await assertFails(updateDoc(doc(as('bob'), 'tribes', T, 'members', 'alice'), { todayStatus: 'missed' }));
});

test('DENY: member changes the tribe alarm directly', async () => {
  await assertFails(updateDoc(doc(as('bob'), 'tribes', T), { alarmTime: '04:00' }));
});

test('DENY: admin of a multi-member tribe changes the alarm directly', async () => {
  await assertFails(updateDoc(doc(as('alice'), 'tribes', T), { alarmTime: '04:00' }));
});

test('DENY: spoofing memberCount', async () => {
  await assertFails(updateDoc(doc(as('bob'), 'tribes', T), { memberCount: 1 }));
});

test('DENY: listing tribes or invites', async () => {
  const db = as('eve');
  await assertFails(getDocs(collection(db, 'tribes')));
  await assertFails(getDocs(query(collection(db, 'tribes'), where('inviteCode', '==', 'ABC123'), limit(1))));
  await assertFails(getDocs(collection(db, 'invites')));
});

test('DENY: joining without the right code, or as admin', async () => {
  const db = as('eve');
  const base = {
    displayName: 'e', photoUrl: null, todayStatus: 'sleeping', todayWakeTime: null,
    streakDays: 0, bestStreak: 0, totalWakes: 0, vetoesUsed: 0, rank: 1, joinedAt: serverTimestamp(),
  };
  let b = writeBatch(db);
  b.set(doc(db, 'tribes', T, 'members', 'eve'), { ...base, role: 'member', joinedWith: 'WRONG1' });
  b.update(doc(db, 'tribes', T), { memberCount: increment(1) });
  await assertFails(b.commit());

  b = writeBatch(db);
  b.set(doc(db, 'tribes', T, 'members', 'eve'), { ...base, role: 'admin', joinedWith: 'ABC123' });
  b.update(doc(db, 'tribes', T), { memberCount: increment(1) });
  await assertFails(b.commit());
});

test('DENY: joining without bumping memberCount', async () => {
  const db = as('eve');
  await assertFails(setDoc(doc(db, 'tribes', T, 'members', 'eve'), {
    displayName: 'e', photoUrl: null, role: 'member', todayStatus: 'sleeping', todayWakeTime: null,
    streakDays: 0, bestStreak: 0, totalWakes: 0, vetoesUsed: 0, rank: 1,
    joinedAt: serverTimestamp(), joinedWith: 'ABC123',
  }));
});

test('DENY: joining with a pre-inflated streak', async () => {
  const db = as('eve');
  const b = writeBatch(db);
  b.set(doc(db, 'tribes', T, 'members', 'eve'), {
    displayName: 'e', photoUrl: null, role: 'member', todayStatus: 'sleeping', todayWakeTime: null,
    streakDays: 50, bestStreak: 50, totalWakes: 0, vetoesUsed: 0, rank: 1,
    joinedAt: serverTimestamp(), joinedWith: 'ABC123',
  });
  b.update(doc(db, 'tribes', T), { memberCount: increment(1) });
  await assertFails(b.commit());
});

test('DENY: outsider reads members', async () => {
  await assertFails(getDocs(collection(as('eve'), 'tribes', T, 'members')));
});

test('DENY: accepting before the window passes', async () => {
  await seedProposal('p1', { hoursAgo: 1 });
  const db = as('alice');
  const b = writeBatch(db);
  b.update(doc(db, 'tribes', T, 'proposals', 'p1'), { status: 'accepted' });
  b.update(doc(db, 'tribes', T), { alarmTime: '06:00', activeProposalId: null });
  await assertFails(b.commit());
});

test('DENY: accepting a stale (non-active) proposal', async () => {
  await seedProposal('old', { hoursAgo: 5, time: '05:00', active: false });
  const db = as('alice');
  const b = writeBatch(db);
  b.update(doc(db, 'tribes', T, 'proposals', 'old'), { status: 'accepted' });
  b.update(doc(db, 'tribes', T), { alarmTime: '05:00' });
  await assertFails(b.commit());
});

test('DENY: accepting with a different time than proposed', async () => {
  await seedProposal('p1', { hoursAgo: 3 });
  const db = as('alice');
  const b = writeBatch(db);
  b.update(doc(db, 'tribes', T, 'proposals', 'p1'), { status: 'accepted' });
  b.update(doc(db, 'tribes', T), { alarmTime: '04:00', activeProposalId: null });
  await assertFails(b.commit());
});

test('DENY: second proposal while one is active', async () => {
  await seedProposal('p1');
  const db = as('alice');
  const b = writeBatch(db);
  b.set(doc(db, 'tribes', T, 'proposals', 'p2'), {
    proposedBy: 'alice', proposedByName: 'A', proposedTime: '08:00', status: 'pending',
    vetoedBy: [], vetoCount: 0, createdAt: serverTimestamp(),
  });
  b.update(doc(db, 'tribes', T), { activeProposalId: 'p2' });
  await assertFails(b.commit());
});

test('DENY: back-dated proposal (client clock createdAt)', async () => {
  const db = as('bob');
  const b = writeBatch(db);
  b.set(doc(db, 'tribes', T, 'proposals', 'p1'), {
    proposedBy: 'bob', proposedByName: 'Bob', proposedTime: '06:00', status: 'pending',
    vetoedBy: [], vetoCount: 0, createdAt: Timestamp.fromMillis(Date.now() - 3 * 3600000),
  });
  b.update(doc(db, 'tribes', T), { activeProposalId: 'p1' });
  await assertFails(b.commit());
});

test('DENY: proposer vetoes own proposal', async () => {
  await seedProposal('p1', { by: 'bob' });
  const db = as('bob');
  const b = writeBatch(db);
  b.update(doc(db, 'tribes', T, 'proposals', 'p1'), { status: 'vetoed', vetoedBy: arrayUnion('bob'), vetoCount: increment(1) });
  b.update(doc(db, 'tribes', T, 'members', 'bob'), { vetoesUsed: increment(1) });
  b.update(doc(db, 'tribes', T), { activeProposalId: null });
  await assertFails(b.commit());
});

test('DENY: third veto', async () => {
  await seedProposal('p1');
  await env.withSecurityRulesDisabled(async (ctx) => {
    await updateDoc(doc(ctx.firestore(), 'tribes', T, 'members', 'alice'), { vetoesUsed: 2 });
  });
  const db = as('alice');
  const b = writeBatch(db);
  b.update(doc(db, 'tribes', T, 'proposals', 'p1'), { status: 'vetoed', vetoedBy: arrayUnion('alice'), vetoCount: increment(1) });
  b.update(doc(db, 'tribes', T, 'members', 'alice'), { vetoesUsed: increment(1) });
  b.update(doc(db, 'tribes', T), { activeProposalId: null });
  await assertFails(b.commit());
});

test('DENY: veto after the window', async () => {
  await seedProposal('p1', { hoursAgo: 3 });
  const db = as('alice');
  const b = writeBatch(db);
  b.update(doc(db, 'tribes', T, 'proposals', 'p1'), { status: 'vetoed', vetoedBy: arrayUnion('alice'), vetoCount: increment(1) });
  b.update(doc(db, 'tribes', T, 'members', 'alice'), { vetoesUsed: increment(1) });
  b.update(doc(db, 'tribes', T), { activeProposalId: null });
  await assertFails(b.commit());
});

test('DENY: deleting a tribe that still has members', async () => {
  await assertFails(deleteDoc(doc(as('alice'), 'tribes', T)));
});

test('DENY: hijacking an existing invite code', async () => {
  const db = as('eve');
  const b = writeBatch(db);
  b.set(doc(db, 'tribes', 'evil'), {
    name: 'Evil', description: '', inviteCode: 'ABC123', createdBy: 'eve', alarmTime: '07:30',
    alarmDays: [1, 2, 3, 4, 5], memberCount: 1, photoUrl: null, activeProposalId: null, createdAt: serverTimestamp(),
  });
  b.set(doc(db, 'tribes', 'evil', 'members', 'eve'), {
    displayName: 'e', photoUrl: null, role: 'admin', todayStatus: 'sleeping', todayWakeTime: null,
    streakDays: 0, bestStreak: 0, totalWakes: 0, vetoesUsed: 0, rank: 1, joinedAt: serverTimestamp(),
  });
  b.set(doc(db, 'invites', 'ABC123'), { tribeId: 'evil', createdAt: serverTimestamp() });
  await assertFails(b.commit());
});

test('DENY: reading another user\'s profile or alarm', async () => {
  const db = as('eve');
  await assertFails(getDoc(doc(db, 'users', 'bob')));
  await assertFails(getDoc(doc(db, 'user_alarms', 'bob')));
});
