package com.test.gitclient.format;

public enum ContentType {
    ADD("add"),
    DELETE("delete"),
    ORIGINAL("original");
    private String type;


    ContentType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
