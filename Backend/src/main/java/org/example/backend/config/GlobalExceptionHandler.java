package org.example.backend.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.result.Result;
import org.example.backend.service.CrawlerCredentialException;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<Result> handleUploadTooLarge(
            org.springframework.web.multipart.MaxUploadSizeExceededException exception, HttpServletRequest request) {
        String message = "/files/avatars".equals(request.getServletPath()) || request.getRequestURI().endsWith("/files/avatars")
                ? "头像文件不能超过 2MB，请选择较小的图片后重试"
                : "上传文件超过服务器大小限制，请缩小文件后重试";
        log.warn("upload too large method={} uri={} requestId={}", request.getMethod(), request.getRequestURI(), MDC.get("requestId"));
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Result.error(413, message));
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<Result> handleInvalidRequestBody(org.springframework.http.converter.HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(Result.error("填写的数据格式有误，请检查数量和日期后重试"));
    }
    @ExceptionHandler(CrawlerCredentialException.class)
    public ResponseEntity<Result> handleCrawlerCredentialException(CrawlerCredentialException exception, HttpServletRequest request) {
        log.warn("crawler credentials require update method={} uri={} requestId={}",
                request.getMethod(), request.getRequestURI(), MDC.get("requestId"), exception);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Result.error(409, CrawlerCredentialException.USER_MESSAGE));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result> handleIllegalArgumentException(IllegalArgumentException exception, HttpServletRequest request) {
        log.warn("bad request method={} uri={} requestId={} message={}",
                request.getMethod(),
                request.getRequestURI(),
                MDC.get("requestId"),
                exception.getMessage());
        return ResponseEntity.badRequest().body(Result.error(exception.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Result> handleDataIntegrityViolationException(DataIntegrityViolationException exception, HttpServletRequest request) {
        String message = isDeleteConstraintRequest(request)
                ? "当前数据已被其他业务引用，请先解除关联后再删除"
                : "数据格式或约束不符合要求，请检查后重试";

        log.warn("data integrity violation method={} uri={} requestId={} message={}",
                request.getMethod(),
                request.getRequestURI(),
                MDC.get("requestId"),
                exception.getMostSpecificCause().getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Result.error(message));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Result> handleResponseStatusException(ResponseStatusException exception, HttpServletRequest request) {
        String message = exception.getReason() == null || exception.getReason().isBlank()
                ? "请求被拒绝"
                : exception.getReason();
        log.warn("request rejected method={} uri={} requestId={} status={} message={}",
                request.getMethod(),
                request.getRequestURI(),
                MDC.get("requestId"),
                exception.getStatusCode().value(),
                message);
        return ResponseEntity
                .status(exception.getStatusCode())
                .body(Result.error(message));
    }

    private boolean isDeleteConstraintRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return "DELETE".equalsIgnoreCase(request.getMethod())
                || (uri != null && uri.contains("batch/delete"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result> handleException(Exception exception, HttpServletRequest request) {
        String requestId = MDC.get("requestId");
        log.error("request failed method={} uri={} requestId={}",
                request.getMethod(),
                request.getRequestURI(),
                requestId,
                exception);
        String message = "服务暂时不可用，请稍后重试";
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(message));
    }
}
