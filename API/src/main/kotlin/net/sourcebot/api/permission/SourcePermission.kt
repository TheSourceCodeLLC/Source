package net.sourcebot.api.permission

import net.sourcebot.api.database.MongoSerial
import org.bson.Document
import java.util.*

class SourcePermission internal constructor(
    val node: String,
    val flag: Boolean,
    val context: String? = null
) {
    class Serial : MongoSerial<SourcePermission> {
        override fun deserialize(document: Document) = document.let {
            val node = document["node"] as String
            val flag = document["flag"] as Boolean
            val context = document["context"] as String?
            SourcePermission(node, flag, context)
        }

        override fun serialize(obj: SourcePermission) = Document().apply {
            append("node", obj.node)
            append("flag", obj.flag)
            if (obj.context != null) append("context", obj.context)
        }
    }

    override fun hashCode() = Objects.hash(node, flag, context)
    override fun equals(other: Any?): Boolean {
        if (other !is SourcePermission) return false
        return node == other.node && flag == other.flag && context == other.context
    }
}