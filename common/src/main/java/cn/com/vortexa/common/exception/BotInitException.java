package cn.com.vortexa.common.exception;

public class BotInitException extends Exception{

    // 默认构造函数
    public BotInitException() {
        super("Account initialization failed.");
    }

    // 传入错误信息的构造函数
    public BotInitException(String message) {
        super(message);
    }

    // 传入错误信息和异常原因的构造函数
    public BotInitException(String message, Throwable cause) {
        super(message, cause);
    }

    // 传入异常原因的构造函数
    public BotInitException(Throwable cause) {
        super(cause);
    }
}
