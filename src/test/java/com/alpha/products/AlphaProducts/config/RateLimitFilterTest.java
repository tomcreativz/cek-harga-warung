package com.alpha.products.AlphaProducts.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitFilterTest {

  private final FilterChain chain = (req, res) -> {};

  private int call(RateLimitFilter f, String ip, String uri, String method) throws Exception {
    MockHttpServletRequest req = new MockHttpServletRequest(method, uri);
    req.setRemoteAddr(ip);
    MockHttpServletResponse res = new MockHttpServletResponse();
    f.doFilter(req, res, chain);
    return res.getStatus();
  }

  @Test
  void blocksAfterLimitPerIp() throws Exception {
    RateLimitFilter f = new RateLimitFilter(true, 2, 60);
    assertThat(call(f, "1.1.1.1", "/api/v1/products", "GET")).isEqualTo(200);
    assertThat(call(f, "1.1.1.1", "/api/v1/products", "GET")).isEqualTo(200);
    assertThat(call(f, "1.1.1.1", "/api/v1/products", "GET")).isEqualTo(429);
    assertThat(call(f, "2.2.2.2", "/api/v1/products", "GET")).isEqualTo(200);
  }

  @Test
  void ignoresPreflightAndNonApi() throws Exception {
    RateLimitFilter f = new RateLimitFilter(true, 1, 60);
    for (int i = 0; i < 5; i++) {
      assertThat(call(f, "1.1.1.1", "/api/v1/products", "OPTIONS")).isEqualTo(200);
      assertThat(call(f, "1.1.1.1", "/index.html", "GET")).isEqualTo(200);
    }
  }

  @Test
  void disabledFlagSkipsLimiting() throws Exception {
    RateLimitFilter f = new RateLimitFilter(false, 1, 60);
    for (int i = 0; i < 5; i++) {
      assertThat(call(f, "1.1.1.1", "/api/v1/products", "GET")).isEqualTo(200);
    }
  }
}
