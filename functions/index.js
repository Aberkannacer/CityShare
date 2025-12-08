const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

exports.sendChatNotification = functions.firestore
    .document("conversations/{conversationId}/messages/{messageId}")
    .onCreate(async (snap, context) => {
      const data = snap.data();
      if (!data) return null;

      const receiverId = data.receiverId;
      const senderId = data.senderId;
      const text = data.text || "";

      if (!receiverId) return null;

      // Haal tokens op van de ontvanger
      const userDoc = await db.collection("users").doc(receiverId).get();
      const tokens = userDoc.get("fcmTokens") || [];
      if (!tokens.length) return null;

      // Haal afzender naam op
      const senderDoc = await db.collection("users").doc(senderId).get();
      const senderName = senderDoc.get("displayName") ||
            senderDoc.get("email") ||
            "Nieuw bericht";

      const message = {
        notification: {
          title: senderName,
          body: text.substring(0, 120),
        },
        data: {
          senderId: senderId || "",
          conversationId: context.params.conversationId || "",
        },
        tokens,
      };

      return admin.messaging().sendEachForMulticast(message);
    });
