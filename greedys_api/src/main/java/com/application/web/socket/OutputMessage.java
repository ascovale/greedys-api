package com.application.web.socket;

public class OutputMessage extends Message {

    private String time;

    public OutputMessage() {
        super();
    }
    
    public OutputMessage(final String from, final String text, final String time) {
        setFrom(from);
        setText(text);
        this.time = time;
    }
    public OutputMessage(final String from, String to, final String text, final String time) {
        setFrom(from);
        setText(text);
        setTo(to);
        this.time = time;
    }

    public String getTime() {
        return time;
    }
    public void setTime(String time) { this.time = time; }
}
