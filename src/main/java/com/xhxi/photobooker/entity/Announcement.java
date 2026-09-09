package com.xhxi.photobooker.entity;

import lombok.Data;
import java.util.Date;

@Data
public class Announcement {
    private Long id;
    private String title;
    private String content;
    private Date time;
} 