package com.laboschqpa.filehost;

import com.zaxxer.hikari.HikariDataSource;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;


@TestConfiguration
public class TestConfig {

    @Bean
    public HikariDataSource dataSource() {
        return Mockito.mock(HikariDataSource.class);
    }

    @Bean
    public PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();//Returning an empty bean
    }
}
