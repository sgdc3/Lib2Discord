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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class RemoveSubscriptionCommand implements CommandExecutor {

    @Inject
    private Storage storage;

    @Command(
            aliases = {"!removesubscription"},
            description = "Unsubscribe this channel from a repositor",
            usage = "!removesubscription [repository]",
            requiresMention = true,
            privateMessages = false,
            async = true
    )
    public String onRemoveSubscription(ServerTextChannel channel, User user, String command, String repository) {
        if (channel == null) {
            return "This command can be used only in server text channels!";
        }

        if (!channel.getServer().hasPermission(user, PermissionType.ADMINISTRATOR)) {
            return "Only server administrators can run this command!";
        }

        if (repository == null) {
            return "Not enough arguments!";
        }
        if (repository.contains(" ") | !repository.contains("/")) {
            return "Illegal repository syntax!";
        }

        try {
            if (!storage.hasSubscription(channel.getId(), repository).get(5, TimeUnit.SECONDS)) {
                log.debug("Tried to unsubscribe from unsubscribed repository (" + repository + ") from channel " + channel.getId());
                return "This channel isn't subscribed to " + repository + "!";
            }
            storage.removeSubscription(channel.getId(), repository).get(5, TimeUnit.SECONDS);
            log.info("Unsubscribed channel " + channel.getId() + " from repository " + repository);
            return "Channel unsubscribed from " + repository + " successfully!";
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            log.error("An error occurred while executing command " + command, e);
        }

        return "Unable to handle the request at the moment, I'm sorry...";
    }

}
