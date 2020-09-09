package me.thesourcecode.sourcebot.api.objects.incidents;

import com.mongodb.client.MongoCollection;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.User;
import org.bson.Document;

public class SourceBan extends SourceIncident {

    public SourceBan(String staffId, String targetId, String reason) {
        super(staffId, targetId, IncidentType.BAN, reason);
        insertDocument();

        MongoCollection<Document> punishmentHandler = dbManager.getCollection("PunishmentHandler");
        punishmentHandler.deleteMany(new Document("ID", getTargetId()));
    }

    @Override
    public boolean execute() {
        try {
            User targetUser = jda.getUserById(getTargetId());
            guild.ban(targetUser, 7, getReason()).complete();
        } catch (Throwable ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public void sendIncidentEmbed() {
        User staffUser = jda.getUserById(getStaffId());
        User targetUser = jda.getUserById(getTargetId());

        String staffInfo = staffUser == null ? "(" + getStaffId() + ")" : staffUser.getAsTag() + " (" + getStaffId() + ")";
        String targetInfo = targetUser == null ? "(" + getTargetId() + ")" : targetUser.getAsTag() + " (" + getTargetId() + ")";

        CriticalAlert incidentAlert = new CriticalAlert();
        incidentAlert.setDescription("**Banned By:** " + staffInfo + "\n" +
                "**Banned User:** " + targetInfo + "\n" +
                "**Reason:** " + getReason());

        MessageEmbed incidentEmbed = incidentAlert.build(targetUser, "Banned | Id: " + getCaseId());

        incidentLog.sendMessage(incidentEmbed).queue();
        if (targetUser != null) {
            try {
                PrivateChannel privateChannel = targetUser.openPrivateChannel().complete();
                privateChannel.sendMessage(incidentEmbed).complete();
                privateChannel.sendMessage("If you wish to appeal your ban, please go to https://board.thesourcecode.dev/").complete();
            } catch (Exception ignored) {
            }
        }
    }
}
