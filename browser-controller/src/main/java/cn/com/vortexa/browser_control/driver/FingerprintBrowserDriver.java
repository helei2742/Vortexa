package cn.com.vortexa.browser_control.driver;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import cn.com.vortexa.browser_control.exception.BrowserRequestException;
import cn.com.vortexa.browser_control.dto.QueryEntity;
import cn.com.vortexa.common.constants.HttpMethod;
import cn.com.vortexa.common.util.http.RestApiClientFactory;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * @author helei
 * @since 2025/3/26 9:25
 */
public abstract class FingerprintBrowserDriver {

    private final String connectUrl;

    public FingerprintBrowserDriver(String connectUrl) {
        this.connectUrl = connectUrl;
    }

    /**
     * 检查健康状态
     *
     */
    public JSON health() {
        try {
            QueryEntity<JSON> queryEntity = healthBody();
            CompletableFuture<String> request = request(queryEntity);
            String str = request.get();
            return queryEntity.getResultStrHandler().apply(str);
        } catch (InterruptedException | ExecutionException e) {
            throw new BrowserRequestException("page query window error", e);
        }
    }

    /**
     * 构建分页查询窗口的请求
     *
     * @return QueryEntity
     */
    protected abstract QueryEntity<JSON> healthBody();

    /**
     * 分页查询窗口
     *
     * @param params params
     */
    public JSON pageQueryWindow(JSONObject params) {
        try {
            QueryEntity<JSON> queryEntity = buildPageQueryWindowBody(params);
            CompletableFuture<String> request = request(queryEntity);
            String str = request.get();
            return queryEntity.getResultStrHandler().apply(str);
        } catch (InterruptedException | ExecutionException e) {
            throw new BrowserRequestException("page query window error", e);
        }
    }

    /**
     * 构建分页查询窗口的请求
     *
     * @return QueryEntity
     */
    protected abstract QueryEntity<JSON> buildPageQueryWindowBody(JSONObject params);

    /**
     * 打开浏览器窗口
     *
     * @param params params
     * @return JSON
     */
    public JSON openBrowserWindow(JSONObject params) {
        try {
            QueryEntity<JSON> queryEntity = buildOpenBrowserWindowBody(params);
            CompletableFuture<String> request = request(queryEntity);
            String str = request.get();
            return queryEntity.getResultStrHandler().apply(str);
        } catch (InterruptedException | ExecutionException e) {
            throw new BrowserRequestException("open browser window error", e);
        }
    }

    /**
     * 构建打开窗口窗口的请求
     *
     * @return QueryEntity
     */
    protected abstract QueryEntity<JSON> buildOpenBrowserWindowBody(JSONObject params);

    /**
     * 关闭浏览器窗口
     *
     * @param params params
     * @return JSON
     */
    public JSON closeBrowserWindow(JSONObject params) {
        try {
            QueryEntity<JSON> queryEntity = buildCloseBrowserWindowBody(params);
            CompletableFuture<String> request = request(queryEntity);
            String str = request.get();
            return queryEntity.getResultStrHandler().apply(str);
        } catch (InterruptedException | ExecutionException e) {
            throw new BrowserRequestException("close browser window error", e);
        }
    }

    /**
     * 构建关闭窗口的请求
     *
     * @return QueryEntity
     */
    protected abstract QueryEntity<JSON> buildCloseBrowserWindowBody(JSONObject params);

    /**
     * 关闭所有浏览器窗口
     *
     * @return JSON
     */
    public JSON closeAllBrowserWindow() {
        try {
            QueryEntity<JSON> queryEntity = closeAllBrowserWindowBody();
            CompletableFuture<String> request = request(queryEntity);
            String str = request.get();
            return queryEntity.getResultStrHandler().apply(str);
        } catch (InterruptedException | ExecutionException e) {
            throw new BrowserRequestException("close all browser window error", e);
        }
    }

    /**
     * 构建关闭所有浏览器窗口的请求
     *
     * @return QueryEntity
     */
    protected abstract QueryEntity<JSON> closeAllBrowserWindowBody();

    /**
     * 删除浏览器窗口
     *
     * @param params params
     * @return JSON
     */
    public JSON deleteBrowserWindow(JSONObject params) {
        try {
            QueryEntity<JSON> queryEntity = deleteCloseBrowserWindowBody(params);
            CompletableFuture<String> request = request(queryEntity);
            String str = request.get();
            return queryEntity.getResultStrHandler().apply(str);
        } catch (InterruptedException | ExecutionException e) {
            throw new BrowserRequestException("delete browser window error", e);
        }
    }

    /**
     * 构建删除窗口的请求
     *
     * @return QueryEntity
     */
    protected abstract QueryEntity<JSON> deleteCloseBrowserWindowBody(JSONObject params);

    /**
     * 删除批量浏览器窗口
     *
     * @param params params
     * @return JSON
     */
    public JSON deleteBrowserWindowBatch(JSONObject params) {
        try {
            QueryEntity<JSON> queryEntity = deleteBrowserWindowBatchBody(params);
            CompletableFuture<String> request = request(queryEntity);
            String str = request.get();
            return queryEntity.getResultStrHandler().apply(str);
        } catch (InterruptedException | ExecutionException e) {
            throw new BrowserRequestException("batch delete browser window error", e);
        }
    }

