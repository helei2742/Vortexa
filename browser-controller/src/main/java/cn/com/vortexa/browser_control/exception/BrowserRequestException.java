package cn.com.vortexa.browser_control.exception;

public class BrowserRequestException extends RuntimeException{

    // 默认构造函数
    public BrowserRequestException() {
        super("request browser error.");
    }

    // 传入错误信息的构造函数
    public BrowserRequestException(String message) {
        super(message);
    }

    // 传入错误信息和异常原因的构造函数
    public BrowserRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    // 传入异常原因的构造函数
    public BrowserRequestException(Throwable cause) {
        super(cause);
    }
}
