package com.ideatec.spring_scheduler.client;

import com.ideatec.spring_scheduler.config.JobConfig;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class ReactiveWebClientClient {
    private final WebClient webClient;

    public ReactiveWebClientClient() {
        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create()
                                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                                .responseTimeout(Duration.ofSeconds(30))
                                .doOnConnected(conn -> 
                                    conn.addHandlerLast(new ReadTimeoutHandler(30, TimeUnit.SECONDS))
                                        .addHandlerLast(new WriteTimeoutHandler(30, TimeUnit.SECONDS))
                                )
                ))
                .build();
    }

    public Mono<String> executeRequest(JobConfig.Job job) {
        JobConfig.Job.Request request = job.getRequest();
        
        WebClient.RequestBodySpec requestSpec = webClient
                .method(HttpMethod.valueOf(request.getMethod()))
                .uri(request.getUrl());

        // POST 요청의 경우 body 처리
        if (HttpMethod.POST.equals(HttpMethod.valueOf(request.getMethod())) && request.getBodyFile() != null) {
            // TODO: bodyFile에서 데이터 읽어서 설정
            requestSpec.bodyValue("{}"); // 임시로 빈 JSON
        }

        return requestSpec
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(request.getTimeoutMs()))
                .retryWhen(Retry.backoff(
                        request.getRetry().getMax(),
                        Duration.ofMillis(request.getRetry().getBackoffMs())
                ))
                .doOnSuccess(response -> 
                    log.info("Job [{}] 성공: {}", job.getId(), response.length() > 100 ? 
                        response.substring(0, 100) + "..." : response)
                )
                .doOnError(error -> 
                    log.error("Job [{}] 실패: {}", job.getId(), error.getMessage())
                )
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.error("Job [{}] HTTP 오류: {} {}", job.getId(), ex.getStatusCode(), ex.getResponseBodyAsString());
                    return Mono.just("ERROR: " + ex.getStatusCode());
                })
                .onErrorResume(Exception.class, ex -> {
                    log.error("Job [{}] 일반 오류: {}", job.getId(), ex.getMessage());
                    return Mono.just("ERROR: " + ex.getMessage());
                });
    }

    public Mono<String> get(String url) {
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .retryWhen(Retry.backoff(3, Duration.ofMillis(200)));
    }
}

