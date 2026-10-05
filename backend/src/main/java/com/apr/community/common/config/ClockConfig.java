package com.apr.community.common.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 현재 시각은 항상 이 Clock 으로 읽는다 (테스트에서 교체 가능). 저장 시각은 초 단위로 자른다 (Integration spec C4). */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
