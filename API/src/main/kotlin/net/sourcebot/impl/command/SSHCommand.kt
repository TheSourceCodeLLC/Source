package net.sourcebot.impl.command

import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import me.hwiggy.kommander.arguments.Adapter
import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.Synopsis
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardErrorResponse
import net.sourcebot.api.response.StandardSuccessResponse
import java.util.*

class SSHCommand : RootCommand() {
    override val name = "ssh"
    override val description = "Force an SSH connection"
    override val requiresGlobal = true

    override val synopsis = Synopsis {
        reqParam("command", "The command to send when connected.", Adapter.slurp())
        reqParam("username", "The username to connect as.", Adapter.single(), "root")
        reqParam("host", "The host to connect to.", Adapter.single(), "localhost")
        reqParam("port", "The port to connect to.", Adapter.int(), 22)
    }

    override fun execute(sender: Message, arguments: Arguments.Processed): Response {
        val keyId = UUID.randomUUID()
        val command = arguments.required<String>("command", "You must provide a command!")
        val username = arguments.required<String>("username", "You must provide a username!")
        val host = arguments.required<String>("host", "You must provide a host!")
        val port = arguments.required<Int>("port", "You must provide a port!")
        val keyFile = sender.attachments.firstOrNull() ?: return StandardErrorResponse(
            "You must attach a key file!"
        )
        keyFile.downloadToFile("keys/$keyId").thenAccept {
            val jsch = JSch().apply {
                addIdentity(it.path)
            }
            val session = jsch.getSession(username, host, port).apply {
                setConfig("StrictHostKeyChecking", "no")
                connect()
            }
            val channel = session.openChannel("exec") as ChannelExec
            channel.setCommand(command)
            channel.connect();
            while (channel.isConnected) {
                Thread.sleep(50)
            }
            it.delete()
        }
        return StandardSuccessResponse("SSH command executed...")
    }
}