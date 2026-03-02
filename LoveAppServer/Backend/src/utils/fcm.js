// src/utils/fcm.js – FCM push notification helpers
const { admin } = require('../config/firebase');
const pool = require('../config/db');
const logger = require('../config/logger');

function moodToEmoji(type) {
  const map = {
    great: '😄', отлично: '😄', good: '🙂', хорошо: '🙂',
    okay: '😐', нормально: '😐', bad: '😔', плохо: '😔',
    terrible: '😢', ужасно: '😢',
  };
  return map[(type || '').toLowerCase()] || '💬';
}

/**
 * Send a data-only FCM push to the partner of the given userId.
 * @param {number} userId - the sender's user id
 * @param {object} data   - { type, moodType?, count?, isNewCycle?, destination? }
 */
async function sendPushToPartner(userId, data) {
  if (!admin.apps.length) return;
  try {
    const relRes = await pool.query(
      'SELECT partner_user_id FROM relationship_info WHERE user_id = $1 LIMIT 1',
      [userId]
    );
    const partnerId = relRes.rows[0]?.partner_user_id;
    if (!partnerId) return;

    const userRes = await pool.query(
      'SELECT display_name FROM users WHERE id = $1',
      [userId]
    );
    const senderName = userRes.rows[0]?.display_name || 'Partner';

    const tokenRes = await pool.query(
      'SELECT fcm_token FROM fcm_tokens WHERE user_id = $1',
      [partnerId]
    );
    const fcmToken = tokenRes.rows[0]?.fcm_token;
    if (!fcmToken) return;

    let title, body;
    if (data.type === 'partner_mood') {
      const emoji = moodToEmoji(data.moodType);
      title = `${senderName} shared a mood ${emoji}`;
      body  = 'Open the app to see how they feel';
    } else if (data.type === 'partner_cycle') {
      title = data.isNewCycle === true || data.isNewCycle === 'true'
        ? `${senderName} started a new cycle 🌸`
        : `${senderName} updated cycle data 🌸`;
      body = 'Open the app to view';
    } else if (data.type === 'data_change') {
      title = `${senderName} made a change`;
      body = 'Tap to sync the latest updates';
    } else {
      const c = data.count || 1;
      title = `${senderName} added ${c === 1 ? '1 activity' : `${c} activities`} 🏃`;
      body  = 'Check what your partner has been up to';
    }

    await admin.messaging().send({
      token: fcmToken,
      data: {
        type:        String(data.type),
        partnerName: senderName,
        title,
        body,
        destination: data.destination || '',
        ...(data.moodType    ? { moodType: data.moodType, moodEmoji: moodToEmoji(data.moodType) } : {}),
        ...(data.count != null ? { count: String(data.count) } : {}),
        ...(data.isNewCycle != null ? { isNewCycle: String(data.isNewCycle) } : {}),
      },
      android: { priority: 'high' },
    });
  } catch (e) {
    logger.error(`FCM send error: ${e.message}`);
  }
}

module.exports = { sendPushToPartner, moodToEmoji };
