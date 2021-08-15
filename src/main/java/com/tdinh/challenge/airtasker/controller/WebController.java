package com.tdinh.challenge.airtasker.controller;

import com.tdinh.challenge.airtasker.throttling.RequestThrottler;
import com.tdinh.challenge.airtasker.util.DateTimeUtils;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * This class is a HTTP controller that serves web requests.
 * The service exposes a simple GET /hello endpoint, where clients can request for greetings
 * and the server would return a greeting message with the timestamp at which the request is received.
 * 
 * The controller, however, employs a {@link RequestThrottler} to throttle requests to avoid 
 * request overload. If a request is throttled, HTTP 429 error response is returned with the expiry time
 * until which the throttling is applied.
 * 
 */
@RestController
public class WebController {
  
  @Autowired
  private RequestThrottler requestThrottler;
  
  @Autowired
  private DateTimeUtils dateTimeUtils;
  
  @GetMapping("/hello")
  public ResponseEntity<String> hello() {
    LocalDateTime timestamp = LocalDateTime.now();
    Optional<Long> wait = requestThrottler.handleRequest(timestamp);
    if (wait.isEmpty()) {
      return ResponseEntity.ok(
          (new StringBuilder())
            .append("Hello Airtasker! Received at ")
            .append(dateTimeUtils.toDateString(timestamp)).append("\n")
            .toString());
    } else {
      return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
          .body((new StringBuilder())
              .append("Rate Limit Exceeded! Try again in ")
              .append(wait.get())
              .append(" seconds\n")
              .toString());
    }
  }

}
