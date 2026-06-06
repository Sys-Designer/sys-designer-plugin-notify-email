package com.sys.designer.plugin.notify.email;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EmailMessage {
    private String fromUser;
    private String fromUserNickname;
    private List<String> toUsers = new ArrayList<>();
    private List<File> files = new ArrayList<>();

    private String title;
    private String content;
    private boolean isHtml;

    public static EmailMessage of() {
        return new EmailMessage();
    }

    public String fromUser() {
        return fromUser;
    }

    public String fromUserNickname() {
        return this.fromUserNickname;
    }

    public EmailMessage fromUserNickname(String nickname) {
        this.fromUserNickname = nickname;
        return this;
    }


    public EmailMessage fromUser(String fromUser) {
        this.fromUser = fromUser;
        return this;
    }

    public List<String> toUsers() {
        return toUsers.stream().distinct().collect(Collectors.toList());
    }

    public EmailMessage addToUser(String toUser) {
        toUsers.add(toUser);
        return this;
    }

    public EmailMessage addToUsers(List<String> toUsers) {
        toUsers.addAll(toUsers);
        return this;
    }

    public String title() {
        return title;
    }

    public EmailMessage title(String title) {
        this.title = title;
        return this;
    }

    public String content() {
        return content;
    }

    public EmailMessage content(String text, boolean html) {
        this.content = text;
        this.isHtml = html;
        return this;
    }

    public EmailMessage content(String text) {
        return content(text, false);
    }

    public List<File> files() {
        return files;
    }

    public boolean isHtml() {
        return isHtml;
    }

}
