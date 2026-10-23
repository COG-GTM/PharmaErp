# syntax=docker/dockerfile:1.7
# Multi-stage build: Vite production bundle -> nginx serving the SPA and proxying the API.
# Build context is frontend/ (see the root docker-compose.yml).

FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci --no-audit --no-fund
COPY index.html tsconfig.json tsconfig.app.json tsconfig.node.json vite.config.ts eslint.config.js ./
COPY src src
RUN npm run build

FROM nginx:1.27-alpine
# Upstream for /api, /login, /token, /actuator; the template is rendered by the nginx entrypoint (envsubst).
ENV BACKEND_URL=http://backend:8090
COPY nginx.conf /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=10s --retries=6 \
  CMD wget -q -O /dev/null http://127.0.0.1:8080/healthz || exit 1
