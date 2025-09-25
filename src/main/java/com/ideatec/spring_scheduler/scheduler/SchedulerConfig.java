package com.ideatec.spring_scheduler.scheduler;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

@Configuration
@EnableScheduling
public class SchedulerConfig {

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean() {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.setOverwriteExistingJobs(true);
        factory.setAutoStartup(true);
        factory.setWaitForJobsToCompleteOnShutdown(true);
        factory.setQuartzProperties(quartzProperties());
        return factory;
    }

    @Bean
    public Scheduler scheduler(SchedulerFactoryBean factory) throws SchedulerException {
        return factory.getScheduler();
    }

    private java.util.Properties quartzProperties() {
        java.util.Properties props = new java.util.Properties();
        props.setProperty("org.quartz.scheduler.instanceName", "SpringScheduler");
        props.setProperty("org.quartz.scheduler.instanceId", "AUTO");
        props.setProperty("org.quartz.threadPool.threadCount", "8");
        props.setProperty("org.quartz.threadPool.threadNamePrefix", "quartz-");
        props.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");
        return props;
    }
}

