package models

import rss._
import atom._

object RssAtomAdapter {

  def convert(rss: Channel): AtomFeed = {
    AtomFeed(
      id = Id(""),
      title = rss.title,
      subtitle = "",
      fetched = None,
      updated = Some(rss.lastBuildDate),
      feedUrl = "",
      siteUrl = rss.siteUrl,
      entries = rss.items.map {
        item =>
          Entry(
            id = Id(item.guid.id),
            title = item.title,
            url = item.link,
            updated = item.pubDate,
            summary = item.description,
            author = None
          )
      }
    )
  }

}
