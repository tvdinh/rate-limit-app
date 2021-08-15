package com.tdinh.challenge.airtasker.throttling;

import com.tdinh.challenge.airtasker.util.DateTimeUtils;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * An implementation of {@link RequestThrottler} basing on request rate.
 * 
 * This throttling strategy is influenced by 2 configurable parameters: "timeDuration" and
 * "volume". The request rate should not exceed volume/timeDuration. In other words,
 * there should not be more than "volume" request(s) for the last "timeDuration".
 * 
 * This implementation uses an in-memory cache to keep track of the past requests.
 *  
 */
@Service
public class LocalCacheRateLimiter implements RequestThrottler {
  
  @Value("${app.throttle.ratelimit.time-duration:3600}")
  private Integer timeDuration;
  
  @Value("${app.throttle.ratelimit.volume:100}")
  private Integer volume;
  
  @Autowired
  private DateTimeUtils dateTimeUtils;
  
  private Deque<LocalDateTime> requestHistory;
  
  @PostConstruct
  public void init() {
    requestHistory = new ArrayDeque<>();
  }

  @Override
  public Optional<Long> handleRequest(LocalDateTime currentTimestamp) {
    if(requestHistory.size() < volume) {
      requestHistory.addLast(currentTimestamp);
      return Optional.empty();
    } else {
      //timestamp queue size = volume, 
      //check the gap between the earliest timestamp and the currentTimestamp
      //if gap <= "time-duration", meaning accepting this request would result in more than "volume"
      //requests in a "time-duration", hence throttled.
      LocalDateTime headTimestamp = requestHistory.getFirst(), 
          expiry = headTimestamp.plusSeconds((long)timeDuration);
      if (!currentTimestamp.isAfter(expiry)) {
        return Optional.of(dateTimeUtils.getGapInSecond(currentTimestamp, expiry));
      } else {
        //Otherwise, or gap > "time-duration", hence no throttle, simply remove the earliest timestamp and
        //add the currentTimestamp
        requestHistory.removeFirst();
        requestHistory.addLast(currentTimestamp);
        return Optional.empty();
      }
    }
  }
}
