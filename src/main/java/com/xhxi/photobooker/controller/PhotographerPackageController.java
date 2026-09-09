package com.xhxi.photobooker.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xhxi.photobooker.entity.PhotographerPackage;
import com.xhxi.photobooker.result.Result;
import com.xhxi.photobooker.service.PhotographerPackageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/photo/photographer/package")
public class PhotographerPackageController {

    @Autowired
    private PhotographerPackageService packageService;

    // 获取摄影师所有套餐
    @GetMapping("/list/{photographerId}")
    public Result<List<PhotographerPackage>> list(@PathVariable Long photographerId) {
        return Result.success(packageService.listByPhotographerId(photographerId));
    }

    // 获取摄影师指定分类的套餐
    @GetMapping("/list/{photographerId}/category/{category}")
    public Result<List<PhotographerPackage>> listByCategory(
            @PathVariable Long photographerId, 
            @PathVariable String category) {
        QueryWrapper<PhotographerPackage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("photographer_id", photographerId)
                   .eq("category", category)
                   .eq("status", 1) // 只查询上架的套餐
                   .orderByAsc("price");
        return Result.success(packageService.list(queryWrapper));
    }

    // 获取所有套餐分类
    @GetMapping("/categories")
    public Result<List<String>> getCategories() {
        return Result.success(packageService.getAllCategories());
    }

    // 按价格范围查询套餐
    @GetMapping("/list/{photographerId}/price")
    public Result<List<PhotographerPackage>> listByPriceRange(
            @PathVariable Long photographerId,
            @RequestParam(defaultValue = "0") BigDecimal minPrice,
            @RequestParam(defaultValue = "9999") BigDecimal maxPrice) {
        QueryWrapper<PhotographerPackage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("photographer_id", photographerId)
                   .between("price", minPrice, maxPrice)
                   .eq("status", 1)
                   .orderByAsc("price");
        return Result.success(packageService.list(queryWrapper));
    }

    // 新增套餐
    @PostMapping("/add")
    public Result<PhotographerPackage> add(@RequestBody PhotographerPackage pkg) {
        // 设置默认值
        if (pkg.getStatus() == null) {
            pkg.setStatus(1); // 默认上架
        }
        if (pkg.getIsDefault() == null) {
            pkg.setIsDefault(0); // 默认非默认套餐
        }
        return Result.success(packageService.save(pkg) ? pkg : null);
    }

    // 修改套餐
    @PutMapping("/update")
    public Result<PhotographerPackage> update(@RequestBody PhotographerPackage pkg) {
        return Result.success(packageService.update(pkg));
    }

    // 删除套餐
    @DeleteMapping("/delete/{id}/{photographerId}")
    public Result<?> delete(@PathVariable Long id, @PathVariable Long photographerId) {
        return packageService.delete(id, photographerId) ? Result.success() : Result.error("删除失败");
    }

    // 设为默认套餐
    @PostMapping("/set-default/{id}/{photographerId}")
    public Result<?> setDefault(@PathVariable Long id, @PathVariable Long photographerId) {
        return packageService.setDefault(id, photographerId) ? Result.success() : Result.error("设置失败");
    }

    // 上架/下架套餐
    @PostMapping("/toggle-status/{id}")
    public Result<?> toggleStatus(@PathVariable Long id) {
        return packageService.toggleStatus(id) ? Result.success() : Result.error("操作失败");
    }

    // 获取套餐详情
    @GetMapping("/detail/{id}")
    public Result<PhotographerPackage> getDetail(@PathVariable Long id) {
        return Result.success(packageService.getById(id));
    }
}
