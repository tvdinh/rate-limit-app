package com.tdinh.challenge.airtasker.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

/**
 * Class to provide common processing utility for DateTime.
 *
 */
@Component
public class DateTimeUtils {
  
  private static final ZoneId DEFAULT_ZONE_ID = ZoneId.of("Australia/Sydney");
  private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

  public long getGapInSecond(LocalDateTime fromDate, LocalDateTime toDate) {
    if (fromDate == null || toDate == null) {
      throw new IllegalArgumentException("null date");
    }
    return (toDate.atZone(DEFAULT_ZONE_ID).toInstant().toEpochMilli() - 
        fromDate.atZone(DEFAULT_ZONE_ID).toInstant().toEpochMilli()) / 1000;
  }

  public String toDateString(LocalDateTime datetime) {
    if (datetime == null) {
      throw new IllegalArgumentException("null date");
    }
    return datetime.format(DATETIME_FORMATTER);
  }

}
