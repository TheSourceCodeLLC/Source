package me.thesourcecode.sourcebot.commands.administrator.giveaway;

import com.mongodb.client.MongoCollection;
import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandHandler;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.message.alerts.SuccessAlert;
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

public class GiveawayRerollCommand extends Command {
    private final CommandInfo INFO = new CommandInfo(
            "reroll",
            "Rerolls a giveaway.",
            "<giveaway id>",
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
        if (!expired) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription("You can not reroll a giveaway that has not ended!");
            return new MessageBuilder(alert.build(user)).build();
        }
        TextChannel giveawayChannel = source.getJda().getTextChannelById(found.getString("CHANNEL_ID"));
        String reward = found.getString("REWARD");
        String senderId = found.getString("USER_ID");
        long winner_ammount = found.getLong("WINNER_AMOUNT");

        Message gaMessage;
        try {
            gaMessage = giveawayChannel.retrieveMessageById(found.getString("MESSAGE_ID")).complete();
        } catch (Exception ex) {
            mongoGiveaways.deleteOne(found);
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription("The giveaway you specified was invalid!");
            return new MessageBuilder(alert.build(user)).build();
        }

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
                CriticalAlert alert = new CriticalAlert();
                alert.setTitle("Uh Oh!").setDescription("You can not reroll a giveaway where no one has entered!");
                return new MessageBuilder(alert.build(user)).build();
            }
            winners.addAll(users);
        }
        if (users.size() == 1) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription("You can not reroll a giveaway where only one person has entered!");
            return new MessageBuilder(alert.build(user)).build();
        } else if (users.size() == winner_ammount) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription("You can not reroll a giveaway where the amount of entries equals the winner amount!");
            return new MessageBuilder(alert.build(user)).build();
        }

        int max = users.size() - 1;

        Random rand = new Random();
        int randomNum = rand.nextInt(100);
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
            if (randomNum >= 50) randomNum = 1;
            else randomNum = 0;
            winners.add(users.get(randomNum));
        }


        reward = reward.replace("`", "").trim();
        String winnerMessage = "__%s | Id: %d__\n\nThe %s %s %s won `%s`";

        StringBuilder sbWinners = new StringBuilder();
        winners.forEach(winnerUser -> {
            sbWinners.append(winnerUser.getAsMention()).append(", ");
        });

        boolean singularWinner = winner_ammount == 1;

        sbWinners.deleteCharAt(sbWinners.length() - 2);

        if (!singularWinner) {
            int lastIndexOfComma = sbWinners.lastIndexOf(",");
            sbWinners.replace(lastIndexOfComma, lastIndexOfComma + 1, " and");
        }


        String winnersList = sbWinners.toString().trim();


        winnerMessage = String.format(winnerMessage, reward, id, singularWinner ? "user" : "users", winnersList,
                singularWinner ? "has" : "have", reward);

        giveawayChannel.sendMessage(winnerMessage).queue();

        SuccessAlert alert = new SuccessAlert();
        alert.setDescription("You have successfully rerolled the giveaway with the id: " + id + "!");
        return new MessageBuilder(alert.build(user)).build();
    }

}



