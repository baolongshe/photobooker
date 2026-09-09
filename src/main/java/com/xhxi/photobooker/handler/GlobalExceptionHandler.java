package com.xhxi.photobooker.handler;

import com.xhxi.photobooker.result.Result;
import com.xhxi.photobooker.exception.BusinessException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.xhxi.photobooker.result.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.BindException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    //自定义异常
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException ex) {
        return Result.error(ex.getMessage());
    }


    // 处理所有未捕获的异常
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception ex) {
        ex.printStackTrace(); // 生产环境可去掉
        return Result.error("服务器异常：" + ex.getMessage());
    }

    // 处理参数校验异常
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<?> handleValidationException(Exception ex) {
        String msg = "参数校验失败";
        if (ex instanceof MethodArgumentNotValidException) {
            msg = ((MethodArgumentNotValidException) ex).getBindingResult().getAllErrors().get(0).getDefaultMessage();
        } else if (ex instanceof BindException) {
            msg = ((BindException) ex).getAllErrors().get(0).getDefaultMessage();
        }
        return Result.error(msg);
    }

} 