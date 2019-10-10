package com.asadeq.rxdownloadmanager;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class DownloadFilesModel {

    @SerializedName("url")
    @Expose
    private Boolean url;

    @SerializedName("name")
    @Expose
    private String name;

    @SerializedName("type")
    @Expose
    private String type;

    public Boolean getUrl() {
        return url;
    }

    public void setUrl(Boolean url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
