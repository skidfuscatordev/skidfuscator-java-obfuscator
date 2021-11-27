package dev.skidfuscator.discord;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

public class ObfuscatorRequest {
    private final User user;
    private final Message message;
    private final String name;
    private final String url;

    public ObfuscatorRequest(User user, Message message, String name, String url) {
        this.user = user;
        this.message = message;
        this.name = name;
        this.url = url;
    }

    public User getUser() {
        return user;
    }

    public Message getMessage() {
        return message;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }
}
