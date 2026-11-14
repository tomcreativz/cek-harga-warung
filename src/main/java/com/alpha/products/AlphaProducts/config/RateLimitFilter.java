package com.alpha.products.AlphaProducts.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Pembatas request sederhana (fixed window per IP, in-memory). */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

  private static final class Window {
    final long startEpochSecond;
    final AtomicInteger count = new AtomicInteger();

    Window(long startEpochSecond) {
      this.startEpochSecond = startEpochSecond;
    }
  }

  private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
  private final boolean enabled;
  private final int maxRequests;
  private final long windowSeconds;

  public RateLimitFilter(
      @Value("${app.rate-limit.enabled:true}") boolean enabled,
      @Value("${app.rate-limit.max-requests:120}") int maxRequests,
      @Value("${app.rate-limit.window-seconds:60}") long windowSeconds) {
    this.enabled = enabled;
    this.maxRequests = maxRequests;
    this.windowSeconds = windowSeconds;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !enabled
        || !request.getRequestURI().startsWith("/api/")
        || HttpMethod.OPTIONS.matches(request.getMethod());
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain chain) throws ServletException, IOException {
    long now = Instant.now().getEpochSecond();
    evictExpired(now);

    Window window = windows.compute(request.getRemoteAddr(), (ip, w) ->
        (w == null || now - w.startEpochSecond >= windowSeconds) ? new Window(now) : w);
    int used = window.count.incrementAndGet();

    if (used > maxRequests) {
      long retryAfter = Math.max(1, windowSeconds - (now - window.startEpochSecond));
      response.setStatus(429);
      response.setHeader("Retry-After", String.valueOf(retryAfter));
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.setCharacterEncoding("UTF-8");
      response.getWriter().write("{\"status\":429,\"error\":\"Too Many Requests\","
          + "\"message\":\"Terlalu banyak permintaan, coba lagi dalam " + retryAfter + " detik\"}");
      return;
    }
    chain.doFilter(request, response);
  }

  private void evictExpired(long now) {
    if (windows.size() > 10_000) {
      windows.entrySet().removeIf(e -> now - e.getValue().startEpochSecond >= windowSeconds);
    }
  }
}
