// Usage: node scripts/send-fcm.js <device_fcm_token>
// Requires: npm install axios google-auth-library
// Expects service account JSON path via env SERVICE_ACCOUNT_PATH (or ./service-account.json)

const { GoogleAuth } = require('google-auth-library');
const axios = require('axios');
const path = require('path');

const projectId = 'cityshare-dc0b2'; // change if your Firebase project ID differs
const keyFile = process.env.SERVICE_ACCOUNT_PATH || path.join(__dirname, '..', 'service-account.json');
const targetToken = process.argv[2];

if (!targetToken) {
  console.error('Geef een device FCM token mee: node scripts/send-fcm.js <token>');
  process.exit(1);
}

async function main() {
  const auth = new GoogleAuth({
    scopes: ['https://www.googleapis.com/auth/firebase.messaging'],
    keyFile
  });
  const client = await auth.getClient();
  const { token } = await client.getAccessToken();

  const message = {
    message: {
      token: targetToken,
      notification: {
        title: 'Nieuw bericht',
        body: 'Je hebt een nieuw chatbericht'
      },
      data: {
        route: '/chat'
      }
    }
  };

  await axios.post(
    `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`,
    message,
    { headers: { Authorization: `Bearer ${token}` } }
  );
  console.log('Verstuurd naar token:', targetToken);
}

main().catch(err => {
  console.error('Fout bij versturen:', err.response?.data || err.message);
  process.exit(1);
});
