package actors

import play.api.libs.iteratee.{Enumerator, PushEnumerator}
import models.atom.Entry
import akka.actor.{ActorLogging, Actor}


object EntryEventSource {

  type Listener = PushEnumerator[Entry]

  case object RegisterListener
  /** returned to the registered listener, and sent to the current listener */
  case class ListenerRegistered(listener: Listener)
  case class UnregisterListener(listener: Listener)
}

trait EntryEventSource { this: Actor with ActorLogging =>

  import EntryEventSource._

  private var listeners0 = Set[Listener]()

  protected def listeners: Set[Listener] = listeners0

  protected def entryEventSourceReceive: Receive = {

    case RegisterListener =>

      log.debug("Registering listener")
      val enumerator = Enumerator.imperative[Entry]()
      listeners0 += enumerator
      val msg = ListenerRegistered(enumerator)

      // inform sender as well as actor inheriting this trait about registration
      sender ! msg
      self ! msg

    case UnregisterListener(enumerator) =>
      log.debug("Unregistering listener")
      listeners0 = listeners0.filterNot(_ == enumerator)

  }

  def publish(entry: Entry, listener: Listener) {
    log.debug("Sending entry " + entry.id + " to listener " + listener)
    listener.push(entry)
  }

  /** push the given entry to all listeners */
  def publishToAll(entry: Entry) {
    log.debug("Publishing entry: " + entry.id + " to all " + listeners.size + " listeners")
    listeners.foreach(publish(entry, _))
  }

}
