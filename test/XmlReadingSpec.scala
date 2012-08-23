import models.rss.RssXmlReader
import org.specs2.mutable.Specification
import xml.XML

class XmlReadingSpec extends Specification {

  "The RSS XML Reader" should {

    "Parse XML into objects" in {
      val xml = getClass.getClassLoader.getResource("rss-feed-1.xml")

      val result = RssXmlReader.parse(XML.load(xml))

      result.isRight must beTrue

      val channel = result.right.get

      channel.title mustEqual ("Sjukt Galen")
      channel.siteUrl mustEqual ("http://www.sjuktgalen.se")
      channel.description mustEqual ("Vägen till total frihet")
      channel.pubDate must beNone
      // todo lastBuildDate

      channel.items must haveSize (10)

      val head = channel.items.head
      head.title mustEqual ("Mandela’s Way: Fifteen Lessons on Life, Love, and Courage")
      head.link mustEqual ("http://www.sjuktgalen.se/2012/08/mandelas-way-fifteen-lessons-on-life-love-and-courage/")

      val last = channel.items.last
      last.title mustEqual ("Christopher McDougall – Born to Run")
    }

  }

}
