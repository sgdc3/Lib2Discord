package com.github.sgdc3.lib2discord;

import com.github.sgdc3.lib2discord.data.request.UpdateRequest;
import com.google.common.util.concurrent.FutureCallback;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import de.btobastian.javacord.DiscordAPI;
import de.btobastian.javacord.Javacord;
import de.btobastian.javacord.entities.Channel;
import de.btobastian.javacord.entities.Server;
import de.btobastian.javacord.entities.User;
import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import de.btobastian.sdcf4j.handler.JavacordHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

import static spark.Spark.*;

@Slf4j(topic = "Lib2Discord")
public final class Lib2Discord implements CommandExecutor {

    private final static Gson GSON = new Gson();
    private DiscordAPI discordAPI;

    public Lib2Discord() {
        log.info("Loading Lib2Discord...");

        // Load the configuration file
        File propertiesFile = new File("config.properties");
        if (!propertiesFile.exists()) {
            log.info("Copying the default config file...");
            try {
                Files.copy(getClass().getResourceAsStream("/config.properties"), propertiesFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                log.error("An error occurred while copying the default config file!", e);
                return;
            }
        }
        log.info("Loading the configuration file...");
        Properties properties = new Properties();
        try {
            properties.load(new FileReader(propertiesFile));
        } catch (IOException e) {
            log.error("An error occurred while loading the configuration file!", e);
            return;
        }
        log.info("Configuration loaded successfully!");

        // Connect the bot instance
        Javacord.getApi(properties.getProperty("botToken"), true).connect(new FutureCallback<DiscordAPI>() {
            @Override
            public void onSuccess(DiscordAPI result) {
                discordAPI = result;
                new JavacordHandler(discordAPI).registerCommand(Lib2Discord.this);
                log.info("Successfully connected the DiscordBot!");
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("An error occurred while connecting the DiscordBot!", t);
                System.exit(-1);
            }
        });

        // Set the http listener
        log.info("Initializing the http server...");
        ipAddress(properties.getProperty("bindAddress"));
        port(Integer.parseInt(properties.getProperty("port")));
        post("/" + properties.getProperty("path"), (request, response) -> {
            log.info("Received a new HTTP POST requst from " + request.ip() + "!");

            UpdateRequest updateRequest;
            try {
                updateRequest = GSON.fromJson(request.body(), UpdateRequest.class);
            } catch (JsonSyntaxException e) {
                log.warn("Illegal request!", e);
                return "err";
            }

            if (!updateRequest.getEvent().equals("new_update")) {
                return "ok";
            }

            Channel channel = discordAPI.getChannelById(properties.getProperty("channelId"));
            if (channel == null) {
                log.error("No channel found with the given ID!");
                return "err";
            }

            String message = properties.getProperty("message")
                    .replace("<nl>", "\n")
                    .replace("%projectName%", updateRequest.getProject().getName())
                    .replace("%libraryName%", updateRequest.getName())
                    .replace("%libraryVersion%", updateRequest.getVersion());
            channel.sendMessage(message);

            return "ok";
        });
    }

    @Command(aliases = {"!addrepository"}, description = "Add a repository to the repository whitelist.", privateMessages = false)
    public void onSetRepositoriesCommand(Server server, Channel channel, User user, String) {

    }

    public static void main(String[] args) {
        new Lib2Discord();
    }

}
