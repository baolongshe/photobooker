package com.xhxi.photobooker.service.impl;


import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xhxi.photobooker.entity.AfterSalesService;
import com.xhxi.photobooker.entity.Order;
import com.xhxi.photobooker.enums.ServiceStatus;
import com.xhxi.photobooker.mapper.AfterSalesServiceMapper;
import com.xhxi.photobooker.service.AfterSalesServiceService;
import com.xhxi.photobooker.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AfterSalesServiceServiceImpl extends ServiceImpl<AfterSalesServiceMapper, AfterSalesService>
        implements AfterSalesServiceService {

    @Autowired
    private OrderService orderService;

    @Autowired
    private AlipayClient alipayClient;

    @Override
    public AfterSalesService createService(AfterSalesService service) {
        service.setStatus(ServiceStatus.PENDING.getCode());
        service.setCreateTime(LocalDateTime.now());
        save(service);
        return service;
    }

    @Override
    public boolean updateStatus(Long id, String status) {
        AfterSalesService service = getById(id);
        if (service != null) {
            service.setStatus(status);
            if (ServiceStatus.COMPLETED.getCode().equals(status) || ServiceStatus.REJECTED.getCode().equals(status)) {
                service.setHandleTime(LocalDateTime.now());
            }
            updateById(service);
            return true;
        }
        return false;
    }

    @Override
    public List<AfterSalesService> listByOrderId(Long orderId) {
        LambdaQueryWrapper<AfterSalesService> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AfterSalesService::getOrderId, orderId)
                .orderByDesc(AfterSalesService::getCreateTime);
        return list(wrapper);
    }

    @Override
    public List<AfterSalesService> listByUserId(Long userId) {
        LambdaQueryWrapper<AfterSalesService> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AfterSalesService::getUserId, userId)
                .orderByDesc(AfterSalesService::getCreateTime);
        return list(wrapper);
    }

    @Override
    @Transactional
    public boolean approveRefund(Long serviceId, Long handlerId) {
        AfterSalesService service = getById(serviceId);
        if (service == null) {
            throw new RuntimeException("售后服务记录不存在");
        }

        if (!"REFUND".equalsIgnoreCase(service.getServiceType())) {
            throw new RuntimeException("该服务不是退款申请");
        }

        if (!ServiceStatus.PENDING.getCode().equalsIgnoreCase(service.getStatus())) {
            throw new RuntimeException("该申请已处理，无法重复操作");
        }

        // 验证订单是否存在
        Order order = orderService.selectByOrderId(service.getOrderId());
        if (order == null) {
            throw new RuntimeException("关联订单不存在");
        }

        // 验证退款金额
        if (service.getRefundAmount() == null || service.getRefundAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("退款金额不合法");
        }
        if (service.getRefundAmount().compareTo(order.getTotalPrice()) > 0) {
            throw new RuntimeException("退款金额不能超过订单金额");
        }

        // 更新状态为处理中
        service.setStatus(ServiceStatus.PROCESSING.getCode());
        service.setHandlerId(handlerId);
        updateById(service);

        try {
            // 1. 调用支付宝退款接口
            AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
            String bizContent = "{" +
                    "\"out_trade_no\":\"" + service.getOrderId() + "\"," +
                    "\"refund_amount\":\"" + service.getRefundAmount() + "\"," +
                    "\"refund_reason\":\"" + (service.getRefundReason() != null ? service.getRefundReason() : "售后退款") + "\"" +
                    "}";
            request.setBizContent(bizContent);

            AlipayTradeRefundResponse alipayResponse = alipayClient.execute(request);

            // 2. 验证支付宝退款结果
            if (!alipayResponse.isSuccess()) {
                throw new RuntimeException("支付宝退款失败：" + alipayResponse.getMsg() + " - " + alipayResponse.getSubMsg());
            }

            // 3. 退款成功，更新订单状态为退款中
            orderService.handleRefundOrder(service.getOrderId());

            // 4. 更新售后状态为已完成
            service.setStatus(ServiceStatus.COMPLETED.getCode());
            service.setHandleTime(LocalDateTime.now());
            service.setHandlerResponse("退款申请已批准，退款金额 ¥" + service.getRefundAmount() + " 已成功退回，支付宝交易号：" + alipayResponse.getTradeNo());
            updateById(service);

            // 5. 完成退款，将订单状态更新为已退款
            orderService.completeRefundOrder(service.getOrderId());

            return true;
        } catch (AlipayApiException e) {
            // 支付宝接口调用异常，回滚状态
            service.setStatus(ServiceStatus.REJECTED.getCode());
            service.setHandlerResponse("支付宝接口调用失败：" + e.getMessage());
            service.setHandleTime(LocalDateTime.now());
            updateById(service);
            throw new RuntimeException("退款处理失败：" + e.getMessage());
        } catch (Exception e) {
            // 其他异常，回滚状态
            service.setStatus(ServiceStatus.REJECTED.getCode());
            service.setHandlerResponse("退款处理失败：" + e.getMessage());
            service.setHandleTime(LocalDateTime.now());
            updateById(service);
            throw new RuntimeException("退款处理失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public boolean rejectRefund(Long serviceId, Long handlerId, String reason) {
        AfterSalesService service = getById(serviceId);
        if (service == null) {
            throw new RuntimeException("售后服务记录不存在");
        }

        if (!ServiceStatus.PENDING.getCode().equals(service.getStatus()) && !ServiceStatus.PROCESSING.getCode().equals(service.getStatus())) {
            throw new RuntimeException("该申请已处理，无法重复操作");
        }

        service.setStatus(ServiceStatus.REJECTED.getCode());
        service.setHandlerId(handlerId);
        service.setHandlerResponse(reason);
        service.setHandleTime(LocalDateTime.now());
        updateById(service);

        return true;
    }

    @Override
    @Transactional
    public boolean cancelService(Long serviceId, Long userId) {
        AfterSalesService service = getById(serviceId);
        if (service == null) {
            throw new RuntimeException("售后服务记录不存在");
        }

        if (!service.getUserId().equals(userId)) {
            throw new RuntimeException("无权撤销该申请");
        }

        if (!ServiceStatus.PENDING.getCode().equals(service.getStatus())) {
            throw new RuntimeException("该申请已开始处理，无法撤销");
        }

        service.setStatus(ServiceStatus.CANCELLED.getCode());
        service.setHandleTime(LocalDateTime.now());
        updateById(service);

        return true;
    }
}
