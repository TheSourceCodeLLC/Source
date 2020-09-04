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

public class MinecraftRoleCommand extends Command {

    private static CommandInfo INFO = new CommandInfo(
            "minecraft",
            "Gives the user the minecraft role")
            .withUsageChannels(SourceChannel.COMMANDS).withAliases("mc", "mcr", "minecraftrole");

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

        Role minecraftRole = SourceRole.MINECRAFT.resolve(source.getJda());
        List<Role> memberRoles = member.getRoles();

        ColoredAlert alert = new ColoredAlert(SourceColor.ORANGE);
        if (memberRoles.contains(minecraftRole)) {
            guild.removeRoleFromMember(member, minecraftRole).queue();

            alert.setDescription("The minecraft role has successfully been removed from you!");
        } else {
            alert = new ColoredAlert(SourceColor.GREEN);
            guild.addRoleToMember(member, minecraftRole).queue();

            alert.setDescription("The minecraft role has successfully been added to you!");
        }

        return new MessageBuilder(alert.build(user)).build();
    }
}
