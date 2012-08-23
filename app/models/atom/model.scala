package models.atom

import models.FeedType
import java.util.Date

object Atom extends FeedType

// model classes
case class Id(id: String)

/** works both as a model for external feeds and aggregations */
case class AtomFeed(id: Id, title: String, subtitle: String, updated: Option[Date], feedUrl: String, siteUrl: String, entries: Seq[Entry]) {

  /**create a new updated feed with the combined entries of this and a newer feed instance */
  def combine(newFeed: AtomFeed): AtomFeed = {
    newFeed.copy(entries = combine(entries, newFeed.entries))
  }

  /** aggregate the entries of this feed with the given updated feed */
  def aggregate(updatedFeed: AtomFeed): AtomFeed = {
    this.copy(entries = combine(entries, updatedFeed.entries))
  }

  private def combine(e1: Seq[Entry], e2: Seq[Entry]): Seq[Entry] =
    (e1 ++ e2)
      .groupBy(_.id)
      .map(_._2.head)
      .toSeq
      .sortWith((a, b) => a.updated.after(b.updated))

}

case class Entry(id: Id, title: String, url: String, updated: Date, summary: String, author: Option[Author])

case class Author(name: String, email: String)

