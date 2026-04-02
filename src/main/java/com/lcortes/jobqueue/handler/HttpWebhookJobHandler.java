package com.lcortes.jobqueue.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class HttpWebhookJobHandler implements JobHandler {

    private static final Logger log = LoggerFactory.getLogger(HttpWebhookJobHandler.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    // Strict constructor injection, no @Autowired
    public HttpWebhookJobHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        // We reuse one client for performance
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String getType() {
        return "HTTP_WEBHOOK";
    }

    @Override
    public JobResult execute(JobContext context) throws Exception {
        JsonNode payload = context.payload();

        // 1. Extract data from the JSONB payload
        String url = payload.get("url").asText();
        String body = payload.has("body") ? payload.get("body").toString() : "{}";

        log.info("Dispatching Webhook [Job: {}] to URL: {}", context.jobId(), url);

        // 2. Build the request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        // 3. Execute the request
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // 4. Handle failures
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            // Throwing an exception here triggers the engine's retry logic
            throw new RuntimeException("Webhook failed with status: " + response.statusCode());
        }

        // 5. (else) Return success result
        JsonNode resultNode = objectMapper.createObjectNode()
                .put("statusCode", response.statusCode())
                .put("responseBody", response.body());

        return new JobResult(resultNode);
    }
}