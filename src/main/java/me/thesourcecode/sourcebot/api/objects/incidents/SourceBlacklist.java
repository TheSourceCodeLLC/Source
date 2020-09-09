package me.thesourcecode.sourcebot.api.objects.incidents;

import me.thesourcecode.sourcebot.api.entity.SourceRole;
import me.thesourcecode.sourcebot.api.message.alerts.WarningAlert;
import me.thesourcecode.sourcebot.api.utility.Utility;
import net.dv8tion.jda.api.entities.*;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;

public class SourceBlacklist extends SourceIncident {

    private static final Role blacklistRole = SourceRole.BLACKLIST.resolve(jda);

    private String duration;

    public SourceBlacklist(String staffId, String targetId, String duration, String reason) {
        super(staffId, targetId, IncidentType.BLACKLIST, reason);

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
        String[] durationArgs = getDuration().split("\\s+");

        long durationTime = Long.parseLong(durationArgs[0]);
        String durationType = durationArgs[1];

        long endTime = Utility.getUnpunishTime(durationType, durationTime);

        Document mongoPunish = new Document("ID", getTargetId())
                .append("TYPE", getIncidentType().toString())
                .append("CATEGORY", "DEVELOPMENT")
                .append("END", endTime);

        dbManager.getCollection("PunishmentHandler").insertOne(mongoPunish);
    }

    @Override
    public boolean execute() {
        try {
            Member targetMember = guild.getMemberById(getTargetId());

            guild.addRoleToMember(targetMember, blacklistRole).complete();
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

        WarningAlert incidentAlert = new WarningAlert();
        incidentAlert.setDescription("**Blacklisted By:** " + staffInfo + "\n" +
                "**Blacklisted User:** " + targetInfo + "\n" +
                "**Duration:** " + getDuration() + "\n" +
                "**Reason:** " + getReason());

        MessageEmbed incidentEmbed = incidentAlert.build(targetUser, "Development Help Channel Blacklist | Id: " + getCaseId());

        incidentLog.sendMessage(incidentEmbed).queue();
        if (targetUser != null) {
            PrivateChannel privateChannel = targetUser.openPrivateChannel().complete();

            try {
                privateChannel.sendMessage("You have been blacklisted from the development help channels! See below for more info.").complete();
                privateChannel.sendMessage(incidentEmbed).complete();
            } catch (Exception ignored) {
            }
        }
    }
}
