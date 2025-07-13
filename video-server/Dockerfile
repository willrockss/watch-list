# Build stage
FROM node:20-alpine AS builder
WORKDIR /build
COPY package*.json ./
# RUN npm install
# Use cache mount for npm cache
RUN --mount=type=cache,target=/root/.npm \
    npm ci --production
COPY . .

# Runtime stage
FROM node:20-alpine
WORKDIR /app
COPY --from=builder /build/package*.json ./
COPY --from=builder /build/node_modules ./node_modules
COPY --from=builder /build/index.js ./

EXPOSE 8000
CMD ["node", "index.js"]
