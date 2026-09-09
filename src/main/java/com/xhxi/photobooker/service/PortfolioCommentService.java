package com.xhxi.photobooker.service;

import com.xhxi.photobooker.entity.PortfolioComment;
import java.util.List;

public interface PortfolioCommentService {
    List<PortfolioComment> getCommentsByPortfolioId(Long portfolioId, int page, int size);
    int countCommentsByPortfolioId(Long portfolioId);
    PortfolioComment addComment(PortfolioComment comment);
    boolean deleteComment(Long commentId, Long userId);
} 