package cn.com.vortexa.bot_platform.wsController;

public class FrontWSException extends Exception {

    // 默认构造函数
    public FrontWSException() {
        super("front websocket exception.");
    }

    // 传入错误信息的构造函数
    public FrontWSException(String message) {
        super(message);
    }

    // 传入错误信息和异常原因的构造函数
    public FrontWSException(String message, Throwable cause) {
        super(message, cause);
    }

    // 传入异常原因的构造函数
    public FrontWSException(Throwable cause) {
        super(cause);
    }
}
