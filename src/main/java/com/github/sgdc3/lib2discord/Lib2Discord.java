package com.github.sgdc3.lib2discord;

import ch.jalu.injector.Injector;
import ch.jalu.injector.InjectorBuilder;
import com.github.sgdc3.lib2discord.commands.AddSubscriptionCommand;
import com.github.sgdc3.lib2discord.commands.GetSubscriptionsCommand;
import com.github.sgdc3.lib2discord.commands.RemoveSubscriptionCommand;
import com.github.sgdc3.lib2discord.routes.RequestHandler;
import com.github.sgdc3.lib2discord.storage.SqlStorage;
import com.github.sgdc3.lib2discord.storage.Storage;
import com.google.gson.Gson;
import de.btobastian.javacord.DiscordApi;
import de.btobastian.javacord.DiscordApiBuilder;
import de.btobastian.sdcf4j.CommandExecutor;
import de.btobastian.sdcf4j.CommandHandler;
import de.btobastian.sdcf4j.handler.JavacordHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static spark.Spark.*;

@Slf4j
public final class Lib2Discord implements CommandExecutor {

    private Injector injector;
    private Gson gson;
    private Properties config;
    private Storage storage;
    private DiscordApi discordApi;
    private CommandHandler commandHandler;

    public Lib2Discord() {
        log.info("Loading Lib2Discord...");
        injector = new InjectorBuilder().addDefaultHandlers("").create();

        gson = new Gson();
        injector.register(Gson.class, gson);

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
        config = new Properties();
        try {
            config.load(new FileReader(propertiesFile));
        } catch (IOException e) {
            log.error("An error occurred while loading the configuration file!", e);
            return;
        }
        injector.register(Properties.class, config);
        log.info("Configuration loaded successfully!");

        // Initialize storage
        storage = injector.newInstance(SqlStorage.class);
        injector.register(Storage.class, storage);

        // Connect the bot instance
        log.info("Connecting to the DiscordAPI...");
        try {
            discordApi = new DiscordApiBuilder().setToken(config.getProperty("bot.token")).login().get(10, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
            log.error("An error occurred while connecting the DiscordBot!", e);
            System.exit(1);
        }
        injector.register(DiscordApi.class, discordApi);
        log.info("Connected to the DiscordApi successfully!");

        // Register commands
        log.info("Registering commands...");
        commandHandler = new JavacordHandler(discordApi);
        commandHandler.registerCommand(injector.getSingleton(AddSubscriptionCommand.class));
        commandHandler.registerCommand(injector.getSingleton(RemoveSubscriptionCommand.class));
        commandHandler.registerCommand(injector.getSingleton(GetSubscriptionsCommand.class));
        log.info("Commands registered successfully!");

        // Set the http listener
        log.info("Initializing the http server...");
        ipAddress(config.getProperty("http.bindAddress"));
        port(Integer.parseInt(config.getProperty("http.port")));
        post("/webhook", injector.getSingleton(RequestHandler.class));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Thread.sleep(200);
                log.info("Shouting down...");
                stop();
                if(discordApi != null) {
                    discordApi.disconnect();
                }
                if (storage != null) {
                    storage.close();
                }
                log.info("Bye bye!");
            } catch (InterruptedException e) {
                log.error("An error occurred while shouting down!", e);
            }
        }));
    }

    public static void main(String[] args) {
        new Lib2Discord();
    }

}
