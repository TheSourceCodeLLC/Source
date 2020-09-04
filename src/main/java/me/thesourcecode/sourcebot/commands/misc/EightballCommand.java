package me.thesourcecode.sourcebot.commands.misc;

import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceChannel;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.message.alerts.InfoAlert;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;

public class EightballCommand extends Command {
    private static CommandInfo INFO = new CommandInfo("8ball",
            "Answers your question.",
            "<question>",
            CommandInfo.Category.GENERAL)
            .withUsageChannels(SourceChannel.COMMANDS);

    private final String[] answers = {
            "Maybe", "Possibly in the future", "I don't think so",
            "No", "Yes", "Most Definitely",
            "I can not answer this question right now", "I think so"
    };

    @Override
    public Message execute(Source source, Message message, String[] args) {
        String question = String.join(" ", args);
        if (question.length() > 1024) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Something went wrong!").setDescription("Please ask a question under 1024 characters!");
            MessageEmbed embed = alert.build(message.getAuthor());
            return new MessageBuilder(embed).build();
        }
        String answer = answers[(int) Math.floor(Math.random() * answers.length)];
        InfoAlert alert = new InfoAlert();
        alert.setDescription("**You asked:** " + question + "\n**Source says:** " + answer);
        message.getChannel().sendMessage(alert.build(message.getAuthor(), "The Magic Eight Ball")).queue();
        return null;
    }

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }
}
