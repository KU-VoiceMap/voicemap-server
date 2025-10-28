package org.ku.voicemap.domain.oauth.verify;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import org.ku.voicemap.domain.member.entity.Provider;
import org.ku.voicemap.domain.oauth.dto.RegisterDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TokenVerify {

  private final GoogleIdTokenVerifier googleIdTokenVerifier;
  private final String googleClientId;

  public TokenVerify(
      @Value("${spring.security.oauth2.client.registration.google.client-id}") String googleClientId) {
    this.googleClientId = googleClientId;
    this.googleIdTokenVerifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(),
        JacksonFactory.getDefaultInstance())
        .setAudience(Collections.singletonList(googleClientId))
        .build();
  }

  public RegisterDto toGoogle(String idTocken)  {
    try {
      GoogleIdToken googleToken = googleIdTokenVerifier.verify(idTocken);

      if (!googleToken.getPayload().getAudience().equals(googleClientId)) {

        throw new IllegalArgumentException("토큰의 Client ID가 일치하지 않음.");
      }
      Payload payload = googleToken.getPayload();
      return new RegisterDto(payload.getSubject(), payload.getEmail(), Provider.GOOGLE);
    } catch (GeneralSecurityException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }


  }


}
