package com.xhxi.photobooker.controller;



import com.xhxi.photobooker.context.BaseContext;
import com.xhxi.photobooker.entity.OrderComment;
import com.xhxi.photobooker.entity.User;
import com.xhxi.photobooker.result.Result;
import com.xhxi.photobooker.service.OrderCommentService;
import com.xhxi.photobooker.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/order-comment")
public class OrderCommentController {

    @Autowired
    private OrderCommentService orderCommentService;
    
    @Autowired
    private UserService userService;

    /**
     * 创建评价
     */
    @PostMapping
    public Result<OrderComment> createComment(@RequestBody OrderComment comment) {
        comment.setUserId(BaseContext.getCurrentId());
        OrderComment saved = orderCommentService.createComment(comment);
        return Result.success(saved);
    }

    /**
     * 根据订单 ID 查询评价
     */
    @GetMapping("/order/{orderId}")
    public Result<OrderComment> getByOrderId(@PathVariable Long orderId) {
        OrderComment comment = orderCommentService.getByOrderId(orderId);
        if (comment == null) {
            return Result.error("该订单暂无评价");
        }
        return Result.success(comment);
    }

    /**
     * 查询当前用户的评价列表
     */
    @GetMapping("/user")
    public Result<List<OrderComment>> listByCurrentUser() {
        List<OrderComment> list = orderCommentService.listByUserId(BaseContext.getCurrentId());
        return Result.success(list);
    }

    /**
     * 根据摄影师 ID 查询评价列表
     */
    @GetMapping("/photographer/{photographerId}")
    public Result<List<OrderComment>> listByPhotographerId(@PathVariable Long photographerId) {
        List<OrderComment> list = orderCommentService.listByPhotographerId(photographerId);
        return Result.success(list);
    }

    /**
     * 获取摄影师平均评分（带分页和用户信息）
     */
    @GetMapping("/photographer/{photographerId}/rating")
    public Result<Map<String, Object>> getAverageRating(
        @PathVariable Long photographerId,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int pageSize
    ) {
        // 获取所有评价（用于计算平均分）
        List<OrderComment> allComments = orderCommentService.listByPhotographerId(photographerId);
        
        // 计算平均评分
        Double average = 0.0;
        if (allComments != null && !allComments.isEmpty()) {
            double totalRating = allComments.stream()
                .mapToInt(OrderComment::getRating)
                .sum();
            average = totalRating / allComments.size();
        }
        
        // 分页查询
        int startIndex = (page - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, allComments.size());
        List<OrderComment> pageComments = allComments.subList(startIndex, endIndex);
        
        // 为每个评价添加用户信息
        List<Map<String, Object>> commentsWithUserInfo = pageComments.stream()
            .map(comment -> {
                Map<String, Object> commentMap = new HashMap<>();
                commentMap.put("id", comment.getId());
                commentMap.put("orderId", comment.getOrderId());
                commentMap.put("userId", comment.getUserId());
                commentMap.put("photographerId", comment.getPhotographerId());
                commentMap.put("rating", comment.getRating());
                commentMap.put("content", comment.getContent());
                commentMap.put("isAnonymous", comment.getIsAnonymous());
                commentMap.put("createTime", comment.getCreateTime());
                
                // 添加用户信息
                User user = userService.findUserById(comment.getUserId());
                if (user != null) {
                    commentMap.put("userName", user.getUsername());
                    commentMap.put("userAvatar", user.getAvatar());
                } else {
                    commentMap.put("userName", "用户");
                    commentMap.put("userAvatar", null);
                }
                
                return commentMap;
            })
            .collect(java.util.stream.Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("average", average);
        result.put("count", allComments.size());
        result.put("comments", commentsWithUserInfo);
        result.put("hasMore", endIndex < allComments.size());
        
        return Result.success(result);
    }

    /**
     * 删除评价
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteComment(@PathVariable Long id) {
        OrderComment comment = orderCommentService.getById(id);
        if (comment != null) {
            comment.setStatus(1);
            comment.setUpdateTime(LocalDateTime.now());
            orderCommentService.updateById(comment);
        }
        return Result.success();
    }
}
