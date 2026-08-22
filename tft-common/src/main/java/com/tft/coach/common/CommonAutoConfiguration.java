package com.tft.coach.common;

import com.tft.coach.common.flags.FeatureFlags;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "com.tft.coach.common")
@EnableConfigurationProperties(FeatureFlags.class)
public class CommonAutoConfiguration {
}
