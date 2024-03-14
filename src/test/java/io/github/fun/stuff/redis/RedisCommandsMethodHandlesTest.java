package io.github.fun.stuff.redis;

import org.jboss.jandex.Index;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisCommands;

import java.io.IOException;

class RedisCommandsMethodHandlesTest {

    @Test
    void testClassLoader() throws ClassNotFoundException, IOException {
        var contextClassLoader = Thread.currentThread().getContextClassLoader();

        var index = Index.of(RedisCommands.class);
        var classInfo = index.getClassByName(RedisCommands.class);

        Assertions.assertEquals(RedisCommands.class, contextClassLoader.loadClass(classInfo.name().toString()));
    }

    @Test
    void testLoadRedisCommandsMethodHandle() {
        Assertions.assertDoesNotThrow(() -> RedisCommandsMethodHandles.METHOD_HANDLE_MAP.keySet().forEach(System.out::println));
    }


}
