package com.test.gitclient.format;

import java.util.ArrayList;
import java.util.List;

public class FormatEntry {
    private DiffEntryType type;
    private String filePath;

    private List<DiffContent> contents = new ArrayList<>();

    public DiffEntryType getType() {
        return type;
    }

    public void setType(DiffEntryType type) {
        this.type = type;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public List<DiffContent> getContents() {
        return contents;
    }

    public void setContents(List<DiffContent> contents) {
        this.contents = contents;
    }

    public void addContent(DiffContent content) {
        this.contents.add(content);
    }
}
