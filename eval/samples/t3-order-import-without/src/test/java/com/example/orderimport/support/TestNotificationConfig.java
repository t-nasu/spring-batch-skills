package com.example.orderimport.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * 通知の宛先をテスト用の記録係に差し替える。
 */
@TestConfiguration
public class TestNotificationConfig {

    @Bean
    @Primary
    public RecordingNotificationGateway recordingNotificationGateway() {
        return new RecordingNotificationGateway();
    }
}
