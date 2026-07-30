package com.waker.mission;

import static org.assertj.core.api.Assertions.assertThat;

import com.waker.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
class MissionConfigJsonTest extends AbstractIntegrationTest {

  @Autowired private ObjectMapper objectMapper;

  @Test
  void qrCodeConfigRoundTripsWithClassDiscriminator() throws Exception {
    var original = new QrCodeMissionConfig("kitchen-fridge-2026");

    String json = objectMapper.writeValueAsString(original);
    JsonNode node = objectMapper.readTree(json);

    assertThat(node.get("_class").asText()).isEqualTo("QR_CODE");
    assertThat(objectMapper.readValue(json, MissionConfig.class)).isEqualTo(original);
  }

  @Test
  void writingTaskConfigRoundTripsWithClassDiscriminator() throws Exception {
    var original = new WritingTaskMissionConfig("Write why you will wake up on time", 50);

    String json = objectMapper.writeValueAsString(original);
    JsonNode node = objectMapper.readTree(json);

    assertThat(node.get("_class").asText()).isEqualTo("WRITING_TASK");
    assertThat(objectMapper.readValue(json, MissionConfig.class)).isEqualTo(original);
  }

  @Test
  void mathGameConfigRoundTripsWithClassDiscriminator() throws Exception {
    var original = new MathGameMissionConfig("7 + 5", "12");

    String json = objectMapper.writeValueAsString(original);
    JsonNode node = objectMapper.readTree(json);

    assertThat(node.get("_class").asText()).isEqualTo("MATH_GAME");
    assertThat(objectMapper.readValue(json, MissionConfig.class)).isEqualTo(original);
  }
}
