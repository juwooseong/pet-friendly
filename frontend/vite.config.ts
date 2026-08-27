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
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  test: {
    environment: 'jsdom',
    globals: true,
  }
});
