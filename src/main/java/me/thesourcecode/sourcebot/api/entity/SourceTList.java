package me.thesourcecode.sourcebot.api.entity;

import me.theforbiddenai.trellowrapperkotlin.TrelloApi;
import me.theforbiddenai.trellowrapperkotlin.objects.TrelloList;
import me.thesourcecode.sourcebot.api.Source;

public enum SourceTList {


    SOURCE_SUGGESTIONS("5cc9f94d2ead053b89c2cbb4"),
    SOURCE_BUGS("5cc9f94a45cb6443ec0d498e"),
    WEBSITE_SUGGESTIONS("5e75685056f47d2416403cef"),
    WEBSITE_BUGS("5e7568649128e0027526bb92"),
    TSC_SUGGESTIONS("5cd337c09cb7f665ff7d4141"),
    TSC_ISSUES("5cd337cac0abd76c744740e8"),
    FIXED("5cc9f96672fde13c52f9addb"),
    DEAD("5cc9fdc1890236850d644368");

    private static final TrelloApi trelloApi = Source.getInstance().getTrelloApi();
    private final String listId;

    SourceTList(String listId) {
        this.listId = listId;
    }

    /**
     * Resolves a list from it's name
     *
     * @param name The name of the list being searched for
     * @return The found list or null if not found
     */
    public static TrelloList resolveListFromName(String name) {
        switch (name.toLowerCase()) {
            case "sourcebot suggestions":
            case "suggestion":
            case "suggestions":
                return SOURCE_SUGGESTIONS.asList();
            case "sourcebot bugs":
            case "bug":
            case "bugs":
                return SOURCE_BUGS.asList();
            case "tsc suggestions":
            case "tsc_suggestions":
            case "discord/channel suggestions":
                return TSC_SUGGESTIONS.asList();
            case "tsc issues":
            case "tsc_issues":
            case "discord/channel issues":
                return TSC_ISSUES.asList();
            case "website_suggestions":
            case "website suggestions":
                return WEBSITE_SUGGESTIONS.asList();
            case "website_bugs":
            case "website bugs":
                return WEBSITE_BUGS.asList();
            case "fixed":
                return FIXED.asList();
            case "dead":
                return DEAD.asList();
            default:
                return null;
        }
    }

    /**
     * Gets the TList object from an enum
     *
     * @return The trello list found
     */
    public TrelloList asList() {
        try {
            return trelloApi.getList(listId);
        } catch (Exception ex) {
            return null;
        }
    }

}
