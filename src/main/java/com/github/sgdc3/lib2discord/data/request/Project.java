package com.github.sgdc3.lib2discord.data.request;

import lombok.Data;

import java.util.List;

@Data
public class Project {

    private String name;
    private String platform;
    private String description;
    private String homepage;
    private String repository_url;
    private List<String> normalized_licenses;
    private String latest_release_published_at;
    private String language;

}
