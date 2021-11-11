package com.gaian.service.enensyscloud.config;

import static java.util.concurrent.Executors.newFixedThreadPool;

import java.util.concurrent.ExecutorService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {

    private ExecutorService seqExecutorService;

    @Bean
    @Qualifier("seqExecutorService")
    public ExecutorService seqExecutor() {
        seqExecutorService = newFixedThreadPool(1);
        return seqExecutorService;
    }
}
