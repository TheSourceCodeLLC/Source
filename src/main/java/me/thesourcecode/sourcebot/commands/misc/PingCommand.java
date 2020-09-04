package me.thesourcecode.sourcebot.commands.misc;

import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceChannel;
import me.thesourcecode.sourcebot.api.message.alerts.InfoAlert;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;

public class PingCommand extends Command {
    private static CommandInfo INFO = new CommandInfo("ping", "Test the bot's ping.")
            .withAliases("pong", "p").withUsageChannels(SourceChannel.COMMANDS);

    @Override
    public Message execute(Source source, Message message, String[] args) {
        long createMillis = message.getTimeCreated().toEpochSecond() * 1000L;
        long nowMillis = System.currentTimeMillis();
        long ping = (nowMillis - createMillis);
        String format = "Pong! **%dms**";
        InfoAlert alert = new InfoAlert();
        alert.setTitle("Ping Response!").setDescription(String.format(format, ping));
        MessageEmbed embed = alert.build(message.getAuthor());
        return new MessageBuilder(embed).build();
    }

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }
}
