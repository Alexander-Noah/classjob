package org.example.backend.service;

public final class CrawlerCredentialException extends RuntimeException {
    public static final String USER_MESSAGE = "已保存的教务账号无法读取，请重新填写账号和密码后保存";

    public CrawlerCredentialException(Throwable cause) {
        super(USER_MESSAGE, cause);
    }
}
