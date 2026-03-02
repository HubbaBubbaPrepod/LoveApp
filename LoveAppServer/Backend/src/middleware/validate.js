// src/middleware/validate.js
// Joi-based request validation middleware factory.
//
// Usage in a route file:
//   const { validate, schemas } = require('../middleware/validate');
//   router.post('/notes', validate(schemas.createNote), handler);
//
const Joi = require('joi');

// ─── Reusable field validators ────────────────────────────────────────────
const dateStr   = Joi.string().pattern(/^\d{4}-\d{2}-\d{2}$/).messages({
  'string.pattern.base': '{{#label}} must be in YYYY-MM-DD format',
});
const hexColor  = Joi.string().pattern(/^#[0-9A-Fa-f]{6}$/).messages({
  'string.pattern.base': '{{#label}} must be a 6-digit hex color like #FF6B9D',
});
const trimStr   = (min = 1, max = 500) => Joi.string().trim().min(min).max(max);

// ─── Schemas ──────────────────────────────────────────────────────────────
const schemas = {
  // Auth
  register: Joi.object({
    name:            trimStr(1, 100).required(),
    email:           Joi.string().email().required(),
    password:        Joi.string().min(8).max(100).required(),
    partnerCode:     Joi.string().alphanum().length(8).optional(),
  }),
  login: Joi.object({
    email:    Joi.string().email().required(),
    password: Joi.string().required(),
  }),
  refreshToken: Joi.object({
    refreshToken: Joi.string().required(),
  }),

  // Notes
  createNote: Joi.object({
    title:     trimStr(1, 200).required(),
    content:   trimStr(1, 10000).required(),
    isPrivate: Joi.boolean().default(false),
    tags:      Joi.string().max(500).default(''),
  }),
  updateNote: Joi.object({
    title:     trimStr(1, 200),
    content:   trimStr(1, 10000),
    isPrivate: Joi.boolean(),
    tags:      Joi.string().max(500),
  }).min(1),

  // Wishes
  createWish: Joi.object({
    title:       trimStr(1, 200).required(),
    description: trimStr(0, 2000).default(''),
    priority:    Joi.number().integer().min(0).max(2).default(0),
    category:    Joi.string().max(100).default(''),
    isPrivate:   Joi.boolean().default(false),
    imageUrls:   Joi.string().max(2000).optional(),
    emoji:       Joi.string().max(10).optional(),
  }),

  // Moods
  createMood: Joi.object({
    moodType: Joi.string().valid('excellent','good','neutral','sad','romantic','nervous','tired').required(),
    date:     dateStr.required(),
    note:     trimStr(0, 1000).default(''),
  }),

  // Activities
  createActivity: Joi.object({
    title:           trimStr(1, 200).required(),
    description:     trimStr(0, 2000).default(''),
    date:            dateStr.required(),
    category:        Joi.string().max(100).required(),
    activityType:    Joi.string().max(100),
    durationMinutes: Joi.number().integer().min(1).max(1440),
    startTime:       Joi.string().pattern(/^\d{2}:\d{2}$/),
    note:            trimStr(0, 1000).default(''),
  }),

  // Calendars
  createCalendar: Joi.object({
    name:        trimStr(1, 100).required(),
    description: trimStr(0, 500).default(''),
    type:        Joi.string().valid('special_dates','sex','sports','events','custom').required(),
    colorHex:    hexColor.required(),
  }),
  createCalendarEvent: Joi.object({
    eventDate: dateStr.required(),
    title:     trimStr(0, 200).default(''),
    note:      trimStr(0, 1000).default(''),
    emoji:     Joi.string().max(10).optional(),
  }),

  // Cycles
  createCycle: Joi.object({
    cycleStartDate: dateStr.required(),
    cycleDuration:  Joi.number().integer().min(21).max(45).default(28),
    periodDuration: Joi.number().integer().min(1).max(10).default(5),
    symptoms:       Joi.object().default({}),
    mood:           Joi.object().default({}),
    notes:          trimStr(0, 1000).default(''),
  }),

  // Relationship
  updateRelationship: Joi.object({
    relationshipStartDate: dateStr.required(),
    firstKissDate:         dateStr.optional(),
    anniversaryDate:       dateStr.optional(),
    myBirthday:            dateStr.optional(),
    partnerBirthday:       dateStr.optional(),
  }),

  // Pagination
  pagination: Joi.object({
    page:  Joi.number().integer().min(1).default(1),
    limit: Joi.number().integer().min(1).max(200).default(20),
  }),
};

// ─── Middleware factory ───────────────────────────────────────────────────
/**
 * @param {Joi.Schema} schema  - The Joi schema to validate against
 * @param {'body'|'query'|'params'} [source='body'] - Request property to validate
 */
function validate(schema, source = 'body') {
  return (req, res, next) => {
    const { error, value } = schema.validate(req[source], {
      abortEarly:    false,
      stripUnknown:  true,
      convert:       true,
    });
    if (error) {
      const details = error.details.map(d => ({ field: d.path.join('.'), message: d.message }));
      return res.status(400).json({ success: false, message: 'Validation failed', errors: details });
    }
    req[source] = value;   // replace with sanitised/coerced values
    next();
  };
}

module.exports = { validate, schemas };
