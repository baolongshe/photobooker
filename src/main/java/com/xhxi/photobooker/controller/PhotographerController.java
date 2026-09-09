package com.xhxi.photobooker.controller;

import com.xhxi.photobooker.entity.Order;
import com.xhxi.photobooker.entity.Photographer;
import com.xhxi.photobooker.entity.User;
import com.xhxi.photobooker.result.Result;
import com.xhxi.photobooker.service.OrderService;
import com.xhxi.photobooker.service.PhotographerService;
import com.xhxi.photobooker.service.UserService;
import com.xhxi.photobooker.vo.PhotographerNearbyVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/photo/photographer")
public class PhotographerController {
    @Autowired
    private PhotographerService photographerService;
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private UserService userService;
    @GetMapping("/list")
    public Result<List<Photographer>> list() {
        List<Photographer> list = photographerService.findAllPhotographer();
        return Result.success(list);
    }

    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody Photographer photographer) {
        photographerService.updatePhotographer(photographer.getId(), photographer);
        return Result.success(true);
    }
    @PostMapping("/deleteBatch")
    @Transactional
    public Result<Boolean> deleteBatch(@RequestBody List<Long> ids) {
        for (Long id : ids) {
            // 删除摄影师时，服务层已经处理了相关套餐的删除
            photographerService.deletePhotographer(id);
        }
        return Result.success(true);
    }

    @GetMapping("/orders/{photographerId}")
    public Result<List<Order>> listPhotographerOrders(@PathVariable Long photographerId) {
        List<Order> orders = orderService.listOrdersByPhotographerId(photographerId);
        for (Order order : orders) {
            User user = userService.findUserById(order.getUserId());
            order.setUserName(user != null ? user.getUsername() : "未知用户");
        }
        return Result.success(orders);
    }
    @GetMapping("/{id}")
    public Result<Photographer> getPhotographerById(@PathVariable Long id) {
        Photographer photographer = photographerService.findPhotographerById(id);
        return photographer != null ? Result.success(photographer) : Result.error("摄影师不存在");
    }
    @GetMapping("/byUser/{userId}")
    public Result<Photographer> getByUserId(@PathVariable Long userId) {
        Photographer photographer = photographerService.findByUserId(userId);
        if (photographer == null) {
            return Result.success(null); // 不是摄影师
        }
        return Result.success(photographer); // 是摄影师
    }

    @GetMapping("/map")
    public Result<List<Map<String, Object>>> getPhotographerMap() {
        List<Photographer> list = photographerService.getAllPhotographers();
        List<Map<String, Object>> result = list.stream()
            .filter(p -> p.getLocationVisible() == null || p.getLocationVisible() == 1)
            .map(p -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", p.getId());
                map.put("name", p.getName());
                map.put("latitude", p.getLatitude());
                map.put("longitude", p.getLongitude());
                map.put("phone",p.getPhone());
                return map;
            }).collect(Collectors.toList());
        return Result.success(result);
    }

    //附近摄影师检索：基于 Redis GEO 索引，按经纬度+半径返回附近摄影师及距离
    @GetMapping("/nearby")
    public Result<List<PhotographerNearbyVO>> nearby(@RequestParam Double latitude,
                                                     @RequestParam Double longitude,
                                                     @RequestParam(defaultValue = "5") Double radius,
                                                     @RequestParam(defaultValue = "20") Integer limit) {
        if (latitude == null || latitude < -90 || latitude > 90) {
            return Result.error("参数不合法: latitude 需在 [-90, 90]");
        }
        if (longitude == null || longitude < -180 || longitude > 180) {
            return Result.error("参数不合法: longitude 需在 [-180, 180]");
        }
        if (radius == null || radius <= 0 || radius > 100) {
            return Result.error("参数不合法: radius 需在 (0, 100] km");
        }
        if (limit == null || limit < 1 || limit > 50) {
            return Result.error("参数不合法: limit 需在 [1, 50]");
        }
        List<PhotographerNearbyVO> list = photographerService.searchNearbyPhotographers(latitude, longitude, radius, limit);
        return Result.success(list);
    }

    //修改摄影师位置可见性
    @PostMapping("/toggle-location-visible")
    public Result<?> toggleLocationVisible(@RequestParam Long id, @RequestParam Integer visible) {
        Photographer photographer = photographerService.findPhotographerById(id);
        if (photographer == null) return Result.error("摄影师不存在");
        photographer.setLocationVisible(visible);
        photographerService.updatePhotographer(id, photographer);
        return Result.success();
    }
    //修改定位
    @PostMapping("/update-location")
    public Result<?> updateLocation(@RequestParam Long id, @RequestParam Double latitude, @RequestParam Double longitude) {
        Photographer photographer = photographerService.findByUserId(id);
        if (photographer == null) return Result.error("摄影师不存在");
        
        Date now = new Date();
        photographer.setLatitude(latitude);
        photographer.setLongitude(longitude);
        photographer.setLastLocationUpdateTime(now);
        
        photographerService.updatePhotographer(id, photographer);
        return Result.success();
    }

    //设置摄影师工作状态
    @PostMapping("/set-working-status")
    public Result<?> setWorkingStatus(@RequestParam Long id, @RequestParam int status) {
        Photographer photographer = photographerService.findPhotographerById(id);
        if (photographer == null) return Result.error("摄影师不存在");
        photographer.setWorking(status);
        photographerService.updatePhotographer(id, photographer);
        return Result.success();
    }

}
