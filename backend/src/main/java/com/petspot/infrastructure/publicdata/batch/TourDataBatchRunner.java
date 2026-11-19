package com.petspot.infrastructure.publicdata.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

/**
 * 수집 배치를 애플리케이션 시작 시 한 번 실행한다 (TSK-DATA-010). 예약 시각을 기다리지 않고 바로 적재할 때 사용한다.
 *
 * <p>기본으로는 등록되지 않는다({@code public-data.batch.run-on-startup=true}일 때만). 운영 서버와 별도로 한 번만
 * 실행하고 끝내는 용도라면 웹 서버 없이 띄우고 실행 후 종료한다:
 * <pre>
 * ./gradlew bootRun --args="--spring.profiles.active=local --spring.main.web-application-type=none
 *     --spring.devtools.restart.enabled=false
 *     --public-data.batch.run-on-startup=true --public-data.batch.exit-after-run=true"
 * </pre>
 * <p><b>주의:</b> {@code spring.devtools.restart.enabled=false}를 빼면, 실행 도중 IDE 자동 빌드 등으로 클래스가 바뀔 때
 * DevTools가 앱을 재시작하면서 배치가 처음부터 다시 실행된다(2026-11-19 실제 발생: 목록·정책 호출 약 900회 중복).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "public-data.batch.run-on-startup", havingValue = "true")
public class TourDataBatchRunner implements ApplicationRunner {

    private final TourDataBatch tourDataBatch;
    private final ConfigurableApplicationContext context;

    @Value("${public-data.batch.exit-after-run:false}")
    private boolean exitAfterRun;

    @Override
    public void run(ApplicationArguments args) {
        log.info("[BATCH RUNNER] run-on-startup=true -> 수집 배치를 지금 한 번 실행합니다.");
        TourDataBatch.BatchResult result = tourDataBatch.runOnce();
        log.info("[BATCH RUNNER] 완료: {}", result);
        if (exitAfterRun) {
            log.info("[BATCH RUNNER] exit-after-run=true -> 애플리케이션을 종료합니다.");
            System.exit(SpringApplication.exit(context, () -> 0));
        }
    }
}
