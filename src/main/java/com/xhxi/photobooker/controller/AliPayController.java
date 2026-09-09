package com.xhxi.photobooker.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.xhxi.photobooker.entity.Order;
import com.xhxi.photobooker.enums.OrderStatus;
import com.xhxi.photobooker.result.Result;
import com.xhxi.photobooker.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/pay")
public class AliPayController {
    private static final Logger logger = LoggerFactory.getLogger(AliPayController.class);

    @Autowired
    private AlipayClient alipayClient;
    @Autowired
    private OrderService orderService;
    @Value("${alipay.return-url}")
    private String returnUrl;
    @Value("${alipay.notify-url}")
    private String notifyUrl;

    @Value("${alipay.public-key}")
    private String alipayPublicKey;

    private final String charset = "UTF-8";
    private final String signType = "RSA2";

    //发起支付
    @PostMapping("/create")
    public String createPay(
            @RequestParam String orderId,
            @RequestParam String totalAmount
    ) throws AlipayApiException {
        logger.info("收到支付请求，订单ID: {}, 金额: {}", orderId, totalAmount);
        
        long longOrderId = Long.parseLong(orderId);
        //1.校验订单状态（确保为待支付）
        Order order = orderService.selectByOrderId(longOrderId);
        logger.debug("查询到的订单: {}", order);
        
        if(order == null){
            throw new RuntimeException("订单不存在，订单ID: " + orderId);
        }
        
        if(!OrderStatus.PENDING.equals(order.getStatus())){
            throw new RuntimeException("订单状态异常，当前状态: " + order.getStatus() + "，期望状态: PENDING");
        }

        //2.构建支付宝请求参数
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setReturnUrl(returnUrl);
        request.setNotifyUrl(notifyUrl);

        // 3. 组装业务参数（JSON格式）
        String bizContent = "{" +
                "\"out_trade_no\":\"" + orderId + "\"," +  // 商户订单号
                "\"total_amount\":\"" + totalAmount + "\"," +  // 支付金额（单位：元）
                "\"subject\":\"订单" + orderId + "\"," +  // 订单标题
                "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"" +  // 支付产品类型
                "}";
        request.setBizContent(bizContent);

        // 4.调用支付宝接口，获取支付表单
        return alipayClient.pageExecute(request).getBody();
    }

    //同步接口和异步接口

    //异步通知接口（支付宝主动调用）
    @PostMapping("/notify")
    public String handleNotify(HttpServletRequest request)throws AlipayApiException{
        // 1.解析请求参数
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key,values) ->{
            params.put(key,values[0]);
        });

        // 2.验签（关键：验证通知是否来自支付宝，防止伪造）
        boolean signVerified = AlipaySignature.rsaCheckV1(
                params, alipayPublicKey,charset,signType
        );
        if(!signVerified){
            return "fail";
        }

        // 3.处理支付结果
        String tradeStatus = params.get("trade_status");
        String totalAmount = params.get("total_amount");
        String outTradeNo = params.get("out_trade_no");

        // 4.校验金额和订单状态（防止金额被篡改）
        long no = Long.parseLong(outTradeNo);
        Order order = orderService.selectByOrderId(no);
        if(order == null || !order.getTotalPrice().toString().equals(totalAmount)){
            return "fail";
        }

        // 5.支付成功：更新订单状态
        if("TRADE_SUCCESS".equals(tradeStatus)){
            orderService.finishOrder(no);
        }

        return "success"; //支付宝主动调用，必须返回success,否则支付宝会重复通知
    }

    //同步回调（仅用于前端跳转，不处理业务）
    @GetMapping("/return")
    public void handleReturn(HttpServletRequest request, HttpServletResponse response) throws IOException {
        //跳转到前端Vue应用的支付成功页面
        String redirectUrl = "http://localhost:3000/#/payment-success?out_trade_no=" + request.getParameter("out_trade_no") + 
                           "&total_amount=" + request.getParameter("total_amount");
        response.sendRedirect(redirectUrl);
    }

    /**
     * 支付宝退款接口
     */
    @PostMapping("/refund")
    public Result<Map<String, Object>> refund(@RequestParam String orderId, 
                                              @RequestParam String refundAmount,
                                              @RequestParam(required = false) String refundReason) {
        try {
            // 1. 查询订单信息
            long orderNo = Long.parseLong(orderId);
            Order order = orderService.selectByOrderId(orderNo);
            if (order == null) {
                return Result.error("订单不存在");
            }

            // 2. 验证退款金额
            double amount = Double.parseDouble(refundAmount);
            if (amount <= 0 || amount > order.getTotalPrice().doubleValue()) {
                return Result.error("退款金额不合法");
            }

            // 3. 调用支付宝退款接口
            AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
            String bizContent = "{" +
                    "\"out_trade_no\":\"" + orderId + "\"," +
                    "\"refund_amount\":\"" + amount + "\"," +
                    (refundReason != null ? "\"refund_reason\":\"" + refundReason + "\"" : "") +
                    "}";
            request.setBizContent(bizContent);

            AlipayTradeRefundResponse response = alipayClient.execute(request);
            
            Map<String, Object> result = new HashMap<>();
            if (response.isSuccess()) {
                result.put("tradeNo", response.getTradeNo());
                result.put("refundFee", response.getRefundFee());
                result.put("gmtRefundPay", response.getGmtRefundPay());
                return Result.success(result);
            } else {
                return Result.error("退款失败：" + response.getMsg() + " - " + response.getSubMsg());
            }
        } catch (AlipayApiException e) {
            logger.error("退款异常: {}", e.getMessage(), e);
            return Result.error("退款异常：" + e.getMessage());
        } catch (Exception e) {
            logger.error("系统错误: {}", e.getMessage(), e);
            return Result.error("系统错误：" + e.getMessage());
        }
    }
}
