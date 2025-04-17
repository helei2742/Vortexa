package cn.com.vortexa.control_server.exception;

public class ControlServerException extends Exception{

    // 默认构造函数
    public ControlServerException() {
        super("nameserver start error.");
    }

    // 传入错误信息的构造函数
    public ControlServerException(String message) {
        super(message);
    }

    // 传入错误信息和异常原因的构造函数
    public ControlServerException(String message, Throwable cause) {
        super(message, cause);
    }

    // 传入异常原因的构造函数
    public ControlServerException(Throwable cause) {
        super(cause);
    }
}
