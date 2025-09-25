package com.ideatec.spring_scheduler.actuator;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobMetrics {
    private final MeterRegistry meterRegistry;
    private final Map<String, Counter> successCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> failureCounters = new ConcurrentHashMap<>();
    private final Map<String, Timer> executionTimers = new ConcurrentHashMap<>();

    public void recordJobSuccess(String jobId) {
        getSuccessCounter(jobId).increment();
        log.debug("잡 성공 메트릭 기록: {}", jobId);
    }

    public void recordJobFailure(String jobId) {
        getFailureCounter(jobId).increment();
        log.debug("잡 실패 메트릭 기록: {}", jobId);
    }

    public void recordJobExecutionTime(String jobId, Duration duration) {
        getExecutionTimer(jobId).record(duration);
        log.debug("잡 실행 시간 메트릭 기록: {} - {}", jobId, duration);
    }

    private Counter getSuccessCounter(String jobId) {
        return successCounters.computeIfAbsent(jobId, 
            id -> Counter.builder("job.success")
                .tag("job_id", id)
                .description("Number of successful job executions")
                .register(meterRegistry));
    }

    private Counter getFailureCounter(String jobId) {
        return failureCounters.computeIfAbsent(jobId,
            id -> Counter.builder("job.failure")
                .tag("job_id", id)
                .description("Number of failed job executions")
                .register(meterRegistry));
    }

    private Timer getExecutionTimer(String jobId) {
        return executionTimers.computeIfAbsent(jobId,
            id -> Timer.builder("job.execution.time")
                .tag("job_id", id)
                .description("Job execution time")
                .register(meterRegistry));
    }
}
