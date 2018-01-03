package com.github.sgdc3.lib2discord.data;

import lombok.Data;

@Data
public class UpdateRequest {

    private String event;
    private String repository;
    private String platform;
    private String name;
    private String version;
    private String package_manager_url;
    private String published_at;
    private Project project;

}
