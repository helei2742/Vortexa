package cn.com.vortexa.script_node.exception;


import com.github.dockerjava.api.exception.BadRequestException;

/**
 * @author helei
 * @since 2025-04-21
 */
public class BotRuntimeException extends BadRequestException {
    // 默认构造函数
    public BotRuntimeException() {
        super("Bot runtime exception.");
    }

    // 传入错误信息的构造函数
    public BotRuntimeException(String message) {
        super(message);
    }

    // 传入错误信息和异常原因的构造函数
    public BotRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    // 传入异常原因的构造函数
    public BotRuntimeException(Throwable cause) {
        super(cause);
    }
}
