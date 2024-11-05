package forex.cache

object errors {

  sealed trait Error

  object Error {
    final case class KeyNotFoundInCache(key: String, error: String) extends Error
    final case class CacheNotReachable(key: String, error: String) extends Error
  }

}
