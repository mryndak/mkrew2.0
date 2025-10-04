FROM node:18-alpine AS builder

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm ci

# Copy source code
COPY . .

# Build the application (static output)
RUN npm run build

# Production stage
FROM node:18-alpine

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies (needed for astro preview)
RUN npm ci --only=production

# Copy built application from builder
COPY --from=builder /app/dist ./dist

# Expose port
EXPOSE 4321

# Use astro preview to serve static files
CMD ["npm", "run", "preview", "--", "--host", "0.0.0.0"]
