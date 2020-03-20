package util.bin

import util.html.*
import util.zip.ZipUtils

object HtmlEncoder {
    private fun encodeString(string: String): UByteArray {
        var result = ubyteArrayOf()
        for (i in string) result += i.toByte().toUByte()
        result += '\u0000'.toByte().toUByte()
        return result
    }
    private fun encodeStringTag(tag: StringTag): UByteArray {
        return ubyteArrayOf(tag.type.byteCode) + encodeString(tag.string)
    }

    private var customAttributes: Array<String> = emptyArray()

    private fun encodeAttribute(attribute: Attribute): UByteArray {
        return ubyteArrayOf(attribute.attributeType.byteCode) +
                (if (attribute.attributeType === AttributeType.CUSTOM) {
                    if ((attribute as CustomAttribute).identifier !in customAttributes) {
                        println("Warning: custom attribute '${attribute.identifier}' encoded")
                        customAttributes += attribute.identifier
                    }
                    encodeString(attribute.identifier)
                } else ubyteArrayOf()) +
                if (attribute.value === null) ubyteArrayOf(0u) else (ubyteArrayOf(1u) + encodeString(attribute.value))
    }

    fun generateAttributeCode(): String { // лень - двигатель прогресса!
        var result = ""
        var index = 15
        customAttributes.forEach {
            index++
            result += "${it.toUpperCase()}(\"$it\", ${index}u),\n"
        }
        return result
    }

    fun generateTagCode(): String { // опять
        var result = ""
        var index = 22
        customTags.forEach {
            index++
            result += "${it.toUpperCase()}(\"$it\", ${index}u),\n"
        }
        return result
    }

    private var customTags: Array<String> = emptyArray()

    private fun encodeTag(tag: Tag): UByteArray {
        if (tag.type == TagType.COMMENT) return ubyteArrayOf(TagType.COMMENT.byteCode) + encodeString((tag as CommentTag).value)
        if (tag.type == TagType.STRING) return encodeStringTag(tag as StringTag)
        var result = ubyteArrayOf(tag.type.byteCode)
        if (tag.type == TagType.CUSTOM) {
            result += encodeString((tag as CustomTag).identifier)
            if (tag.identifier !in customTags) {
                println("Warning: custom tag <${tag.identifier}> encoded")
                customTags += tag.identifier
            }
        }
        for (attribute in tag.attributes) result += encodeAttribute(attribute)
        result += ubyteArrayOf(AttributeType.CONTENT.byteCode)
        result += encodeTags(tag.content)
        result += ubyteArrayOf(0u)
        return result
    }
    private fun encodeTags(tags: Array<Tag>): UByteArray {
        var result = ubyteArrayOf()
        for (tag in tags) result += encodeTag(tag)
        return result
    }
    fun encodeHtmlFile(htmlFile: HtmlFile): UByteArray {
        return encodeTags(htmlFile.rootTags)
    }
}

class HtmlDecoder(val uByteArray: UByteArray, var index: Int = 0) {
    private inline fun consumeByte(): UByte? {
        return if (index < uByteArray.size) {
            val result = uByteArray[index]
            index++
            result
        } else null
    }
    private inline fun nextByte(): UByte? {
        return if (index < uByteArray.size) uByteArray[index] else null
    }
    private fun decodeString(): String {
        var result = ""
        while (true) {
            var byte = consumeByte()!!
            if (byte == 0u.toUByte()) return result
            result += byte.toByte().toChar()
        }
    }
    private fun decodeStringTag(): StringTag {
        if (consumeByte() != TagType.STRING.byteCode) throw IllegalStateException()
        return StringTag(decodeString())
    }
    private fun decodeComment(): CommentTag {
        if (consumeByte() != TagType.COMMENT.byteCode) throw IllegalStateException()
        return CommentTag(decodeString())
    }
    private fun decodeAttribute(): Attribute {
        val attributeType = AttributeType.forByteCode(consumeByte()!!)
        val identifier = if (attributeType === AttributeType.CUSTOM) decodeString() else attributeType.identifier
        return if (consumeByte() == 0u.toUByte()) Attribute.createAttribute(identifier)
               else Attribute.createAttribute(identifier, decodeString())
    }
    private fun decodeTag(): Tag {
        if (nextByte() == TagType.STRING.byteCode) return decodeStringTag()
        if (nextByte() == TagType.COMMENT.byteCode) return decodeComment()
        val tagType = TagType.forByteCode(consumeByte()!!)
        val identifier: String = if (tagType == TagType.CUSTOM) decodeString() else tagType.identifier
        var attributes = emptyArray<Attribute>()
        while (nextByte() != AttributeType.CONTENT.byteCode) {
            attributes += decodeAttribute()
        }
        consumeByte()
        var content = emptyArray<Tag>()
        while (nextByte() != 0u.toUByte()) {
            content += decodeTag()
        }
        consumeByte()
        return Tag.createTag(identifier, attributes, content)
    }
    fun decodeHtml(): HtmlFile {
        var tags = emptyArray<Tag>()
        while (nextByte() != null) {
            tags += decodeTag()
        }
        return HtmlFile(tags)
    }
}

object BinUtils {
    fun htmlToZip(htmlFile: HtmlFile): ByteArray {
        return ZipUtils.encode(HtmlEncoder.encodeHtmlFile(htmlFile).toByteArray())
        //return HtmlEncoder.encodeHtmlFile(htmlFile).toByteArray()
    }
    fun zipToHtml(byteArray: ByteArray): HtmlFile {
        return HtmlDecoder(ZipUtils.decode(byteArray).toUByteArray()).decodeHtml()
        //return HtmlDecoder(byteArray.toUByteArray()).decodeHtml()
    }
}