package forex.http.rateLimiter.error

trait RateLimitError

case class InvalidToken(message: String) extends RateLimitError
case class TokenExhausted(message: String) extends RateLimitError
