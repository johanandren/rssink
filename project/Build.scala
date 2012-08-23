import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "rssink"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      // Add your project dependencies here,
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      // Add your own project settings here

      // add test/ to test classpath so that we can read sample XML:s
      unmanagedClasspath in Test <+= (baseDirectory) map { bd => Attributed.blank(bd / "test") }
    )
}
