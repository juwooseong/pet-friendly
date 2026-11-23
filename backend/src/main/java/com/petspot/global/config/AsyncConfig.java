package com.petspot.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * {@code @Async} 활성화 (TSK-SEC-003: 메일 발송을 요청 스레드와 분리).
 * 실행기는 Spring Boot 기본 {@code applicationTaskExecutor}를 쓴다.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
