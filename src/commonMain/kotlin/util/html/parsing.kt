package util.html

enum class TagType(val identifier: String, val byteCode: UByte, val single: Boolean = false, val textOnly: Boolean = false) {
    CUSTOM("", 1u),
    COMMENT("", 2u, true),
    STRING("", 3u),
    DOCTYPE("!doctype", 4u, true),
    HTML("html", 5u),
    HEAD("head", 6u),
    BODY("body", 7u),
    DIV("div", 8u),
    SCRIPT("script", 9u, false, true),
    STYLE("style", 10u, false, true),
    A("a", 11u),
    TITLE("title", 12u),
    HEADER("header", 13u),
    H1("h1", 14u),
    P("p", 15u),
    UL("ul", 16u),
    LI("li", 17u),
    META("meta", 18u, true),
    BR("br", 19u, true),
    LINK("link", 20u, true),
    IMG("img", 21u, true),
    SPAN("span", 23u),
    I("i", 24u),
    NAV("nav", 25u),
    H4("h4", 26u),
    H3("h3", 27u),
    H2("h2", 28u),
    STRONG("strong", 29u),
    IFRAME("iframe", 30u),
    FOOTER("footer", 31u),
    ;

    companion object {
        fun forIdentifier(identifier: String): TagType {
            values().forEach {
                if (it.identifier == identifier.toLowerCase()) return it
            }
            return CUSTOM
        }
        fun forByteCode(byteCode: UByte): TagType {
            values().forEach {
                if (it.byteCode == byteCode) return it
            }
            throw IllegalArgumentException("Tag with bytecode $byteCode does not exist")
        }
    }
}

open class Tag(val type: TagType, val attributes: Array<Attribute> = emptyArray(), val content: Array<Tag> = emptyArray()) {
    constructor(identifier: String, attributes: Array<Attribute> = emptyArray(), content: Array<Tag> = emptyArray())
            : this(TagType.forIdentifier(identifier), attributes, content)

    override fun toString(): String {
        return "Tag(type=$type, attributes=${attributes.contentToString()}, content=${content.contentToString()})"
    }

    open fun toHtml(): String {
        var result = "<${type.identifier}"
        for (attribute in attributes) result += " ${attribute.toHtml()}"
        return if (content.isEmpty()) {
            if (type.single) "$result>" else "$result></${type.identifier}>"
        } else {
            result += ">"
            for (tag in content) result += tag.toHtml()
            result += "</${type.identifier}>"
            result
        }
    }

    companion object {
        fun createTag(identifier: String, attributes: Array<Attribute> = emptyArray(), content: Array<Tag> = emptyArray()): Tag {
            val tagType = TagType.forIdentifier(identifier)
            return if (tagType === TagType.CUSTOM) CustomTag(identifier, attributes, content) else Tag(tagType, attributes, content)
        }
    }
}

class StringTag(val string: String): Tag(TagType.STRING) {
    override fun toString(): String {
        return "StringTag(string='$string')"
    }

    override fun toHtml(): String {
        return string
    }
}

class CommentTag(val value: String): Tag(TagType.COMMENT) {
    override fun toString(): String {
        return "CommentTag(value='$value')"
    }

    override fun toHtml(): String {
        return "<!$value>"
    }
}

class CustomTag(val identifier: String, attributes: Array<Attribute> = emptyArray(), content: Array<Tag> = emptyArray())
    : Tag(TagType.CUSTOM, attributes, content) {
    override fun toString(): String {
        return "CustomTag(identifier='$identifier')"
    }

    override fun toHtml(): String {
        var result = "<${identifier}"
        for (attribute in attributes) result += " ${attribute.toHtml()}"
        return if (content.isEmpty()) {
            if (type.single) "$result>" else "$result></${identifier}>"
        } else {
            result += ">"
            for (tag in content) result += tag.toHtml()
            result += "</${identifier}>"
            result
        }
    }
}

enum class AttributeType(val identifier: String, val byteCode: UByte) {
    CUSTOM("", 1u),
    CONTENT("", 2u),
    HREF("href", 3u),
    HTML("html", 4u),
    LANG("lang", 5u),
    CLASS("class", 6u),
    CHARSET("charset", 7u),
    NAME("name", 8u),
    SRC("src", 9u),
    CONTENT_ATTR("content", 10u),
    STYLE("style", 11u),
    ONCLICK("onclick", 12u),
    TARGET("target", 13u),
    ID("id", 14u),
    REL("rel", 16u),
    TYPE("type", 17u),
    ASYNC("async", 18u),
    TITLE("title", 19u),
    ALT("alt", 20u),
    ;

    companion object {
        fun forIdentifier(identifier: String): AttributeType {
            values().forEach {
                if (it.identifier == identifier.toLowerCase()) return it
            }
            return CUSTOM
        }
        fun forByteCode(byteCode: UByte): AttributeType {
            values().forEach {
                if (it.byteCode == byteCode) return it
            }
            throw IllegalArgumentException("Attribute with bytecode $byteCode does not exist")
        }
    }
}

open class Attribute(val attributeType: AttributeType, val value: String? = null) {
    constructor(identifier: String, value: String? = null)
            : this(AttributeType.forIdentifier(identifier), value)

    override fun toString(): String {
        return "Attribute(attributeType=$attributeType, value='$value')"
    }

    open fun toHtml(): String {
        if (value == null) return attributeType.identifier
        val quote = if (value.contains('"')) '\'' else '"'
        return "${attributeType.identifier}=$quote$value$quote"
    }

