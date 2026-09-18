package org.phuchoang.ecp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.data.redis.autoconfigure.DataRedisReactiveAutoConfiguration;
import org.springframework.boot.data.redis.autoconfigure.DataRedisRepositoriesAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * The three {@code DataRedis*AutoConfiguration} classes are excluded because they each assume a
 * single default {@code RedisConnectionFactory}/bean named {@code redisTemplate} — ambiguous or
 * simply absent by construction once {@code configuration.redis.RedisConfig} declares two connection factories,
 * neither {@code @Primary} (`ADR-0034` §5.5), and neither used through Spring Data's repository
 * abstraction. {@code configuration.redis.RedisConfig} replaces this platform's Redis wiring entirely.
 */
@SpringBootApplication(exclude = {
    DataRedisAutoConfiguration.class,
    DataRedisReactiveAutoConfiguration.class,
    DataRedisRepositoriesAutoConfiguration.class
})
@EnableScheduling
@ConfigurationPropertiesScan
public class EcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcpApplication.class, args);
    }
}
