package com.example;
import java.time.LocalDateTime;

public class Todo {
    private int id;
    private String content;
    private LocalDateTime dateTime;
    private Priority priority;

    public Todo(){
    }

    public Todo(String content, LocalDateTime dateTime, Priority priority){
        this.content = content;
        this.dateTime = dateTime;
        this.priority = priority;   
    }

    public Todo(int id, String content, LocalDateTime dateTime, Priority priority){
        this.id = id;
        this.content = content;
        this.dateTime = dateTime;
        this.priority = priority;
    }

    public int getId(){
        return id;
    }

    public void setId(int id){
        this.id = id;
    }

    public String getContent(){
        return content;
    }

    public void setContent(String content){
        this.content = content;
    }

    public LocalDateTime getDateTime(){
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime){
        this.dateTime = dateTime;
    }

    public Priority getPriority(){
        return priority;
    }

    public void setPriority(Priority priority){
        this.priority = priority;
    }

    @Override
    public String toString() {
        return "Todo{" +
                "id=" + id +
                ", content='" + content + '\'' +
                ", dateTime=" + dateTime +
                ", priority=" + priority +
                '}';
    }
}
