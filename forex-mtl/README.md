# Forex Rates Local Proxy

## Key Assumptions and Limitations

1. API rate limit for this proxy: 10,000 requests per day per token.
2. API rate limit for external forex service: 1,000 requests per day.
3. Proxy should handle at least 10,000 requests daily.
4. Exchange rate responses should not be older than 5 minutes.
5. External service may be unreliable, with potential failures or timeouts.
6. Exchange rates are non-invertible (e.g., AUD-SGD != 1/SGD-AUD).

## Implementation Details

The project consists of four main components:

- `OneFrameService`: Client to access live forex rates from the external service.
- `CurrencyRateCacheAlgebra`: Provides a cache for currency rates, integrated with `OneFrameService`. **Redis** is used for caching to enable fast, persistent, and distributed storage.
- `TokenProvider`: Supplies tokens for accessing the external service. **Redis** is used here as well for centralized, scalable token management.
- `RateLimitterAlgebra`: Manages rate-limiting checks for the proxy API. We use **Redis** to track and enforce token limits.

### Why Redis?

Redis is used in three roles:
- Rate caching
- Token storage
- Rate-limiting

Here’s a breakdown of the trade-offs for each role:

#### Rate Caching (5-minute expiration)

Pros
- Distributed and scalable, ideal for horizontal scaling.
- In-memory speed with persistence to prevent data loss in case of failure.

Cons

- Adds dependency on an external service.
- In production, Redis should be clustered to avoid a single point of failure.

#### Rate Limiting (Tracking tokens and limits per token)

Pros

- Shared storage across instances to track token usage limits accurately.

Cons

- Without clustering, Redis could fail, making it a single point of failure.
- Potential for increased latency if network performance is poor.
- High accuracy may require distributed locking, which increases system complexity.

#### Token Management (for external OneFrameService tokens)

Pros
- Shared storage across instances to track token usage limits accurately.

Cons
- Same as rate limiting, with an additional limitation: Redis failures will prevent token access.

## Running the Service

### Docker Compose

To build and run the service:
`docker-compose up --build`

Once the containers are built, you can use `docker-compose up` to avoid building again.

This command builds the Proxy image and pulls the `OneFrame` service image, then starts three containers on a shared network:

- `forex-service` on **port 9000**
- `redis` on **port 6379**
- `one-frame` on **port 8080**

### Accessing API

## API Documentation

### Exchange Rate Lookup Endpoint

Retrieves the exchange rate between two specified currencies.

**URL:** `http://localhost:9090/rates`

**Method:** `GET`

#### URL Parameters

| Parameter | Type   | Description              | Required |
|-----------|--------|--------------------------|----------|
| `from`    | string | The base currency code   | Yes      |
| `to`      | string | The target currency code | Yes      |

#### Supported Currencies

- AUD (Australian Dollar)
- CAD (Canadian Dollar)
- CHF (Swiss Franc)
- EUR (Euro)
- GBP (British Pound)
- NZD (New Zealand Dollar)
- JPY (Japanese Yen)
- SGD (Singapore Dollar)
- USD (United States Dollar)

#### Success Response

**Code:** `200 OK`

**Content example:**

**Sample Request**
```shell
curl -i -H "Token: <INSERT_TOKEN_HERE>" 'http://127.0.0.1:9090/rates?from=USD&to=JPY'
```
**Sample Response**
```json
{
  "from":"USD",
  "to":"JPY",
  "price":0.83959869033317815,
  "timestamp":"2024-11-05T19:35:48.529Z"
}
```


#### Error Responses

**Condition:** If token is invalid or missing.

**Code:** `403 Forbidden`

**Content:**

```
Invalid token: <INSERT_TOKEN_HERE>
```

**Condition:** If rate limit is exceeded.

**Code:** `429 Too Many Requests`

**Content:**

``` 
Token 123 exhausted the limit 1
```

**Condition:** If parameters are invalid or missing.

**Code:** `400 Bad Request`

**Content:**

```
Invalid currency to
```
or
```
Invalid currency from
```
or
```
Invalid currencies
```
**Condition:** If the external service is unavailable or redis is down

**Code:** `503 Service Unavailable`

**Content:**

```
Service Unavailable
```

## Potential Enhancements

- **Enhanced Aggregation**: OneFrameService could aggregate rates from multiple sources for improved redundancy.
- **Local Fallback Cache**: Adding a local fallback cache in case Redis goes down.
- **Improved Rate Limiting**: Distributed locking could ensure accuracy in multi-instance setups.
- **Testing**: Expanding test coverage to cover additional scenarios.
- **Metrics Collection**: Implementing metrics for response times and error rates to monitor Redis and external service dependencies.