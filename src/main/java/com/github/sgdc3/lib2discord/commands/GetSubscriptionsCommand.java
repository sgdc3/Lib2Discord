package com.github.sgdc3.lib2discord.commands;

import com.github.sgdc3.lib2discord.storage.Storage;
import de.btobastian.javacord.entities.User;
import de.btobastian.javacord.entities.channels.ServerTextChannel;
import de.btobastian.javacord.entities.permissions.PermissionType;
import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class GetSubscriptionsCommand implements CommandExecutor {

    @Inject
    private Storage storage;

    @Command(
            aliases = {"!getsubscriptions"},
            description = "Returns a list of all the subscribed repositories.",
            usage = "!getsubscriptions",
            requiresMention = true,
            privateMessages = false,
            async = true
    )
    public String onGetSubscriptions(ServerTextChannel channel, User user, String command) {
        if (channel == null) {
            return "This command can be used only in server text channels!";
        }

        if (!channel.getServer().hasPermission(user, PermissionType.ADMINISTRATOR)) {
            return "Only server administrators can run this command!";
        }

        try {
            List<String> subscriptions = storage.getSubscriptions(channel.getId()).get(5, TimeUnit.SECONDS);
            return "Subscribed repositories:" + String.join("\n", subscriptions);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            log.error("An error occurred while executing command " + command, e);
        }

        return "Unable to handle the request at the moment, I'm sorry...";
    }

}
