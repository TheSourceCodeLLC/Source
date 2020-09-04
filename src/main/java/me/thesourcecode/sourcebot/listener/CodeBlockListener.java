package me.thesourcecode.sourcebot.listener;

import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.utility.AbstractListener;
import me.thesourcecode.sourcebot.api.utility.Listener;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CodeBlockListener extends AbstractListener<GuildMessageReceivedEvent> {
    //Old pattern: ```(?:(?:\\w+)\n?+)?((?:.*\n+){14,}?)```
    public static final Pattern pattern = Pattern.compile("```(?:(\\w+\\n)|\\n)((?:.*\\n){20,})+\\n*?```");
    private final String OldpasteUrl = "https://paste.tscforum.com";
    private final String pasteUrl = "https://sourceb.in";
    private final String pasteEndpoint = pasteUrl + "/api/bin";
    private final HttpClient client = HttpClient.newHttpClient();

    public CodeBlockListener() {
        super(GuildMessageReceivedEvent.class);
    }

    @Override
    public void listen(Listener listener) {
        super.listen(listener);
        new CodeBlockEditListener().listen(listener);
    }

    @Override
    public void accept(GuildMessageReceivedEvent event) {
        checkForCodeblocks(event.getMessage());
    }

    private String createBin(String code) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(pasteEndpoint))
                .setHeader("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(code))
                .build();
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
        JSONObject responseJSON = new JSONObject(response.body());
        return responseJSON.getString("key");
    }

    private void checkForCodeblocks(Message message) {
        final User author = message.getAuthor();
        if (author.isBot()) return;
        String content = message.getContentRaw();
        Matcher matcher = pattern.matcher(content);
        final StringBuffer buffer = new StringBuffer();
        boolean matches = false;
        while (matcher.find()) {
            matches = true;
            final String language = matcher.group(1);
            final String code = matcher.group(2);
            if (code == null) return;
            if (code.trim().isBlank()) return;
            final String bin = pasteUrl + "/" + createBin(code);
            matcher.appendReplacement(buffer, bin + (language != null ? "." + language : ""));
        }
        if (matches) {
            matcher.appendTail(buffer);
            message.delete().queue();
            CriticalAlert alert = new CriticalAlert();
            alert.setTitle("Long Code Blocks Detected!")
                    .setDescription(
                            "I've pasted those code blocks for you!\n" +
                                    "Please read the rules regarding code blocks!"
                    );
            alert.addField("Message:", buffer.toString().trim(), false);
            message.getChannel().sendMessage(alert.build(message.getAuthor())).queue();
        }
    }

    private final class CodeBlockEditListener extends AbstractListener<GuildMessageUpdateEvent> {

        CodeBlockEditListener() {
            super(GuildMessageUpdateEvent.class);
        }

        @Override
        public void accept(GuildMessageUpdateEvent event) {
            checkForCodeblocks(event.getMessage());
        }
    }
}
