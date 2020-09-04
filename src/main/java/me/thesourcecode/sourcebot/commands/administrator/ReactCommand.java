package me.thesourcecode.sourcebot.commands.administrator;

import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.message.alerts.SuccessAlert;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;

public class ReactCommand extends Command {
    private final CommandInfo INFO = new CommandInfo(
            "react",
            "Makes source add a reaction to all current reactions on a message.",
            "(channel id) <message id>",
            CommandInfo.Category.ADMIN)
            .asGuildOnly()
            .withControlRoles(SourceRole.ADMIN, SourceRole.OWNER);

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public Message execute(Source source, Message message, String[] args) {
        final JDA jda = source.getJda();
        final User user = message.getAuthor();
        final MessageChannel messageChannel = message.getChannel();

        final String channelId = args.length == 1 ? messageChannel.getId() : args[0];
        final String messageId = args.length == 1 ? args[0] : args[1];

        final TextChannel reactChannel = jda.getTextChannelById(channelId);
        if (reactChannel == null) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription("I could not find a text channel with the given channel id!");
            return new MessageBuilder(alert.build(user)).build();
        }

        final Message reactMessage;

        try {
            reactMessage = reactChannel.retrieveMessageById(messageId).complete();
        } catch (ErrorResponseException ex) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!")
                    .setDescription("I could not find a message in the given text channel with that id!");
            return new MessageBuilder(alert.build(user)).build();
        }

        reactMessage.getReactions().forEach(reaction -> {
            final MessageReaction.ReactionEmote reactionEmote = reaction.getReactionEmote();
            try {
                reactMessage.addReaction(reactionEmote.getEmote()).queue();
            } catch (IllegalStateException ex) {
                reactMessage.addReaction(reactionEmote.getName()).queue();
            }
        });

        SuccessAlert alert = new SuccessAlert();
        alert.setDescription("Successfully reacted to all reactions on the given message!");
        return new MessageBuilder(alert.build(user)).build();
    }
}
