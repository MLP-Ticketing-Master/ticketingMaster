# 1단계 빌드 — Vite 정적 파일 생성
FROM node:20 AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
# VITE_ 변수는 빌드 시점에 박히므로 build arg 로 주입
ARG VITE_API_BASE_URL=/api
ARG VITE_TOSS_CLIENT_KEY
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_TOSS_CLIENT_KEY=$VITE_TOSS_CLIENT_KEY
RUN npm run build

# 2단계 서빙 — nginx 로 dist 노출
FROM nginx:stable-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY cache.conf /etc/nginx/conf.d/00-cache.conf
EXPOSE 80