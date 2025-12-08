// Luistert lokaal naar nieuwe chatberichten en stuurt push via FCM HTTP v1.
// Start: node scripts/watch-chat-notify.js
// Vereist: firebase-admin, service-account JSON (in projectroot: cityshare-dc0b2-1171d6151244.json)
// Opmerking: dit draait lokaal; voor automatische meldingen moet dit proces blijven draaien.

const admin = require('firebase-admin');
const path = require('path');

const keyPath = process.env.SERVICE_ACCOUNT_PATH || path.join(__dirname, '..', 'service-account.json'); // pas aan indien andere naam
admin.initializeApp({
  credential: admin.credential.cert(require(keyPath))
});

const db = admin.firestore();
const messaging = admin.messaging();
const seen = new Set();

console.log('Watching for new messages...');

db.collectionGroup('messages')
  .onSnapshot(snapshot => {
    snapshot.docChanges().forEach(change => {
      if (change.type !== 'added') return;
      const doc = change.doc;
      const data = doc.data();
      const id = doc.id;
      if (seen.has(id)) return;
      seen.add(id);

      const receiverId = data.receiverId;
      const senderId = data.senderId;
      const text = data.text || '';
      if (!receiverId) return;

      sendNotification(receiverId, senderId, text, doc.ref.parent.parent?.id || '');
    });
  }, err => {
    console.error('Listen error', err);
  });

async function sendNotification(receiverId, senderId, text, conversationId) {
  try {
    const userDoc = await db.collection('users').doc(receiverId).get();
    const tokens = userDoc.get('fcmTokens') || [];
    if (!tokens.length) {
      console.log('Geen tokens voor', receiverId);
      return;
    }
    const senderDoc = await db.collection('users').doc(senderId).get();
    const senderName = senderDoc.get('displayName') || senderDoc.get('email') || 'Nieuw bericht';

    const message = {
      notification: {
        title: senderName,
        body: text.substring(0, 120)
      },
      data: {
        senderId: senderId || '',
        conversationId: conversationId
      },
      tokens
    };

    const res = await messaging.sendEachForMulticast(message);
    console.log('Push verstuurd', res.successCount, 'ok', res.failureCount, 'fail');
  } catch (e) {
    console.error('Fout bij push', e.message || e);
  }
}
