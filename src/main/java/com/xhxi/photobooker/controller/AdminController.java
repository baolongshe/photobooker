package com.xhxi.photobooker.controller;

import com.xhxi.photobooker.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/photo/admin")
public class AdminController {
    @Autowired
    private AdminService adminService;
}
