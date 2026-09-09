package com.xhxi.photobooker.dto;

import lombok.Data;

@Data
public class UpdatePhoneDTO {
    private Long userId;
    private String phone;
    private String code;
    // getter/setter
}