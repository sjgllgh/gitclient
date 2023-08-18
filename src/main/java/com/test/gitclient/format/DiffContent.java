package com.test.gitclient.format;

public class DiffContent {
    private ContentType type;
    private int lineNumber;
    private String content;

    public DiffContent(ContentType type, int lineNumber, String content) {
        this.type = type;
        this.lineNumber = lineNumber;
        this.content = content;
    }

    public ContentType getType() {
        return type;
    }

    public void setType(ContentType type) {
        this.type = type;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
