package com.ideatec.spring_scheduler.checkpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
public class CheckpointStore {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Path checkpointPath = Paths.get("state/checkpoints.json");
    private final ReentrantLock lock = new ReentrantLock();

    public CheckpointStore() {
        try {
            Files.createDirectories(checkpointPath.getParent());
        } catch (IOException e) {
            log.error("체크포인트 디렉토리 생성 실패", e);
        }
    }

    public synchronized void save(String jobId, Map<String, Object> state) {
        lock.lock();
        try {
            Map<String, Object> allCheckpoints = loadAll();
            allCheckpoints.put(jobId, state);
            
            // 원자적 쓰기: .tmp 파일로 쓰고 ATOMIC_MOVE로 교체
            Path tempPath = Paths.get(checkpointPath.toString() + ".tmp");
            Files.writeString(
                tempPath,
                objectMapper.writeValueAsString(allCheckpoints),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            );
            
            Files.move(tempPath, checkpointPath, 
                StandardCopyOption.ATOMIC_MOVE, 
                StandardCopyOption.REPLACE_EXISTING);
                
            log.debug("체크포인트 저장 완료: jobId={}", jobId);
        } catch (IOException e) {
            log.error("체크포인트 저장 실패: jobId={}", jobId, e);
        } finally {
            lock.unlock();
        }
    }

    public Map<String, Object> load(String jobId) {
        Map<String, Object> allCheckpoints = loadAll();
        return (Map<String, Object>) allCheckpoints.getOrDefault(jobId, new HashMap<>());
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> loadAll() {
        if (!Files.exists(checkpointPath)) {
            return new HashMap<>();
        }
        
        try {
            String content = Files.readString(checkpointPath);
            return objectMapper.readValue(content, Map.class);
        } catch (IOException e) {
            log.error("체크포인트 로드 실패", e);
            return new HashMap<>();
        }
    }

    public void saveLastSuccess(String jobId, Instant timestamp) {
        Map<String, Object> state = new HashMap<>();
        state.put("lastSuccess", timestamp.toString());
        state.put("lastUpdate", Instant.now().toString());
        save(jobId, state);
    }

    public Instant getLastSuccess(String jobId) {
        Map<String, Object> state = load(jobId);
        String lastSuccessStr = (String) state.get("lastSuccess");
        if (lastSuccessStr != null) {
            try {
                return Instant.parse(lastSuccessStr);
            } catch (Exception e) {
                log.warn("마지막 성공 시간 파싱 실패: jobId={}, value={}", jobId, lastSuccessStr);
            }
        }
        return null;
    }

    public void saveOffset(String jobId, String offset) {
        Map<String, Object> state = load(jobId);
        state.put("offset", offset);
        state.put("lastUpdate", Instant.now().toString());
        save(jobId, state);
    }

    public String getOffset(String jobId) {
        Map<String, Object> state = load(jobId);
        return (String) state.get("offset");
    }
}

