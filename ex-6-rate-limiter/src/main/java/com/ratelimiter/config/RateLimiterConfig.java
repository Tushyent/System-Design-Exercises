package com.ratelimiter.config;

import com.ratelimiter.ratelimit.BucketStore;
import com.ratelimiter.ratelimit.InMemoryBucketStore;
import com.ratelimiter.ratelimit.RedisBucketStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableConfigurationProperties(RateLimiterProperties.class)
public class RateLimiterConfig {

    /** The atomic token-bucket script, loaded once from the classpath. */
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
        String lua = loadLuaScript(LUA_SCRIPT_PATH);
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(lua);
        script.setResultType(Long.class);
        return script;
    }

    private static String loadLuaScript(String path) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                RateLimiterConfig.class.getClassLoader().getResourceAsStream(path),
                StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load Lua script: " + path, e);
        }
    }
}