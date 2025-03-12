package cn.com.vortexa.nameserver.exception;

public class NameserverException extends Exception{

    // 默认构造函数
    public NameserverException() {
        super("nameserver start error.");
    }

    // 传入错误信息的构造函数
    public NameserverException(String message) {
        super(message);
    }

    // 传入错误信息和异常原因的构造函数
    public NameserverException(String message, Throwable cause) {
        super(message, cause);
    }

    // 传入异常原因的构造函数
    public NameserverException(Throwable cause) {
        super(cause);
    }
}