    /**
     * 构建批量删除窗口的请求
     *
     * @return QueryEntity
     */
    protected abstract QueryEntity<JSON> deleteBrowserWindowBatchBody(JSONObject params);

    /**
     * 浏览器窗口详情查询
     *
     * @param params params
     * @return JSON
     */
    public JSON browserWindowDetail(JSONObject params) {
        try {
            QueryEntity<JSON> queryEntity = browserWindowDetailBody(params);
            CompletableFuture<String> request = request(queryEntity);
            String str = request.get();
            return queryEntity.getResultStrHandler().apply(str);
        } catch (InterruptedException | ExecutionException e) {
            throw new BrowserRequestException("query browser window detail error", e);
        }
    }

    /**
     * 构建窗口详情查询请求
     *
     * @return QueryEntity
     */
    protected abstract QueryEntity<JSON> browserWindowDetailBody(JSONObject params);

    /**
     * 排列已打开窗口
     *
     * @param params params
     * @return JSON
     */
    public JSON boundsBrowserWindow(JSONObject params) {
        try {
            QueryEntity<JSON> queryEntity = boundsBrowserWindowBody(params);
            CompletableFuture<String> request = request(queryEntity);
            String str = request.get();
            return queryEntity.getResultStrHandler().apply(str);
        } catch (InterruptedException | ExecutionException e) {
            throw new BrowserRequestException("bounds browser window error", e);
        }
    }

    /**
     * 构建排列已打开窗口请求
     *
     * @return QueryEntity
     */
    protected abstract QueryEntity<JSON> boundsBrowserWindowBody(JSONObject params);

    /**
     * 自适应排列窗口
     *
     * @return JSON
     */
    public JSON autoFlexWindow() {
        try {
            QueryEntity<JSON> queryEntity = autoFlexWindowBody();
            CompletableFuture<String> request = request(queryEntity);
            String str = request.get();
            return queryEntity.getResultStrHandler().apply(str);
        } catch (InterruptedException | ExecutionException e) {
            throw new BrowserRequestException("auto flex browser window error", e);
        }
    }

    /**
     * 构建自适应排列窗口窗口请求
     *
     * @return QueryEntity
     */
    protected abstract QueryEntity<JSON> autoFlexWindowBody();

    /**
     * 修改浏览器窗口分组
     *
     * @param params params
     * @return JSON
     */
    public JSON updateWindowGroup(JSONObject params) {
        try {
            QueryEntity<JSON> queryEntity = updateWindowGroupBody(params);
            CompletableFuture<String> request = request(queryEntity);
            String str = request.get();
            return queryEntity.getResultStrHandler().apply(str);
        } catch (InterruptedException | ExecutionException e) {
            throw new BrowserRequestException("update browser window group error", e);
        }
    }

    /**
     * 修改浏览器窗口分组请求
     *
     * @return QueryEntity
     */
    protected abstract QueryEntity<JSON> updateWindowGroupBody(JSONObject params);

    /**
     * 修改窗口备注
     *
     * @param params params
     * @return JSON
     */
    public JSON updateWindowRemark(JSONObject params) {
        try {
            QueryEntity<JSON> queryEntity = updateWindowRemarkBody(params);
            CompletableFuture<String> request = request(queryEntity);
            String str = request.get();
            return queryEntity.getResultStrHandler().apply(str);
        } catch (InterruptedException | ExecutionException e) {
            throw new BrowserRequestException("update browser window remark error", e);
        }
    }

    /**
     * 修改窗口备注请求
     *
     * @return QueryEntity
     */
    protected abstract QueryEntity<JSON> updateWindowRemarkBody(JSONObject params);

    /**
     * 检查代理
     *
     * @param params params
     * @return JSON
     */
    public JSON proxyCheckOut(JSONObject params) {
        try {
            QueryEntity<JSON> queryEntity = proxyCheckOutBody(params);
            CompletableFuture<String> request = request(queryEntity);
            String str = request.get();
            return queryEntity.getResultStrHandler().apply(str);
        } catch (InterruptedException | ExecutionException e) {
            throw new BrowserRequestException("update browser window remark error", e);
        }
    }

    /**
     * 检查代理请求
     *
     * @return QueryEntity
     */
    protected abstract QueryEntity<JSON> proxyCheckOutBody(JSONObject params);

    /**
     * 查询显示器列表
     *
     * @return JSON
     */
    public JSON displayListQuery() {
        try {
            QueryEntity<JSON> queryEntity = displayListQueryBody();
            CompletableFuture<String> request = request(queryEntity);
            String str = request.get();
            return queryEntity.getResultStrHandler().apply(str);
        } catch (InterruptedException | ExecutionException e) {
            throw new BrowserRequestException("query display list error", e);
        }
    }

    /**
     * 查询显示器列表
     *
     * @return QueryEntity
     */
    protected abstract QueryEntity<JSON> displayListQueryBody();

    protected CompletableFuture<String> request(QueryEntity<JSON> queryEntity) {
        return RestApiClientFactory.getClient(null).request(
                connectUrl + queryEntity.getContentPath(),
                queryEntity.getMethod() == null ? HttpMethod.POST : queryEntity.getMethod(),
                new HashMap<>(),
                queryEntity.getMethod() == HttpMethod.POST ? null : queryEntity.getBody(),
                queryEntity.getMethod() == HttpMethod.POST ? queryEntity.getBody() : new JSONObject()
        );
    }
}
