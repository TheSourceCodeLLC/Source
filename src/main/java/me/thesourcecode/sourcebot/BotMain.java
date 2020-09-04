package me.thesourcecode.sourcebot;

import me.thesourcecode.sourcebot.api.Source;
import org.json.JSONObject;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class BotMain {
    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    public static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a zzz");
    public static final ZoneId TIME_ZONE = ZoneId.of("America/New_York");

    public static void main(String[] args) throws IOException, LoginException, InterruptedException {
        JSONObject config = getConfig();
        Source source = new Source(config);
        source.getJda().awaitReady();
        source.enable();

        source.registerCommands();
        source.registerEvents();
    }

    private static JSONObject getConfig() throws IOException {
        File configFile = new File("config.json");
        if (!configFile.exists()) {
            InputStream in = BotMain.class.getResourceAsStream("/config.json");
            Files.copy(in, Path.of("config.json"));
            in.close();
        }
        FileInputStream configStream = new FileInputStream(configFile);
        JSONObject config = new JSONObject(new String(configStream.readAllBytes(), StandardCharsets.UTF_8));
        configStream.close();
        return config;
    }


}
