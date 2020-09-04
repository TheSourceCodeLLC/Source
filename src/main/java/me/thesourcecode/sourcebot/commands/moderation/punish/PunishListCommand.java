package me.thesourcecode.sourcebot.commands.moderation.punish;

import com.mongodb.client.MongoCollection;
import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.message.alerts.ColoredAlert;
import me.thesourcecode.sourcebot.api.utility.SourceColor;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import org.bson.Document;

import java.util.ArrayList;

public class PunishListCommand extends Command {

    private final CommandInfo INFO = new CommandInfo(
            "list",
            "Lists all of the available punishments.",
            CommandInfo.Category.MODERATOR
    ).withControlRoles(SourceRole.STAFF_MOD);

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public Message execute(Source source, Message message, String[] args) {
        User user = message.getAuthor();
        ColoredAlert alert = new ColoredAlert(SourceColor.BLUE);

        MongoCollection guildSettings = source.getDatabaseManager().getCollection("Settings");
        Document punishmentsDocument = (Document) guildSettings.find(new Document("name", "offenses")).first();

        int count = 1;
        for (int level = 1; level <= 5; level++) {
            ArrayList<String> foundOffenses = (ArrayList<String>) punishmentsDocument.get("level " + level);
            if (foundOffenses == null) continue;

            int checkNewLine = 1;
            StringBuilder fieldStringBuilder = new StringBuilder();

            for (String offenseName : foundOffenses) {
                fieldStringBuilder.append("**").append(count).append("** `").append(offenseName)
                        .append(checkNewLine != 3 ? (offenseName.length() > 40 ? "` " : "` | ") : "` ");

                if (checkNewLine == 3 || offenseName.length() > 40) {
                    fieldStringBuilder.append("\n");
                    checkNewLine = 0;
                }
                checkNewLine++;
                count++;
            }

            String feildDescription = fieldStringBuilder.toString().trim();
            feildDescription = feildDescription.endsWith("`") ? feildDescription : feildDescription.substring(0, feildDescription.length() - 1);

            switch (level) {
                case 1:
                    alert.addField("Level One - 3.7 Points", feildDescription, false);
                    break;
                case 2:
                    alert.addField("Level Two - 11.1 Points", feildDescription, false);
                    break;
                case 3:
                    alert.addField("Level Three - 33.3 Points", feildDescription, false);
                    break;
                case 4:
                    alert.addField("Level Four  - 77.8 Points", feildDescription, false);
                    break;
                case 5:
                    alert.addField("Level Five - 100 Points", feildDescription, false);
                    break;
            }
        }

        message.getChannel().sendMessage(alert.build(user, "Offense List")).queue();
        return null;
    }

}
