package cn.com.vortexa.bot_platform.handler;

import cn.com.vortexa.common.dto.Result;
import com.alibaba.fastjson.JSON;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;


import java.io.IOException;
import java.io.OutputStreamWriter;

@Slf4j
//@Component
public class GlobalExceptionHandler implements HandlerExceptionResolver {
    @Override
    public ModelAndView resolveException(@NotNull HttpServletRequest request,
                                         @NotNull HttpServletResponse response,
                                         Object handler,
                                         @NotNull Exception e) {

        log.error("error", e);
        //设置默认错误视图
        ModelAndView mv = new ModelAndView();
        mv.setViewName("error");
        mv.addObject("code", 400);
        mv.addObject("msg", "系统异常，请稍后重试");

        if (handler instanceof HandlerMethod handlerMethod) {

            //获取方法上的ResponseBody注解
            ResponseBody responseBody = handlerMethod.getMethod().getDeclaredAnnotation(ResponseBody.class);

            //没有该注解，说明返回视图
            if (null == responseBody) {
                return mv;
            } else {
                //有注解，返回JSON字符串，参数异常
                Result resultInfo = new Result();
                resultInfo.setSuccess(false);
                resultInfo.setErrorMsg("系统异常, " + e.getCause().getMessage());

                //设置格式
                response.setContentType("application/json;charset=utf-8");

                //由于controller层也会返回数据，调用response.getWriter,
                // 所以这里使用response.getOutputStream()避免报错
                ServletOutputStream out = null;
                OutputStreamWriter ow = null;
                try {
                    out = response.getOutputStream();
                    ow = new OutputStreamWriter(out);
                    ow.append(JSON.toJSONString(resultInfo));
                    ow.flush();
                } catch (IOException ioException) {
                    log.error("unknown error", ioException);
                } finally {
                    try {
                        if (out != null) {
                            out.close();
                        }
                        if (ow != null)
                            ow.close();
                    } catch (IOException ioException) {
                        log.error("unknown error", ioException);
                    }
                }
            }
        }

        return null;
    }
}
