package se.magnus.util.http;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.ZonedDateTime;

@Getter
public class HttpErrorInfo {
  private final ZonedDateTime timestamp;
  private final String path;
  private final HttpStatus httpStatus;
  private final String message;

  public HttpErrorInfo() {
    timestamp = null;
    path = null;
    httpStatus = null;
    message = null;
  }

  public HttpErrorInfo(ZonedDateTime timestamp, String path, HttpStatus httpStatus, String message) {
    this.timestamp = timestamp;
    this.path = path;
    this.httpStatus = httpStatus;
    this.message = message;
  }

  public HttpErrorInfo(HttpStatus httpStatus, String path, String message) {
    this.timestamp = ZonedDateTime.now();
    this.path = path;
    this.httpStatus = httpStatus;
    this.message = message;
  }
}
