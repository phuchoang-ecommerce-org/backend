package org.phuchoang.ecp.redis;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * The two-instance Redis topology (`ADR-0034`). Two independent connection factories, deliberately
 * <b>neither</b> {@code @Primary} — an unqualified {@code RedisConnectionFactory}/{@code
 * RedisTemplate} injection is ambiguous by construction, so a developer must name which instance
 * they mean (`ADR-0034` §5.5, Backend Architecture.md §5.5, §9 rule B4).
 */
@Configuration
public class RedisConfig {

    @Bean
    public LettuceConnectionFactory cacheConnectionFactory(
            @Value("${ecp.redis.cache.host:localhost}") String host,
            @Value("${ecp.redis.cache.port:6379}") int port) {
        return connectionFactory(host, port, Duration.ofMillis(150));
    }

    @Bean
    public LettuceConnectionFactory stateConnectionFactory(
            @Value("${ecp.redis.state.host:localhost}") String host,
            @Value("${ecp.redis.state.port:6380}") int port) {
        return connectionFactory(host, port, Duration.ofMillis(250));
    }

    private LettuceConnectionFactory connectionFactory(String host, int port, Duration commandTimeout) {
        RedisStandaloneConfiguration standalone = new RedisStandaloneConfiguration(host, port);
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
            .commandTimeout(commandTimeout)
            .shutdownTimeout(Duration.ofMillis(100))
            .build();
        LettuceConnectionFactory factory = new LettuceConnectionFactory(standalone, clientConfig);
        factory.afterPropertiesSet();
        return factory;
    }

    @Bean
    public RedisTemplate<String, Object> cacheRedisTemplate(
            @Qualifier("cacheConnectionFactory") RedisConnectionFactory cacheConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(cacheConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJacksonJsonRedisSerializer(tools.jackson.databind.json.JsonMapper.builder().build()));
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisTemplate<String, String> stateRedisTemplate(
            @Qualifier("stateConnectionFactory") RedisConnectionFactory stateConnectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(stateConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}
