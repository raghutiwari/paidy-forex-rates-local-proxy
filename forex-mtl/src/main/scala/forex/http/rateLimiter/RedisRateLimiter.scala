package forex.http.rateLimiter

import cats.effect.{ Async, Resource }
import forex.config.RateLimitConfig
import forex.http.rateLimiter.error.{ InvalidToken, RateLimitError, TokenExhausted }
import forex.http.rateLimiter.interpreters.RateLimiterAlgebra
import redis.clients.jedis.Jedis
import cats.implicits._
import redis.clients.jedis.params.SetParams

class RedisRateLimiter[F[_]: Async](redisClient: Resource[F, Jedis], config: RateLimitConfig)
    extends RateLimiterAlgebra[F] {

  private val rateLimitPrefix = "rate_limit"

  override def isAllowed(token: String): F[Either[RateLimitError, RateLimiterSuccess]] = {

    val key = s"$rateLimitPrefix:$token"
    redisClient.use { redis =>
      Async[F].delay(Option(redis.get(key))).map {
        case Some(count) if count.toIntOption.exists(_ >= config.limitPerToken) =>
          Left(TokenExhausted(s"Token exhausted the limit ${config.limitPerToken}"))
        case Some(count) if count.toIntOption.exists(_ < config.limitPerToken) =>
          Right(true)
        case _ => Left(InvalidToken(s"Invalid token"))
      }
    }

  }

  override def increment(key: String): F[Unit] = {

    val rateLimitKey = s"$rateLimitPrefix:$key"
    redisClient.use { redis =>
      Async[F].delay(redis.incr(rateLimitKey)).void
    }
  }
}

object RedisRateLimiter {

  def apply[F[_]: Async](redisClient: Resource[F, Jedis],
                         config: RateLimitConfig,
                         keys: List[String]): F[RateLimiterAlgebra[F]] =
    redisClient
      .use { redis =>
        keys.traverse_ { key =>
          val rateLimitKey = s"rate_limit:$key"
          Async[F].delay(Option(redis.get(rateLimitKey)).flatMap(_.toIntOption)).flatMap {
            case Some(_) => Async[F].pure("")
            case _ =>
              Async[F].delay {
                redis.set(rateLimitKey, "0", new SetParams().ex(config.windowSize.toSeconds))
              }
          }

        }
      }
      .as(new RedisRateLimiter[F](redisClient, config))
}
