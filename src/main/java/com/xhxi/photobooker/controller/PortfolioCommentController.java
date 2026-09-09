package com.xhxi.photobooker.controller;

import com.xhxi.photobooker.entity.PortfolioComment;
import com.xhxi.photobooker.result.Result;
import com.xhxi.photobooker.service.PortfolioCommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/portfolio")
public class PortfolioCommentController {
    @Autowired
    private PortfolioCommentService commentService;

    // 分页获取评论
    @GetMapping("/{portfolioId}/comments")
    public Result<Map<String, Object>> getComments(@PathVariable Long portfolioId,
                                                   @RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        List<PortfolioComment> comments = commentService.getCommentsByPortfolioId(portfolioId, page, size);
        int total = commentService.countCommentsByPortfolioId(portfolioId);
        Map<String, Object> result = new HashMap<>();
        result.put("comments", comments);
        result.put("total", total);
        return Result.success(result);
    }

    // 新增评论/回复
    @PostMapping("/{portfolioId}/comment")
    public Result<PortfolioComment> addComment(@PathVariable Long portfolioId, @RequestBody PortfolioComment comment) {
        comment.setPortfolioId(portfolioId);

        return Result.success(commentService.addComment(comment));
    }

    // 删除评论
    @DeleteMapping("/comment/{commentId}")
    public Result<String> deleteComment(@PathVariable Long commentId, @RequestParam Long userId) {
        boolean success = commentService.deleteComment(commentId, userId);
        return success ? Result.success("删除成功") : Result.error("删除失败");
    }
} 