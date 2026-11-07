FROM node:24-bookworm-slim AS dependencies
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

FROM dependencies AS build
ARG MEDORA_API_ORIGIN=http://api:8080
ARG NEXT_PUBLIC_FACILITY_ZONE=Asia/Ho_Chi_Minh
ARG NEXT_PUBLIC_FACILITY_NAMES={}
ENV MEDORA_API_ORIGIN=$MEDORA_API_ORIGIN \
    NEXT_PUBLIC_FACILITY_ZONE=$NEXT_PUBLIC_FACILITY_ZONE \
    NEXT_PUBLIC_FACILITY_NAMES=$NEXT_PUBLIC_FACILITY_NAMES \
    NEXT_TELEMETRY_DISABLED=1 \
    MEDORA_STANDALONE=true
COPY . .
RUN npm run build

FROM node:24-bookworm-slim AS runtime
WORKDIR /app
ENV NODE_ENV=production NEXT_TELEMETRY_DISABLED=1 PORT=3000 HOSTNAME=0.0.0.0
COPY --from=build --chown=node:node /app/.next/standalone ./
COPY --from=build --chown=node:node /app/.next/static ./.next/static
USER node
EXPOSE 3000
HEALTHCHECK --interval=15s --timeout=5s --start-period=20s --retries=5 \
  CMD node -e "fetch('http://127.0.0.1:3000').then(r=>process.exit(r.ok?0:1)).catch(()=>process.exit(1))"
CMD ["node", "server.js"]
