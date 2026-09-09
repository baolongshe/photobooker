package com.xhxi.photobooker.service;

import com.xhxi.photobooker.entity.Application;
import java.util.List;

public interface ApplicationService {
    void submitApplication(Application application);
    List<Application> getApplicationsByUserId(Long userId);
    List<Application> getAllApplications();
    void reviewApplication(Long id, String status, String remark);
    Application getApplicationById(Long id);
} 