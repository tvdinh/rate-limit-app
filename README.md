# rate-limit-app
A webservice with request throttling enabled using rate limiting strategy.

# Overview

This is a webservice module that responds to client's greeting requests with request throtlling enabled. The request throtlling is in the form of a rate limiter defined by two configurable parameters: `TIME-DURATION` and `VOLUME`. The rate limiter ensures that there are no more than `VOLUME` number of requests are served within the last `TIME-DURATION` period. For example, if `TIME-DURATION = 3600s` and `VOLUME = 100`, the rate limiter allows only 100 requests per hour, exceeding requests will result in a HTTP 429 Too Many Request response from the server. 

Exposing HTTP method: `GET /hello`. Port: `8080`.

Sample response without throttling:

```
< HTTP/1.1 200 
Hello Airtasker! Received at 2021-08-15T17:45:29
```

When rate limiter kicks in:

```
< HTTP/1.1 429 
Rate Limit Exceeded! Try again in x seconds
```

where `x` is the remmaining time until the throttling is lifted.

# Run/Deploying service

## Bash

### Runtime prerequisite: 

* Java 11.

### Run

Start service via bash:

```
bash run-service.sh <TIME-DURATION> <VOLUME>
```

(`TIME-DURATION` and `VOLUME` are configurable, default to 3600s and 100 requests)
e.g

```
bash run-service.sh 
```

starts the module with the applied rate limit of 100 requests per hour.

```
bash run-service.sh 60 5
```

starts the module with the applied rate limit of 5 requests per minute.

## Docker

A language agnostic deployment method is also provided using Docker (e.g Java is not available):

* First build the service docker image:

```
docker build -t rate-limiter-app:latest .
```

* then run:

```
docker run -e "APP_THROTTLE_RATELIMIT_TIME-DURATION=3600" -e "APP_THROTTLE_RATELIMIT_VOLUME=100" -p 8080:8080 rate-limit-app:latest 
```

## Usage by examples:

Let's start the module with a rate limit of 3 requests per minute.

```
bash run-service.sh 60 3
```

And send 4 consecutive http requests with `curl` client

* 1:

```
date; curl http://localhost:8080/hello
```

Got:

```
Sun 15 Aug 2021 18:10:15 AEST
Hello Airtasker! Received at 2021-08-15T08:10:15
```

* 2:

```
date; curl http://localhost:8080/hello
```

Got:

```
Sun 15 Aug 2021 18:10:16 AEST
Hello Airtasker! Received at 2021-08-15T08:10:16
```

* 3:

```
date; curl http://localhost:8080/hello
```

Got:

```
Sun 15 Aug 2021 18:10:18 AEST
Hello Airtasker! Received at 2021-08-15T08:10:18
```

* 4: 

```
date; curl http://localhost:8080/hello
```

Got:

```
< HTTP/1.1 429 
Sun 15 Aug 2021 18:10:22 AEST
Rate Limit Exceeded! Try again in 53 seconds
```

As 3 requests have been served since `Sun 15 Aug 2021 18:10:15 AEST`, requests will be throttled until `Sun 15 Aug 2021 18:11:15 AEST` which 53 seconds away from `Sun 15 Aug 2021 18:10:22 AEST`.

# Build (Domain Language Specific)

So far, the module has been described in a language agnostic manner, that is hiding the specific framework and programming language underneath. This section discusses how the service is built from source code.

* Web Framework: Springboot
* Language: Java 11
* Build tool: Gradle 7.1.1

## Build:

```
./gradlew clean build
```

## Run with Springboot:

```
./gradlew bootrun
```

# Implementation

This section explains some key parts of implementation of rate limiter. It uses Domain Specific Language terms and framework notations (e.g Java and SpringBoot).

* `WebController`

The module is first of all a webservice that serves a HTTP request via endpoint `GET /hello`, hence there is a `WebController` that maps the request to a service method:

```java
  @GetMapping("/hello")
  public ResponseEntity<String> hello() {
    LocalDateTime timestamp = LocalDateTime.now();
    Optional<Long> wait = requestThrottler.handleRequest(timestamp);
    if (wait.isEmpty()) {
      return ResponseEntity.ok(
          (new StringBuilder())
            .append("Hello Airtasker! Received at ")
            .append(dateTimeUtils.toDateString(timestamp)).append("\n")
            .toString());
    } else {
      return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
          .body((new StringBuilder())
              .append("Rate Limit Exceeded! Try again in ")
              .append(wait.get())
              .append(" seconds\n")
              .toString());
    }
```

