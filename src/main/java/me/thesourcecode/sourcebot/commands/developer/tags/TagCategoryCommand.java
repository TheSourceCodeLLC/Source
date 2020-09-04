package me.thesourcecode.sourcebot.commands.developer.tags;

import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.manager.DatabaseManager;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.message.alerts.SuccessAlert;
import me.thesourcecode.sourcebot.api.objects.SourceTag;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Iterator;

public class TagCategoryCommand extends Command {

    private final CommandInfo INFO = new CommandInfo(
            "category",
            "Creates, removes, renames, and sets tag categories",
            "<create|remove|set> (tag name) <category name>",
            CommandInfo.Category.DEVELOPER
    ).withControlRoles(SourceRole.DEVELOPERS_STAFF);

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public Message execute(Source source, Message message, String[] args) {
        User user = message.getAuthor();
        final String action = args[0].toLowerCase();
        final DatabaseManager dbManager = source.getDatabaseManager();

        Document catListDoc = dbManager.getTagCategories();
        ArrayList<String> categoryList = (ArrayList<String>) catListDoc.get("categoryList");

        String categoryName = args[1];
        String tagName;

        CriticalAlert alert = new CriticalAlert();
        alert.setTitle("Uh Oh!");

        SuccessAlert sAlert = new SuccessAlert();
        sAlert.setTitle("Success!");
        switch (action) {
            case "add":
            case "create":
                if (containsCategory(categoryList, categoryName)) {
                    alert.setDescription("There is already a category with that name!");
                    return new MessageBuilder(alert.build(user)).build();
                }

                categoryList.add(categoryName);
                catListDoc.put("categoryList", categoryList);

                dbManager.updateTagCategories(catListDoc);

                sAlert.setDescription("You have successfully added a category with the name of " + categoryName);
                break;
            case "delete":
            case "remove":
                if (categoryName.equalsIgnoreCase("misc")) {
                    alert.setDescription("Uh Oh! You can't delete that category.");
                    return new MessageBuilder(alert.build(user)).build();
                } else if (!containsCategory(categoryList, categoryName)) {
                    alert.setDescription("There is no category with that name!");
                    return new MessageBuilder(alert.build(user)).build();
                }

                SourceTag.tagList.forEach(sourceTag -> {
                    if (sourceTag.getCategoryName().equalsIgnoreCase(args[1].toLowerCase())) {
                        sourceTag.setCategoryName(null, "misc");
                    }
                });

                Iterator<String> iterator = categoryList.iterator();

                while (iterator.hasNext()) {
                    String foundCat = iterator.next();
                    if (foundCat.equalsIgnoreCase(categoryName)) {
                        iterator.remove();
                        break;
                    }
                }

                catListDoc.put("categoryList", categoryList);

                dbManager.updateTagCategories(catListDoc);


                sAlert.setDescription("You have successfully removed the category with the name of " + categoryName);
                break;
            case "set":
                tagName = args[1];
                categoryName = args[2];

                SourceTag sourceTag;
                try {
                    sourceTag = new SourceTag(tagName);
                } catch (NullPointerException ex) {
                    alert.setDescription("There is no tag with that name (or alias)!");
                    return new MessageBuilder(alert.build(user)).build();
                }

                if (!containsCategory(categoryList, categoryName)) {
                    alert.setDescription("There is no category with that name!");
                    return new MessageBuilder(alert.build(user)).build();
                }

                if (sourceTag.getCategoryName() == null) sourceTag.setCategoryName(null, "uncategorized");
                if (sourceTag.getCategoryName().equalsIgnoreCase(categoryName)) {
                    alert.setDescription("That tag is already in that category!");
                    return new MessageBuilder(alert.build(user)).build();
                }

                sourceTag.setCategoryName(user.getId(), categoryName.toLowerCase());

                sAlert.setDescription("You have successfully added the specified tag to the category: " + categoryName);
                break;
            default:
                alert.setDescription("You did not specify a valid action!");
                return new MessageBuilder(alert.build(user)).build();

        }
        return new MessageBuilder(sAlert.build(user)).build();
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
