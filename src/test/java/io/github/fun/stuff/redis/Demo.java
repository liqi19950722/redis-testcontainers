package io.github.fun.stuff.redis;

import io.microsphere.redis.spring.annotation.EnableRedisInterceptor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.testcontainers.utility.DockerImageName.parse;

@Testcontainers
@SpringJUnitConfig(classes = {
        RedisAutoConfiguration.class,
})
@TestPropertySource(
        properties = {"microsphere.redis.enabled=true"}
)
@EnableRedisInterceptor
class Demo {
    @Container
    @ServiceConnection("redis")
    static GenericContainer<?> redisContainer = new GenericContainer<>(parse("redis:latest")).withExposedPorts(6379);

    @Autowired
    ApplicationContext applicationContext;
    @Autowired
    RedisTemplate<String, String> redisTemplate;

    @Test
    void test() {
        Assertions.assertNotNull(applicationContext);
        Assertions.assertNotNull(redisTemplate);

        redisTemplate.opsForValue().set("key", "value");

    }
}
