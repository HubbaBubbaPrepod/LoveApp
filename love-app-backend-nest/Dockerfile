# ── Build stage ────────────────────────────────────────
FROM node:20-alpine AS builder
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

# ── Production stage ──────────────────────────────────
FROM node:20-alpine
WORKDIR /app

COPY package*.json ./
RUN npm ci --omit=dev && npm cache clean --force
RUN npm install pm2 -g

COPY --from=builder /app/dist ./dist
COPY ecosystem.config.js .

RUN mkdir -p logs uploads

EXPOSE 3005

HEALTHCHECK --interval=30s --timeout=10s --start-period=20s --retries=3 \
  CMD wget -qO- http://localhost:3005/health || exit 1

CMD ["pm2-runtime", "ecosystem.config.js", "--env", "production"]
