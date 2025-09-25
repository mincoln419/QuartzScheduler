package com.ideatec.spring_scheduler.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Slf4j
@Component
public class JobConfigLoader {
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private final Map<String, JobConfig.Job> jobCache = new ConcurrentHashMap<>();
    private volatile JobConfig lastConfig;
    private long lastModified = 0;

    public JobConfig load() {
        try {
            // 1. 외부 설정 파일 우선 확인 (JAR과 같은 디렉토리)
            Path externalConfigPath = Paths.get("scheduler-jobs.yml");
            Path configPath;
            
            if (Files.exists(externalConfigPath)) {
                // 외부 설정 파일이 있으면 사용
                configPath = externalConfigPath;
                log.debug("외부 설정 파일 사용: {}", configPath.toAbsolutePath());
            } else {
                // 외부 설정 파일이 없으면 JAR 내부 리소스 사용
                ClassPathResource resource = new ClassPathResource("scheduler-jobs.yml");
                configPath = Paths.get(resource.getURI());
                log.debug("JAR 내부 설정 파일 사용: {}", configPath.toAbsolutePath());
            }
            
            // 파일 변경 감지
            long currentModified = Files.getLastModifiedTime(configPath).toMillis();
            if (currentModified > lastModified || lastConfig == null) {
                log.info("설정 파일이 변경되었습니다. 다시 로드합니다.");
                lastConfig = yamlMapper.readValue(Files.newInputStream(configPath), JobConfig.class);
                lastModified = currentModified;
                
                // 캐시 업데이트
                jobCache.clear();
                if (lastConfig.getJobs() != null) {
                    lastConfig.getJobs().forEach(job -> jobCache.put(job.getId(), job));
                }
            }
            
            return lastConfig;
        } catch (IOException e) {
            log.error("설정 파일 로드 중 오류 발생", e);
            return lastConfig != null ? lastConfig : new JobConfig();
        }
    }

    public JobConfig.Job getJob(String jobId) {
        return jobCache.get(jobId);
    }

    public Map<String, JobConfig.Job> getAllJobs() {
        return Map.copyOf(jobCache);
    }
}

