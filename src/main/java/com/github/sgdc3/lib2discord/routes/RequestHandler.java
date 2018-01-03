package com.github.sgdc3.lib2discord.routes;

import com.github.sgdc3.lib2discord.data.UpdateRequest;
import com.github.sgdc3.lib2discord.storage.Storage;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import de.btobastian.javacord.DiscordApi;
import de.btobastian.javacord.entities.channels.Channel;
import de.btobastian.javacord.entities.channels.ServerTextChannel;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import spark.Request;
import spark.Response;
import spark.Route;

import javax.inject.Inject;
import java.util.Optional;
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
            if (updateRequest == null) {
                throw new JsonSyntaxException("Empty data!");
            }
        } catch (JsonSyntaxException e) {
            log.warn("Illegal request!", e);
            return "err";
        }

        if (!updateRequest.getEvent().equals("new_version")) {
            return "ok";
        }

        log.info("Received an update for repository " + updateRequest.getRepository());
        storage.getSubscribers(updateRequest.getRepository()).thenAcceptAsync(destinations -> {
            destinations.forEach(destination -> {
                Optional<Channel> optionalChannel = discordApi.getChannelById(destination);
                if (optionalChannel.isPresent()) {
                    Optional<ServerTextChannel> optionalTextChannel = optionalChannel.get().asServerTextChannel();
                    if (optionalTextChannel.isPresent()) {
                        ServerTextChannel channel = optionalTextChannel.get();
                        String message = config.getProperty("message")
                                .replace("<nl>", "\n")
                                .replace("%projectName%", updateRequest.getProject().getName())
                                .replace("%libraryName%", updateRequest.getName())
                                .replace("%libraryVersion%", updateRequest.getVersion());
                        channel.sendMessage(message);
                    } else {
                        log.error("The channel (" + destination + ") isn't a server text channel!");
                        storage.removeSubscriptions(destination).thenRunAsync(() -> {
                            log.info("Removed invalid channel from the storage! ID:" + destination);
                        });
                    }
                } else {
                    log.error("No channel found with the given ID (" + destination + ")!");
                    storage.removeSubscriptions(destination);
                }
            });
        });

        return "ok";
    }

}