The logic is rather simple: once the controller receives a request, it calls the `requestThrottler` passing on the timestamp when the request is received. The `requestThrottler` returns an `Optional` of a wait value. If that `Optional` is empty, hence no need to wait, then it goes ahead and serves the request. Otherwise it returns a HTTP 429 response indicating how long the requestor must wait until the next request is accepted.

* `LocalCacheRateLimiter`

The `requestThrottler` is an instance of a `LocalCacheRateLimiter` that implements `RequestThrotller` with the following interface:

```
public Optional<Long> handleRequest(LocalDateTime timestamp);
```
As the name suggests, the `LocalCacheRateLimiter` keeps an in-memory `Deque<LocalDateTime>` which is basically a queue with easy access to the head and the tail. This queue basically stores the timestamps of the last `VOLUME` requests, with the latest on tail. 

```java
  @Override
  public Optional<Long> handleRequest(LocalDateTime currentTimestamp) {
    if(requestHistory.size() < volume) {
      requestHistory.addLast(currentTimestamp);
      return Optional.empty();
    } else {
      LocalDateTime headTimestamp = requestHistory.getFirst(), 
          expiry = headTimestamp.plusSeconds((long)timeDuration);
      if (!currentTimestamp.isAfter(expiry)) {
        return Optional.of(dateTimeUtils.getGapInSecond(currentTimestamp, expiry));
      } else {
        requestHistory.removeFirst();
        requestHistory.addLast(currentTimestamp);
        return Optional.empty();
      }
    }
  }
```

Processing logic:
- If the queue size is less than `VOLUME` (at the start of the service), simply add the timestamp to the head of the queue, and return no throtlling.
- If the queue size = `VOLUME`, hence the queue is full and there is an `expiry` time until which all requests will be throttled. And that `expiry` is `headRequest's timestamp + TIME-DURATION`, so if the current request's timestamp is less than or equal to the `expiry`, the request is throtlled, and the requestor should wait for `expiry = current request's timestamp`. If the current request's timestamp is greater than `expiry`, hence it should not be throttled. And to maintain the constant queue size, remove the `headRequest` as it's no longer relevant and add the current request's timestamp to tail.

Other parts are very common and stardard to a webservice, which won't be discussed here.

# Testing

Unit tests and Integration tests are written using tools provided by SpringBoot/Junit5.

Test coverage: 96.8%.

# Remarks:

## Extensibility

The webservice employs a `RequestThrottler` which is abstracted as an interface where the current Rate Limiter is an implementation. Should a new strategy is required, only that implementation needs to be written and applied, no amendment is required from other parts of the service.

This webservice is written in a form of a web application with an embedded rate limiter. But the rate limiter service can be externalised as a standablone module that can be placed infront of other applications (e.g an API Gateway) so that these applications can be free of request throttling concern. 

Should that be the case, the module can expose another method e.g `GET /throttle?endpoint=[]`, that returns throtlling time for a given endpoint (0 means no throlling). 

## Limitations

- The Rate Limiter uses an in-memory cache to keep track of past request timestamps. If the application is crashed and restarted (which is common for microservices), the cache is gone hence it may not work correctly afterwards. To remedy this point, a remote cache (memcache) or a centralised database can be used. A centralised cache can also be useful in case the service is deployed with multiple instances (e.g auto-scaling). Using in-memory cache then will result in each cache per instance, hence unexpected behaviours if the load is not distributed uniformly. Note that even though in-memory cache is used, memory utilisation is not a big concern here as the cache size is a constant (size <= VOLUME).

- The current rate limiter is designed for throttling requests for the entire application not per endpoint. Normally, a webservice serves many endpoints, and it can be useful that the rate limiter can distinguish endpoints too (e.g `/hello` and `/hi`). However, this is not a complex enhancement, i.e we can add another interface `EndpointRequestThrottler` which a method that passes in the endpoint too:

```java
Optional<Long> handleRequest(String endpoint, LocalDateTime timestamp);
```



