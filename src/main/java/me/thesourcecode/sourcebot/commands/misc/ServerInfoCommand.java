package me.thesourcecode.sourcebot.commands.misc;

import me.thesourcecode.sourcebot.BotMain;
import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceChannel;
import me.thesourcecode.sourcebot.api.message.alerts.InfoAlert;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;

import java.time.ZonedDateTime;

public class ServerInfoCommand extends Command {
    private static final CommandInfo INFO = new CommandInfo(
            "serverinfo",
            "Show server information."
    ).withAliases("info").withUsageChannels(SourceChannel.COMMANDS).asGuildOnly();

    @Override
    public Message execute(Source source, Message message, String[] args) {
        Guild guild = source.getGuild();
        String name = guild.getName();
        Member owner = guild.getOwner();
        ZonedDateTime creationDate = guild.getTimeCreated().atZoneSameInstant(BotMain.TIME_ZONE);
        String creationDateStr = creationDate.format(BotMain.DATE_FORMAT);
        String creationTimeStr = creationDate.format(BotMain.TIME_FORMAT);
        String creation = creationDateStr + " " + creationTimeStr;
        String image = guild.getIconUrl();
        int roles = guild.getRoles().size();
        int textChannels = guild.getTextChannels().size();
        int voiceChannels = guild.getVoiceChannels().size();
        int categories = guild.getCategories().size();
        int members = guild.getMembers().size();
        String format = "" +
                "**Owner:** %s\n" +
                "**Created:** %s\n" +
                "**Members:** %d\n" +
                "**Roles:** %d";
        String channelFormat = "" +
                "**Categories:** %d\n" +
                "**Text Channels:** %d\n" +
                "**Voice Channels:** %d";
        InfoAlert alert = new InfoAlert();
        alert.setThumbnail(image);
        alert.setDescription(String.format(format, owner.getAsMention(), creation, members, roles));
        alert.addField("Channel Information:", String.format(channelFormat, categories, textChannels, voiceChannels), false);
        message.getChannel().sendMessage(alert.build(message.getAuthor(), name + " Guild Information:")).queue();
        return null;
    }

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }
}
