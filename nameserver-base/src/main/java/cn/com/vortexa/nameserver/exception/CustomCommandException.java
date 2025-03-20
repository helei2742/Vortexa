package cn.com.vortexa.nameserver.exception;

public class CustomCommandException extends Exception{

    // 默认构造函数
    public CustomCommandException() {
        super("custom command exception.");
    }

    // 传入错误信息的构造函数
    public CustomCommandException(String message) {
        super(message);
    }

    // 传入错误信息和异常原因的构造函数
    public CustomCommandException(String message, Throwable cause) {
        super(message, cause);
    }

    // 传入异常原因的构造函数
    public CustomCommandException(Throwable cause) {
        super(cause);
    }
}
