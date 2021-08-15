package com.tdinh.challenge.airtasker;

import static org.hamcrest.CoreMatchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration test for rate-limiting application.
 *
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"app.throttle.ratelimit.time-duration=60", 
    "app.throttle.ratelimit.volume=2"})
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
class AirtaskerApplicationTests {

  @Autowired
  private MockMvc mvc;

  @Test
  public void givenNoInitialRequests_whenGetHello_thenStatus200() throws Exception {
    mvc.perform(get("/hello")).andExpect(status().isOk())
        .andExpect(content().string(containsString("Hello Airtasker!")));

    mvc.perform(get("/hello")).andExpect(status().isOk())
        .andExpect(content().string(containsString("Hello Airtasker!")));

  }
  
  @Test
  public void givenNoInitialRequests_whenGetExcessiveHello__thenReturn429() throws Exception {
    mvc.perform(get("/hello")).andExpect(status().isOk())
        .andExpect(content().string(containsString("Hello Airtasker!")));

    mvc.perform(get("/hello")).andExpect(status().isOk())
        .andExpect(content().string(containsString("Hello Airtasker!")));
    
    mvc.perform(get("/hello")).andExpect(status().is(429))
    .andExpect(content().string(containsString("Rate Limit Exceeded! Try again")));
    
  }

}
