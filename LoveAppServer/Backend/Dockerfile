# Dockerfile – LoveApp Backend
FROM node:20-alpine AS base
WORKDIR /app

# Install dependencies
COPY package*.json ./
RUN npm install --omit=dev

# Install PM2 globally
RUN npm install pm2 -g

# Copy source
COPY . .

# Create logs directory
RUN mkdir -p logs

EXPOSE 3005

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=20s --retries=3 \
  CMD wget -qO- http://localhost:3005/api/health || exit 1

CMD ["pm2-runtime", "ecosystem.config.js", "--env", "production"]
