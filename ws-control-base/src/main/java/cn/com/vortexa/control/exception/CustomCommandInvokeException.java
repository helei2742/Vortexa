package cn.com.vortexa.control.exception;

public class CustomCommandInvokeException extends RuntimeException {

    // 默认构造函数
    public CustomCommandInvokeException() {
        super("custom command exception.");
    }

    // 传入错误信息的构造函数
    public CustomCommandInvokeException(String message) {
        super(message);
    }

    // 传入错误信息和异常原因的构造函数
    public CustomCommandInvokeException(String message, Throwable cause) {
        super(message, cause);
    }

    // 传入异常原因的构造函数
    public CustomCommandInvokeException(Throwable cause) {
        super(cause);
    }
}
