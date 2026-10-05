package cn.iyque.sales.controller;

import cn.iyque.domain.ResponseResult;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Order(-100)
@lombok.extern.slf4j.Slf4j
@RestControllerAdvice(assignableTypes=SalesController.class)
public class SalesExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseResult<?> status(ResponseStatusException e) { return new ResponseResult<>(e.getStatus().value(),e.getReason(),null); }
    @ExceptionHandler({DataIntegrityViolationException.class,ObjectOptimisticLockingFailureException.class})
    public ResponseResult<?> conflict(Exception e) { return new ResponseResult<>(409,"重复操作或资料已更新，请刷新后重试",null); }
    @ExceptionHandler({IllegalArgumentException.class,org.springframework.http.converter.HttpMessageNotReadableException.class})
    public ResponseResult<?> invalid(Exception e) { return new ResponseResult<>(400,"输入格式不正确，请检查后重试",null); }
    @ExceptionHandler(Exception.class)
    public ResponseResult<?> unavailable(Exception e) {
        String reference=java.util.UUID.randomUUID().toString().substring(0,8);
        Throwable cause=e; while(cause.getCause()!=null && cause.getCause()!=cause) cause=cause.getCause();
        log.error("Sales failure ref={} type={} root={} frames={}",reference,e.getClass().getSimpleName(),cause.getClass().getSimpleName(),java.util.Arrays.stream(cause.getStackTrace()).limit(6).toArray());
        if(cause instanceof java.sql.SQLException) log.error("Sales SQL failure ref={} state={} code={}",reference,((java.sql.SQLException)cause).getSQLState(),((java.sql.SQLException)cause).getErrorCode());
        return new ResponseResult<>(500,"操作未完成，请稍后重试；错误编号："+reference,null);
    }
}
