package dev.skidfuscator.discord;

import dev.skidfuscator.discord.utils.EvictingArrayList;
import dev.skidfuscator.obf.Skidfuscator;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.guild.GuildAvailableEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Appender;
import org.apache.log4j.AsyncAppender;
import org.apache.log4j.LogManager;
import org.apache.log4j.spi.LoggingEvent;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class DiscordBot {
    public static void main(String[] args) throws LoginException {
        JDABuilder builder = JDABuilder.createDefault(args[0]);

        // Disable parts of the cache
        builder.disableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE);
        // Enable the bulk delete event
        builder.setBulkDeleteSplittingEnabled(false);
        // Set activity (like "playing Something")
        builder.setActivity(Activity.watching("Skids like you"));

        JDA jda = builder.build();

        update(jda);

        jda.addEventListener(new EventListener() {
            @Override
            public void onEvent(@NotNull GenericEvent event) {
                if (event instanceof GuildJoinEvent || event instanceof GuildAvailableEvent || event instanceof GuildReadyEvent) {
                    update(jda);
                }
            }
        });
        jda.addEventListener(new EventListener() {
            private final ObfuscatorQueue obfuscatorRequests = new ObfuscatorQueue(new Consumer<ObfuscatorRequest>() {
                @lombok.SneakyThrows
                @Override
                public void accept(ObfuscatorRequest request) {
                    request.getMessage().editMessage(new MessageBuilder().setEmbeds(new EmbedBuilder()
                            .addField(new MessageEmbed.Field(
                                    ":gear: Currently Downloading",
                                    "Your jar is currently being downloaded by Skidfuscator!"
                                            + " \nCheck back in a couple of seconds!",
                                    false,
                                    true
                            )).setColor(Color.ORANGE).build()).build()).queue();

                    final File temp = File.createTempFile("skidfuscator", Math.random() + "-abc.jar");

                    InputStream stream = null;
                    try {
                        URLConnection connection = new URL(request.getUrl()).openConnection();
                        connection.setConnectTimeout(10000);
                        connection.setReadTimeout(10000);
                        connection.setRequestProperty("User-Agent", "Skidfuscator Bot 1.0.0");
                        stream = connection.getInputStream();
                        FileUtils.copyInputStreamToFile(stream, temp);
                        stream.close();
                    } catch (Throwable e) {
                        e.printStackTrace();
                        request.getMessage().editMessage(new MessageBuilder().setEmbeds(new EmbedBuilder()
                                .addField(new MessageEmbed.Field(
                                        ":thumbsdown: Failed to download",
                                        "Your jar failed to be downloaded by Skidfuscator!"
                                                + " \nPlease try again!",
                                        false,
                                        true
                                )).setColor(Color.RED).build()).build()).queue();

                        if (stream != null) {
                            stream.close();
                        }
                        return;
                    }

                    request.getMessage().editMessage(new MessageBuilder().setEmbeds(new EmbedBuilder()
                            .addField(new MessageEmbed.Field(
                                    ":gear: Currently Validating",
                                    "Your jar is currently being validated by Skidfuscator!"
                                            + " \nCheck back in a couple of seconds!",
                                    false,
                                    true
                            )).setColor(Color.ORANGE).build()).build()).queue();

                    try {
                        ZipFile file = new ZipFile(temp);
                        Enumeration<? extends ZipEntry> e = file.entries();
                        while(e.hasMoreElements()) {
                            e.nextElement();
                        }
                    } catch(Exception ex) {
                        request.getMessage().editMessage(new MessageBuilder().setEmbeds(new EmbedBuilder()
                                .addField(new MessageEmbed.Field(
                                        ":thumbsdown: Failed to validate",
                                        "Your jar failed to be validated by Skidfuscator!"
                                                + " \nPlease try again!",
                                        false,
                                        true
                                )).setColor(Color.RED).build()).build()).queue();
                        ex.printStackTrace();
                        return;
                    }

                    request.getMessage().editMessage(new MessageBuilder().setEmbeds(new EmbedBuilder()
                            .addField(new MessageEmbed.Field(
                                    ":gear: Currently Obfuscating",
                                    "Your jar is currently being obfuscated by Skidfuscator!"
                                            + " \nCheck back in a couple of seconds!",
                                    false,
                                    true
                            )).setColor(Color.ORANGE).build()).build()).queue();

                    final List<String> logs = new EvictingArrayList<>(16);
                    final Appender appender = new AsyncAppender() {
                        private long lastAppend;

                        @Override
                        public void append(LoggingEvent event) {
                            super.append(event);

                            logs.add(event.getRenderedMessage());

                            if (System.currentTimeMillis() - lastAppend < 500) {
                                return;
                            }

                            this.lastAppend = System.currentTimeMillis();

                            final StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("```");

                            for (String log : logs) {
                                stringBuilder.append(log).append("\n");
                            }

                            stringBuilder.append("```");

                            final EmbedBuilder embedBuilder = new EmbedBuilder()
                                    .addField(new MessageEmbed.Field(
                                            ":gear: Currently Obfuscating",
                                            "Your jar is currently being obfuscated by Skidfuscator!"
                                                    + " \nCheck back in a couple of seconds!",
                                            false,
                                            true
                                    ))
                                    .addField(new MessageEmbed.Field(":computer: Console", stringBuilder.toString(), false, true))
                                    .setColor(Color.ORANGE);

                            request.getMessage().editMessage(new MessageBuilder().setEmbeds(embedBuilder.build()).build()).queue();
                        }
                    };
                    LogManager.getRootLogger().addAppender(appender);

                    final File file;
                    try {
                        file = Skidfuscator.start(temp);
                    } catch (Throwable e) {
                        LogManager.getRootLogger().fatal("Failed to obfuscate", e);
                        final StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("```");

                        for (String log : logs) {
                            stringBuilder.append(log).append("\n");
                        }

                        stringBuilder.append("```");

                        request.getMessage().editMessage(new MessageBuilder().setEmbeds(new EmbedBuilder()
                                .addField(new MessageEmbed.Field(
                                        ":thumbsdown: Failed to validate",
                                        "Your jar failed to be validated by Skidfuscator!"
                                                + " \nPlease try again!",
                                        false,
                                        true
                                ))
                                .addField(new MessageEmbed.Field(":computer: Console", stringBuilder.toString(), false, true))
                                .setColor(Color.RED).build()).build()).queue();
                        temp.delete();
                        LogManager.getRootLogger().removeAppender(appender);
                        return;
                    }

                    temp.delete();

                    request.getMessage().editMessage(new MessageBuilder().setEmbeds(new EmbedBuilder()
                            .addField(new MessageEmbed.Field(
                                    ":thumbsup: Completed",
                                    "Your jar has been obfuscated by Skidfuscator!"
                                            + " \nCheck below for the result!",
                                    false,
                                    true
                            )).setColor(Color.GREEN).build()).build()).queue();

                    request.getMessage().getChannel().sendMessage(new MessageBuilder().mention(request.getUser())
                                    .appendCodeBlock(request.getName(), "bash")
                                    .build())
                            .mention(request.getUser())
                            .addFile(file, request.getName())
                            .queue(new Consumer<Message>() {
                                @Override
                                public void accept(Message message) {
                                    file.delete();
                                }
                            });

                    LogManager.getRootLogger().removeAppender(appender);
                }
            });

            @Override
            public void onEvent(@NotNull GenericEvent e) {
                if (e instanceof MessageReceivedEvent) {
                    final MessageReceivedEvent event = (MessageReceivedEvent) e;

                    if (!event.getMessage().getContentDisplay().equalsIgnoreCase("!skidthis"))
                        return;

                    if (event.getMessage().getAttachments().isEmpty()) {
                        event.getMessage().getChannel().sendMessage(new MessageBuilder()
                                .mention(event.getAuthor()).append("You cannot skid something without uploading a file... duh")
                                .build()).queue();
                        return;
                    }

                    if (event.getMessage().getAttachments().size() > 1) {
                        event.getMessage().getChannel().sendMessage(new MessageBuilder()
                                .mention(event.getAuthor()).append("Too many files cmon gimme a break")
                                .build()).queue();
                        return;
                    }

                    final Message.Attachment attachment = event.getMessage().getAttachments().get(0);

                    if (attachment.getFileExtension() == null) {
                        event.getMessage().getChannel().sendMessage(new MessageBuilder()
                                .mention(event.getAuthor()).append("Extension is null dude")
                                .build()).queue();
                        return;
                    }

                    if (!attachment.getFileExtension().equalsIgnoreCase("jar")) {
                        event.getMessage().getChannel().sendMessage(new MessageBuilder()
                                .mention(event.getAuthor()).append("Not a jar bro")
                                .build()).queue();
                        return;
                    }

                    if (attachment.getSize() > 2e+6) {
                        event.getMessage().getChannel().sendMessage(new MessageBuilder()
                                .mention(event.getAuthor()).append("Jar too fat bro")
                                .build()).queue();
                        return;
                    }

                    Message message = new MessageBuilder().setEmbeds(new EmbedBuilder()
                            .addField(new MessageEmbed.Field(
                                    ":eyes: You're in queue!",
                                    "Your jar is currently being queued by Skidfuscator!"
                                            + " \nCheck back in a couple of minutes!",
                                    false,
                                    true
                            )).setColor(Color.ORANGE).build()).mention(event.getAuthor()).build();

                    event.getChannel().sendMessage(message).queue(new Consumer<Message>() {
                        @Override
                        public void accept(Message msg) {
                            System.out.println("Attempting to download url " + attachment.getProxyUrl() + " from OG source " + attachment.getUrl());
                            obfuscatorRequests.add(new ObfuscatorRequest(event.getAuthor(), msg, attachment.getFileName(), attachment.getUrl()));
                        }
                    });

                }
            }
        });
    }

    private static void update(final JDA jda) {
        jda.getPresence().setActivity(Activity.watching(jda.getGuilds().size() + " skids like you"));
    }
}
