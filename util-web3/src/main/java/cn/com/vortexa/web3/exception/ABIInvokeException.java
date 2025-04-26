package cn.com.vortexa.web3.exception;


/**
 * @author helei
 * @since 2025-04-25
 */
public class ABIInvokeException extends Exception{

    // 默认构造函数
    public ABIInvokeException() {
        super("signature failed.");
    }

    // 传入错误信息的构造函数
    public ABIInvokeException(String message) {
        super(message);
    }

    // 传入错误信息和异常原因的构造函数
    public ABIInvokeException(String message, Throwable cause) {
        super(message, cause);
    }

    // 传入异常原因的构造函数
    public ABIInvokeException(Throwable cause) {
        super(cause);
    }
}
