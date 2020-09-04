package me.thesourcecode.sourcebot.api.objects.incidents;

import me.thesourcecode.sourcebot.api.message.alerts.WarningAlert;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;

public class SourceWarn extends SourceIncident {

    public SourceWarn(String staffId, String targetId, String reason) {
        super(staffId, targetId, IncidentType.WARN, reason);
        insertDocument();
    }

    @Override
    public void execute() {
    }

    @Override
    public void sendIncidentEmbed() {
        User staffUser = jda.getUserById(getStaffId());
        User targetUser = jda.getUserById(getTargetId());

        String staffInfo = staffUser == null ? "(" + getStaffId() + ")" : staffUser.getAsTag() + " (" + getStaffId() + ")";
        String targetInfo = targetUser == null ? "(" + getTargetId() + ")" : targetUser.getAsTag() + " (" + getTargetId() + ")";

        WarningAlert incidentAlert = new WarningAlert();
        incidentAlert.setDescription("**Warned By:** " + staffInfo + "\n" +
                "**Warned User:** " + targetInfo + "\n" +
                "**Reason:** " + getReason());

        MessageEmbed incidentEmbed = incidentAlert.build(targetUser, "Warned | Id: " + getCaseId());

        incidentLog.sendMessage(incidentEmbed).queue();
        if (targetUser != null) {
            try {
                targetUser.openPrivateChannel().complete().sendMessage(incidentEmbed).complete();
            } catch (Exception ignored) {
            }
        }
    }
}
