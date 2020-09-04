package me.thesourcecode.sourcebot.commands.misc.doc;

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
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.util.Arrays;
import java.util.List;

import static me.thesourcecode.sourcebot.api.entity.SourceChannel.devHelpAndCommands;

public class DJSCommand extends Command {

    private static final String[] DEFAULT_SOURCES = {
            "stable", "master", "rpc", "commando", "akairo", "akairo-master", "collection"
    };
    private final CommandInfo INFO = new CommandInfo(
            "djs",
            "Pulls information from the Discord.JS documentation.",
            "(version) (query)",
            CommandInfo.Category.DEVELOPMENT)
            .withUsageChannels(devHelpAndCommands)
            .withAliases("discordjs");

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
            alert.setDescription("You can find the Discord.JS Documentation at [discord.js.org](https://discord.js.org/)");
            alert.appendDescription("\nTo query information from the documentation use: `" + CommandHandler.getPrefix() + INFO.getLabel() + " " + INFO.getArguments() + "`");
            MessageEmbed embed = alert.build(message.getAuthor());
            textChannel.sendMessage(embed).queue();
            return null;
        }

        // Gets the version
        String version = "stable";
        String query = args[0];
        if (args.length > 1) {
            List<String> sourceList = Arrays.asList(DEFAULT_SOURCES);

            if (!sourceList.contains(args[0].toLowerCase())) {
                version = "https://raw.githubusercontent.com/discordjs/discord.js/docs/" + args[0].toLowerCase() + ".json";

                try {
                    Connection.Response document = Jsoup.connect(version)
                            .ignoreContentType(true)
                            .execute();

                    query = args[1];
                } catch (Exception ex) {
                    version = "stable";
                }
            } else {
                version = args[0].toLowerCase();
                query = args[1];
            }


        }

        query = query.replaceAll("#", ".");

        // Gets the query information
        String url = "https://djsdocs.sorta.moe/v2/embed?src=" + version + "&q=" + query;

        try {
            Connection.Response document = Jsoup.connect(url)
                    .ignoreContentType(true)
                    .method(Connection.Method.GET)
                    .execute();

            JSONObject embedJson = new JSONObject(document.body());

            EmbedBuilder builder = new EmbedBuilder();

            int color = embedJson.getInt("color");
            builder.setColor(color);

            JSONObject author = embedJson.getJSONObject("author");
            String authorName = author.getString("name");
            String authorUrl = author.getString("url");
            String authorIcon = author.getString("icon_url");
            builder.setAuthor(authorName, authorUrl, authorIcon);

            String description = embedJson.getString("description");
            builder.setDescription(description);

            JSONArray fields = (JSONArray) getJSONVariable(embedJson, "fields");
            if (fields != null) {
                fields.forEach(field -> {
                    JSONObject fieldObject = (JSONObject) field;

                    String fieldName = fieldObject.getString("name");
                    String fieldValue = fieldObject.getString("value");

                    builder.addField(fieldName, fieldValue, false);
                });
            }

            if (getJSONVariable(embedJson, "title") != null) {
                String embedTitle = (String) getJSONVariable(embedJson, "title");
                builder.setTitle(embedTitle);
            }

            builder.setFooter("Ran By: " + user.getAsTag(), user.getEffectiveAvatarUrl());

            Message documentationMessage = textChannel.sendMessage(builder.build()).complete();
            documentationMessage.addReaction(EmojiParser.parseToUnicode(":x:")).queue();
        } catch (Exception ex) {
            ex.printStackTrace();
            version = args.length > 1 ? args[0].toLowerCase() : version;
            CriticalAlert cAlert = new CriticalAlert();
            cAlert.setDescription("I couldn't find `" + String.join("#", query) + "` in this Discord.js documentation for version: " + version + "!");
            cAlert.appendDescription("\nYou can find the Discord.JS Documentation at [discord.js.org](https://discord.js.org/)");
            return new MessageBuilder(cAlert.build(user)).build();
        }
        return null;
    }

    private Object getJSONVariable(JSONObject embedJson, String nameOfObject) {
        try {
            return embedJson.get(nameOfObject);
        } catch (JSONException ex) {
            return null;
        }
    }

}