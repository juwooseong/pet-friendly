/// <reference types="vitest/config" />
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import path from 'path';

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src')
    }
  },
  server: {
    port: 3000,
    // 기본값(host 미지정)은 일부 환경에서 IPv6 루프백(::1)에만 바인딩되어 127.0.0.1 기반 호스트명이
    // 연결 거부될 수 있어 IPv4 루프백도 명시적으로 포함시킨다.
    host: '127.0.0.1',
    // Kakao 개발자 콘솔 Web 플랫폼 도메인(http://localtest.me:3000)으로 접속해 SDK를 검증할 수 있도록 허용.
    allowedHosts: ['localtest.me'],
    proxy: {
      '/api': {
        // E2E(TSK-QA-002)는 별도 포트의 백엔드로 보내기 위해 환경변수로 바꿀 수 있다.
        target: process.env.VITE_API_PROXY_TARGET ?? 'http://localhost:8080',
        changeOrigin: true,
        // E2E 서버(포트 3100)는 백엔드 CORS 허용 목록에 없다. 프록시를 거치는 요청은 브라우저 입장에서 같은 출처이므로
        // E2E일 때만 Origin 헤더를 지워 CORS 검사를 받지 않게 한다(백엔드 CORS 설정은 그대로).
        configure: process.env.VITE_API_PROXY_TARGET
          ? (proxy) => proxy.on('proxyReq', (proxyReq) => proxyReq.removeHeader('origin'))
          : undefined
      }
    }
  },
  test: {
    environment: 'jsdom',
    globals: true,
  }
});
