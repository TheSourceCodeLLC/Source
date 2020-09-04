package me.thesourcecode.sourcebot.commands.misc;

import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceChannel;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.message.alerts.ColoredAlert;
import me.thesourcecode.sourcebot.api.utility.SourceColor;
import me.thesourcecode.sourcebot.api.utility.Utility;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;

import java.util.List;

public class GFMSCommand extends Command {

    private static CommandInfo INFO = new CommandInfo(
            "gmfs",
            "Gives the user the give me free stuff role")
            .withUsageChannels(SourceChannel.COMMANDS);

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public Message execute(Source source, Message message, String[] args) {
        User user = message.getAuthor();

        Guild guild = source.getGuild();
        Member member = Utility.getMemberByIdentifier(guild, user.getId());
        if (member == null) return null;

        Role gmfs = SourceRole.GMFS.resolve(source.getJda());
        List<SourceRole> memberRoles = SourceRole.getRolesFor(member);

        ColoredAlert alert = new ColoredAlert(SourceColor.ORANGE);
        if (memberRoles.contains(SourceRole.GMFS)) {
            guild.removeRoleFromMember(member, gmfs).queue();

            alert.setDescription("The give me free stuff role has successfully been removed from you!");
        } else {
            alert = new ColoredAlert(SourceColor.GREEN);
            guild.addRoleToMember(member, gmfs).queue();

            alert.setDescription("The give me free stuff role has successfully been added to you!");
        }

        return new MessageBuilder(alert.build(user)).build();
    }
}
