package com.chatroom.entity;

import java.io.Serializable;

public class Message implements Serializable {

    private String type;
    private String target;
    private String content;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Message() {
    }

    public Message(String type, String target, String content) {
        this.type = type;
        this.target = target;
        this.content = content;
    }
}
