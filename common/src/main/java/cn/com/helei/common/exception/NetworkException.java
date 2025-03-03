package cn.com.helei.common.exception;

public class NetworkException extends RuntimeException{

    // 默认构造函数
    public NetworkException() {
        super("network error.");
    }

    // 传入错误信息的构造函数
    public NetworkException(String message) {
        super(message);
    }

    // 传入错误信息和异常原因的构造函数
    public NetworkException(String message, Throwable cause) {
        super(message, cause);
    }

    // 传入异常原因的构造函数
    public NetworkException(Throwable cause) {
        super(cause);
    }
}
