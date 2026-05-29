package io.fanloop.publish;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ApiKeyValidatorTest {

    @Test
    void validKey_isAccepted() {
        ApiKeyValidator validator = new ApiKeyValidator("secret");
        assertThat(validator.isValid("secret")).isTrue();
    }

    @Test
    void wrongKey_isRejected() {
        ApiKeyValidator validator = new ApiKeyValidator("secret");
        assertThat(validator.isValid("nope")).isFalse();
    }

    @Test
    void nullKey_isRejected() {
        ApiKeyValidator validator = new ApiKeyValidator("secret");
        assertThat(validator.isValid(null)).isFalse();
    }
}
