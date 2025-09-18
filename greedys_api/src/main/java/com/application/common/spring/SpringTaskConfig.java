package com.application.common.spring;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration class that enables scheduling and component scanning for task-related beans.
 * Specifically, it scans for components in the "com.application.common.task" package,
 * such as TokenSpirgeTask, to enable scheduled task execution within the Spring application context.
 */
@Configuration
@EnableScheduling
@ComponentScan({ "com.application.common.task" })
public class SpringTaskConfig {

}
