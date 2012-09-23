package actors

final class TimeoutException(msg: String) extends RuntimeException(msg)
final class UnparseableResponseException(msg: String) extends RuntimeException(msg)
final class UnexpectedResponseCodeException(msg: String) extends RuntimeException(msg)


