package me.thesourcecode.sourcebot.commands.administrator.giveaway;

import com.mongodb.client.MongoCollection;
import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandHandler;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.message.alerts.ColoredAlert;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.message.alerts.SuccessAlert;
import me.thesourcecode.sourcebot.api.utility.SourceColor;
import me.thesourcecode.sourcebot.api.utility.Utility;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import org.bson.Document;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public class GiveawayStartCommand extends Command {
    private final CommandInfo INFO = new CommandInfo(
            "start",
            "Starts a giveaway.",
            "<duration + s|m|h|d> <amount of winners> (channel) <reward>",
            CommandInfo.Category.ADMIN)
            .withControlRoles(SourceRole.ADMIN, SourceRole.OWNER);

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public Message execute(Source source, Message message, String[] args) {
        User user = message.getAuthor();
        if (args.length < 3) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Incorrect Usage!").setDescription("Syntax: " + CommandHandler.getPrefix() + "giveaway " + INFO.getLabel() + " " + INFO.getArguments());
            MessageEmbed embed = alert.build(message.getAuthor());
            return new MessageBuilder(embed).build();
        }


        long durationInt;
        String durationType;
        long winnerCount;
        String[] durationList = {"s", "m", "h", "d"};
        String reward;
        TextChannel giveawayChannel = (TextChannel) message.getChannel();
        int skipArgs = 2;
        try {
            durationInt = Long.parseLong(args[0].substring(0, args[0].length() - 1));
            durationType = args[0].replaceAll("[0-9]", "");
            winnerCount = Long.parseLong(args[1]);
            if (message.getMentionedChannels().size() > 0 && args[2].equalsIgnoreCase(message.getMentionedChannels().get(0).getAsMention())) {
                giveawayChannel = message.getMentionedChannels().get(0);
                skipArgs = 3;
            }
        } catch (Exception ex) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Incorrect Usage!").setDescription("You did not enter a valid duration or winner count!");
            MessageEmbed embed = alert.build(message.getAuthor());
            return new MessageBuilder(embed).build();
        }
        if (durationInt <= 0) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Incorrect Usage!").setDescription("You can not start a giveaway for 0 or less seconds");
            MessageEmbed embed = alert.build(message.getAuthor());
            return new MessageBuilder(embed).build();
        }
        if (!Arrays.asList(durationList).contains(durationType)) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Incorrect Usage!").setDescription(durationType + ", is not a valid duration type");
            MessageEmbed embed = alert.build(message.getAuthor());
            return new MessageBuilder(embed).build();
        }


        StringBuilder sb = new StringBuilder();
        for (int i = skipArgs; i < args.length; i++) {
            sb.append(args[i]).append(" ");
        }
        reward = sb.toString();

        String expiresIn = Utility.convertDurationType(durationType);
        expiresIn = durationInt == 1 ? expiresIn.substring(0, expiresIn.length() - 1) : expiresIn;
        ColoredAlert cAlert = new ColoredAlert(SourceColor.BLUE);

        MongoCollection mGiveaways = source.getDatabaseManager().getCollection("Giveaways");
        long id = mGiveaways.countDocuments() + 1;
        cAlert.setAuthor(reward + "| Id: " + id, null, source.getJda().getSelfUser().getAvatarUrl())
                .setDescription("React with \uD83C\uDF89 to enter!" +
                        "\n**Expires In:** " + durationInt + " " + expiresIn +
                        "\n**Amount of Winners:** " + winnerCount)
                .setFooter("Expires On: " + getExpirationDate(durationType, durationInt) + " EST", null);
        Message gaSent = giveawayChannel.sendMessage(cAlert.build()).complete();
        gaSent.addReaction("\uD83C\uDF89").queue();

        Document mongoGiveaway = new Document("ID", id)
                .append("MESSAGE_ID", gaSent.getId())
                .append("REWARD", reward)
                .append("WINNER_AMOUNT", winnerCount)
                .append("USER_ID", user.getId())
                .append("CHANNEL_ID", giveawayChannel.getId())
                .append("END", Utility.getUnpunishTime(durationType, durationInt))
                .append("EXPIRED", false);
        mGiveaways.insertOne(mongoGiveaway);

        if (giveawayChannel != message.getTextChannel()) {
            SuccessAlert successAlert = new SuccessAlert();
            successAlert.setDescription("You have successfully started a giveaway for " + reward + " in " + giveawayChannel.getName());
            return new MessageBuilder(successAlert.build(user)).build();
        }
        return null;

    }

    private String getExpirationDate(String durationType, long durationTime) {
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm a");
        LocalDateTime localDate = LocalDateTime.now();

        switch (durationType.toLowerCase()) {
            case "s":
                localDate = localDate.plusSeconds(durationTime);
                break;
            case "m":
                localDate = localDate.plusMinutes(durationTime);
                break;
            case "h":
                localDate = localDate.plusHours(durationTime);
                break;
            case "d":
                localDate = localDate.plusDays(durationTime);
                break;
        }
        return dateFormat.format(localDate).replaceFirst(" ", " at ");
    }

}

