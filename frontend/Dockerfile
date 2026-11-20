# ---- 1단계: Vue 빌드 ----
# package-lock.json이 로컬 npm 11(Node 24)로 생성되어 있어 같은 메이저 버전을 사용한다.
FROM node:24-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
# Kakao 지도 키는 빌드 시점에 번들에 들어가므로 build arg로 받는다.
ARG VITE_KAKAO_MAP_APP_KEY
ENV VITE_KAKAO_MAP_APP_KEY=$VITE_KAKAO_MAP_APP_KEY
RUN npm run build

# ---- 2단계: Nginx로 정적 파일 제공 + /api 프록시 ----
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
