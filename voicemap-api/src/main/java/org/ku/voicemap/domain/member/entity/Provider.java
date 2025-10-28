package org.ku.voicemap.domain.member.entity;

import lombok.Getter;

@Getter
public enum Provider {
  GOOGLE;

  public static boolean checkProvider(String name) {
    if (name == null) {
      return false;
    }
    try {
      Provider.valueOf(name.toUpperCase());
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }
}
