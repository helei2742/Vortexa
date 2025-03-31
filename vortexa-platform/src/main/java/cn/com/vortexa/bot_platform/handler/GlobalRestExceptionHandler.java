package cn.com.vortexa.bot_platform.handler;

import cn.com.vortexa.common.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalRestExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result> handleException(Exception ex) {
        log.error("error", ex);
        Result response = new Result();
        response.setSuccess(false);
        response.setErrorMsg(ex.getMessage());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
