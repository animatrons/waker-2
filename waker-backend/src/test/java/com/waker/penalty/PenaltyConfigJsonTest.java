package com.waker.penalty;

import static org.assertj.core.api.Assertions.assertThat;

import com.waker.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
class PenaltyConfigJsonTest extends AbstractIntegrationTest {

  @Autowired private ObjectMapper objectMapper;

  @Test
  void emailToContactConfigRoundTripsWithClassDiscriminator() throws Exception {
    var original =
        new EmailToContactPenaltyConfig(
            "friend@example.com", "I failed my wake-up commitment again.");

    String json = objectMapper.writeValueAsString(original);
    JsonNode node = objectMapper.readTree(json);

    assertThat(node.get("_class").asText()).isEqualTo("EMAIL_TO_CONTACT");
    assertThat(objectMapper.readValue(json, PenaltyConfig.class)).isEqualTo(original);
  }

  @Test
  void leaderboardConfigRoundTripsWithClassDiscriminator() throws Exception {
    var original = new LeaderboardPenaltyConfig(true);

    String json = objectMapper.writeValueAsString(original);
    JsonNode node = objectMapper.readTree(json);

    assertThat(node.get("_class").asText()).isEqualTo("LEADERBOARD");
    assertThat(objectMapper.readValue(json, PenaltyConfig.class)).isEqualTo(original);
  }
}
