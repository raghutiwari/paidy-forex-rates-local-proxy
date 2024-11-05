package forex.http.rateLimiter.interpreters

import forex.http.rateLimiter.RateLimiterSuccess
import forex.http.rateLimiter.error.RateLimitError

trait RateLimiterAlgebra[F[_]] {

  def isAllowed(token: String): F[Either[RateLimitError, RateLimiterSuccess]]
  def increment(key: String): F[Unit]

}
