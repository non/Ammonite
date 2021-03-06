package ammonite.repl.frontend
import acyclic.file
import fastparse.all._
import fastparse.parsers.Combinators.Rule
import scalaparse.Scala._
import scalaparse.syntax.Identifiers._
object Highlighter {

  object BackTicked{
    def unapplySeq(s: Any): Option[List[String]] = {
      "`([^`]+)`".r.unapplySeq(s.toString)
    }
  }

  def defaultHighlight(buffer: Vector[Char],
                       comment: String,
                       `type`: String,
                       literal: String,
                       keyword: String,
                       reset: String) = {
    val boundedIndices = defaultHighlightIndices(buffer, comment, `type`, literal, keyword, reset)
    flattenIndices(boundedIndices, buffer)
  }
  def defaultHighlightIndices(buffer: Vector[Char],
                              comment: String,
                              `type`: String,
                              literal: String,
                              keyword: String,
                              reset: String) = Highlighter.highlightIndices(
    ammonite.repl.Parsers.Splitter,
    buffer,
    {
      case Literals.Expr.Interp | Literals.Pat.Interp => reset
      case Literals.Comment => comment
      case ExprLiteral => literal
      case TypeId => `type`
      case BackTicked(body)
        if alphaKeywords.contains(body) => keyword
    },
    endColor = reset
  )
  def highlightIndices(parser: fastparse.core.Parser[_],
                buffer: Vector[Char],
                ruleColors: PartialFunction[Rule[_], String],
                endColor: String) = {
    val indices = {
      var indices = collection.mutable.Buffer((0, endColor, false))
      var done = false
      val input = buffer.mkString
      parser.parse(input, instrument = (rule, idx, res) => {
        for{
          color <- ruleColors.lift(rule.asInstanceOf[Rule[_]])
          if !done // If we're done, do nothing
          if idx >= indices.last._1 // If this is a re-parse, ignore it
          if color != indices.last._2 // If it does't change the color, why bother?
        } {
          val closeColor = indices.last._2
          val startIndex = indices.length
          indices += ((idx, color, true))

          res() match {
            case s: Result.Success[_] =>
              indices += ((s.index, closeColor, false))
              if (s.index == buffer.length) done = true
            case f: Result.Failure
              if f.index == buffer.length
              && (WL ~ End).parse(input, idx).isInstanceOf[Result.Failure] =>
              // EOF, stop all further parsing
              done = true
            case _ =>  // hard failure, or parsed nothing. Discard all progress
              indices.remove(startIndex, indices.length - startIndex)
          }
        }
      })
      indices
    }
    // Make sure there's an index right at the start and right at the end! This
    // resets the colors at the snippet's end so they don't bleed into later output
    indices ++ Seq((9999, endColor, false))
  }
  def flattenIndices(boundedIndices: Seq[(Int, String, Boolean)],
                     buffer: Vector[Char]) = {
    boundedIndices
      .sliding(2)
      .flatMap{case Seq((s, c1, _), (e, c2, _)) => c1 ++ buffer.slice(s, e) }
      .toVector
  }
  def highlight(parser: Parser[_],
                buffer: Vector[Char],
                ruleColors: PartialFunction[Rule[_], String],
                endColor: String) = {
    val boundedIndices = highlightIndices(parser, buffer, ruleColors, endColor)
    flattenIndices(boundedIndices, buffer)
  }

}
