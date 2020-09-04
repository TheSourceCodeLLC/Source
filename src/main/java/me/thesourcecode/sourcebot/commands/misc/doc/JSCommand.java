package me.thesourcecode.sourcebot.commands.misc.doc;

import com.overzealous.remark.Remark;
import com.vdurmont.emoji.EmojiParser;
import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandHandler;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.message.alerts.ColoredAlert;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.utility.SourceColor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.List;

import static me.thesourcecode.sourcebot.api.entity.SourceChannel.devHelpAndCommands;

public class JSCommand extends Command {

    private final Remark remark = new Remark();

    private CommandInfo INFO = new CommandInfo(
            "mdn",
            "Pulls information from the JavaScript Documentation.",
            "(query)",
            CommandInfo.Category.DEVELOPMENT)
            .withUsageChannels(devHelpAndCommands)
            .withAliases("javascript").withAliases("js");

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public Message execute(Source source, Message message, String[] args) {
        User user = message.getAuthor();
        MessageChannel textChannel = message.getChannel();
        if (args.length < 1) {
            ColoredAlert alert = new ColoredAlert(SourceColor.BLUE);
            alert.setDescription("You can find the MDN Documentation at [developer.mozilla.org](https://developer.mozilla.org/en-US/docs/)");
            alert.appendDescription("\nTo query information from the documentation use: `" + CommandHandler.getPrefix() + INFO.getLabel() + " " + INFO.getArguments() + "`");
            MessageEmbed embed = alert.build(message.getAuthor());
            textChannel.sendMessage(embed).queue();
            return null;
        }

        // Gets query
        String query = args[0].replaceAll("#", ".");
        String[] queryArgs = query.split("\\.");

        String url = "https://mdn.pleb.xyz/search?q=" + query;

        try {
            Connection.Response document = Jsoup.connect(url)
                    .ignoreContentType(true)
                    .maxBodySize(0)
                    .method(Connection.Method.GET)
                    .execute();


            JSONObject foundJson = new JSONObject(document.body());
            EmbedBuilder builder = new EmbedBuilder();

            String objectLabel = foundJson.getString("Label");

            JSONArray methods = foundJson.getJSONArray("Subpages");


            if (queryArgs.length == 1) {
                StringBuilder methodList = new StringBuilder();
                StringBuilder staticMethodList = new StringBuilder();
                StringBuilder propertyList = new StringBuilder();

                methods.forEach(page -> {
                    JSONObject pageObject = (JSONObject) page;

                    String label = ((JSONObject) page).getString("Label");
                    List<Object> tags = pageObject.getJSONArray("Tags").toList();

                    if (label.contains("(")) {
                        label = label.substring(0, label.indexOf("("));
                    }

                    String appendString = "`" + label + "` ";
                    if (tags.contains("Method")) {

                        if (tags.contains("Prototype")) {
                            if (label.contains(".prototype")) {
                                if (label.contains("[")) {
                                    label = label.substring(objectLabel.length() + 10); // + 10 for .prototype
                                    label = objectLabel + label;
                                } else {
                                    label = label.substring(objectLabel.length() + 11); // + 11 for .prototype.
                                }
                                appendString = "`" + label + "` ";

                            }
                            methodList.append(appendString);
                        } else {
                            staticMethodList.append(appendString);
                        }


                    } else if (tags.contains("Property")) {
                        propertyList.append(appendString);
                    }
                });

                String sProperties = propertyList.toString().trim();
                String sMethods = methodList.toString().trim();
                String sStaticMethod = staticMethodList.toString().trim();

                if (!sProperties.isBlank()) {
                    builder.addField("Properties:", propertyList.toString().trim(), false);
                }
                if (!sMethods.isBlank()) {
                    builder.addField("Methods:", methodList.toString().trim(), false);
                }
                if (!sStaticMethod.isBlank()) {
                    builder.addField("Static Methods:", staticMethodList.toString().trim(), false);
                }
            }

            String description = foundJson.getString("Summary");
            description = remark.convertFragment(description);

            String mdnBaseURL = "https://developer.mozilla.org/en-US/docs/";
            String slug = foundJson.getString("Slug");

            String newLabel = objectLabel.replace(".", "#").replace("#prototype#", "#");
            newLabel = newLabel.replace("[", "\\[").replace("]", "\\]");


            String descriptionTitle = "__**[" + newLabel + "](" + mdnBaseURL + slug + ")**__";

            builder.setDescription(descriptionTitle + "\n" + description);

            String iconUrl = "https://developer.mozilla.org/static/img/opengraph-logo.72382e605ce3.png";
            String authorUrl = "https://developer.mozilla.org/en-US/docs/";
            String authorName = "MDN Documentation";

            builder.setAuthor(authorName, authorUrl, iconUrl);
            builder.setFooter("Ran By: " + user.getAsTag(), user.getEffectiveAvatarUrl());
            builder.setColor(SourceColor.BLUE.asColor());

            Message documentationMessage = textChannel.sendMessage(builder.build()).complete();
            documentationMessage.addReaction(EmojiParser.parseToUnicode(":x:")).queue();
        } catch (IOException ex) {
            CriticalAlert cAlert = new CriticalAlert();
            cAlert.setDescription("I couldn't find " + String.join("#", query) + " in this MDN documentation");
            cAlert.appendDescription("\nYou can find the MDN Documentation at [developer.mozilla.org](https://developer.mozilla.org/en-US/docs/)");
            return new MessageBuilder(cAlert.build(user)).build();
        }

        return null;
    }

}