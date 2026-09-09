package com.xhxi.photobooker.service;

// 新建文件：src/main/java/com/xhxi/photobooker/service/AfterSalesServiceService.java


import com.baomidou.mybatisplus.extension.service.IService;
import com.xhxi.photobooker.entity.AfterSalesService;
import java.util.List;

public interface AfterSalesServiceService extends IService<AfterSalesService> {

    /**
     * 创建售后申请
     */
    AfterSalesService createService(AfterSalesService service);

    /**
     * 更新售后状态
     */
    boolean updateStatus(Long id, String status);

    /**
     * 根据订单 ID 查询售后记录
     */
    List<AfterSalesService> listByOrderId(Long orderId);

    /**
     * 根据用户 ID 查询售后记录
     */
    List<AfterSalesService> listByUserId(Long userId);

    /**
     * 批准退款申请
     */
    boolean approveRefund(Long serviceId, Long handlerId);

    /**
     * 拒绝退款申请
     */
    boolean rejectRefund(Long serviceId, Long handlerId, String reason);

    /**
     * 撤销售后申请
     */
    boolean cancelService(Long serviceId, Long userId);
}
