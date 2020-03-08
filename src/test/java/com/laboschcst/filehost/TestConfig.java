package com.laboschcst.filehost;

import com.zaxxer.hikari.HikariDataSource;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;


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

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        return Mockito.mock(ClientRegistrationRepository.class);
    }
}
