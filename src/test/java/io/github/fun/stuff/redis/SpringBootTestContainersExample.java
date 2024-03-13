package io.github.fun.stuff.redis;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Arrays;

import static org.testcontainers.utility.DockerImageName.parse;

public class SpringBootTestContainersExample {

    private static final Logger LOG = LoggerFactory.getLogger(SpringBootTestContainersExample.class);

    @Nested
    @Testcontainers
    @SpringBootTest(classes = TestContainersExample.RedisConfiguration.class)
    class TestContainersExample {

        @Container
        @ServiceConnection("redis")
        static GenericContainer<?> redisContainer = new GenericContainer<>(parse("redis:latest")).withExposedPorts(6379);

        @Autowired
        RedisTemplate<String, String> redisTemplate;
        @Autowired
        ApplicationContext applicationContext;

        @Test
        void testRedisConnect() {
            doTestRedis(applicationContext, redisTemplate);
        }

        @ImportAutoConfiguration(RedisAutoConfiguration.class)
        static class RedisConfiguration {

        }

    }

    @Nested
    @SpringBootTest(classes = SpringBootExample.RedisConfiguration.class)
    class SpringBootExample {

        @Autowired
        RedisTemplate<String, String> redisTemplate;
        @Autowired
        ApplicationContext applicationContext;

        @Test
        void testRedisConnect() {
            doTestRedis(applicationContext, redisTemplate);
        }

        @TestComponent
        @ImportAutoConfiguration(classes = {
                RedisAutoConfiguration.class,
                ServiceConnectionAutoConfiguration.class
        })
        static class RedisConfiguration {
            @Bean
            @ServiceConnection("redis")
            public GenericContainer<?> redisContainer() {
                return new GenericContainer<>(parse("redis:latest")).withExposedPorts(6379);
            }

        }
    }

    @Nested
    @SpringBootTest(classes = SpringBootImportContainerExample.RedisConfiguration.class)
    class SpringBootImportContainerExample {

        @Autowired
        RedisTemplate<String, String> redisTemplate;
        @Autowired
        ApplicationContext applicationContext;

        @Test
        void testRedisConnect() {
            doTestRedis(applicationContext, redisTemplate);
        }

        @TestComponent
        @ImportAutoConfiguration(classes = {
                RedisAutoConfiguration.class,
                ServiceConnectionAutoConfiguration.class
        })
        @ImportTestcontainers(value = RedisContainer.class)
        static class RedisConfiguration {
        }

        interface RedisContainer {
            @Container
            @ServiceConnection("redis")
            GenericContainer<?> redisContainer = new GenericContainer<>(parse("redis:latest")).withExposedPorts(6379);

        }
    }


    static void doTestRedis(ApplicationContext applicationContext, RedisTemplate<String, String> redisTemplate) {
        LOG.info("Beans: \t{}", applicationContext.getBeanDefinitionCount());
        Arrays.stream(applicationContext.getBeanDefinitionNames())
                .forEach(beanName -> LOG.info("Bean Name: \t{}", beanName));


        redisTemplate.opsForValue().set("key", "value");

        Assertions.assertEquals("value", redisTemplate.opsForValue().get("key"));
    }
}