    companion object {
        fun createAttribute(identifier: String, value: String? = null): Attribute {
            val attributeType = AttributeType.forIdentifier(identifier)
            return if (attributeType == AttributeType.CUSTOM) CustomAttribute(identifier, value) else Attribute(attributeType, value)
        }
    }
}

class CustomAttribute(val identifier: String, value: String? = null)
    : Attribute(AttributeType.CUSTOM, value) {
    override fun toString(): String {
        return "CustomAttribute(identifier='$identifier')"
    }

    override fun toHtml(): String {
        if (value == null) return identifier
        val quote = if (value.contains('"')) '\'' else '"'
        return "$identifier=$quote$value$quote"
    }
}

data class HtmlFile(val rootTags: Array<Tag>) {
    fun toHtml(): String {
        return rootTags.joinToString("") { it.toHtml() }
    }
}

class ParsingException(override val message: String?): Exception(message)

class HtmlParser(val code: String) {
    private var index: Int = 0

    private fun consumeChar(): Char? {
        return if (index < code.length) {
            val result: Char = code[index]
            index++
            result
        } else null
    }

    private fun nextChar(): Char? {
        return if (index < code.length) code[index] else null
    }

    private fun consumeWhile(predicate: (Char) -> Boolean): String {
        var result = ""
        while (true) {
            val char: Char? = nextChar()
            if (char !== null && predicate(char)) {
                result += char
                consumeChar()
            } else break
        }
        return result
    }

    private fun consumeWhitespace(): String {
        return consumeWhile { it.isWhitespace() }
    }

    private fun checkString(string: String): Boolean {
        return code.substring(index).startsWith(string)
    }

    private fun consumeString(string: String) {
        var index = 0
        consumeWhile {
            if (index >= string.length) return@consumeWhile false
            val char: Char = string[index]
            index++
            it == char
        }
    }

    private fun parseIdentifier(): String {
        return consumeWhile { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' || it == '!' || it == '-' }
    }

    private fun checkAndSkipChars(vararg chars: Char): Char {
        val char: Char? = consumeChar()
        if (char === null || char !in chars) throw ParsingException("Unexpected char '${consumeChar()}' (expected: $chars)")
        return char
    }

    private fun checkChars(vararg chars: Char): Boolean {
        val char: Char? = nextChar()
        return char !== null && char in chars
    }

    private fun parseAttributeValue(): String {
        consumeWhitespace()
        val openQuote = checkAndSkipChars('\'', '"')
        val result = consumeWhile { it != openQuote }
        checkAndSkipChars(openQuote)
        return result
    }

    private fun parseAttribute(): Attribute {
        consumeWhitespace()
        val identifier = parseIdentifier()
        return if (nextChar() == '=') {
            consumeChar()
            val value = parseAttributeValue()
            Attribute.createAttribute(identifier, value)
        } else Attribute.createAttribute(identifier)
    }

    private fun parseString(): StringTag {
        return StringTag(consumeWhile { it != '<' })
    }

    private fun checkAndConsumeString(string: String): Boolean {
        return if (checkString(string)) {
            consumeString(string)
            true
        } else false
    }

    private fun parseTag(): Tag {
        consumeWhitespace()
        checkAndSkipChars('<')
        if (checkString("!--") || checkString("![endif]")) {
            consumeString("!")
            val content = if (checkString("--[")) consumeWhile { !checkAndConsumeString("]>") } + "]" else consumeWhile { !checkAndConsumeString("-->") } + "--"
            return CommentTag(content)
        }
        val identifier = parseIdentifier()
        if (identifier.isEmpty()) throw ParsingException("Identifier expected")
        val tagType = TagType.forIdentifier(identifier)
        var attributes: Array<Attribute> = emptyArray()
        while (true) {
            consumeWhitespace()
            if (checkChars('/', '>')) break
            attributes += parseAttribute()
        }
        if (checkChars('/') || (checkChars('>') && tagType.single)) {
            consumeString(if (checkChars('/')) "/>" else ">")
            return Tag.createTag(identifier, attributes)
        } else {
            consumeString(">")
            if (tagType.textOnly) {
                var string = ""
                while (!checkString("</$identifier")) string += consumeChar()
                consumeString("</$identifier")
                consumeWhitespace()
                consumeString(">")
                return Tag.createTag(identifier, attributes, arrayOf(StringTag(string)))
            } else {
                var children = emptyArray<Tag>()
                while (true) {
                    if (checkString("</")) {
                        consumeString("</")
                        val endIdentifier = parseIdentifier()
                        if (endIdentifier == identifier) {
                            consumeWhitespace()
                            consumeString(">")
                            return Tag.createTag(identifier, attributes, children)
                        } else {
                            throw ParsingException("Bad closing tag identifier </$endIdentifier>, </$identifier> expected")
                        }
                    } else children += parseEntity()
                }
            }
        }
    }

    private fun parseEntity(): Tag {
        return if (checkChars('<')) parseTag() else parseString()
    }

    private fun hasChars(): Boolean {
        return nextChar() != null
    }

    private fun nextTag(): Boolean {
        var i = index
        while (i < code.length && code[i].isWhitespace()) i++
        return i < code.length && code[i] == '<'
    }

    fun parseAll(): HtmlFile {
        var elements = emptyArray<Tag>()
        while (true) {
            if (hasChars()) elements += parseEntity() else break
        }
        return HtmlFile(elements)
    }
}

