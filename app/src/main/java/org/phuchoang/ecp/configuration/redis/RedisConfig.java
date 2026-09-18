package org.phuchoang.ecp.configuration.redis;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;

/**
 * The two-instance Redis topology (`ADR-0034`). Two independent connection factories, deliberately
 * <b>neither</b> {@code @Primary} — an unqualified {@code RedisConnectionFactory}/{@code
 * RedisTemplate} injection is ambiguous by construction, so a developer must name which instance
 * they mean ({@code cacheRedisTemplate} / {@code stateRedisTemplate}; `ADR-0034` §5.5, Backend
 * Architecture.md §5.5, §9 rule B4). Cache commands time out sooner than state commands: a slow
 * cache is a miss, a slow rate limiter is a decision.
 */
@Configuration
@EnableConfigurationProperties(RedisProperties.class)
public class RedisConfig {

    private static final Duration CACHE_COMMAND_TIMEOUT = Duration.ofMillis(150);
    private static final Duration STATE_COMMAND_TIMEOUT = Duration.ofMillis(250);

    @Bean
    public LettuceConnectionFactory cacheConnectionFactory(RedisProperties properties) {
        return connectionFactory(properties.cache().host(), properties.cache().port(), CACHE_COMMAND_TIMEOUT);
    }

    @Bean
    public LettuceConnectionFactory stateConnectionFactory(RedisProperties properties) {
        return connectionFactory(properties.state().host(), properties.state().port(), STATE_COMMAND_TIMEOUT);
    }

    private static LettuceConnectionFactory connectionFactory(String host, int port, Duration commandTimeout) {
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
        template.setValueSerializer(new GenericJacksonJsonRedisSerializer(JsonMapper.builder().build()));
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
