package me.thesourcecode.sourcebot.commands.misc;

import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceChannel;
import me.thesourcecode.sourcebot.api.entity.SourceGuild;
import me.thesourcecode.sourcebot.api.message.alerts.InfoAlert;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

public class OnlineCommand extends Command {

    private static CommandInfo INFO = new CommandInfo(
            "online",
            "Tells you the amount of people online.")
            .withUsageChannels(SourceChannel.COMMANDS);

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public Message execute(Source source, Message message, String[] args) {
        Guild guild = source.isBeta() ? SourceGuild.BETA.resolve(source.getJda()) : SourceGuild.MAIN.resolve(source.getJda());
        long online;
        AtomicLong total = new AtomicLong();


        online = guild.getMemberCache().stream().filter(member -> {
            User user = member.getUser();
            if (user.isBot() || user.isFake()) return false;

            total.incrementAndGet();
            return member.getOnlineStatus() != OnlineStatus.OFFLINE;
        }).count();

        String desc = "**Discord Stats:** \nThere are currently **%s** members online out of **%s** total members." +
                "\n\n**Youtube Stats:**";

        JSONObject tscSubJObject = getJsonData(source, "thesourcecodetutorials");
        JSONObject tjsSubJObject = getJsonData(source, "UCLHUNmmYKVO2SRXBcufvBzQ");

        if (tscSubJObject != null) {
            String tscSubs = String.valueOf(tscSubJObject.getInt("subs"));
            desc += "\nTSC Subscribers: " + tscSubs;
        }

        if (tjsSubJObject != null) {
            String tjsSubs = String.valueOf(tjsSubJObject.getInt("subs"));
            desc += "\nTJS Subscribers: " + tjsSubs;
        }


        InfoAlert alert = new InfoAlert();
        alert.setTitle("Online").setDescription(String.format(desc, online, total));
        message.getChannel().sendMessage(alert.build(message.getAuthor())).queue();
        return null;
    }

    /**
     * Gets the `data` JSONObject from the specified youtube channel
     *
     * @param source    The source object, used to access config file
     * @param ytChannel The channel we are getting the data from
     * @return The `data` JSONObject
     */
    private JSONObject getJsonData(Source source, String ytChannel) {
        JSONObject config = source.getConfig().getJSONObject("socialblade");
        try {
            String urlString = "http://api.socialblade.com/v2/youtube/statistics?query=statistics&username=" + ytChannel + "&email="
                    + config.getString("email") + "&token=" + config.getString("token");

            Document document = Jsoup.connect(urlString).ignoreContentType(true).get();
            JSONObject body = new JSONObject(document.body().text());
            return body.getJSONObject("data");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

}
