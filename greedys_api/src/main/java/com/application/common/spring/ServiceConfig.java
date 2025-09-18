package com.application.common.spring;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;


@Configuration
@ComponentScan({ "com.application.service" })
public class ServiceConfig {

}
