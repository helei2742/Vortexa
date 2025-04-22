package cn.com.vortexa.web3.exception;

public class SignatureException extends Exception{

    // 默认构造函数
    public SignatureException() {
        super("signature failed.");
    }

    // 传入错误信息的构造函数
    public SignatureException(String message) {
        super(message);
    }

    // 传入错误信息和异常原因的构造函数
    public SignatureException(String message, Throwable cause) {
        super(message, cause);
    }

    // 传入异常原因的构造函数
    public SignatureException(Throwable cause) {
        super(cause);
    }
}
