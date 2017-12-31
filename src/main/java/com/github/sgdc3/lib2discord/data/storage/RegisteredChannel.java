package com.github.sgdc3.lib2discord.data.storage;

import lombok.Data;

import java.util.List;

@Data
public class RegisteredChannel {

    private String channelId;
    private List<String> enabledRepositories;

}
