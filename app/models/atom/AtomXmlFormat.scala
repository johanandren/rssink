package models.atom

import models._
import xml.NodeSeq
import java.text.SimpleDateFormat
import utils.DateParser
import java.util.Date

object AtomXmlFormat extends XmlReader[AtomFeed] with XmlWriter[AtomFeed] {

  object Conversions {

    val dateParser = new DateParser(
      "yyyy-MM-dd'T'HH:mm:ss",
      "yyyy-MM-dd'T'hh:mm:ss'Z'",
      "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'"
    )

    def dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssZ")

    implicit def text2date(text: String): Option[Date] = dateParser(text)
  }

  // parsing and rendering
  def parse(xml: NodeSeq): Either[RuntimeException, AtomFeed] = {
    import Conversions._

    try {
      Right((xml \\ "feed").map {
        feedNode =>
          AtomFeed(
            id = Id((feedNode \ "id").text),
            title = (feedNode \ "title").text,
            subtitle = (feedNode \ "subtitle").text,
            fetched = (feedNode \ "fetched").text,
            updated = (feedNode \ "updated").text,
            feedUrl = "", // href link rel=self
            siteUrl = "", // href link rel!=self
            entries = (feedNode \ "entry").map {
              entryNode =>
                Entry(
                  id = Id((feedNode \ "id").text),
                  title = (feedNode \ "title").text,
                  url = (feedNode \ "link").text,
                  updated = dateParser((feedNode \ "updated").text).get,
                  summary = (feedNode \ "summary").text,
                  author = (feedNode \ "author").headOption.map {
                    authorNode =>
                      Author(
                        name = (authorNode \ "name").text,
                        email = (authorNode \ "email").text
                      )
                  }
                )
            }
          )
      }.head)
    } catch {
      case e: RuntimeException => Left(e)
    }
  }

  def write(feed: AtomFeed): NodeSeq = {
    import Conversions._

    <feed xmlns="http://www.w3.org/2005/Atom">
      <title>
        {feed.title}
      </title>
      <subtitle>
        {feed.subtitle}
      </subtitle>
      <link href={feed.feedUrl} rel="self"/>
      <link href={feed.siteUrl}/>
      <id>
        {feed.id.id}
      </id>
      {if(feed.updated.isDefined){
        <updated>{dateFormat.format(feed.updated.get)}</updated>
      }}
      {if(feed.fetched.isDefined){
        <fetched>{dateFormat.format(feed.updated.get)}</fetched>
      }}
      { feed.entries.map { entry =>
        <entry>
          <title>
            {entry.title}
          </title>{/*
          <link href="http://example.org/2003/12/13/atom03" />
          <link rel="alternate" type="text/html" href="http://example.org/2003/12/13/atom03.html"/>
          <link rel="edit" href="http://example.org/2003/12/13/atom03/edit"/>
          */}<id>
          {entry.id.id}
        </id>
          <updated>
            {dateFormat.format(entry.updated)}
          </updated>
          <summary>
            {entry.summary}
          </summary>
          {entry.author.map { author =>
            <author>
              <name>
                {author.name}
              </name>
              <email>
                {author.email}
              </email>
            </author>
          }.getOrElse(scala.xml.Null)}
        </entry>
    }}
    </feed>
  }
}
