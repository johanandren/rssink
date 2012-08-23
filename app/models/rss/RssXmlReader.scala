package models.rss

import xml.NodeSeq
import models.XmlReader
import utils.DateParser
import java.util.Date


object RssXmlReader extends XmlReader[Channel] {

  object Conversions {

    private val dateParser = new DateParser(
      "EEE, d MMM yy HH:mm:ss z",
      "EEE, d MMM yy HH:mm z",
      "EEE, d MMM yyyy HH:mm:ss z",
      "EEE, d MMM yyyy HH:mm:ss Z",
      "EEE, d MMM yyyy HH:mm z",
      "d MMM yy HH:mm z",
      "d MMM yy HH:mm:ss z",
      "d MMM yyyy HH:mm z",
      "d MMM yyyy HH:mm:ss z"
    )

    implicit def text2date(text: String): Date = dateParser(text) getOrElse {
      throw new RuntimeException("Unparseable date: '" + text + "'")
    }

    implicit def text2optInt(text: String): Option[Int] =
      if (text.isEmpty) None else Some(text.toInt)
  }

  def parse(node: NodeSeq): Either[RuntimeException, Channel] = {
    import Conversions._

    try {
      Right((node \\ "channel").map {
        chanNode =>
          Channel(
            title = (chanNode \ "title").text,
            description = (chanNode \ "description").text,
            siteUrl = (chanNode \ "link").text,
            lastBuildDate = (chanNode \ "lastBuildDate").text,
            pubDate = (chanNode \ "pubDate").headOption.map(node => text2date(node.text)),
            ttl = (chanNode \\ "ttl").text,
            items = (chanNode \\ "item").map {
              itemNode =>
                Item(
                  guid = Guid((itemNode \\ "guid").text),
                  title = (itemNode \\ "title").text,
                  description = (itemNode \\ "description").text,
                  link = (itemNode \\ "link").text,
                  pubDate = (itemNode \\ "pubDate").text
                )
            }
          )
      }.head)

    } catch {
      case e: RuntimeException => Left(e)
    }
  }
}
