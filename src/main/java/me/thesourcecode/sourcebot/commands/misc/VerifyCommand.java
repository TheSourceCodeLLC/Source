package me.thesourcecode.sourcebot.commands.misc;

import com.mongodb.client.MongoCollection;
import me.thesourcecode.sourcebot.api.Source;
import me.thesourcecode.sourcebot.api.command.Command;
import me.thesourcecode.sourcebot.api.command.CommandInfo;
import me.thesourcecode.sourcebot.api.entity.SourceChannel;
import me.thesourcecode.sourcebot.api.entity.SourceGuild;
import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.manager.DatabaseManager;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.message.alerts.SuccessAlert;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import org.bson.Document;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;

public class VerifyCommand extends Command {

    private static final CommandInfo INFO = new CommandInfo(
            "verify",
            "Sends a captcha to the user.",
            "(code)",
            CommandInfo.Category.UNLISTED
    )
            .withUsageChannels(SourceChannel.AGREE)
            .withControlRoles(SourceRole.UNVERIFIED)
            .withAliases("agree");

    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public Message execute(Source source, Message message, String[] args) {
        User user = message.getAuthor();
        Guild guild = source.isBeta() ? SourceGuild.BETA.resolve(source.getJda()) : SourceGuild.MAIN.resolve(source.getJda());
        Member member = guild.getMember(user);
        if (args.length == 1) {
            DatabaseManager dbManager = source.getDatabaseManager();
            Document search = new Document("ID", user.getId());

            MongoCollection verify = dbManager.getCollection("Verify");
            Document found = (Document) verify.find(search).first();
            if (found == null) {
                CriticalAlert cAlert = new CriticalAlert();
                cAlert.setDescription("Please run `!verify` to generate a captcha!");
                return new MessageBuilder(cAlert.build(user)).build();
            }

            String eCode = args[0];
            String cCode = found.getString("CODE");

            if (eCode.equals(cCode)) {
                guild.removeRoleFromMember(guild.getMember(user), SourceRole.UNVERIFIED.resolve(source.getJda())).queue();
                verify.deleteOne(found);

                SuccessAlert sAlert = new SuccessAlert();
                sAlert.setDescription("You have successfully completed the captcha, you will now be able to view the rest of the channels on TSC!");
                return new MessageBuilder(sAlert.build(user)).build();
            } else {
                CriticalAlert cAlert = new CriticalAlert();
                cAlert.setDescription("You did not enter the correct code, make sure you used the correct capitalization!");
                return new MessageBuilder(cAlert.build(user)).build();
            }

        } else {
            if (!member.getRoles().contains(SourceRole.UNVERIFIED.resolve(source.getJda()))) {
                CriticalAlert cAlert = new CriticalAlert();
                cAlert.setDescription("You are already verified!");
                return new MessageBuilder(cAlert.build(user)).build();
            }
            try {
                PrivateChannel pChannel = user.openPrivateChannel().complete();
                createCaptcha(user, source.getDatabaseManager());

                pChannel.sendMessage("Please enter the case-sensitive code below, via `!verify <code>`, within 10 " +
                        "minutes, or else you will be automatically kicked.").complete();
                pChannel.sendFile(new File("Image.png")).queue();

                if (message.getChannel() != pChannel) {
                    SuccessAlert sAlert = new SuccessAlert();
                    sAlert.setDescription("I have successfully sent a captcha to your DMs!");
                    return new MessageBuilder(sAlert.build(user)).build();
                }


            } catch (Exception ex) {
                CriticalAlert cAlert = new CriticalAlert();
                cAlert.setDescription("You must open your DMs to use this command!");
                return new MessageBuilder(cAlert.build(user)).build();
            }

        }
        return null;

    }

    private void createCaptcha(User user, DatabaseManager dbManager) {
        try {
            int imgWidth = 150;
            int imgHeight = 50;
            BufferedImage image = new BufferedImage(imgWidth, imgHeight,
                    BufferedImage.TYPE_INT_RGB);

            Graphics2D g = image.createGraphics();

            g.setPaint(Color.decode("#3498db"));
            g.fillRect(0, 0, imgWidth, imgHeight);

            Font font = new Font("1942 Report", Font.PLAIN, 35);
            g.setFont(font);
            g.setPaint(Color.BLACK);

            String text = getSaltString();

            MongoCollection verify = dbManager.getCollection("Verify");

            Document search = new Document("ID", user.getId());
            Document found = (Document) verify.find(search).first();
            if (found != null) {
                verify.deleteOne(found);
            }

            long expireTime = System.currentTimeMillis() + (60000 * 10); // 10 Minutes
            Document document = new Document("ID", user.getId())
                    .append("CODE", text)
                    .append("EXPIRE", expireTime);
            verify.insertOne(document);

            TextLayout textLayout = new TextLayout(text, g.getFont(),
                    g.getFontRenderContext());
            double textHeight = textLayout.getBounds().getHeight();
            double textWidth = textLayout.getBounds().getWidth();

            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g.drawString(text, imgWidth / 2 - (int) textWidth / 2,
                    imgHeight / 2 + (int) textHeight / 2);


            for (int i = 1; i < 30; i++) {
                g.setColor(Color.WHITE);
                Random random = new Random();
                int randomInt = random.nextInt(300 - 70 + 1) + 1;
                int randomInt2 = random.nextInt(100 - 1 + 1) + 1;
                int randomInt3 = random.nextInt(20 - 1 + 1) + 1;

                g.drawLine(i * randomInt2, randomInt / i, imgHeight + i / randomInt2, imgWidth * randomInt / i);
                g.drawLine(i * randomInt2, randomInt / i, imgWidth + i / randomInt3, imgHeight * randomInt / i);
            }
            ImageIO.write(image, "png", new File("Image.png"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private String getSaltString() {
        String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ123456789";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < 6) {
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        return salt.toString();

    }

}
