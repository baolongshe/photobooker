package com.xhxi.photobooker.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xhxi.photobooker.context.BaseContext;
import com.xhxi.photobooker.entity.AfterSalesService;
import com.xhxi.photobooker.result.Result;
import com.xhxi.photobooker.service.AfterSalesServiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/after-sales")
public class AfterSalesController {

    @Autowired
    private AfterSalesServiceService afterSalesServiceService;

    /**
     * 提交售后申请
     */
    @PostMapping
    public Result<AfterSalesService> createService(@RequestBody AfterSalesService service) {
        service.setUserId(BaseContext.getCurrentId());
        AfterSalesService saved = afterSalesServiceService.createService(service);
        return Result.success(saved);
    }

    /**
     * 根据订单 ID 查询售后记录
     */
    @GetMapping("/order/{orderId}")
    public Result<List<AfterSalesService>> listByOrderId(@PathVariable Long orderId) {
        List<AfterSalesService> list = afterSalesServiceService.listByOrderId(orderId);
        return Result.success(list);
    }

    /**
     * 查询当前用户的售后记录
     */
    @GetMapping("/user")
    public Result<List<AfterSalesService>> listByCurrentUser() {
        Long userId = BaseContext.getCurrentId();
        List<AfterSalesService> list = afterSalesServiceService.listByUserId(userId);
        return Result.success(list);
    }

    /**
     * 查询所有退款申请（管理员）
     */
    @GetMapping("/list")
    public Result<List<AfterSalesService>> listAllRefunds(@RequestParam(required = false) String serviceType) {
        LambdaQueryWrapper<AfterSalesService> wrapper = new LambdaQueryWrapper<>();
        if (serviceType != null && !serviceType.isEmpty()) {
            wrapper.eq(AfterSalesService::getServiceType, serviceType);
        }
        wrapper.orderByDesc(AfterSalesService::getCreateTime);
        List<AfterSalesService> list = afterSalesServiceService.list(wrapper);
        return Result.success(list);
    }

    /**
     * 更新售后状态
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam String status) {
        afterSalesServiceService.updateStatus(id, status);
        return Result.success();
    }

    /**
     * 处理售后申请（管理员/客服）
     */
    @PutMapping("/{id}/handle")
    public Result<Void> handleService(@PathVariable Long id,
                                      @RequestParam String response,
                                      @RequestParam String status) {
        AfterSalesService service = afterSalesServiceService.getById(id);
        service.setHandlerId(BaseContext.getCurrentId());
        service.setHandlerResponse(response);
        service.setStatus(status);
        service.setHandleTime(java.time.LocalDateTime.now());
        afterSalesServiceService.updateById(service);
        return Result.success();
    }

    /**
     * 批准退款申请
     */
    @PostMapping("/{id}/approve-refund")
    public Result<Void> approveRefund(@PathVariable Long id) {
        try {
            afterSalesServiceService.approveRefund(id, BaseContext.getCurrentId());
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 拒绝退款申请
     */
    @PostMapping("/{id}/reject-refund")
    public Result<Void> rejectRefund(@PathVariable Long id,
                                     @RequestParam String reason) {
        try {
            afterSalesServiceService.rejectRefund(id, BaseContext.getCurrentId(), reason);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 撤销售后申请
     */
    @PostMapping("/{id}/cancel")
    public Result<Void> cancelService(@PathVariable Long id) {
        try {
            afterSalesServiceService.cancelService(id, BaseContext.getCurrentId());
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
