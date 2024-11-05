package forex.services.rates.token

import cats.effect.Async
import forex.http.rateLimiter.error.InvalidToken
import cats.implicits.{ toFlatMapOps, toFunctorOps }

class TokenProvider[F[_]: Async](cache: F[TokenCacheAlgebra[F]]) {

  def getToken: F[Either[InvalidToken, String]] = cache.flatMap { cache =>
    cache.getToken.flatMap {
      case Some(token) => cache.incrementUsage(token).map(_ => Right(token))
      case None        => Async[F].pure(Left(InvalidToken("No tokens available")))
    }
  }

}
