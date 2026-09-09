package com.xhxi.photobooker.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xhxi.photobooker.entity.PhotographerPackage;
import com.xhxi.photobooker.mapper.PhotographerPackageMapper;
import com.xhxi.photobooker.service.PhotographerPackageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PhotographerPackageServiceImpl extends ServiceImpl<PhotographerPackageMapper, PhotographerPackage> implements PhotographerPackageService {
    
    @Autowired
    private PhotographerPackageMapper packageMapper;

    @Override
    public List<PhotographerPackage> listByPhotographerId(Long photographerId) {
        return packageMapper.selectList(new QueryWrapper<PhotographerPackage>().eq("photographer_id", photographerId));
    }

    @Override
    public PhotographerPackage update(PhotographerPackage pkg) {
        packageMapper.updateById(pkg);
        return pkg;
    }

    @Override
    public boolean delete(Long id, Long photographerId) {
        return packageMapper.delete(new QueryWrapper<PhotographerPackage>().eq("id", id).eq("photographer_id", photographerId)) > 0;
    }

    @Override
    public boolean setDefault(Long id, Long photographerId) {
        // 先将该摄影师所有套餐的is_default设为0
        packageMapper.update(new PhotographerPackage() {{ setIsDefault(0); }},
                new QueryWrapper<PhotographerPackage>().eq("photographer_id", photographerId));
        // 再将指定套餐设为默认
        PhotographerPackage pkg = new PhotographerPackage();
        pkg.setId(id);
        pkg.setIsDefault(1);
        return packageMapper.updateById(pkg) > 0;
    }

    @Override
    public boolean toggleStatus(Long id) {
        PhotographerPackage pkg = packageMapper.selectById(id);
        if (pkg != null) {
            pkg.setStatus(pkg.getStatus() == 1 ? 0 : 1);
            return packageMapper.updateById(pkg) > 0;
        }
        return false;
    }

    @Override
    public List<String> getAllCategories() {
        QueryWrapper<PhotographerPackage> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("DISTINCT category")
                   .isNotNull("category")
                   .ne("category", "");
        List<PhotographerPackage> packages = packageMapper.selectList(queryWrapper);
        return packages.stream()
                      .map(PhotographerPackage::getCategory)
                      .distinct()
                      .collect(Collectors.toList());
    }
}