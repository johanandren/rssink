package actors

import play.api.libs.iteratee.{Enumerator, PushEnumerator}
import models.atom.Entry
import akka.actor.{ActorLogging, Actor}


object EntryEventSource {
  case object RegisterListener
  case class ListenerRegistered(enumerator: Enumerator[Entry])
  case class UnregisterListener(enumerator: Enumerator[Entry])
}

trait EntryEventSource { this: Actor with ActorLogging =>

  import EntryEventSource._

  var listeners = Set[PushEnumerator[Entry]]()

  def entryEventSourceReceive: Receive = {

    case RegisterListener =>
      log.debug("Registering listener")
      val enumerator = Enumerator.imperative[Entry]()
      listeners += enumerator
      sender ! ListenerRegistered(enumerator)

    case UnregisterListener(enumerator) =>
      log.debug("Unregistering listener")
      listeners = listeners.filterNot(_ == enumerator)

  }

  /** push the given entry to all listeners */
  def publish(entry: Entry) {
    log.debug("Publishing entry: " + entry.id + " to all " + listeners.size + " listeners")
    listeners.foreach { enumerator =>
      enumerator.push(entry)
    }
  }

}
