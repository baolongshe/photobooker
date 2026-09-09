package com.xhxi.photobooker.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class PasswordDTO implements Serializable {
    private Long userId;
    private String oldPassword;
    private String newPassword;
} 