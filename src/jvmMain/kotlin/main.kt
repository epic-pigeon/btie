import util.bin.BinUtils
import util.bin.HtmlDecoder
import util.bin.HtmlEncoder
import util.html.HtmlParser
import java.io.File

fun main() {
    val htmlFile = HtmlParser(
        File("./test.html").readText()
    ).parseAll()
    println(File("./test.html").readText().length)
    //println(htmlFile)
    val byteArray = BinUtils.htmlToZip(htmlFile)
    println(byteArray.size)
    File("./test.s").writeBytes(byteArray)
    val bytesRead = File("./test.s").readBytes()
    File("./test2.html").writeText(BinUtils.zipToHtml(bytesRead).toHtml())
}
