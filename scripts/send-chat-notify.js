// Stuur een chat push naar alle FCM tokens van een userId.
// Gebruik: node scripts/send-chat-notify.js <receiverUid> "Titel" "Body"
// Vereist: npm install (firebase-admin al in package.json)

const admin = require('firebase-admin');
const path = require('path');

const keyPath = path.join(__dirname, '..', 'cityshare-dc0b2-1171d6151244.json');
if (!process.argv[2]) {
  console.error('Gebruik: node scripts/send-chat-notify.js <receiverUid> "Titel" "Body"');
  process.exit(1);
}

const receiverUid = process.argv[2];
const title = process.argv[3] || 'Nieuw bericht';
const body = process.argv[4] || 'Je hebt een nieuw chatbericht';

admin.initializeApp({
  credential: admin.credential.cert(require(keyPath))
});

const db = admin.firestore();
const messaging = admin.messaging();

async function main() {
  const userDoc = await db.collection('users').doc(receiverUid).get();
  const tokens = userDoc.get('fcmTokens') || [];
  if (!tokens.length) {
    console.log('Geen tokens gevonden voor', receiverUid);
    return;
  }

  const message = {
    notification: { title, body },
    tokens
  };

  const res = await messaging.sendEachForMulticast(message);
  console.log('Result:', res.successCount, 'succes', res.failureCount, 'fail');
}

main().catch(err => {
  console.error(err);
  process.exit(1);
});
