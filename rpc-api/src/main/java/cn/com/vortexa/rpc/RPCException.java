package cn.com.vortexa.rpc;

public class RPCException extends Exception {

    // 默认构造函数
    public RPCException() {
        super("rpc exception.");
    }

    // 传入错误信息的构造函数
    public RPCException(String message) {
        super(message);
    }

    // 传入错误信息和异常原因的构造函数
    public RPCException(String message, Throwable cause) {
        super(message, cause);
    }

    // 传入异常原因的构造函数
    public RPCException(Throwable cause) {
        super(cause);
    }
}
