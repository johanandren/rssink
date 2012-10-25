package actors

import models.atom.{AtomXmlFormat, AtomFeed}
import scalax.file._
import play.api.Play.current
import xml.{Node, XML}
import scalax.io.Codec
import akka.actor.ActorLogging

trait PersistedFeed { this: ActorLogging =>

  def feedName: String

  private def fileName: String = feedName + ".xml"

  private implicit val codec = Codec.UTF8

  private val persistedFile: Path = Path.fromString(current.path.getAbsolutePath + "/store/" + fileName)

  private def existingFile(path: Path) =
    if (path.exists)
      Some(path)
    else
      None

  def loadFeed: Option[AtomFeed] = {
    existingFile(persistedFile).flatMap { path =>

      log.debug("Loading persisted feed from " + path)
      path.inputStream().acquireFor(XML.load(_)).fold(
        error => None,
        xml =>  {
          AtomXmlFormat.parse(xml).fold(
            error => None,
            feed => Some(feed)
          )
        })
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
