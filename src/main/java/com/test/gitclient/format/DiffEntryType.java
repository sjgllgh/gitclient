package com.test.gitclient.format;

public enum DiffEntryType {
    ADD("add"),
    DELETE("delete"),
    MODIFY("modify");
    private String type;


    DiffEntryType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
