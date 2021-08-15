package com.tdinh.challenge.airtasker.utils;

import com.tdinh.challenge.airtasker.util.DateTimeUtils;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit test class for {@link DateTimeUtils}.
 *
 */
public class DateTimeUtilsTest {

  private DateTimeUtils dateTimeUtils = new DateTimeUtils();
  
  @Test
  public void testGetGapInSecond() {
    LocalDateTime now = LocalDateTime.now();
    Assertions.assertEquals(120L, dateTimeUtils.getGapInSecond(now, now.plusMinutes(2L)));
    Assertions.assertEquals(100L, dateTimeUtils.getGapInSecond(now, now.plusSeconds(100L)));
  }
  
}
