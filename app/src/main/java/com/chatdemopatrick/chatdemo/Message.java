package com.chatdemopatrick.chatdemo;

// The message class contains the message text, the author, and, if used, the image URL
public class Message {
    private String text;
    private String name;
    private String imageUrl;

    public Message() {
    }

    public Message(String text, String name, String imageUrl) {
        this.text = text;
        this.name = name;
        this.imageUrl = imageUrl;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
