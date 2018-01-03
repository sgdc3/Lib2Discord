package com.github.sgdc3.lib2discord.routes;

import com.github.sgdc3.lib2discord.data.UpdateRequest;
import com.github.sgdc3.lib2discord.storage.Storage;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import de.btobastian.javacord.DiscordApi;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import spark.Request;
import spark.Response;
import spark.Route;

import javax.inject.Inject;
import java.util.Properties;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class RequestHandler implements Route {

    @Inject
    private Gson gson;
    @Inject
    private Properties config;
    @Inject
    private Storage storage;
    @Inject
    private DiscordApi discordApi;

    @Override
    public Object handle(Request request, Response response) throws Exception {
        log.info("Received a new HTTP POST request from " + request.ip() + "!");

        UpdateRequest updateRequest;
        try {
            updateRequest = gson.fromJson(request.body(), UpdateRequest.class);
        } catch (JsonSyntaxException e) {
            log.warn("Illegal request!", e);
            return "err";
        }

        if (!updateRequest.getEvent().equals("new_update")) {
            return "ok";
        }

        storage.getSubscribers(updateRequest.getRepository()).thenAcceptAsync(destinations -> {
            destinations.forEach(destination -> {
                discordApi.getChannelById(destination).ifPresentOrElse(channel -> {
                    channel.asServerTextChannel().ifPresentOrElse(textChannel -> {
                        String message = config.getProperty("message")
                                .replace("<nl>", "\n")
                                .replace("%projectName%", updateRequest.getProject().getName())
                                .replace("%libraryName%", updateRequest.getName())
                                .replace("%libraryVersion%", updateRequest.getVersion());
                        textChannel.sendMessage(message);
                    }, () -> {
                        log.error("The channel (" + destination + ") isn't a server text channel!");
                        storage.removeSubscriptions(destination).thenRunAsync(() -> {
                            log.info("Removed invalid channel from the storage! ID:" + destination);
                        });
                    });
                }, () -> {
                    log.error("No channel found with the given ID (" + destination + ")!");
                    storage.removeSubscriptions(destination);
                });
            });
        });

        return "ok";
    }

}
