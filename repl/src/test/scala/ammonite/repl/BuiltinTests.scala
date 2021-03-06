package ammonite.repl

import utest._

import scala.collection.{immutable => imm}
import acyclic.file
object BuiltinTests extends TestSuite{

  val tests = TestSuite{
    println("EvaluatorTests")
    val check = new Checker()
    'basicConfig{
      check.session("""
        @ // Set the shell prompt to be something else

        @ repl.prompt() = ">"

        @ // Change the terminal front end; the default is

        @ // Ammonite on Linux/OSX and JLineWindows on Windows

        @ repl.frontEnd() = ammonite.repl.frontend.FrontEnd.JLineUnix

        @ repl.frontEnd() = ammonite.repl.frontend.FrontEnd.JLineWindows

        @ repl.frontEnd() = ammonite.repl.frontend.FrontEnd.Ammonite

        @ // Changing the colors used by Ammonite; all at once:

        @ repl.colors() = ammonite.repl.Colors.BlackWhite

        @ repl.colors() = ammonite.repl.Colors.Default

        @ // or one at a time:

        @ repl.colors().prompt() = Console.RED

        @ repl.colors().ident() = Console.GREEN

        @ repl.colors().`type`() = Console.YELLOW

        @ repl.colors().literal() = Console.MAGENTA

        @ repl.colors().prefix() = Console.CYAN

        @ repl.colors().comment() = Console.RED

        @ repl.colors().keyword() = Console.BOLD

        @ repl.colors().selected() = Console.UNDERLINED

        @ repl.colors().error() = Console.YELLOW
      """)
    }

    'workingDir{
      check.session("""
        @ val originalWd = wd

        @ import ammonite.ops._

        @ val originalLs1 = %%ls

        @ val originalLs2 = ls!

        @ cd! up

        @ assert(wd == originalWd/up)

        @ cd! root

        @ assert(wd == root)

        @ assert(originalLs1 != (%%ls))

        @ assert(originalLs2 != (ls!))
      """)
    }
  }
}
