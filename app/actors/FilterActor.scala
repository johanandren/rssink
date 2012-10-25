package actors

import akka.actor.{ActorRef, Actor}
import models.atom.AtomFeed
import FilterActor._

object FilterActor {

  type Filter = AtomFeed => AtomFeed

  object Filters {

    val removeImages: Filter = htmlTagFilter("img")
    val removeIframes: Filter = htmlBlockFilter("iframe")
    val removeObjects: Filter = htmlBlockFilter("object")

    def htmlTagFilter(tagName: String): Filter = { feed =>
      feed.copy(entries = feed.entries.map { entry =>
        entry.copy(summary = entry.summary.replaceAll("<" + tagName + " [^>]+>", ""))
      })
    }

    def htmlBlockFilter(tagName: String): Filter = { feed =>
      feed.copy(entries = feed.entries.map { entry =>
        entry.copy(summary = entry.summary.replaceAll("<" + tagName + " [^>]+>.*</" + tagName + ">", ""))
      })
    }

  }

}


class FilterActor(destination: ActorRef, filters: Seq[Filter]) extends Actor {

  import FeedActor._

  def receive = {

    case FeedUpdated(descriptor, feed) =>

      val filtered = filters.foldLeft(feed)((feed, filter) => filter(feed))

      destination ! FeedUpdated(descriptor, filtered)

  }
}

