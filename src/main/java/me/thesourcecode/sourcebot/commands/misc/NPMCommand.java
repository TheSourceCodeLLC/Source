package me.thesourcecode.sourcebot.commands.misc;

import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceChannel;
import me.thesourcecode.sourcebot.api.message.alerts.ColoredAlert;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.utility.SourceColor;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Set;

public class NPMCommand extends Command {

    private static CommandInfo INFO = new CommandInfo(
            "npm",
            "Allows you to search npmjs.org",
            "<package> (version)",
            CommandInfo.Category.GENERAL)
            .withUsageChannels(SourceChannel.COMMANDS, SourceChannel.JAVASCRIPT, SourceChannel.DISCORD, SourceChannel.DISCORD2, SourceChannel.OTHER_HELP);

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public Message execute(Source source, Message message, String[] args) {
        User user = message.getAuthor();

        String version = "latest";
        String packageName = args[0].toLowerCase();

        if (args.length == 2) version = args[1].toLowerCase();

        JSONObject packageData = getJson(packageName, version);
        if (packageData == null) {
            packageName = packageName.replaceAll("`", "");
            version = version.replaceAll("`", "");

            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Uh Oh!")
                    .setDescription("I couldn't find an npm package with the name: `" + packageName + "` " +
                            (args.length == 2 ? "with the version: `" + version + "`\n" : "\n") +
                            "You can find npmjs at [npmjs.com](https://www.npmjs.com)");
            return new MessageBuilder(alert.build(user)).build();
        }

        String packageVersion = "";
        packageName = packageData.getString("name");


        String description;
        JSONArray joCollaborators;

        JSONObject time = packageData.getJSONObject("time");
        if (time.has("unpublished")) {
            description = "N/A - Package was unpublished";

            JSONObject unpublished = time.getJSONObject("unpublished");
            joCollaborators = unpublished.getJSONArray("maintainers");
            packageVersion = unpublished.getJSONObject("tags").getString("latest");
        } else {
            description = packageData.getString("description");
            joCollaborators = packageData.getJSONArray("maintainers");

            if (packageData.has("version")) {
                packageVersion = packageData.getString("version");
            } else if (packageData.has("dist-tags")) {
                packageVersion = packageData.getJSONObject("dist-tags").getString("latest");
            }
        }

        String packageLink = "https://www.npmjs.com/package/" + packageName;
        packageLink = version.equalsIgnoreCase("latest") ? packageLink : packageLink + "/v/" + packageVersion;


        StringBuilder sb = new StringBuilder();
        for (Object object : joCollaborators.toList()) {
            HashMap<String, String> collaborator = (HashMap<String, String>) object;
            sb.append("`").append(collaborator.get("name")).append("`, ");
        }
        String collaborators = sb.toString().trim();
        collaborators = collaborators.substring(0, collaborators.length() - 1);


        String packageHyperlink = time.has("unpublished") ? packageName : "[" + packageName + "](" + packageLink + ")";
        String format = "**Package Name/URL:** %s\n" +
                "**Description:** %s\n" +
                "**Version:** %s\n" +
                "**Collaborators:** %s";

        ColoredAlert alert = new ColoredAlert(SourceColor.BLUE);
        alert.setDescription(String.format(format, packageHyperlink, description, packageVersion, collaborators));
        message.getChannel().sendMessage(alert.build(user)).queue();
        return null;
    }

    private JSONObject getJson(String packageName, String version) {
        try {
            String urlString = "https://registry.npmjs.org/" + packageName;

            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();


            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            connection.disconnect();

            if (!version.equalsIgnoreCase("latest")) {
                JSONObject versions = new JSONObject(response.toString()).getJSONObject("versions");
                Set<String> versionLists = versions.keySet();

                if (!versionLists.contains(version)) {
                    return null;
                }

                return versions.getJSONObject(version);
            }
            return new JSONObject(response.toString());
        } catch (Exception ex) {
            return null;
        }
    }
}
