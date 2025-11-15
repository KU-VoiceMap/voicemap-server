package org.ku.voicemap.domain.auth.config;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class AuthenticatedMemberHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private static final String TYPE_CLAIM = "type";
    private static final String MEMBER_NUMBER_CLAIM = "memberNumber";
    private static final String ACCESS_TYPE = "ACCESS";

    private final JWTVerifier tokenVerifier;

    public AuthenticatedMemberHandlerMethodArgumentResolver(String secretKey) {
        tokenVerifier = JWT.require(Algorithm.HMAC256(secretKey)).build();
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthenticatedMember.class);
    }

    // TODO: 예외 정의
    @Override
    public String resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        String authorization = webRequest.getHeader(AUTHORIZATION_HEADER);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new IllegalArgumentException("Invalid Authorization header.");
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        try {
            DecodedJWT decodedToken = tokenVerifier.verify(token);
            String type = decodedToken.getClaim(TYPE_CLAIM).asString();
            if (!ACCESS_TYPE.equals(type)) {
                throw new IllegalArgumentException("Invalid token type.");
            }
            return decodedToken.getClaim(MEMBER_NUMBER_CLAIM).asString();
        } catch (JWTVerificationException e) {
            throw new IllegalArgumentException("Invalid token.");
        }
    }
}
