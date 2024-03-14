package io.github.fun.stuff.redis;

import org.jboss.jandex.ArrayType;
import org.jboss.jandex.Index;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.PrimitiveType;
import org.jboss.jandex.Type;
import org.jboss.jandex.VoidType;
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
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Modifier;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RedisCommandsMethodHandles {

    private static final MethodHandles.Lookup PUBLIC_LOOKUP = MethodHandles.publicLookup();
    private static final EnumMap<PrimitiveType.Primitive, Class<?>> PRIMITIVE_TYPE_CLASS_TABLE;
    private static final Map<String, Class<?>> ARRAY_TYPE_CLASS_TABLE = new HashMap<>();
    static final Map<String, MethodHandle> METHOD_HANDLE_MAP;

    static {
        PRIMITIVE_TYPE_CLASS_TABLE = new EnumMap<>(Map.of(
                PrimitiveType.Primitive.BOOLEAN, boolean.class,
                PrimitiveType.Primitive.BYTE, byte.class,
                PrimitiveType.Primitive.SHORT, short.class,
                PrimitiveType.Primitive.INT, int.class,
                PrimitiveType.Primitive.LONG, long.class,
                PrimitiveType.Primitive.FLOAT, float.class,
                PrimitiveType.Primitive.DOUBLE, double.class,
                PrimitiveType.Primitive.CHAR, char.class
        ));

        ARRAY_TYPE_CLASS_TABLE.put("byte[]", byte[].class);
        ARRAY_TYPE_CLASS_TABLE.put("byte[][]", byte[][].class);
        ARRAY_TYPE_CLASS_TABLE.put("int[]", int[].class);
        ARRAY_TYPE_CLASS_TABLE.put("String[]", String[].class);
        ARRAY_TYPE_CLASS_TABLE.put("RecordId[]", RecordId[].class);
        ARRAY_TYPE_CLASS_TABLE.put("StreamOffset[]", StreamOffset[].class);

        METHOD_HANDLE_MAP = initRedisCommandMethodHandle(getAllRedisCommandMethods(),
                RedisCommandsMethodHandles.class.getClassLoader());
    }

    public static MethodHandle getMethodHandle(String methodSignature) {

        return METHOD_HANDLE_MAP.get(methodSignature);
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
                    .filter(methodInfo -> Modifier.isPublic(methodInfo.flags()))
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<String, MethodHandle> initRedisCommandMethodHandle(List<MethodInfo> methods, ClassLoader contextClassLoader) {
        return methods.stream().map(methodInfo -> {
            Class<?> klass = loadClass(contextClassLoader, methodInfo.declaringClass().name().toString());
            String methodName = methodInfo.name();
            var returnKlass = getClass(methodInfo.returnType(), contextClassLoader);

            var array = methodInfo.parameters().toArray(MethodParameterInfo[]::new);
            Class<?>[] parameterKlass = new Class<?>[array.length];
            for (int i = 0; i < array.length; i++) {
                parameterKlass[i] = getClass(array[i].type(), contextClassLoader);
            }

            MethodType methodType = MethodType.methodType(returnKlass, parameterKlass);
            try {
                return new MethodRecord(methodInfo.toString(), RedisCommandsMethodHandles.PUBLIC_LOOKUP.findVirtual(klass, methodName, methodType));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }

        }).collect(Collectors.toMap(MethodRecord::methodSignature, MethodRecord::methodHandle));
    }

    private static Class<?> getClass(Type type, ClassLoader contextClassLoader) {
        if (type instanceof VoidType) {
            return void.class;
        }

        if (type instanceof PrimitiveType primitiveType) {
            return PRIMITIVE_TYPE_CLASS_TABLE.get(primitiveType.primitive());
        }

        if (type instanceof ArrayType arrayType) {
            Class<?> klass = ARRAY_TYPE_CLASS_TABLE.get(arrayType.elementType().name().local() + "[]".repeat(arrayType.dimensions()));
            if (klass != null) {
                return klass;
            }
            throw new RuntimeException("need to add Class");
        }

        return loadClass(contextClassLoader, getClassName(type));

    }

    private static String getClassName(Type type) {
        if (type instanceof ParameterizedType) {
            return type.asParameterizedType().name().toString();
        } else {
            return type.name().toString();
        }
    }


    private static Class<?> loadClass(ClassLoader classLoader, String className) {
        try {
            return classLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    record MethodRecord(String methodSignature, MethodHandle methodHandle) {
    }
}
