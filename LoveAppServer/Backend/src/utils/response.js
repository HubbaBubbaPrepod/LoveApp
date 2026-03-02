// src/utils/response.js – Unified API response helper
/**
 * Send a JSON API response.
 * @param {import('express').Response} res
 * @param {boolean} success
 * @param {string} message
 * @param {*} data
 * @param {number} statusCode
 */
function sendResponse(res, success, message, data = null, statusCode = 200) {
  const body = { success, message };
  if (data !== null && data !== undefined) body.data = data;
  return res.status(statusCode).json(body);
}

module.exports = { sendResponse };
