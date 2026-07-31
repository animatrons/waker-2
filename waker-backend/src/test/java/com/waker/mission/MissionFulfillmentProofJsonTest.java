package com.waker.mission;

import static org.assertj.core.api.Assertions.assertThat;

import com.waker.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
class MissionFulfillmentProofJsonTest extends AbstractIntegrationTest {

  @Autowired private ObjectMapper objectMapper;

  @Test
  void qrCodeProofRoundTripsWithClassDiscriminator() throws Exception {
    var original = new QrCodeFulfillmentProof("kitchen-fridge-2026");

    String json = objectMapper.writeValueAsString(original);
    JsonNode node = objectMapper.readTree(json);

    assertThat(node.get("_class").asText()).isEqualTo("QR_CODE");
    assertThat(objectMapper.readValue(json, MissionFulfillmentProof.class)).isEqualTo(original);
  }

  @Test
  void writingTaskProofRoundTripsWithClassDiscriminator() throws Exception {
    var original = new WritingTaskFulfillmentProof("I will wake up on time because...");

    String json = objectMapper.writeValueAsString(original);
    JsonNode node = objectMapper.readTree(json);

    assertThat(node.get("_class").asText()).isEqualTo("WRITING_TASK");
    assertThat(objectMapper.readValue(json, MissionFulfillmentProof.class)).isEqualTo(original);
  }

  @Test
  void mathGameProofRoundTripsWithClassDiscriminator() throws Exception {
    var original = new MathGameFulfillmentProof("12");

    String json = objectMapper.writeValueAsString(original);
    JsonNode node = objectMapper.readTree(json);

    assertThat(node.get("_class").asText()).isEqualTo("MATH_GAME");
    assertThat(objectMapper.readValue(json, MissionFulfillmentProof.class)).isEqualTo(original);
  }
}
