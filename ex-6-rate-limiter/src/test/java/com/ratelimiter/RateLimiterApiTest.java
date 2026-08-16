package com.ratelimiter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end HTTP tests using the in-memory store (no Redis needed). A zero refill
 * rate makes the tests deterministic.
 */
@SpringBootTest(properties = {
        "ratelimit.store=memory",
        "ratelimit.capacity=10",
        "ratelimit.refill-per-second=0"
})
@AutoConfigureMockMvc
class RateLimiterApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void helloIsAllowedWithinCapacity() throws Exception {
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/hello").param("clientId", "mock-user"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.clientId").value("mock-user"))
                    .andExpect(jsonPath("$.message").exists());
        }
    }

    @Test
    void excessRequestsReturn429WithRetryAfter() throws Exception {
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/hello").param("clientId", "over-user"));
        }
        mockMvc.perform(get("/api/hello").param("clientId", "over-user"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.error").value("Too Many Requests"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("over-user")));
    }

    @Test
    void bucketStatusAndReset() throws Exception {
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/hello").param("clientId", "reset-user"));
        }
        mockMvc.perform(get("/api/bucket/reset-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value("reset-user"))
                .andExpect(jsonPath("$.currentTokens", greaterThanOrEqualTo(0.0)))
                .andExpect(jsonPath("$.capacity").value(10));

        mockMvc.perform(delete("/api/bucket/reset-user")).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/hello").param("clientId", "reset-user"))
                .andExpect(status().isOk());
    }

    @Test
    void clientsAreRateLimitedIndependently() throws Exception {
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/hello").param("clientId", "user-a"));
        }
        mockMvc.perform(get("/api/hello").param("clientId", "user-a"))
                .andExpect(status().isTooManyRequests());
        mockMvc.perform(get("/api/hello").param("clientId", "user-b"))
                .andExpect(status().isOk());
    }

    @Test
    void headerClientIdTakesPrecedence() throws Exception {
        mockMvc.perform(get("/api/hello").header("X-Client-Id", "from-header")
                        .param("clientId", "from-param"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value("from-header"));
    }

    @Test
    void concurrentTestEndpointReportsLosslessCounts() throws Exception {
        mockMvc.perform(post("/api/test/concurrent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":\"load-user\",\"threads\":10,\"requestsPerThread\":10,\"capacity\":4,\"refillPerSecond\":0.01}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRequests").value(100))
                .andExpect(jsonPath("$.allowed").value(4))
                .andExpect(jsonPath("$.rejected").value(96));
    }

    @Test
    void concurrentTestRejectsInvalidInput() throws Exception {
        mockMvc.perform(post("/api/test/concurrent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":\"\",\"threads\":0,\"requestsPerThread\":0,\"capacity\":0,\"refillPerSecond\":-1}"))
                .andExpect(status().isBadRequest());
    }
}