package com.ocp.screenblockage.service;

import com.ocp.screenblockage.config.AiServiceProperties;
import com.ocp.screenblockage.dto.AiAnalysisResponseDto;
import com.ocp.screenblockage.dto.AiDetectionDto;
import com.ocp.screenblockage.exception.AiServiceClientException;
import com.ocp.screenblockage.exception.AiServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AiServiceClientTest {

    private static final String BASE_URL = "http://localhost:8000";
    private static final String ANALYZE_PATH = "/analyze";

    private AiServiceClient client;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        AiServiceProperties properties =
                new AiServiceProperties(BASE_URL, ANALYZE_PATH, 5000, 30000);
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        client = new AiServiceClient(builder, properties);
    }

    private static final String SAMPLE_JSON = """
            {
              "image_width": 640,
              "image_height": 640,
              "blockage_percentage": 54.84,
              "detections": [
                {
                  "class_id": 0,
                  "class_name": "stone",
                  "confidence": 0.97,
                  "width_cm": 13.12,
                  "height_cm": 17.70,
                  "oversized": true,
                  "mask": [[138.0, 16.0], [134.0, 20.0]]
                }
              ]
            }
            """;

    @Test
    void analyze_with2xx_returnsParsedDto() {
        mockServer.expect(requestTo(BASE_URL + ANALYZE_PATH))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(SAMPLE_JSON, MediaType.APPLICATION_JSON));

        AiAnalysisResponseDto dto = client.analyze(new byte[]{1, 2, 3}, "test.jpg");

        assertThat(dto.imageWidth()).isEqualTo(640);
        assertThat(dto.imageHeight()).isEqualTo(640);
        assertThat(dto.blockagePercentage()).isEqualTo(54.84);

        AiDetectionDto detection = dto.detections().get(0);
        assertThat(detection.classId()).isZero();
        assertThat(detection.className()).isEqualTo("stone");
        assertThat(detection.confidence()).isEqualTo(0.97);
        assertThat(detection.widthCm()).isEqualTo(13.12);
        assertThat(detection.heightCm()).isEqualTo(17.70);
        assertThat(detection.oversized()).isTrue();
        assertThat(detection.mask()).hasSize(2);

        mockServer.verify();
    }

    @Test
    void analyze_withEmptyDetections_returnsEmptyList() {
        String json = """
                {
                  "image_width": 640,
                  "image_height": 480,
                  "blockage_percentage": 0.0,
                  "detections": []
                }
                """;
        mockServer.expect(requestTo(BASE_URL + ANALYZE_PATH))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        AiAnalysisResponseDto dto = client.analyze(new byte[]{1}, "empty.jpg");

        assertThat(dto.detections()).isEmpty();
        assertThat(dto.blockagePercentage()).isZero();
        mockServer.verify();
    }

    @Test
    void analyze_with4xx_throwsClientException() {
        mockServer.expect(requestTo(BASE_URL + ANALYZE_PATH))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withBadRequest());

        assertThatThrownBy(() -> client.analyze(new byte[]{1}, "bad.jpg"))
                .isInstanceOf(AiServiceClientException.class);
    }

    @Test
    void analyze_with5xx_throwsUnavailable() {
        mockServer.expect(requestTo(BASE_URL + ANALYZE_PATH))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.analyze(new byte[]{1}, "bad.jpg"))
                .isInstanceOf(AiServiceUnavailableException.class);
    }

    @Test
    void analyze_withConnectionFailure_throwsUnavailable() {
        mockServer.expect(requestTo(BASE_URL + ANALYZE_PATH))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withException(new IOException("connection timed out")));

        assertThatThrownBy(() -> client.analyze(new byte[]{1}, "slow.jpg"))
                .isInstanceOf(AiServiceUnavailableException.class);
    }

    @Test
    void analyze_withInvalidJson_throwsClientException() {
        mockServer.expect(requestTo(BASE_URL + ANALYZE_PATH))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("not-a-json{", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.analyze(new byte[]{1}, "test.jpg"))
                .isInstanceOf(AiServiceClientException.class);
    }
}