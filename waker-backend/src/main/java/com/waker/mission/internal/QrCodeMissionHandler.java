package com.waker.mission.internal;

import com.waker.mission.MissionConfig;
import com.waker.mission.MissionFulfillmentProof;
import com.waker.mission.MissionHandler;
import com.waker.mission.MissionType;
import com.waker.mission.MissionVerificationResult;
import com.waker.mission.QrCodeFulfillmentProof;
import com.waker.mission.QrCodeMissionConfig;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class QrCodeMissionHandler implements MissionHandler {

  @Override
  public MissionType missionType() {
    return MissionType.QR_CODE;
  }

  @Override
  public void validateConfig(MissionConfig config) {
    if (!(config instanceof QrCodeMissionConfig qr)) {
      throw new IllegalArgumentException("Expected QrCodeMissionConfig for QR_CODE");
    }
    if (qr.codePayload() == null || qr.codePayload().isBlank()) {
      throw new IllegalArgumentException("codePayload must not be blank");
    }
    if (qr.codePayload().length() > 512) {
      throw new IllegalArgumentException("codePayload must not exceed 512 characters");
    }
  }

  @Override
  public MissionVerificationResult verifyFulfillment(
      UUID commitmentId, MissionConfig config, MissionFulfillmentProof proof) {
    if (!(config instanceof QrCodeMissionConfig qr)) {
      throw new IllegalArgumentException("Expected QrCodeMissionConfig for QR_CODE");
    }
    if (!(proof instanceof QrCodeFulfillmentProof qrProof)) {
      throw new IllegalArgumentException("Expected QrCodeFulfillmentProof for QR_CODE");
    }
    if (!Objects.equals(qr.codePayload(), qrProof.scannedPayload())) {
      return MissionVerificationResult.rejected("Scanned payload does not match registered code");
    }
    return MissionVerificationResult.success();
  }
}
