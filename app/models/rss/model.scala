package models.rss

import models.FeedType
import java.util.Date

object Rss extends FeedType

// model
case class Guid(id: String)

case class Channel(
    title: String,
    description: String,
    siteUrl: String,
    lastBuildDate: Date,
    pubDate: Option[Date],
    ttl: Option[Int],
    items: Seq[Item])

case class Item(
    guid: Guid,
    title: String,
    description: String,
    link: String,
    pubDate: Date)