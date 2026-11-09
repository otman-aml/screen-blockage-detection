package com.ocp.screenblockage.service;

import com.ocp.screenblockage.config.AiServiceProperties;
import com.ocp.screenblockage.dto.AiAnalysisResponseDto;
import com.ocp.screenblockage.exception.AiServiceClientException;
import com.ocp.screenblockage.exception.AiServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Client HTTP vers le microservice IA (ai-service, FastAPI).
 *
 * <p>Appelle {@code POST /analyze} avec une image en multipart/form-data
 * (champ {@code file}) et deserialise le JSON de reponse (contrat exact de
 * ai-service/schemas/response.py) dans {@link AiAnalysisResponseDto}.
 *
 * <p>Gere :
 * <ul>
 *   <li>reponse HTTP 2xx -> DTO (ou erreur si JSON invalide) ;</li>
 *   <li>reponse HTTP 4xx -> {@link AiServiceClientException} (502) ;</li>
 *   <li>reponse HTTP 5xx -> {@link AiServiceUnavailableException} (503) ;</li>
 *   <li>timeout / connexion refusee / IA indisponible -> {@link AiServiceUnavailableException} (503) ;</li>
 *   <li>JSON inattendu -> {@link AiServiceClientException} (502).</li>
 * </ul>
 *
 * <p>L'URL de base et les timeouts proviennent de {@link AiServiceProperties}
 * (application.yml) : jamais codes en dur ici.
 */
@Service
public class AiServiceClient {

    private static final Logger LOG = LoggerFactory.getLogger(AiServiceClient.class);

    private final RestClient restClient;
    private final AiServiceProperties properties;

    public AiServiceClient(RestClient.Builder builder, AiServiceProperties properties) {
        this.properties = properties;
        this.restClient = builder
                .baseUrl(properties.baseUrl())
                .build();
    }

    /**
     * Envoie l'image au ai-service et retourne le resultat d'analyse.
     *
     * @param imageBytes contenu binaire de l'image
     * @param filename   nom original (utilise par le ai-service pour valider l'extension)
     */
    public AiAnalysisResponseDto analyze(byte[] imageBytes, String filename) {
        try {
            return restClient.post()
                    .uri(properties.analyzePath())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(buildMultipartBody(imageBytes, filename))
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        throw new AiServiceClientException(
                                "AI service rejected the request with status " + response.getStatusCode() + ".");
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                        throw new AiServiceUnavailableException(
                                "AI service server error with status " + response.getStatusCode() + ".");
                    })
                    .body(AiAnalysisResponseDto.class);
        } catch (AiServiceUnavailableException | AiServiceClientException e) {
            // Erreurs metier deja traduites : on les propage telles quelles.
            throw e;
        } catch (ResourceAccessException e) {
            // Connexion refusee, timeout connect/read, DNS...
            LOG.warn("AI service unreachable: {}", e.getMessage());
            throw new AiServiceUnavailableException(
                    "AI service unreachable or timed out: " + e.getMessage(), e);
        } catch (RestClientResponseException e) {
            // Filet de securite si onStatus n'a pas intercepte.
            if (e.getStatusCode().is5xxServerError()) {
                throw new AiServiceUnavailableException(
                        "AI service error with status " + e.getStatusCode() + ".", e);
            }
            throw new AiServiceClientException(
                    "AI service client error with status " + e.getStatusCode() + ".", e);
        } catch (RuntimeException e) {
            // JSON malforme / champ manquant / conversion impossible.
            LOG.warn("Invalid AI service response: {}", e.getMessage());
            throw new AiServiceClientException(
                    "Invalid or unexpected AI service response: " + e.getMessage(), e);
        }
    }

    /**
     * Construit le corps multipart : champ {@code file} contenant les octets
     * de l'image. Le nom du fichier est conserve (extension requise par le
     * ai-service pour la validation).
     */
    private MultiValueMap<String, org.springframework.http.HttpEntity<?>> buildMultipartBody(
            byte[] imageBytes, String filename) {
        ByteArrayResource resource = new ByteArrayResource(imageBytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", resource);
        return builder.build();
    }
}