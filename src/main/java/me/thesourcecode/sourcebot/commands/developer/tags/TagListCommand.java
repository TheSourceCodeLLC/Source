package me.thesourcecode.sourcebot.commands.developer.tags;

import com.mongodb.Block;
import com.mongodb.client.MongoCollection;
import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.manager.DatabaseManager;
import me.thesourcecode.sourcebot.api.message.alerts.ColoredAlert;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.utility.SourceColor;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Collections;

public class TagListCommand extends Command {
    private final CommandInfo INFO = new CommandInfo(
            "list",
            "Shows all the current tags",
            "(category)",
            CommandInfo.Category.DEVELOPER
    );

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public Message execute(Source source, Message message, String[] args) {
        User user = message.getAuthor();
        DatabaseManager dbManager = source.getDatabaseManager();
        MongoCollection tags = dbManager.getCollection("Tags");

        if (tags.countDocuments() == 0) {
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!").setDescription("There are currently no tags!");
            return new MessageBuilder(alert.build(user)).build();
        }

        Document catListDoc = dbManager.getTagCategories();
        ArrayList<String> categoryList = (ArrayList<String>) catListDoc.get("categoryList");

        ColoredAlert alert = new ColoredAlert(SourceColor.BLUE);
        if (args.length > 0) {
            String categoryName = args[0];
            if (!containsCategory(categoryList, categoryName) && !categoryName.equalsIgnoreCase("uncategorized")) {
                CriticalAlert cAlert = new CriticalAlert();
                cAlert.setDescription("There is no category with that name!");
                return new MessageBuilder(cAlert.build(message.getAuthor())).build();
            }

            String tagList = formatTagList(tags, categoryName);

            alert.setDescription("Here is a list of all tags in the " + categoryName.toLowerCase() + " category:\n" + tagList);

            tags.find(new Document("category", categoryName.toLowerCase()));
        } else {
            alert.setDescription("Here is a list of all the current tag names:\n");
            Collections.reverse(categoryList);

            categoryList.forEach(categoryName -> {
                String tagList = formatTagList(tags, categoryName);
                alert.addField(categoryName, tagList, false);
            });
        }

        message.getChannel().sendMessage(alert.build(user, "Tag List")).queue();

        return null;
    }

    private String formatTagList(MongoCollection tags, String categoryName) {
        StringBuilder sb = new StringBuilder();

        tags.find(new Document("category", categoryName.toLowerCase())).forEach((Block) object -> {
            Document tag = (Document) object;
            String name = tag.getString("name").replace("`", "\\`");
            sb.append("`").append(name).append("`, ");
        });

        String tagList = sb.toString().trim();
        if (!tagList.isBlank()) {
            tagList = tagList.substring(0, tagList.length() - 1);
            return tagList;
        }

        return "`There are currently no tags in this category!`";
    }

    private boolean containsCategory(ArrayList<String> catList, String catName) {
        for (String name : catList) {
            if (name.equalsIgnoreCase(catName)) {
                return true;
            }
        }
        return false;
    }

}
