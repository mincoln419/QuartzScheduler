package com.ideatec.spring_scheduler.scheduler;

import com.ideatec.spring_scheduler.actuator.JobMetrics;
import com.ideatec.spring_scheduler.checkpoint.CheckpointStore;
import com.ideatec.spring_scheduler.client.ReactiveWebClientClient;
import com.ideatec.spring_scheduler.config.JobConfig;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
public class QuartzJobWrapper implements Job {

    // Quartz Job은 기본 생성자가 필요
    public QuartzJobWrapper() {
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
        String jobId = jobDataMap.getString("jobId");
        
        log.info("Quartz 잡 실행 시작: {}", jobId);
        Instant startTime = Instant.now();
        
        try {
            // SchedulerContext에서 빈들을 가져옴
            ReactiveWebClientClient webClient = (ReactiveWebClientClient) context.getScheduler().getContext().get("webClient");
            CheckpointStore checkpointStore = (CheckpointStore) context.getScheduler().getContext().get("checkpointStore");
            JobMetrics jobMetrics = (JobMetrics) context.getScheduler().getContext().get("jobMetrics");
            
            // JobDataMap에서 JobConfig.Job 객체 재구성
            JobConfig.Job job = createJobFromDataMap(jobDataMap);
            
            if (job.getParallelism() != null && job.getParallelism() > 1) {
                // 병렬 처리
                executeJobInParallel(job, webClient);
            } else {
                // 단일 처리
                executeSingleRequest(job, webClient);
            }
            
            // 성공 체크포인트 저장
            checkpointStore.saveLastSuccess(jobId, startTime);
            jobMetrics.recordJobSuccess(jobId);
            
        } catch (Exception e) {
            log.error("Quartz 잡 실행 실패: {}", jobId, e);
            try {
                JobMetrics jobMetrics = (JobMetrics) context.getScheduler().getContext().get("jobMetrics");
                if (jobMetrics != null) {
                    jobMetrics.recordJobFailure(jobId);
                }
            } catch (Exception ex) {
                log.error("메트릭 기록 실패", ex);
            }
            throw new JobExecutionException(e);
        } finally {
            // 실행 시간 메트릭 기록
            try {
                JobMetrics jobMetrics = (JobMetrics) context.getScheduler().getContext().get("jobMetrics");
                if (jobMetrics != null) {
                    jobMetrics.recordJobExecutionTime(jobId, 
                        Duration.between(startTime, Instant.now()));
                }
            } catch (Exception e) {
                log.error("메트릭 기록 실패", e);
            }
        }
    }

    private JobConfig.Job createJobFromDataMap(JobDataMap jobDataMap) {
        JobConfig.Job job = new JobConfig.Job();
        job.setId(jobDataMap.getString("jobId"));
        job.setCron(jobDataMap.getString("cron"));
        job.setParallelism(jobDataMap.getIntValue("parallelism"));
        
        JobConfig.Job.Request request = new JobConfig.Job.Request();
        request.setUrl(jobDataMap.getString("url"));
        request.setMethod(jobDataMap.getString("method"));
        request.setTimeoutMs(jobDataMap.getIntValue("timeoutMs"));
        if (jobDataMap.containsKey("bodyFile")) {
            request.setBodyFile(jobDataMap.getString("bodyFile"));
        }
        
        JobConfig.Job.Request.Retry retry = new JobConfig.Job.Request.Retry();
        retry.setMax(jobDataMap.getIntValue("retryMax"));
        retry.setBackoffMs(jobDataMap.getIntValue("retryBackoffMs"));
        request.setRetry(retry);
        
        job.setRequest(request);
        return job;
    }

    private void executeJobInParallel(JobConfig.Job job, ReactiveWebClientClient webClient) {
        String jobId = job.getId();
        ExecutorService executor = Executors.newFixedThreadPool(
            job.getParallelism(), 
            r -> new Thread(r, "quartz-job-" + jobId + "-")
        );
        
        try {
            for (int i = 0; i < job.getParallelism(); i++) {
                executor.submit(() -> executeSingleRequest(job, webClient));
            }
        } finally {
            executor.shutdown();
        }
    }

    private void executeSingleRequest(JobConfig.Job job, ReactiveWebClientClient webClient) {
        webClient.executeRequest(job)
                .subscribe(
                    result -> log.debug("Quartz 잡 요청 성공: {}", job.getId()),
                    error -> log.error("Quartz 잡 요청 실패: {}", job.getId(), error)
                );
    }
}
