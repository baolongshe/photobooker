package com.xhxi.photobooker.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xhxi.photobooker.entity.PhotographerPackage;
import java.util.List;

public interface PhotographerPackageService extends IService<PhotographerPackage> {
    List<PhotographerPackage> listByPhotographerId(Long photographerId);
    PhotographerPackage update(PhotographerPackage pkg);
    boolean delete(Long id, Long photographerId);
    boolean setDefault(Long id, Long photographerId);
    boolean toggleStatus(Long id);
    List<String> getAllCategories();
}