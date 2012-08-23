package utils

import java.text.{ParseException, SimpleDateFormat}
import java.util.{Locale, Date}

/** Utility class for parsing date strings witch could be one of multiple date formats
  * @param formats standard jdk simple date format strings in the order they should be tried
  *                when parsing a date
  */
final class DateParser(formats: String*) {

  val dateFormats = formats.map(new SimpleDateFormat(_, Locale.US)).toList

  /** @return None if no of the formats can parse the given date */
  def apply(text: String): Option[Date] = {
    def tryToParse(text: String, formats: List[SimpleDateFormat]): Option[Date] = formats match {
      case Nil => None
      case format :: fs =>
        try {
          Some(format.parse(text))
        } catch {
          // failure, try next format
          case _: ParseException => tryToParse(text, fs)
        }
    }
    tryToParse(text, dateFormats)
  }

}
