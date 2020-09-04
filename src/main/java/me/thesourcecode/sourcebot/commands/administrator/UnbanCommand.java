package me.thesourcecode.sourcebot.commands.administrator;

import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.message.alerts.SuccessAlert;
import me.thesourcecode.sourcebot.api.objects.incidents.SourceUnban;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

public class UnbanCommand extends Command {
    private final CommandInfo INFO = new CommandInfo(
            "unban",
            "Unbans the specified user.",
            "<id> <reason>",
            CommandInfo.Category.ADMIN)
            .withControlRoles(SourceRole.ADMIN, SourceRole.OWNER).withAliases("ub");

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public Message execute(Source source, Message message, String[] args) {
        User user = message.getAuthor();

        Guild guild = source.getGuild();

        // Checks if the target is null
        User targetUser;
        try {
            targetUser = guild.retrieveBanById(args[0]).complete().getUser();
        } catch (Exception ex) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription("I couldn't find any banned user with that id!");
            return new MessageBuilder(alert.build(user)).build();
        }

        String reason = String.join(" ", args).replaceFirst(args[0], "");
        SourceUnban sourceUnban = new SourceUnban(user.getId(), targetUser.getId(), reason);
        sourceUnban.sendIncidentEmbed();
        sourceUnban.execute();

        // Sends a success embed
        SuccessAlert sAlert = new SuccessAlert();
        sAlert.setDescription("You have successfully unbanned " + targetUser.getAsTag() + "!");

        return new MessageBuilder(sAlert.build(user)).build();
    }
}
