package com.moneystats.MoneyStats.web;

public class WebException extends Exception {

  private Code code;

  /**
   * Constructs a new exception with {@code null} as its detail message. The cause is not
   * initialized, and may subsequently be initialized by a call to {@link #initCause}.
   */
  public WebException(Code code) {
    this.code = code;
  }

  public Code getCode() {
    return code;
  }

  public static enum Code {
    LOGIN_REQUIRED
  }
}
