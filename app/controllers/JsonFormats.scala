package controllers

import play.api.libs.json._
import play.api.libs.json.Json.toJson
import models.atom.Entry

object JsonFormats {
  implicit object EntryWrites extends Writes[Entry] {
    def writes(entry: Entry) =
      JsObject(Seq(
        "id" -> JsString(entry.id.id),
        "author" -> entry.author.map { author =>
          JsObject(Seq(
            "name" -> JsString(author.name),
            "email" -> JsString(author.email)
          ))
        }.getOrElse(JsNull),
        "title" -> JsString(entry.title),
        "summary" -> JsString(entry.summary),
        "updated" -> JsString(entry.updated.toString), // TODO dateformat
        "url" -> JsString(entry.url)
      ))
  }
}
