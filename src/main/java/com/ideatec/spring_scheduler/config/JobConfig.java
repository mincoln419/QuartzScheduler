package com.ideatec.spring_scheduler.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobConfig {
    private String timezone;
    private List<Job> jobs;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Job {
        private String id;
        private String cron;
        private Integer parallelism;
        private Request request;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Request {
            private String url;
            private String method;
            private Integer timeoutMs;
            private String bodyFile;
            private Retry retry;

            @Data
            @NoArgsConstructor
            @AllArgsConstructor
            public static class Retry {
                private Integer max;
                private Integer backoffMs;
            }
        }
    }
}

