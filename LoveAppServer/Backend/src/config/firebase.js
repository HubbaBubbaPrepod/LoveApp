// src/config/firebase.js – Firebase Admin SDK initialisation
const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');
const logger = require('./logger');

function initFirebase() {
  try {
    let serviceAccount;
    if (process.env.FIREBASE_SERVICE_ACCOUNT) {
      serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
    } else {
      const saPath = path.join(__dirname, '../../service-account.json');
      serviceAccount = JSON.parse(fs.readFileSync(saPath, 'utf8'));
    }
    admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
    logger.info('Firebase Admin SDK initialised — push notifications enabled');
  } catch (e) {
    logger.warn(`Firebase Admin SDK NOT initialised: ${e.message}`);
    logger.warn('Push notifications disabled. Add service-account.json to enable them.');
  }
}

module.exports = { initFirebase, admin };
