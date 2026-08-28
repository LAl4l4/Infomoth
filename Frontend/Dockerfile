ARG IMAGE_PREFIX=docker.1ms.run/library
FROM ${IMAGE_PREFIX}/node:24-alpine AS build
RUN corepack enable
WORKDIR /app
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./

RUN pnpm install --frozen-lockfile
COPY . .
RUN pnpm build

FROM ${IMAGE_PREFIX}/nginx:alpine
RUN apk add --no-cache jq
COPY --from=build /app/build /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY docker-entrypoint.d/40-generate-runtime-config.sh /docker-entrypoint.d/40-generate-runtime-config.sh
RUN chmod +x /docker-entrypoint.d/40-generate-runtime-config.sh
EXPOSE 80
EXPOSE 443
