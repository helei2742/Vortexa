package cn.com.vortexa.common.util.http;

import cn.com.vortexa.common.constants.HttpMethod;
import cn.com.vortexa.common.entity.ProxyInfo;
import cn.com.vortexa.common.exception.NetworkException;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okio.BufferedSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.Proxy;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;


@Slf4j
public class RestApiClient {

    private static final int RETRY_TIMES = 1;

    public static int connectTimeout = 25;
    public static int readTimeout = 120;
    public static int writeTimeout = 60;

    @Getter
    private final OkHttpClient okHttpClient;

    private final ExecutorService executorService;

    public RestApiClient(
            ProxyInfo proxy,
            ExecutorService executorService
    ) {
        this.executorService = executorService;
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder
                // 连接超时
                .connectTimeout(connectTimeout, TimeUnit.SECONDS)
                // 读取超时
                .readTimeout(readTimeout, TimeUnit.SECONDS)
                // 写入超时
                .writeTimeout(writeTimeout, TimeUnit.SECONDS);

        if (proxy != null) {
            builder.proxy(new Proxy(Proxy.Type.HTTP, proxy.generateAddress()));
            if (StrUtil.isNotBlank(proxy.getUsername())) {
                builder.proxyAuthenticator((route, response) -> {
                    String credential = Credentials.basic(proxy.getUsername(), proxy.getPassword());
                    return response.request().newBuilder()
                            .header("Proxy-Authorization", credential)
                            .build();
                });
            }
        }
        this.okHttpClient = builder.build();
    }

    /**
     * 发送请求，如果有asKey参数不为null，则会鉴权
     *
     * @param url     url
     * @param method  method
     * @param headers headers
     * @param params  params
     * @param body    body
     * @return CompletableFuture<JSONObject>
     */
    public CompletableFuture<String> request(
            String url,
            HttpMethod method,
            Map<String, String> headers,
            JSONObject params,
            JSONObject body
    ) {
        return request(url, method, headers, params, body, 1);
    }

    /**
     * 发送请求，如果有asKey参数不为null，则会鉴权
     *
     * @param url     url
     * @param method  method
     * @param headers headers
     * @param params  params
     * @param body    body
     * @return CompletableFuture<JSONObject>
     */
    public CompletableFuture<String> request(
            String url,
            HttpMethod method,
            Map<String, String> headers,
            JSONObject params,
            JSONObject body,
            int retryTimes
    ) {
        return CompletableFuture.supplyAsync(() -> {

            Request request = null;
            try {
                request = buildRequest(url, method, headers, params, body);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return normalRequest(url, method, request, retryTimes);
        }, executorService);
    }

    public CompletableFuture<List<String>> streamRequest(
            String url,
            HttpMethod method,
            Map<String, String> headers,
            JSONObject params,
            JSONObject body
    ) {
        return CompletableFuture.supplyAsync(() -> {
            Request request = null;
            try {
                request = buildRequest(url, method, headers, params, body);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }


            Exception exception = null;
            for (int i = 0; i < RETRY_TIMES; i++) {
                // 发送请求并获取响应
                try (Response response = okHttpClient.newCall(request).execute()) {
                    return streamRequest(url, response);
                } catch (SocketTimeoutException e) {
                    log.warn("请求[{}]超时，尝试重新请求 [{}}/{}],", url, i, RETRY_TIMES);
                    exception = e;
                } catch (IOException e) {
                    throw new RuntimeException("请求url [" + url + "] 失败", e);
                }
            }

            throw new RuntimeException("请求重试次数超过限制, " + RETRY_TIMES, exception);
        });
    }

    @NotNull
    private static Request buildRequest(
            String url,
            HttpMethod method,
            Map<String, String> headers,
            JSONObject params,
            JSONObject body
    ) throws IOException {
        // 创建表单数据
        StringBuilder queryString = new StringBuilder();

        String requestUrl = url;
        if (params != null) {
            params.keySet().forEach(key -> {
                queryString.append(key).append("=").append(params.get(key)).append("&");
            });

            if (!queryString.isEmpty()) {
                queryString.deleteCharAt(queryString.length() - 1);
            }
            requestUrl = url + "?" + queryString;
        }


        Request.Builder builder = new Request.Builder();

        RequestBody requestBody = null;
        if (body != null) {
            String contentType = "application/json";
            if (headers != null) {
                contentType = headers.getOrDefault(
                        "Content-Type",
                        headers.getOrDefault("content-type", "application/json; charset=utf-8")
                ).toLowerCase();
            } else {
                headers = new HashMap<>(1);
            }


            if (contentType.contains("x-www-form-urlencoded")) {
                StringBuilder formData = new StringBuilder();
                for (String key : body.keySet()) {
                    formData.append(key).append("=").append(body.get(key)).append("&");
                }

                if (!formData.isEmpty()) {
                    formData.deleteCharAt(formData.length() - 1);
                }

                requestBody = RequestBody.create(formData.toString(),
                        MediaType.parse("application/x-www-form-urlencoded"));
            } else {
                MediaType JSON = MediaType.parse(
                        "application/" + headers.getOrDefault("Content-Type",
                        headers.getOrDefault("content-type", "json; charset=utf-8"))
                );

                requestBody = RequestBody.create(body.toJSONString(), JSON);
            }

            builder.addHeader("Content-Length", String.valueOf(requestBody.contentLength()));
        }


        // 创建 POST 请求
        builder.url(requestUrl);

        if (HttpMethod.GET.equals(method)) {
            builder.get();
        } else {
            builder.method(method.name(), requestBody);
        }

        if (headers != null) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                builder.addHeader(header.getKey(), header.getValue());
            }
        }

