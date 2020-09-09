package me.thesourcecode.sourcebot.api.objects.incidents;

import com.mongodb.client.MongoCollection;
import me.thesourcecode.sourcebot.api.message.alerts.CriticalAlert;
import me.thesourcecode.sourcebot.api.utility.Utility;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.User;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;

public class SourceTempban extends SourceIncident {

    private String duration;

    public SourceTempban(String staffId, String targetId, String duration, String reason) {
        super(staffId, targetId, IncidentType.TEMPBAN, reason);

        this.duration = duration;

        Document incidentDocument = getIncidentDocument();
        incidentDocument.append("DURATION", duration);

        setIncidentDocument(incidentDocument);
        insertDocument();
        createPunishmentHandlerDocument();
    }

    @NotNull
    public String getDuration() {
        return duration;
    }

    private void createPunishmentHandlerDocument() {
        MongoCollection<Document> punishmentHandler = dbManager.getCollection("PunishmentHandler");
        String[] durationArgs = getDuration().split("\\s+");

        long durationTime = Long.parseLong(durationArgs[0]);
        String durationType = durationArgs[1];

        long endTime = Utility.getUnpunishTime(durationType, durationTime);

        Document mongoPunish = new Document("ID", getTargetId())
                .append("TYPE", getIncidentType().toString())
                .append("END", endTime);

        punishmentHandler.deleteMany(new Document("ID", getTargetId()));
        punishmentHandler.insertOne(mongoPunish);
    }

    @Override
    public boolean execute() {
        try {
            User targetUser = jda.getUserById(getTargetId());
            guild.ban(targetUser, 7, getReason()).queue();
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
        incidentAlert.setDescription("**Tempbanned By:** " + staffInfo + "\n" +
                "**Tempbanned User:** " + targetInfo + "\n" +
                "**Duration:** " + getDuration() + "\n" +
                "**Reason:** " + getReason());

        MessageEmbed incidentEmbed = incidentAlert.build(targetUser, "Tempbanned | Id: " + getCaseId());

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
