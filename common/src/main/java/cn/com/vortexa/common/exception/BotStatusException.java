package cn.com.vortexa.common.exception;

public class BotStatusException extends RuntimeException{

    // 默认构造函数
    public BotStatusException() {
        super("DepinBotStatus error.");
    }

    // 传入错误信息的构造函数
    public BotStatusException(String message) {
        super(message);
    }

    // 传入错误信息和异常原因的构造函数
    public BotStatusException(String message, Throwable cause) {
        super(message, cause);
    }

    // 传入异常原因的构造函数
    public BotStatusException(Throwable cause) {
        super(cause);
    }
}
