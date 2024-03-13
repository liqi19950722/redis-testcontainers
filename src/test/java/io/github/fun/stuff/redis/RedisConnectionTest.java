package io.github.fun.stuff.redis;

import io.lettuce.core.AclCategory;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import io.lettuce.core.protocol.CommandType;
import org.jboss.jandex.Index;
import org.jboss.jandex.MethodInfo;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.connection.RedisCommands;
import org.springframework.data.redis.connection.RedisConnectionCommands;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.connection.RedisHashCommands;
import org.springframework.data.redis.connection.RedisHyperLogLogCommands;
import org.springframework.data.redis.connection.RedisKeyCommands;
import org.springframework.data.redis.connection.RedisListCommands;
import org.springframework.data.redis.connection.RedisPubSubCommands;
import org.springframework.data.redis.connection.RedisScriptingCommands;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.data.redis.connection.RedisSetCommands;
import org.springframework.data.redis.connection.RedisStreamCommands;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.connection.RedisTxCommands;
import org.springframework.data.redis.connection.RedisZSetCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.testcontainers.utility.DockerImageName.parse;


@Testcontainers
@SpringJUnitConfig(classes = {
        RedisAutoConfiguration.class,
})
public class RedisConnectionTest {

    private static final Logger LOG = LoggerFactory.getLogger(RedisConnectionTest.class);
    @Container
    @ServiceConnection("redis")
    static GenericContainer<?> redisContainer = new GenericContainer<>(parse("redis:latest")).withExposedPorts(6379);


    @Test
    void showWriteMethod(@Autowired StringRedisTemplate redisTemplate) {

        var redisFuture = redisTemplate.execute((RedisCallback<RedisFuture<Set<CommandType>>>) connection -> {
            var nativeConnection = connection.getNativeConnection();
            if (nativeConnection instanceof RedisClusterAsyncCommands redisClusterAsyncCommands) {
                return redisClusterAsyncCommands.aclCat(AclCategory.WRITE);
            }
            throw new UnsupportedOperationException();
        });
        var write = redisFuture.thenApply(result ->
                        result.stream().map(Enum::name).collect(Collectors.toList()))
                .toCompletableFuture().join();
        LOG.info("write method count {}", write.size());
        write.stream().sorted().forEach(method -> LOG.info("method name {}", method));

        var allRedisCommandMethods = getAllRedisCommandMethods();
        var writeMethods = allRedisCommandMethods.stream().filter(methodInfo -> write.contains(methodInfo.name().toUpperCase()))
                .toList();
        LOG.info("write method count {}", writeMethods.size());
        writeMethods.stream().sorted(Comparator.comparing(MethodInfo::name)).forEach(method -> LOG.info("method name {}", method));
    }

    @Test
    void showRedisCommandMethods() {
        var allRedisCommandMethods = getAllRedisCommandMethods();
        allRedisCommandMethods.stream().map(methodInfo -> methodInfo.name().toUpperCase()).forEach(System.out::println);
    }

    private static List<MethodInfo> getAllRedisCommandMethods() {
        try {
            var index = Index.of(RedisCommands.class, RedisKeyCommands.class,
                    RedisStringCommands.class, RedisListCommands.class, RedisSetCommands.class,
                    RedisZSetCommands.class, RedisHashCommands.class, RedisTxCommands.class, RedisPubSubCommands.class,
                    RedisConnectionCommands.class, RedisServerCommands.class, RedisStreamCommands.class,
                    RedisScriptingCommands.class, RedisGeoCommands.class, RedisHyperLogLogCommands.class
            );

            return index.getClassByName(RedisCommands.class)
                    .interfaceNames()
                    .stream()
                    .map(index::getClassByName)
                    .flatMap(classInfo -> classInfo.methods().stream())
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
