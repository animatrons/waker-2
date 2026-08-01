package com.waker.commitment;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({CommitmentProperties.class, SweepProperties.class})
public class CommitmentConfig {}
