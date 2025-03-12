package cn.com.vortexa.common.exception;

public class BotStartException extends Exception{

    // 默认构造函数
    public BotStartException() {
        super("Bot start failed.");
    }

    // 传入错误信息的构造函数
    public BotStartException(String message) {
        super(message);
    }

    // 传入错误信息和异常原因的构造函数
    public BotStartException(String message, Throwable cause) {
        super(message, cause);
    }

    // 传入异常原因的构造函数
    public BotStartException(Throwable cause) {
        super(cause);
    }
}
