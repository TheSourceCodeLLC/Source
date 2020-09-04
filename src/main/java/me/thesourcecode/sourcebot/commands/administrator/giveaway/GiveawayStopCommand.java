package me.thesourcecode.sourcebot.commands.administrator.giveaway;

import com.mongodb.client.MongoCollection;
import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandHandler;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.message.alerts.SuccessAlert;
import me.thesourcecode.sourcebot.api.utility.SourceColor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class GiveawayStopCommand extends Command {
    private final CommandInfo INFO = new CommandInfo(
            "stop",
            "Stops a giveaway.",
            "<giveaway id)",
            CommandInfo.Category.ADMIN)
            .withControlRoles(SourceRole.ADMIN, SourceRole.OWNER);

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public Message execute(Source source, Message message, String[] args) {
        User user = message.getAuthor();
        if (args.length < 1) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Incorrect Usage!").setDescription("Syntax: " + CommandHandler.getPrefix() + "giveaway " + INFO.getLabel() + " " + INFO.getArguments());
            MessageEmbed embed = alert.build(message.getAuthor());
            return new MessageBuilder(embed).build();
        }

        long id;
        try {
            id = Long.parseLong(args[0]);
        } catch (NumberFormatException ex) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription("You did not enter a valid id!");
            return new MessageBuilder(alert.build(user)).build();
        }

        MongoCollection mongoGiveaways = source.getDatabaseManager().getCollection("Giveaways");
        Document search = new Document("ID", id);
        Document found = (Document) mongoGiveaways.find(search).first();
        if (found == null) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription("There is no giveaway with the id: " + id + "!");
            return new MessageBuilder(alert.build(user)).build();
        }
        boolean expired = found.getBoolean("EXPIRED");
        if (expired) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription("You can not stop a giveaway which has already ended!");
            return new MessageBuilder(alert.build(user)).build();
        }
        Document update = new Document("EXPIRED", true);
        Document query = new Document().append("ID", id);
        mongoGiveaways.updateOne(query, new Document("$set", update));

        TextChannel giveawayChannel = source.getJda().getTextChannelById(found.getString("CHANNEL_ID"));
        String reward = found.getString("REWARD");
        String senderId = found.getString("USER_ID");
        long winner_ammount = found.getLong("WINNER_AMOUNT");

        Message gaMessage;
        try {
            gaMessage = giveawayChannel.retrieveMessageById(found.getString("MESSAGE_ID")).complete();
        } catch (Exception ex) {
            mongoGiveaways.deleteOne(found);
            return null;
        }

        SuccessAlert successAlert = new SuccessAlert();
        successAlert.setDescription("You have successfully stopped the giveaway with the id: " + id + "!");

        EmbedBuilder newEmbed = new EmbedBuilder()
                .setAuthor(reward + " | Id: " + id, null, source.getJda().getSelfUser().getAvatarUrl())
                .setColor(SourceColor.RED.asColor())
                .setDescription("**Expires In:** Expired" +
                        "\n**Amount of Winners:** " + winner_ammount)
                .setFooter(gaMessage.getEmbeds().get(0).getFooter().getText().replace("Expires On:", "Expired On:"), null);
        gaMessage.editMessage(newEmbed.build()).queue();

        List<User> users = gaMessage.getReactions().get(0).retrieveUsers().complete();

        Iterator it = users.iterator();
        while (it.hasNext()) {
            User fWinner = (User) it.next();
            if (fWinner.isBot() || fWinner.getId().equals(senderId)) it.remove();
        }

        List<User> winners = new ArrayList<>();
        boolean skip = false;
        if (users.size() <= winner_ammount) {
            if (users.size() == 0) {
                EmbedBuilder embed = new EmbedBuilder()
                        .setAuthor(reward, null, source.getJda().getSelfUser().getAvatarUrl())
                        .setColor(SourceColor.RED.asColor())
                        .setDescription("No one won the reward `" + reward + "`!");

                giveawayChannel.sendMessage(embed.build()).queue();
                return new MessageBuilder(successAlert.build(user)).build();
            }
            winners.addAll(users);
            skip = true;
        }
        if (!skip) {
            int max = users.size() - 1;

            Random rand = new Random();
            int randomNum = rand.nextInt((max - 1) + 1) + 1;
            if (winner_ammount > 1) {
                for (int i = 0; i < winner_ammount; i++) {
                    randomNum = rand.nextInt((max - 1) + 1) + 1;
                    User uFound = users.get(randomNum);

                    if (winners.contains(uFound) || uFound.isBot()) {
                        continue;
                    }
                    winners.add(uFound);
                }
            } else {
                winners.add(users.get(randomNum));
            }
        }

        EmbedBuilder embed = new EmbedBuilder()
                .setAuthor(reward + " | Id: " + id, null, source.getJda().getSelfUser().getAvatarUrl())
                .setColor(SourceColor.BLUE.asColor())
                .setDescription("The users ");
        int temp = 1;
        for (User fWinner : winners) {
            embed.appendDescription(temp == 1 ? fWinner.getAsMention() : ", " + fWinner.getAsMention());
            temp++;
        }
        if (winner_ammount == 1) {
            embed.setDescription("The user " + winners.get(0).getAsMention() + " has won `" + reward + "`!");
        } else {
            embed.appendDescription(" have won `" + reward + "`!");
        }
        giveawayChannel.sendMessage(embed.build()).queue();

        return new MessageBuilder(successAlert.build(user)).build();
    }


}

