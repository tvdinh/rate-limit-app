package com.tdinh.challenge.airtasker.throttling;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * An interface for request throttling service.
 * 
 */
@Service
public interface RequestThrottler {
 
  /**
   * Determines if a request should be throttled, and returns the time duration (in second)
   * in which the throttling is still in effect. 
   * 
   * The business logic to determine if a request should be throttled depends on
   * the specific strategy implementation.
   * 
   * @param timestamp: the timestamp when the request is received.
   * @return An optional time duration (in second) that throttling is applied.
   * If the returned time duration is empty, it means no throttling is in place.
   */
  public Optional<Long> handleRequest(LocalDateTime timestamp);

}
