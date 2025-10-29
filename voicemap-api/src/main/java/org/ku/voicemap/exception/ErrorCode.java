package org.ku.voicemap.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

  MEMBER_EXIST_REGISTER("이미 가입한 유저입니다"),
  MEMBER_NOT_FOUND("유저를 찾을수 없습니다")
  ,
  AUTHENTICATION_FAILED("인증 토큰이 잘못되었습니다");

  private final String message;

  ErrorCode(String message) {
    this.message = message;
  }
}
