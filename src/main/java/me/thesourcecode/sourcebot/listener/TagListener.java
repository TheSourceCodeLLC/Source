package me.thesourcecode.sourcebot.listener;

import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.entity.SourceChannel;
import me.thesourcecode.sourcebot.api.manager.DatabaseManager;
import me.thesourcecode.sourcebot.api.message.alerts.ColoredAlert;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.objects.SourceTag;
import me.thesourcecode.sourcebot.api.utility.AbstractListener;
import me.thesourcecode.sourcebot.api.utility.SourceColor;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class TagListener extends AbstractListener<GuildMessageReceivedEvent> {

    public static final String TAG_PREFIX = "?";
    private final DatabaseManager dbManager;

    public TagListener(Source source) {
        super(GuildMessageReceivedEvent.class);
        this.dbManager = source.getDatabaseManager();
    }

    @Override
    public void accept(GuildMessageReceivedEvent event) {
        String message = event.getMessage().getContentRaw();
        User author = event.getAuthor();
        if (message.startsWith("?")) {

            if (author.isBot() || author.isFake()) return;
            TextChannel channel = event.getChannel();
            if (channel == SourceChannel.AGREE.resolve(event.getJDA())
                    || channel == SourceChannel.SUGGESTIONS.resolve(event.getJDA())) return;


            String[] args = message.split(" ");

            String tagName = args[0].substring(1);
            SourceTag sourceTag;

            try {
                sourceTag = new SourceTag(tagName);
            } catch (NullPointerException ex) {
                return;
            }

            String description = sourceTag.getDescription();
            String type = sourceTag.getType();

            sourceTag.incrementUses();

            switch (type.toLowerCase()) {
                case "embed":
                    ColoredAlert alert = new ColoredAlert(SourceColor.BLUE);
                    alert.setDescription(description);

                    event.getChannel().sendMessage(alert.build(null)).queue();
                    break;
                case "text":
                    if (description.contains("@everyone")) {

                        description = description.replace("@everyone", "@\u200beveryone");
                        sourceTag.setDescription(null, description);
                    }

                    channel.sendMessage(description).queue();
                    break;
                default:
                    CriticalAlert error = new CriticalAlert();
                    error.setTitle("Uh Oh!").setDescription("Something went wrong! Please message Forbidden.");
                    channel.sendMessage(error.build(author)).queue();

            }

        }
    }
}
