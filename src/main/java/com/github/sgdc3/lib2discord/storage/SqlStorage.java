package com.github.sgdc3.lib2discord.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.sql2o.Connection;
import org.sql2o.Sql2o;

import javax.inject.Inject;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class SqlStorage implements Storage {

    private HikariDataSource dataSource;
    private Sql2o sql;

    @Inject
    SqlStorage(Properties config) throws ClassNotFoundException {
        log.info("Initializing the SqlStorage...");
        HikariConfig hikariConfig = new HikariConfig();
        String driver = config.getProperty("sql.driver");
        Class.forName(driver);
        hikariConfig.setDriverClassName(driver);
        hikariConfig.setJdbcUrl(config.getProperty("sql.jdbcUrl"));
        hikariConfig.setUsername(config.getProperty("sql.user"));
        hikariConfig.setPassword(config.getProperty("sql.password"));
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        dataSource = new HikariDataSource(hikariConfig);
        sql = new Sql2o(dataSource);
        log.info("Initializing schema...");
        initSchema();
        log.info("SqlStorage initialized successfully!");
    }

    private void initSchema() {
        sql.open().createQuery(
                "CREATE TABLE IF NOT EXISTS subscriptions (" +
                        " subscription_id INT PRIMARY KEY AUTO_INCREMENT," +
                        " channel_id BIGINT UNSIGNED NOT NULL," +
                        " repository TINYTEXT NOT NULL" +
                        ");"
        ).executeUpdate();
    }

    @Override
    public CompletableFuture<List<Long>> getSubscribers(@NonNull String repository) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = sql.open()) {
                return connection.createQuery("SELECT channel_id FROM subscriptions WHERE repository=:repository;")
                        .addParameter("repository", repository)
                        .executeAndFetch(Long.class);
            }
        });
    }

    @Override
    public CompletableFuture<List<String>> getSubscriptions(@NonNull Long channelId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = sql.open()) {
                return connection.createQuery("SELECT repository FROM subscriptions WHERE channel_id=:channel_id;")
                        .addParameter("channel_id", channelId)
                        .executeAndFetch(String.class);
            }
        });
    }

    @Override
    public CompletableFuture<Void> addSubscribtion(@NonNull Long channelId, @NonNull String repository) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = sql.open()) {
                connection.createQuery("INSERT INTO subscriptions(repository, channel_id) VALUES (:repository, :channel_id);")
                        .addParameter("repository", repository)
                        .addParameter("channel_id", channelId)
                        .executeUpdate();
            }
        });
    }

    @Override
    public CompletableFuture<Void> removeSubscription(@NonNull Long channelId, @NonNull String repository) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = sql.open()) {
                connection.createQuery(
                        "DELETE FROM subscriptions WHERE repository=:repository AND channel_id=:channel_id;")
                        .addParameter("repository", repository)
                        .addParameter("channel_id", channelId)
                        .executeUpdate();
            }
        });
    }

    @Override
    public CompletableFuture<Void> removeSubscriptions(@NonNull Long channelId) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = sql.open()) {
                connection.createQuery(
                        "DELETE FROM subscriptions WHERE channel_id=:channel_id;")
                        .addParameter("channel_id", channelId)
                        .executeUpdate();
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> hasSubscription(@NonNull Long channelId, @NonNull String repository) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = sql.open()) {
                return connection.createQuery(
                        "SELECT channel_id FROM subscriptions WHERE repository=:repository AND channel_id=:channel_id")
                        .addParameter("repository", repository)
                        .addParameter("channel_id", channelId)
                        .executeAndFetch(Integer.class)
                        .size() > 0;
            }
        });
    }

    @Override
    public void close() {
        log.info("Closing the SqlStorage...");
        dataSource.close();
        log.info("SqlStorage closed successfully!");
    }

}
