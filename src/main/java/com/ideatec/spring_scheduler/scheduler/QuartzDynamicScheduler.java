package com.ideatec.spring_scheduler.scheduler;

import com.ideatec.spring_scheduler.actuator.JobMetrics;
import com.ideatec.spring_scheduler.checkpoint.CheckpointStore;
import com.ideatec.spring_scheduler.client.ReactiveWebClientClient;
import com.ideatec.spring_scheduler.config.JobConfig;
import com.ideatec.spring_scheduler.config.JobConfigLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuartzDynamicScheduler {
    private final Scheduler scheduler;
    private final JobConfigLoader configLoader;
    private final ReactiveWebClientClient webClient;
    private final CheckpointStore checkpointStore;
    private final JobMetrics jobMetrics;
    private final Map<String, JobDetail> scheduledJobs = new ConcurrentHashMap<>();

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul") // 매분 설정 스캔
    public void schedule() {
        try {
            // SchedulerContext에 Spring 빈들을 추가
            setupSchedulerContext();
            
            JobConfig config = configLoader.load();
            if (config.getJobs() == null) {
                return;
            }

            // 설정 변경 감지 및 리스케줄
            for (JobConfig.Job job : config.getJobs()) {
                ensureScheduled(job);
            }

            // 제거된 잡 정리
            scheduledJobs.keySet().removeIf(jobId -> 
                config.getJobs().stream().noneMatch(job -> job.getId().equals(jobId))
            );

        } catch (Exception e) {
            log.error("Quartz 스케줄링 중 오류 발생", e);
        }
    }

    private void setupSchedulerContext() {
        try {
            SchedulerContext schedulerContext = scheduler.getContext();
            schedulerContext.put("webClient", webClient);
            schedulerContext.put("checkpointStore", checkpointStore);
            schedulerContext.put("jobMetrics", jobMetrics);
        } catch (SchedulerException e) {
            log.error("SchedulerContext 설정 실패", e);
        }
    }

    private void ensureScheduled(JobConfig.Job job) {
        String jobId = job.getId();
        JobDetail existingJob = scheduledJobs.get(jobId);
        
        try {
            // 새로운 잡이거나 설정이 변경된 경우
            if (existingJob == null || isJobConfigChanged(existingJob, job)) {
                log.info("Quartz 잡 스케줄링: {} (cron: {})", jobId, job.getCron());
                
                // 기존 잡이 있으면 삭제
                if (existingJob != null) {
                    scheduler.deleteJob(JobKey.jobKey(jobId));
                }
                
                // 새 잡 생성 및 스케줄링
                JobDetail jobDetail = createJobDetail(job);
                Trigger trigger = createTrigger(job);
                
                scheduler.scheduleJob(jobDetail, trigger);
                scheduledJobs.put(jobId, jobDetail);
            }
        } catch (SchedulerException e) {
            log.error("Quartz 잡 스케줄링 실패: {}", jobId, e);
        }
    }

    private JobDetail createJobDetail(JobConfig.Job job) {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("jobId", job.getId());
        jobDataMap.put("cron", job.getCron());
        jobDataMap.put("parallelism", job.getParallelism());
        jobDataMap.put("url", job.getRequest().getUrl());
        jobDataMap.put("method", job.getRequest().getMethod());
        jobDataMap.put("timeoutMs", job.getRequest().getTimeoutMs());
        jobDataMap.put("retryMax", job.getRequest().getRetry().getMax());
        jobDataMap.put("retryBackoffMs", job.getRequest().getRetry().getBackoffMs());
        if (job.getRequest().getBodyFile() != null) {
            jobDataMap.put("bodyFile", job.getRequest().getBodyFile());
        }
        
        return JobBuilder.newJob(QuartzJobWrapper.class)
                .withIdentity(job.getId())
                .withDescription("Dynamic job: " + job.getId())
                .setJobData(jobDataMap)
                .storeDurably()
                .build();
    }

    private Trigger createTrigger(JobConfig.Job job) {
        return TriggerBuilder.newTrigger()
                .withIdentity(job.getId() + "-trigger")
                .withDescription("Trigger for job: " + job.getId())
                .withSchedule(CronScheduleBuilder.cronSchedule(job.getCron())
                        .inTimeZone(java.util.TimeZone.getTimeZone("Asia/Seoul")))
                .build();
    }

    private boolean isJobConfigChanged(JobDetail existingJob, JobConfig.Job currentJob) {
        JobDataMap existingData = existingJob.getJobDataMap();
        String existingCron = existingData.getString("cron");
        Integer existingParallelism = existingData.getIntValue("parallelism");
        String existingUrl = existingData.getString("url");
        
        return !Objects.equals(existingCron, currentJob.getCron()) ||
               !Objects.equals(existingParallelism, currentJob.getParallelism()) ||
               !Objects.equals(existingUrl, currentJob.getRequest().getUrl());
    }
}
