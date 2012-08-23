package actors

import models.atom.{AtomXmlFormat, AtomFeed}
import scalax.file._
import scalax.file.ImplicitConversions._
import play.api.Play.current
import xml.{Node, PrettyPrinter, XML}
import scalax.io.Codec
import play.api.Logger

trait PersistedFeed {

  def feedName: String

  def log: Logger

  private def fileName: String = feedName + ".xml"

  private implicit val codec = Codec.UTF8

  private val persistedFile: Path = Path.fromString(current.path.getAbsolutePath + "/store/" + fileName)

  def loadFeed: Option[AtomFeed] = {
    if (persistedFile.exists) {
      log.debug("Loading persisted feed from " + persistedFile)
      persistedFile.inputStream().acquireFor(XML.load(_)).fold(
        error => None,
        xml =>  {
          AtomXmlFormat.parse(xml).fold(
            error => None,
            feed => Some(feed)
          )
        })
    } else {
      None
    }
  }

  def storeFeed(maybeFeed: Option[AtomFeed]) {
    maybeFeed.foreach { feed =>
      val xml = AtomXmlFormat.write(feed)

      persistedFile.createFile(failIfExists = false)
      XML.save(persistedFile.path, xml.asInstanceOf[Node], "UTF-8", xmlDecl = true)

      log.debug("Feed written to " + persistedFile)
    }
  }
}
