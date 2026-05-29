package io.fanloop.publish;

import io.fanloop.backplane.RedisBackplane;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublishController.class)
@Import(ApiKeyValidator.class)
class PublishControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean RedisBackplane backplane;

    @Test
    void publish_withValidKey_returns202_andCallsBackplane() throws Exception {
        mvc.perform(post("/publish/orders")
                .header("X-API-Key", "dev-key")
                .contentType("application/json")
                .content("{\"x\":1}"))
           .andExpect(status().isAccepted());
        verify(backplane).publish("orders", "{\"x\":1}");
    }

    @Test
    void publish_withWrongKey_returns401() throws Exception {
        mvc.perform(post("/publish/orders")
                .header("X-API-Key", "wrong")
                .contentType("application/json")
                .content("{\"x\":1}"))
           .andExpect(status().isUnauthorized());
    }
}
