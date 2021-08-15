package com.tdinh.challenge.airtasker;

import com.tdinh.challenge.airtasker.throttling.LocalCacheRateLimiter;
import com.tdinh.challenge.airtasker.util.DateTimeUtils;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit test class for {@link LocalCacheRateLimiter}.
 *
 */
public class LocalCacheRateLimiterTest {
  
  private LocalCacheRateLimiter rateLimiter;
  
  @BeforeEach
  public void setUp() {
    rateLimiter = new LocalCacheRateLimiter();
    ReflectionTestUtils.setField(rateLimiter, "dateTimeUtils", new DateTimeUtils());
    ReflectionTestUtils.setField(rateLimiter, "timeDuration", 60);
    ReflectionTestUtils.setField(rateLimiter, "volume", 3);
    rateLimiter.init();
  }
  
  @Test
  public void testRequestBelowVolumeThresholdNoThrottled() {
    LocalDateTime now = LocalDateTime.now();
    Assertions.assertTrue(rateLimiter.handleRequest(now).isEmpty());
    Assertions.assertTrue(rateLimiter.handleRequest(now.plusSeconds(10L)).isEmpty());
    Assertions.assertTrue(rateLimiter.handleRequest(now.plusSeconds(20L)).isEmpty());
  }
  
  @Test
  public void testRequestAboveRateThresholdReturnThresholdAndExpiry() {
    LocalDateTime now = LocalDateTime.now();
    Assertions.assertTrue(rateLimiter.handleRequest(now).isEmpty());
    Assertions.assertTrue(rateLimiter.handleRequest(now.plusSeconds(10L)).isEmpty());
    Assertions.assertTrue(rateLimiter.handleRequest(now.plusSeconds(20L)).isEmpty());
    Optional<Long>  expiry = rateLimiter.handleRequest(now.plusSeconds(35L));
    Assertions.assertTrue(expiry.isPresent());
    Assertions.assertEquals(25L, expiry.get());
    expiry = rateLimiter.handleRequest(now.plusSeconds(55L));
    Assertions.assertTrue(expiry.isPresent());
    Assertions.assertEquals(5L, expiry.get());
  }
  
  @Test
  public void testRequestBelowRateThresholdNoThrottled() {
    LocalDateTime now = LocalDateTime.now();
    Assertions.assertTrue(rateLimiter.handleRequest(now).isEmpty());
    Assertions.assertTrue(rateLimiter.handleRequest(now.plusSeconds(20L)).isEmpty());
    Assertions.assertTrue(rateLimiter.handleRequest(now.plusSeconds(40L)).isEmpty());
    Optional<Long>  expiry = rateLimiter.handleRequest(now.plusSeconds(65L));
    //No throttling as the 4th request comes after 60s mark.
    Assertions.assertTrue(expiry.isEmpty());
    expiry = rateLimiter.handleRequest(now.plusSeconds(70L));
    //Throttled, as the the 5th request, there are already 3 requests for the last 60 seconds:
    //at 20, 40, 65 marks. Only accept more request after (20+60 - 70) = 10 more seconds.
    Assertions.assertTrue(expiry.isPresent());
    Assertions.assertEquals(10L, expiry.get());
    
    //6th request
    expiry = rateLimiter.handleRequest(now.plusSeconds(75L));
    Assertions.assertTrue(expiry.isPresent());
    Assertions.assertEquals(5L, expiry.get());
    
    //7th request
    expiry = rateLimiter.handleRequest(now.plusSeconds(80L));
    Assertions.assertTrue(expiry.isPresent());
    Assertions.assertEquals(0L, expiry.get());
    
    //8th request
    expiry = rateLimiter.handleRequest(now.plusSeconds(81L));
    Assertions.assertTrue(expiry.isEmpty());
  }

}