        return builder.build();
    }


    @NotNull
    private static List<String> streamRequest(String url, Response response) throws IOException {
        ResponseBody responseBody = response.body();
        if (response.isSuccessful() || responseBody == null) {
            throw new RuntimeException("请求 " + url + "失败, " + (responseBody == null ? null : responseBody.string()));
        }

        List<String> result = new ArrayList<>();

        BufferedSource source = responseBody.source();
        while (!source.exhausted()) {
            String chunk = source.readUtf8Line();
            System.out.println(chunk);
            if (chunk != null) {
                result.add(chunk);
            }
        }
        return result;
    }


    @Nullable
    private String normalRequest(String url, HttpMethod method, Request request, int retryTimes) {
        log.debug("创建请求 url[{}], method[{}]成功，开始请求服务器", url, method);

        Exception exception = null;
        for (int i = 0; i < retryTimes; i++) {
            // 发送请求并获取响应
            try (Response response = okHttpClient.newCall(request).execute()) {
                ResponseBody responseBody = response.body();
                if (response.isSuccessful()) {
                    try {
                        return responseBody == null ? null : responseBody.string();
                    } catch (IOException e) {
                        log.warn("请求[{}]失败, response body解析失败，尝试重新请求 [{}}/{}],",
                                url, i, retryTimes);
                        exception = e;
                    }
                } else {
                    String body = "";
                    if (responseBody != null) {
                        body = responseBody.string();
                        body = body.substring(0, Math.min(body.length(), 200));
                    }
                    log.warn("请求[{}]失败, code[{}]-body[{}]，尝试重新请求 [{}}/{}],",
                            url, response.code(), body, i, retryTimes);
                    exception = new NetworkException("请求[%s]失败, code[%s]-body[%s]".formatted(url, response.code(), body));
                }
            } catch (SocketTimeoutException e) {
                log.warn("请求[{}]超时，尝试重新请求 [{}}/{}],", url, i, retryTimes);
                exception = e;
            } catch (IOException e) {
                log.warn("请求[{}]失败, exception[{}]，尝试重新请求 [{}}/{}],",
                        url, e.getCause() != null ? e.getCause().getMessage() : e.getMessage(), i, retryTimes);
                exception = e;
            } catch (Exception e) {
                throw new NetworkException("未知异常", e);
            }
        }

        throw new NetworkException("请求重试次数超过限制[" + retryTimes + "], "
                + (exception != null ? exception.getMessage() : "known"), exception);
    }
}
