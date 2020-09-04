package me.thesourcecode.sourcebot.api.objects.incidents;

import me.thesourcecode.sourcebot.api.message.alerts.ColoredAlert;
import me.thesourcecode.sourcebot.api.utility.SourceColor;
import net.dv8tion.jda.api.entities.*;
import org.bson.Document;

import java.util.List;

public class SourceClear extends SourceIncident {

    private int clearedMessageAmount;

    public SourceClear(String staffId, String channelId, int clearedMessageAmount, String reason) {
        super(staffId, channelId, IncidentType.CLEAR, reason);

        this.clearedMessageAmount = clearedMessageAmount;

        Document incidentDocument = getIncidentDocument();
        incidentDocument.append("MESSAGE_AMOUNT", String.valueOf(clearedMessageAmount));

        setIncidentDocument(incidentDocument);
        insertDocument();
    }

    public int getClearedMessageAmount() {
        return clearedMessageAmount;
    }

    @Override
    public void execute() {
        TextChannel targetChannel = guild.getTextChannelById(getTargetId());

        MessageHistory history = new MessageHistory(targetChannel);
        List<Message> msgs;

        int clearMsgAmnt = getClearedMessageAmount();
        msgs = history.retrievePast(clearMsgAmnt == 100 ? clearMsgAmnt : clearMsgAmnt + 1).complete();
        targetChannel.purgeMessages(msgs);
    }

    @Override
    public void sendIncidentEmbed() {
        User staffUser = jda.getUserById(getStaffId());
        TextChannel targetChannel = jda.getTextChannelById(getTargetId());

        String staffInfo = staffUser == null ? "(" + getStaffId() + ")" : staffUser.getAsTag() + " (" + getStaffId() + ")";
        String targetInfo = targetChannel == null ? "(" + getTargetId() + ")" : targetChannel.getName() + " (" + getTargetId() + ")";

        ColoredAlert incidentAlert = new ColoredAlert(SourceColor.BLUE);
        incidentAlert.setDescription("**Cleared By:** " + staffInfo + "\n" +
                "**Cleared Channel:** " + targetInfo + "\n" +
                "**Amount of Messages Cleared:** " + getClearedMessageAmount() + "\n" +
                "**Reason:** " + getReason());

        MessageEmbed incidentEmbed = incidentAlert.build(staffUser, "Clear | Id: " + getCaseId());

        incidentLog.sendMessage(incidentEmbed).queue();
    }

}
