package me.thesourcecode.sourcebot.commands.administrator;

import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class AntiRaidCommand extends Command {
    private final CommandInfo INFO = new CommandInfo(
        "antiraid",
        "Ban suspected raid participants.",
        CommandInfo.Category.ADMIN
    ).asGuildOnly().withControlRoles(SourceRole.ADMIN, SourceRole.OWNER);

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public Message execute(Source source, Message message, String[] args) {
        final Guild guild = message.getGuild();
        final Instant now = Instant.now();
        guild.getMembersWithRoles(
            SourceRole.UNVERIFIED.resolve(message.getJDA())
        ).stream().filter(
            it -> it.getTimeJoined()
                .toInstant()
                .isAfter(now.minus(15, ChronoUnit.MINUTES)
                )).forEach(it -> it.ban(7, "Raid Candidate.").queue());
        return null;
    }
}
