package forex.cache

import cats.effect.{ Async, Resource }
import cats.implicits.{ catsSyntaxApplicativeError, toFunctorOps }
import forex.cache.errors.Error
import forex.cache.errors.Error.{ CacheNotReachable, KeyNotFoundInCache }
import forex.domain.{ Currency, Rate }
import io.circe.syntax.EncoderOps
import io.circe.{ parser, Decoder, Encoder }
import org.slf4j.LoggerFactory
import redis.clients.jedis.Jedis
import redis.clients.jedis.params.SetParams

class RedisCurrencyRateCache[F[_]: Async](redisClient: Resource[F, Jedis]) extends CurrencyRateCacheAlgebra[F] {

  private val logger = LoggerFactory.getLogger(getClass)

  implicit val currencyDecoder: Decoder[Currency] = Currency.decodeCurrency
  implicit val rateDecoder: Decoder[Rate]         = Rate.rateDecoder
  implicit val rateEncoder: Encoder[Rate]         = Rate.rateEncoder

  override def getRates(key: String): F[Error Either Rate] =
    redisClient
      .use { redis =>
        Async[F]
          .delay(redis.get(key))
          .map { x =>
            parser.decode[Rate](x)
          }
          .map {
            case Right(value) => Right(value)
            case Left(_)      => Left(KeyNotFoundInCache(key, s"Key not found in cache").asInstanceOf[Error])
          }
      }
      .handleErrorWith { error =>
        logger.error("Error getting rates from Cache", error)
        Async[F].pure(Left(CacheNotReachable(key, s"${error.toString}")))
      }

  override def updateRates(key: String, rate: Rate, timeoutInSeconds: Long): F[String] = redisClient.use { redis =>
    Async[F].delay(redis.set(key, rate.asJson.toString(), new SetParams().ex(timeoutInSeconds)))
  }

}
