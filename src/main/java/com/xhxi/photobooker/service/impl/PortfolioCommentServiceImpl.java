package com.xhxi.photobooker.service.impl;

import com.xhxi.photobooker.entity.PortfolioComment;
import com.xhxi.photobooker.mapper.PortfolioCommentMapper;
import com.xhxi.photobooker.service.PortfolioCommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Date;
import java.util.List;

@Service
public class PortfolioCommentServiceImpl implements PortfolioCommentService {
    @Autowired
    private PortfolioCommentMapper commentMapper;

    @Override
    public List<PortfolioComment> getCommentsByPortfolioId(Long portfolioId, int page, int size) {
        int offset = (page - 1) * size;
        return commentMapper.selectByPortfolioId(portfolioId, offset, size);
    }

    @Override
    public int countCommentsByPortfolioId(Long portfolioId) {
        return commentMapper.countByPortfolioId(portfolioId);
    }

    @Override
    public PortfolioComment addComment(PortfolioComment comment) {
        comment.setCreateTime(new Date());
        comment.setUpdateTime(new Date());
        comment.setStatus(0);
        commentMapper.insert(comment);
        return comment;
    }

    @Override
    public boolean deleteComment(Long commentId, Long userId) {
        PortfolioComment comment = commentMapper.selectById(commentId);
        if (comment != null && comment.getUserId().equals(userId)) {
            comment.setStatus(1);
            comment.setUpdateTime(new Date());
            return commentMapper.updateById(comment) > 0;
        }
        return false;
    }
} 