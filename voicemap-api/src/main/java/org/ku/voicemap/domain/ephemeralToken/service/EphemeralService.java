package org.ku.voicemap.domain.ephemeralToken.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.ku.voicemap.config.EphemeralProperties;
import org.ku.voicemap.domain.ephemeralToken.dto.CreateEphemeralRequest;
import org.ku.voicemap.domain.ephemeralToken.dto.EphemeralTokenResponse;
import org.ku.voicemap.domain.ephemeralToken.entity.EphemeralToken;
import org.ku.voicemap.domain.ephemeralToken.repository.EphemeralTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EphemeralService {

    private final EphemeralProperties ephemeralProperties;
    private final PythonExecutor pythonExecutor;
    private final EphemeralTokenRepository ephemeralTokenRepository;

    public EphemeralTokenResponse createEphemeralToken(Long userId, CreateEphemeralRequest createEphemeralRequest) {

        String token = pythonExecutor.createGoogleAuthToken(ephemeralProperties.getApikey(), createEphemeralRequest.uses(),
            createEphemeralRequest.expireMinutes(), createEphemeralRequest.sessionExpireMinutes());
        EphemeralToken ephemeralToken = new EphemeralToken(userId, createEphemeralRequest.uses(),
            createEphemeralRequest.expireMinutes(), createEphemeralRequest.sessionExpireMinutes(), token);
        ephemeralTokenRepository.save(ephemeralToken);
        return EphemeralTokenResponse.toDto(ephemeralToken);
    }

    public EphemeralTokenResponse rotateEphemeralToken(Long userId, String oldToken,
                                                       CreateEphemeralRequest createEphemeralRequest) {

        ephemeralTokenRepository.findByUserIdAndEphemeralToken(userId, oldToken)
            .orElseThrow(EntityNotFoundException::new);

        String token = pythonExecutor.createGoogleAuthToken(ephemeralProperties.getApikey(), createEphemeralRequest.uses(),
            createEphemeralRequest.expireMinutes(),
            createEphemeralRequest.sessionExpireMinutes());

        EphemeralToken ephemeralToken = new EphemeralToken(userId, createEphemeralRequest.uses()
            , createEphemeralRequest.expireMinutes(), createEphemeralRequest.sessionExpireMinutes(), token);
        ephemeralTokenRepository.save(ephemeralToken);

        return EphemeralTokenResponse.toDto(ephemeralToken);
    }

}
