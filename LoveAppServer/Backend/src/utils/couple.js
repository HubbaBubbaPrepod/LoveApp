// src/utils/couple.js – Shared couple / partner utilities used by all route files

const pool = require('../config/db');
const { publish } = require('../config/redis');

/**
 * Get the partner_user_id for a given user, or null if unpaired.
 * @param {number} userId
 * @returns {Promise<number|null>}
 */
async function getPartnerId(userId) {
  const r = await pool.query(
    'SELECT partner_user_id FROM relationship_info WHERE user_id = $1 LIMIT 1',
    [userId]
  );
  return r.rows[0]?.partner_user_id || null;
}

/**
 * Build the deterministic couple room key from two user IDs.
 * Solo users get `solo_<userId>`.
 * @param {number} userId
 * @param {number|null} partnerId
 * @returns {string}
 */
function buildCoupleKey(userId, partnerId) {
  if (!partnerId) return `solo_${userId}`;
  return `${Math.min(userId, partnerId)}_${Math.max(userId, partnerId)}`;
}

/**
 * Broadcast a data-change event to the couple's Socket.IO room and publish
 * it over Redis so other API instances also fan out the message.
 *
 * @param {import('socket.io').Server|null} io
 * @param {string} coupleKey
 * @param {number} senderId
 * @param {string} entityType
 * @param {string} action
 * @param {object} data
 */
async function broadcastChange(io, coupleKey, senderId, entityType, action, data) {
  if (!io) return;
  const payload = {
    entityType,
    action,
    data,
    serverTimestamp: data?.server_updated_at?.toISOString?.() || new Date().toISOString(),
    senderId,
  };
  io.to(`couple:${coupleKey}`).except(String(senderId)).emit('data-change', payload);
  await publish('loveapp:data-changes', { coupleKey, senderId: String(senderId), payload });
}

module.exports = { getPartnerId, buildCoupleKey, broadcastChange };
