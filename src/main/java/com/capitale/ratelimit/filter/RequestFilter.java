package com.capitale.ratelimit.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.capitale.ratelimit.service.RateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import io.github.bucket4j.Bucket;
import io.micrometer.core.instrument.util.StringUtils;

@Component
public class RequestFilter extends OncePerRequestFilter {

	@Autowired
	RateLimiter rateLimiter;
	
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		
		if(isRateLimitedPath(request.getRequestURI())) {
			String clientId = request.getHeader("X-Client-ID");
			if(StringUtils.isNotBlank(clientId)) {
				Bucket bucket = rateLimiter.resolveBucket(clientId);
				if(bucket.tryConsume(1)) {
					filterChain.doFilter(request, response);
				} else {
					sendErrorResponse(response, HttpStatus.TOO_MANY_REQUESTS.value());
				}
			} else {
				sendErrorResponse(response, HttpStatus.FORBIDDEN.value());
			}
		} else {
			filterChain.doFilter(request, response);
		}

	}

	private void sendErrorResponse(HttpServletResponse response, int value) {
        ((HttpServletResponse)response).setStatus(value);
		
		((HttpServletResponse)response).setContentType(MediaType.APPLICATION_JSON_VALUE);
	}
	/**
	 * Checks if the request URI should be rate-limited.
	 */
	private boolean isRateLimitedPath(String requestURI) {
		return requestURI.startsWith("/v1");
	}

}
