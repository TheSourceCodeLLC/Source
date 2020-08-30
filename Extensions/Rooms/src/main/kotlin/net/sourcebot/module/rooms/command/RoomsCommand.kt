package net.sourcebot.module.rooms.command

import net.sourcebot.api.command.Command
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.Argument
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.response.ErrorResponse
import net.sourcebot.module.rooms.data.RoomManager

class RoomsCommand(
    private val roomManager: RoomManager
) : RootCommand() {
    override val name = "rooms"
    override val description = "Manage user-defined rooms."
    override val permission = name
    override val guildOnly = true

    private inner class RoomsCreateCommand : Bootstrap(
        "create", "Create a room"
    ) {
        override val argumentInfo = ArgumentInfo(
            Argument("name", "The name of the room to create")
        )
    }

    private class NoRoomCategoryResponse : ErrorResponse(
        "Rooms Error", "There is no valid rooms category!"
    )
}

abstract class Bootstrap(
    override val name: String,
    override val description: String
) : Command() {
    override val guildOnly = true
    override val permission by lazy { "rooms.$name" }
}