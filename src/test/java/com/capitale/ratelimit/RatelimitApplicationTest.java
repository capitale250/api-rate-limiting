package com.capitale.ratelimit;

import com.capitale.ratelimit.model.User;
import com.capitale.ratelimit.repository.UserRepository;
import org.junit.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@ContainerInit
public class RatelimitApplicationTest  {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void testRateLimiting() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Client-ID", "1");
        HttpEntity<String> request = new HttpEntity<>(headers);

        // Make 5 successful requests
        for (int i = 0; i < 5; i++) {
            ResponseEntity<String> response = restTemplate.exchange(
                    "/v1/secure",
                    HttpMethod.GET,
                    request,
                    String.class
            );
            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        // 6th request should fail due to rate limiting
        ResponseEntity<String> rateLimitedResponse = restTemplate.exchange(
                "/v1/secure",
                HttpMethod.GET,
                request,
                String.class
        );

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, rateLimitedResponse.getStatusCode());
        // Make a request to the open route
        ResponseEntity<String> response = restTemplate.exchange(
                "/v2/open", // open route
                HttpMethod.GET,
                request,
                String.class
        );

        // Assert that the response is OK
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}

