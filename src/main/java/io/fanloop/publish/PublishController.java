package io.fanloop.publish;

import io.fanloop.backplane.RedisBackplane;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class PublishController {

    private final ApiKeyValidator apiKeyValidator;
    private final RedisBackplane backplane;

    public PublishController(ApiKeyValidator apiKeyValidator, RedisBackplane backplane) {
        this.apiKeyValidator = apiKeyValidator;
        this.backplane = backplane;
    }

    @PostMapping("/publish/{channel}")
    public ResponseEntity<Void> publish(@PathVariable String channel,
                                        @RequestHeader(value = "X-API-Key", required = false) String apiKey,
                                        @RequestBody String body) {
        if (!apiKeyValidator.isValid(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        backplane.publish(channel, body);
        return ResponseEntity.accepted().build();
    }
}
