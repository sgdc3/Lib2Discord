package com.github.sgdc3.lib2discord.storage;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface Storage {

    CompletableFuture<List<Long>> getSubscribers(String repository);

    CompletableFuture<List<String>> getSubscriptions(Long channelId);

    CompletableFuture<Void> addSubscribtion(Long channelId, String repository);

    CompletableFuture<Void> removeSubscription(Long channelId, String repository);

    CompletableFuture<Void> removeSubscriptions(Long channelId);

    CompletableFuture<Boolean> hasSubscription(Long channelId, String repository);

    void close();

}
