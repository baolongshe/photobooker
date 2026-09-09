package com.xhxi.photobooker.service.impl;

import com.xhxi.photobooker.entity.Application;
import com.xhxi.photobooker.entity.Photographer;
import com.xhxi.photobooker.mapper.ApplicationMapper;
import com.xhxi.photobooker.service.ApplicationService;
import com.xhxi.photobooker.service.PhotographerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class ApplicationServiceImpl implements ApplicationService {
    @Autowired
    private ApplicationMapper applicationMapper;
    @Autowired
    private PhotographerService photographerService;

    @Override
    public void submitApplication(Application application) {
        application.setStatus("PENDING");
        application.setCreateTime(new Date());
        applicationMapper.insert(application);
    }

    @Override
    public List<Application> getApplicationsByUserId(Long userId) {
        return applicationMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Application>().eq("user_id", userId));
    }

    @Override
    public List<Application> getAllApplications() {
        return applicationMapper.selectList(null);
    }

    @Override
    public void reviewApplication(Long id, String status, String remark) {
        Application application = applicationMapper.selectById(id);
        if (application != null) {
            application.setStatus(status);
            application.setRemark(remark);
            application.setUpdateTime(new Date());
            applicationMapper.updateById(application);
            if ("APPROVED".equals(status)) {
                // 避免重复插入
                Photographer exist = photographerService.findByUserId(application.getUserId());
                if (exist == null) {
                    Photographer photographer = new Photographer();
                    photographer.setUserId(application.getUserId());
                    photographer.setName(application.getRealName());
                    photographer.setPhone(application.getPhone());
                    photographer.setIntro(application.getIntro());
                    photographer.setAuthStatus(1); // 认证通过
                    photographer.setCreateTime(new Date());
                    photographerService.savePhotographer(photographer);
                }
            }
        }
    }

    @Override
    public Application getApplicationById(Long id) {
        return applicationMapper.selectById(id);
    }
} 