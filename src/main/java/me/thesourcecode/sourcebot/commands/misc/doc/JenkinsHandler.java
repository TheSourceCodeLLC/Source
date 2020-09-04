package me.thesourcecode.sourcebot.commands.misc.doc;

import com.overzealous.remark.Remark;
import me.theforbiddenai.jenkinsparserkotlin.Jenkins;
import me.theforbiddenai.jenkinsparserkotlin.entities.*;
import me.thesourcecode.sourcebot.api.utility.SourceColor;
import me.thesourcecode.sourcebot.listener.DocSelectionListener;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;
import net.dv8tion.jda.api.utils.MarkdownUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class JenkinsHandler {

    private final String baseURL;
    private final String iconURL;
    private final String embedTitle;

    private final Jenkins jenkins;
    private final Remark remark;
    private List<String> classList;

    public JenkinsHandler(String docURL, String iconURL, String embedTitle) {
        this.baseURL = docURL.substring(0, docURL.lastIndexOf("/") + 1).trim();

        this.iconURL = iconURL;
        this.embedTitle = embedTitle;

        jenkins = new Jenkins(docURL);
        remark = new Remark();
        getClassList();
    }

    public String getDocURL() {
        return baseURL;
    }

    /**
     * Gets the information from jenkins and gets the embeds
     *
     * @param query The information being looked for
     * @return The embed for the documentation or a selection menu embed
     */
    public MessageEmbed search(User user, String query) {
        try {
            removeUserFromDocCache(user);
            List<Information> foundInformationList = jenkins.search(query);


            if (foundInformationList.size() == 0) return null;

            EmbedBuilder documentationEmbed = new EmbedBuilder()
                    .setColor(SourceColor.BLUE.asColor())
                    .setAuthor(embedTitle, null, iconURL)
                    .setFooter("Ran By: " + user.getAsTag(), user.getEffectiveAvatarUrl());

            if (foundInformationList.size() == 1) {
                Information information = foundInformationList.get(0);

                return createDocumentationEmbed(documentationEmbed, information).build();
            } else {
                DocSelectionListener.selectionStorageCache.put(user, foundInformationList);
                return createSelectionEmbed(documentationEmbed, foundInformationList).build();
            }
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Removes the user from the doc cache if they are in it
     *
     * @param user The user being removed
     */
    private void removeUserFromDocCache(User user) {
        ConcurrentHashMap<User, Map.Entry<Message, JenkinsHandler>> docStorageCache = DocSelectionListener.docStorageCache;
        ConcurrentHashMap<User, List<Information>> selectionStorageCache = DocSelectionListener.selectionStorageCache;


        if (docStorageCache.containsKey(user)) {
            docStorageCache.get(user).getKey().delete().queue();

            selectionStorageCache.remove(user);
            docStorageCache.remove(user);
        }
    }

    /**
     * Creates the selection menu embed
     *
     * @param selectionMenu   The embed builder for the selection menu (this contains the color and author already)
     * @param informationList The list of found information objects
     * @return The selection menu
     */
    private EmbedBuilder createSelectionEmbed(EmbedBuilder selectionMenu, List<Information> informationList) {
        selectionMenu.setTitle("Type the id of the option you would like to select in chat:");
        selectionMenu.setFooter("Type cancel to delete this message.");


        informationList.sort(Comparator.comparing(info -> {
            if (info instanceof MethodInformation) {
                MethodInformation methodInfo = (MethodInformation) info;
                return methodInfo.getName().length();
            }
            return info.getName().length();
        }));

        int count = 1;
        for (Information info : informationList) {
            String name = info.getType() + " " + info.getName();

            String optionText = MarkdownUtil.maskedLink(name, info.getUrl());
            selectionMenu.appendDescription("\n\n**" + count + "** - " + optionText);

            count++;
            if (selectionMenu.getDescriptionBuilder().length() >= 712) {
                String format = "\n\nIds not shown above: %d to %d";
                selectionMenu.appendDescription(String.format(format, count, informationList.size() - 1));
                break;
            }

        }

        return selectionMenu;
    }

    /**
     * Creates the documentation embed for the specified information object
     *
     * @param documentationEmbed The documentation embed (this contains the color and author already)
     * @param information        The information object the embed is being created for
     * @return The modified documentation embed which contains all the information from the information object
     */
    public EmbedBuilder createDocumentationEmbed(EmbedBuilder documentationEmbed, Information information) {
        String infoURL = MarkdownSanitizer.escape(information.getUrl());
        String infoName = MarkdownSanitizer.escape(information.getName());
        String infoRawDescription = information.getRawDescription();

        if (information instanceof ClassInformation) {
            ClassInformation classInfo = (ClassInformation) information;

            List<String> nestedClasses = classInfo.getNestedClassList();
            List<String> methodList = classInfo.getMethodList();
            List<String> enumList = classInfo.getEnumList();
            List<String> fieldList = classInfo.getFieldList();

            if (!nestedClasses.isEmpty()) {
                String nestedClassList = formatList(nestedClasses).replace(infoName + ".", "");
                documentationEmbed.addField("Nested Classes:", nestedClassList, false);
            }
            if (!methodList.isEmpty()) {
                methodList = methodList.stream()
                        .map(methodName -> methodName.substring(0, methodName.indexOf("(")).trim())
                        .collect(Collectors.toList());

                documentationEmbed.addField("Methods:", formatList(methodList), false);
            }
            if (!enumList.isEmpty()) {
                documentationEmbed.addField("Enums:", formatList(enumList), false);
            }
            if (!fieldList.isEmpty()) {
                documentationEmbed.addField("Fields:", formatList(fieldList), false);
            }

            // format list
        } else if (information instanceof MethodInformation) {
            MethodInformation methodInfo = (MethodInformation) information;

            infoName = getInfoName(methodInfo.getClassInfo().getName(), methodInfo.getName());
        } else if (information instanceof EnumInformation) {
            EnumInformation enumInfo = (EnumInformation) information;

            infoName = getInfoName(enumInfo.getClassInfo().getName(), enumInfo.getName());
        } else if (information instanceof FieldInformation) {
            FieldInformation fieldInfo = (FieldInformation) information;

            infoName = getInfoName(fieldInfo.getClassInfo().getName(), fieldInfo.getName());
        }

        Element infoDescriptionElement = Jsoup.parse("<div>" + infoRawDescription + "</div>").selectFirst("div");
        String infoDescription = convertHTMLToMarkdown(convertHyperlinks(infoDescriptionElement));

        String infoHyperLink = MarkdownUtil.maskedLink(infoName, infoURL);

        documentationEmbed.setDescription("**__" + infoHyperLink + "__**")
                .appendDescription("\n" + infoDescription);

        if (!(information instanceof ClassInformation)) {
            Map<String, String> rawExtraInfo = information.getRawExtraInformation();

            rawExtraInfo.forEach((key, value) -> {

                Element valueElement = Jsoup.parse("<div>" + value + "</div>").selectFirst("div");
                String convertedValue = convertHTMLToMarkdown(convertHyperlinks(valueElement));

                documentationEmbed.addField(key, convertedValue, false);
            });
        }


        return documentationEmbed;
    }

    /**
     * Converts raw HTML to markdown while conserving hyperlinks
     *
     * @param rawHTML The html being converted to markdown
     * @return The converted html
     */
    private String convertHTMLToMarkdown(String rawHTML) {
        rawHTML = rawHTML.replaceAll("(<code>.*</code> -)", "§$1");
        String markdown = remark.convertFragment(rawHTML.replaceAll("<code>\\[(.*?)]\\((.*?)\\)</code>", "[<code>$1</code>]($2)"))
                .replaceAll("\\\\(\\W)", "$1").replace("§", "\n");

        if (markdown.replaceAll("\\[.*?]\\(.*?\\)", "").length() == markdown.length()) {
            markdown = limit(markdown, 600);
        }
        return markdown;
    }

    /**
     * Converts `a` elements to hyperlinks
     *
     * @param element The element the `a` elements are coming from
     * @return The raw html with the hyperlinks
     */
    private String convertHyperlinks(Element element) {
        String rawHtml = element.html();

        for (Element hyperLinkElement : element.select("a")) {
            String href = hyperLinkElement.attr("href");
            String text = hyperLinkElement.text();

            if (href == null) continue;

            if ((!href.contains("http") && (href.contains("../") || href.contains("#")))) {
                href = href.replace("../", "");
                href = baseURL + href;
            } else if ((!href.contains("http") && href.contains(".html"))) {
                href = href.contains("/") ? href.substring(href.lastIndexOf("/") + 1) : href;
                href = getClassURL(href.replace(".html", ""));
            }

            String hyperlink = MarkdownUtil.maskedLink(MarkdownSanitizer.escape(text), MarkdownSanitizer.escape(href));
            rawHtml = rawHtml.replace(hyperLinkElement.outerHtml(), hyperlink);
            if (convertHTMLToMarkdown(rawHtml).length() >= 900) {
                rawHtml = rawHtml.substring(0, rawHtml.lastIndexOf(hyperlink) - 3) + " ...";
                break;
            }
        }


        return rawHtml;
    }

    /**
     * Formats a given list into a string
     *
     * @param list The list being formatted
     * @return The formatted list in a string
     */
    private String formatList(List<String> list) {
        StringBuilder formattedList = new StringBuilder();

        list.forEach(item -> {
            if (!formattedList.toString().contains(item) && formattedList.length() <= 512) {
                formattedList.append("`").append(item).append("` ");

                if (formattedList.length() >= 512) {
                    formattedList.append("...");
                }
            }
        });
        return formattedList.toString().trim();
    }

    /**
     * Gets the url to class from a string
     *
     * @param className The name of the class
     * @return The url to the class
     */
    private String getClassURL(String className) {
        List<String> urlList = classList.stream()
                .filter(element -> {
                    element = element.substring(element.lastIndexOf("/") + 1).replace(".html", "");
                    return element.equalsIgnoreCase(className);
                })
                .collect(Collectors.toList());

        if (urlList.size() == 0) return null;

        return urlList.get(0);
    }

    /**
     * Formats the class name and info name
     *
     * @param className The name of the class
     * @param infoName  The name of the information object
     * @return The formatted string
     */
    private String getInfoName(String className, String infoName) {
        infoName = infoName.contains("(") ? infoName.substring(0, infoName.indexOf("(")) : infoName;
        return className + "#" + infoName;
    }

    /**
     * Limit the string to a certain number of characters, adding "..." if it was shortened
     *
     * @param value  The string to limitOptions.
     * @param length The length to limitOptions to (as an int).
     * @return The limited string.
     */
    private String limit(String value, int length) {
        StringBuilder buf = new StringBuilder(value);
        if (buf.length() > length) {
            buf.setLength(length);
            buf.append("...");
        }

        return buf.toString();
    }

    /**
     * Gets the element list from the JenkinsParser api
     */
    private void getClassList() {
        try {
            Field field = Jenkins.class.getDeclaredField("classList");
            field.setAccessible(true);
            this.classList = (List<String>) field.get(jenkins);
            field.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            ex.printStackTrace();
        }
    }

}