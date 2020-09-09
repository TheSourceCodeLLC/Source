package me.thesourcecode.sourcebot.api.objects.incidents;

import me.thesourcecode.sourcebot.api.message.alerts.WarningAlert;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;

public class SourceSoftban extends SourceIncident {

    public SourceSoftban(String staffId, String targetId, String reason) {
        super(staffId, targetId, IncidentType.KICK, reason);
        insertDocument();
    }

    @Override
    public boolean execute() {
        try {
            User targetUser = jda.getUserById(getTargetId());
            guild.ban(targetUser, 7, getReason()).complete();
            guild.unban(targetUser).queue();
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
        incidentAlert.setDescription("**Softbanned By:** " + staffInfo + "\n" +
                "**Softbanned User:** " + targetInfo + "\n" +
                "**Reason:** " + getReason());

        MessageEmbed incidentEmbed = incidentAlert.build(targetUser, "Softban | Id: " + getCaseId());

        incidentLog.sendMessage(incidentEmbed).queue();
        if (targetUser != null) {
            try {
                targetUser.openPrivateChannel().complete().sendMessage(incidentEmbed).complete();
            } catch (Exception ignored) {
            }
        }
    }
}
