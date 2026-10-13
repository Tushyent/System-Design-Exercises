package com.jobportal.config;

import com.jobportal.ratelimit.BucketStore;
import com.jobportal.ratelimit.InMemoryBucketStore;
import com.jobportal.ratelimit.RedisBucketStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Configuration
public class RateLimiterConfig {

    public static final String LUA_SCRIPT_PATH = "scripts/token_bucket.lua";

    @Bean
    @ConditionalOnProperty(name = "ratelimit.store", havingValue = "redis", matchIfMissing = true)
    public BucketStore redisBucketStore(StringRedisTemplate redis,
                                        RateLimiterProperties props,
                                        DefaultRedisScript<Long> tokenBucketScript) {
        return new RedisBucketStore(redis, props, tokenBucketScript);
    }

    @Bean
    @ConditionalOnProperty(name = "ratelimit.store", havingValue = "memory")
    public BucketStore inMemoryBucketStore() {
        return new InMemoryBucketStore();
    }

    @Bean
    public DefaultRedisScript<Long> tokenBucketScript() {
        String lua;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                RateLimiterConfig.class.getClassLoader().getResourceAsStream(LUA_SCRIPT_PATH),
                StandardCharsets.UTF_8))) {
            lua = reader.lines().collect(Collectors.joining("\n")) + "\n";
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load Lua script: " + LUA_SCRIPT_PATH, e);
        }
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(lua);
        script.setResultType(Long.class);
        return script;
    }
}
