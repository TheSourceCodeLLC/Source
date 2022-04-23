package net.sourcebot.module.roleselector.data

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.selections.SelectionMenu
import net.sourcebot.api.database.MongoSerial
import org.bson.Document

class Selector(
    val name: String,
    var roleIds: MutableList<String>,
    var isDisabled: Boolean = false,
    var messageIds: MutableMap<String, MutableList<String>> = hashMapOf(),
    var permission: String = "",
    var placeholder: String = name,
    var message: String = "Select a role here!"
) {

    fun hasPermission(): Boolean = permission.isNotEmpty() && permission.isNotBlank()

    fun toActionRow(guild: Guild): ActionRow {
        val selectionMenu = SelectionMenu.create("roleselector:${name.lowercase()}")
            .setPlaceholder(placeholder)
            .setDisabled(isDisabled)
            .setMinValues(0)
            .setMaxValues(roleIds.size)

        if (roleIds.isEmpty()) selectionMenu.isDisabled = true

        roleIds.map { guild.getRoleById(it) }
            .forEach {
                if (it == null) return@forEach
                selectionMenu.addOption(it.name, "roleselector:${it.id}")
            }

        return ActionRow.of(selectionMenu.build())
    }

    class Serial : MongoSerial<Selector> {
        override fun queryDocument(obj: Selector) = Document("name", obj.name)

        @Suppress("UNCHECKED_CAST")
        override fun deserialize(document: Document): Selector = document.let {
            val name = it["name"] as String
            val isDisabled = it["isDisabled"] as Boolean
            val roleIds = it["roleIds"] as MutableList<String>
            val messageIds = it["messageIds"] as MutableMap<String, MutableList<String>>
            val requiredPermission = it["requiredPermission"] as String
            val placeholder = it["placeholder"] as String
            val message = it["message"] as String
            Selector(name, roleIds, isDisabled, messageIds, requiredPermission, placeholder, message)
        }

        override fun serialize(obj: Selector) = queryDocument(obj).apply {
            append("name", obj.name)
            append("isDisabled", obj.isDisabled)
            append("roleIds", obj.roleIds)
            append("messageIds", obj.messageIds)
            append("requiredPermission", obj.permission)
            append("placeholder", obj.placeholder)
            append("message", obj.message)
        }
    }

}