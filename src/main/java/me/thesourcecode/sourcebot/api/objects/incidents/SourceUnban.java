package me.thesourcecode.sourcebot.api.objects.incidents;

import com.mongodb.client.MongoCollection;
import me.thesourcecode.sourcebot.api.message.alerts.SuccessAlert;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import org.bson.Document;

public class SourceUnban extends SourceIncident {

    private User targetUser;

    public SourceUnban(String staffId, String targetId, String reason) {
        super(staffId, targetId, IncidentType.UNBAN, reason);
        insertDocument();

        MongoCollection<Document> punishmentHandler = dbManager.getCollection("PunishmentHandler");
        punishmentHandler.deleteMany(new Document("ID", getTargetId()));

        try {
            targetUser = guild.retrieveBanById(getTargetId()).complete().getUser();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override

    public boolean execute() {
        try {
            guild.unban(targetUser).complete();
        } catch (Throwable ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public void sendIncidentEmbed() {
        User staffUser = jda.getUserById(getStaffId());

        String staffInfo = staffUser == null ? "(" + getStaffId() + ")" : staffUser.getAsTag() + " (" + getStaffId() + ")";
        String targetInfo = targetUser == null ? "(" + getTargetId() + ")" : targetUser.getAsTag() + " (" + getTargetId() + ")";

        SuccessAlert incidentAlert = new SuccessAlert();
        incidentAlert.setDescription("**Unbanned By:** " + staffInfo + "\n" +
                "**Unbanned User:** " + targetInfo + "\n" +
                "**Reason:** " + getReason());

        MessageEmbed incidentEmbed = incidentAlert.build(targetUser, "Unban | Id: " + getCaseId());

        incidentLog.sendMessage(incidentEmbed).queue();
    }
}