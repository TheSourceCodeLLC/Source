package me.thesourcecode.sourcebot.api.objects.incidents;

import me.thesourcecode.sourcebot.api.message.alerts.WarningAlert;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;

public class SourceKick extends SourceIncident {

    public SourceKick(String staffId, String targetId, String reason) {
        super(staffId, targetId, IncidentType.KICK, reason);
        insertDocument();
    }

    @Override
    public void execute() {
        Member targetMember = guild.getMemberById(getTargetId());
        guild.kick(targetMember, getReason()).queue();
    }

    @Override
    public void sendIncidentEmbed() {
        User staffUser = jda.getUserById(getStaffId());
        User targetUser = jda.getUserById(getTargetId());

        String staffInfo = staffUser == null ? "(" + getStaffId() + ")" : staffUser.getAsTag() + " (" + getStaffId() + ")";
        String targetInfo = targetUser == null ? "(" + getTargetId() + ")" : targetUser.getAsTag() + " (" + getTargetId() + ")";

        WarningAlert incidentAlert = new WarningAlert();
        incidentAlert.setDescription("**Kicked By:** " + staffInfo + "\n" +
                "**Kicked User:** " + targetInfo + "\n" +
                "**Reason:** " + getReason());

        MessageEmbed incidentEmbed = incidentAlert.build(targetUser, "Kick | Id: " + getCaseId());

        incidentLog.sendMessage(incidentEmbed).queue();
        if (targetUser != null) {
            try {
                targetUser.openPrivateChannel().complete().sendMessage(incidentEmbed).complete();
            } catch (Exception ignored) {
            }
        }
    }
}
