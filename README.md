# API Rate Limiting with Bucket4j and Redis

## Introduction

In modern distributed systems, managing API traffic efficiently is crucial to ensure fair resource usage, maintain performance, and prevent abuse. In this tutorial, we will implement **API rate limiting** in a Spring Boot application using **Bucket4j** and **Redis**.

This approach allows us to:
- Apply rate limits per user dynamically.
- Prevent system overload and ensure service stability.
- Store and manage rate limits efficiently using Redis.

## Why Rate Limiting is Essential

Without proper rate limiting, APIs can become vulnerable to:

- **System overload**, leading to degraded performance.
- **Denial of Service (DoS) attacks**, making services unavailable.
- **Unpredictable infrastructure costs** due to uncontrolled usage.

By implementing rate limiting, we can:
- Ensure **fair usage** of resources.
- Prevent **excessive API calls** from any single user.
- Enable **tiered API access**, where premium users get higher limits.

---

## Understanding the Token Bucket Algorithm

We use the **Token Bucket Algorithm** to manage rate limits efficiently:

1. Each user has a **bucket** with a predefined number of tokens.
2. Each API request **consumes** a token.
3. Tokens **refill** at a fixed rate over time.
4. If no tokens remain, requests are **rejected** until replenished.

This algorithm ensures that occasional bursts of traffic are allowed while enforcing an overall rate limit.

---

## Technology Stack

- **Spring Boot** - Backend framework
- **Redis** - Distributed caching for rate limits
- **PostgreSQL** - Stores user-specific rate limits
- **Bucket4j** - Implements token bucket rate limiting
- **Testcontainers** - For integration testing

---

## Prerequisites

Ensure the following before running the project:

- **PostgreSQL** is installed and running.
- A database named **`sample`** is created.
- Insert test users with different rate limits.
- **Redis** is running on **port 6379**.

### Important Configuration

pom.xml
```
<dependency>
  <groupId>org.redisson</groupId>
  <artifactId>redisson-spring-boot-starter</artifactId>
  <version>3.17.0</version>
</dependency>
<dependency>
  <groupId>com.giffing.bucket4j.spring.boot.starter</groupId>
  <artifactId>bucket4j-spring-boot-starter</artifactId>
  <version>0.5.2</version>
  <exclusions>
    <exclusion>
      <groupId>org.ehcache</groupId>
      <artifactId>ehcache</artifactId>
    </exclusion>
  </exclusions>
</dependency>
```
### Cache Configuration
Firstly, we need to start our Redis server. Let's say we have a Redis server running on port 6379 on our local machine.

- This file creates a configuration object that we can use to create a connection.
- Creates a cache manager using the configuration object. This will internally create a connection to the Redis instance and create a hash called "cache" on it.
- Creates a proxy manager that will be used to access the cache. Whatever our application tries to cache using the JCache API, it will be cached on the Redis instance inside the hash named "cache".
- "userList" is used to cache User Table Data, this is explained in UserService.java

```
@Configuration
public class RedisConfig {
    @Value("${capitale.ratelimit.redis}")
    private String redisUrl;

	@Bean
    public Config config() {
        Config config = new Config();

        config.useSingleServer().setAddress(redisUrl);
        return config;
    }
    
    @Bean(name="springCM")
    public CacheManager cacheManager(Config config) {
        CacheManager manager = Caching.getCachingProvider().getCacheManager();
        manager.createCache("limits", RedissonConfiguration.fromConfig(config));
        manager.createCache("userList", RedissonConfiguration.fromConfig(config));
        return manager;
    }

    @Bean
    ProxyManager<String> proxyManager(CacheManager cacheManager) {
        return new JCacheProxyManager<>(cacheManager.getCache("limits"));
    }
}
```
### Creating Bucket

- this class creates a bucket (Refill,Bandwidth and Bucket)
- We created the proxy manager for the purpose of storing buckets on Redis. Once a bucket is created, it needs to be cached on Redis and does not need to be created again
-  ProxyManager's takes two parameters – a key and a configuration object that it will use to create the bucket.
-  in getConfigSupplierForUser method it first gets the User Object by UserId and based on the userLimit its creates a Bucket
```
@Service
public class RateLimiter {

	@Autowired
	UserService userService;

	@Autowired
	ProxyManager<String> proxyManager;

	public Bucket resolveBucket(String key) {
		Supplier<BucketConfiguration> configSupplier = getConfigSupplierForUser(key);

		return proxyManager.builder().build(key, configSupplier);
	}

	private Supplier<BucketConfiguration> getConfigSupplierForUser(String userId) {
		User user = userService.getUser(userId);

		Refill refill = Refill.intervally(user.getLimit(), Duration.ofMinutes(1));
		Bandwidth limit = Bandwidth.classic(user.getLimit(), refill);
		return () -> (BucketConfiguration.builder().addLimit(limit).build());

	}
}
```

### API Controller

```
@GetMapping("/v1/secure")
	public String getSecureUser() {
		return "Hello, Secure User";
	}

	@GetMapping("/v2/open")
	public String getOpenUser() {
		return "Hello, Open User";
	}
```

### How to consume Token

- to make it generic a filter has been created
- all the request URL that start with /v1 are secured with tokens
- user id need to pass in header
- if user exist and have a valid token, the request will too to controller Layer else it will thror 409 (Too many Request Exception) 
```
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
```
