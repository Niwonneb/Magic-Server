package com.ngw.seed.CommunicationObjects;

public class Thought {
    private final String text;
    private final String id;

    public Thought(String id, String text) {
        this.id = id;
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public String getId() {
        return id;
    }
}