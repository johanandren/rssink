package actors

import play.api.Logger

trait PlayActorLogging {

  protected val log = Logger(getClass)

}
