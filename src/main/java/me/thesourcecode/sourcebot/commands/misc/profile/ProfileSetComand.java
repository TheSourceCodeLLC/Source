package me.thesourcecode.sourcebot.commands.misc.profile;

import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandHandler;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceChannel;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.message.alerts.SuccessAlert;
import me.thesourcecode.sourcebot.api.objects.SourceProfile;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

public class ProfileSetComand extends Command {
    private final CommandInfo INFO = new CommandInfo(
            "set",
            "Allows a user to set their bio and github link.",
            "<bio|github|coin> <bio|username|true/false>",
            CommandInfo.Category.GENERAL
    ).withUsageChannels(SourceChannel.COMMANDS);

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }


    @Override
    public Message execute(Source source, Message message, String[] args) {
        // TODO: Add admin override to change any user's bio/github link
        User user = message.getAuthor();

        String option = args[0].toLowerCase();

        SourceProfile userProfile = new SourceProfile(user);

        switch (option) {
            case "bio":
                if (args.length == 1) {
                    userProfile.setBio("");

                    SuccessAlert bioRemoved = new SuccessAlert();
                    bioRemoved.setTitle("Bio Removed!").setDescription("You successfully removed your bio!");
                    return new MessageBuilder(bioRemoved.build(user)).build();
                }
                String updatedBio = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                if (updatedBio.length() > 140) {
                    CriticalAlert bioTooLong = new CriticalAlert();
                    bioTooLong.setTitle("Bio Too Long!").setDescription("Please specify a bio under 140 characters!");
                    return new MessageBuilder(bioTooLong.build(user)).build();
                }

                userProfile.setBio(updatedBio);
                SuccessAlert bioUpdated = new SuccessAlert();
                bioUpdated.setTitle("Bio Updated!").setDescription("Your new bio is, '" + updatedBio + "'!");

                return new MessageBuilder(bioUpdated.build(user)).build();
            case "github":
                if (args.length == 1) {
                    userProfile.setGithub("");

                    SuccessAlert githubRemoved = new SuccessAlert();
                    githubRemoved.setTitle("GitHub Removed!").setDescription("You successfully removed your GitHub!");
                    return new MessageBuilder(githubRemoved.build(user)).build();
                }
                String updatedGithubUsername = args[1];

                if (!isRealAccount(updatedGithubUsername)) {
                    CriticalAlert alert = new CriticalAlert();
                    alert.setTitle("Uh Oh!").setDescription("You did not enter a valid github account!");
                    return new MessageBuilder(alert.build(user)).build();
                }

                String githubUrl = "https://github.com/" + updatedGithubUsername;
                userProfile.setGithub(githubUrl);

                SuccessAlert githubAdded = new SuccessAlert();
                githubAdded.setTitle("GitHub Added!").setDescription("You set your GitHub link to " + githubUrl);

                return new MessageBuilder(githubAdded.build(user)).build();
            case "coin":
                boolean newValue = Boolean.parseBoolean(args[1]);
                userProfile.setCoinMessageToggle(newValue);

                SuccessAlert toggleCoin = new SuccessAlert();
                String action = newValue ? "Enabled" : "Disabled";
                toggleCoin.setTitle("Coin Notifications " + action + "!")
                        .setDescription("You have successfully " + action.toLowerCase() + " your coin notifications");

                return new MessageBuilder(toggleCoin.build(user)).build();
            default:
                CriticalAlert invalidOption = new CriticalAlert();
                invalidOption.setTitle("Invalid Option!").setDescription("Syntax: " + CommandHandler.getPrefix() + "profile set " + INFO.getArguments());
                return new MessageBuilder(invalidOption.build(user)).build();
        }
    }

    /**
     * Checks if a github account exists
     *
     * @param username The username we are checking for
     * @return True or false depending on whether the account is real or not
     */
    private boolean isRealAccount(String username) {
        try {
            String urlString = "https://api.github.com/users/" + username;
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

            JSONObject o = new JSONObject(response.toString());
            String foundUsername = o.getString("login");
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

}
