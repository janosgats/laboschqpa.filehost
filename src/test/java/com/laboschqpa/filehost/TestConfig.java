package com.laboschqpa.filehost;

import com.zaxxer.hikari.HikariDataSource;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;


@TestConfiguration
public class TestConfig {

    @Bean
    public HikariDataSource dataSource() {
        return Mockito.mock(HikariDataSource.class);
    }
}
