package cn.com.vortexa.browser_control.driver;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import cn.com.vortexa.browser_control.dto.QueryEntity;
import cn.com.vortexa.browser_control.exception.BrowserRequestException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * @author helei
 * @since 2025/3/26 9:21
 */
public class BitBrowserDriver extends FingerprintBrowserDriver {

    public BitBrowserDriver(String connectUrl) {
        super(connectUrl);
    }

    @Override
    protected QueryEntity<JSON> healthBody() {
        return QueryEntity.<JSON>builder()
                .contentPath("/health")
                .resultStrHandler(JSONArray::parseObject)
                .build();
    }

    @Override
    protected QueryEntity<JSON> buildPageQueryWindowBody(JSONObject params) {
        if (!params.containsKey("page") || !params.containsKey("pageSize")) {
            throw new IllegalArgumentException("page and pageSize must be provided");
        }
        return QueryEntity.<JSON>builder()
                .body(params)
                .contentPath("/browser/list")
                .resultStrHandler(JSONArray::parseObject)
                .build();
    }

    @Override
    protected QueryEntity<JSON> buildOpenBrowserWindowBody(JSONObject params) {
        if (!params.containsKey("id")) {
            throw new IllegalArgumentException("id must be provided");
        }
        return QueryEntity.<JSON>builder()
                .body(params)
                .contentPath("/browser/open")
                .resultStrHandler(JSONArray::parseObject)
                .build();
    }

    @Override
    protected QueryEntity<JSON> buildCloseBrowserWindowBody(JSONObject params) {
        if (!params.containsKey("id")) {
            throw new IllegalArgumentException("id must be provided");
        }
        return QueryEntity.<JSON>builder()
                .body(params)
                .contentPath("/browser/close")
                .resultStrHandler(JSONArray::parseObject)
                .build();
    }

    @Override
    protected QueryEntity<JSON> closeAllBrowserWindowBody() {
        return QueryEntity.<JSON>builder()
                .contentPath("/browser/close/all")
                .resultStrHandler(JSONArray::parseObject)
                .build();
    }

    @Override
    protected QueryEntity<JSON> deleteCloseBrowserWindowBody(JSONObject params) {
        if (!params.containsKey("id")) {
            throw new IllegalArgumentException("id must be provided");
        }
        return QueryEntity.<JSON>builder()
                .body(params)
                .contentPath("/browser/delete")
                .resultStrHandler(JSONArray::parseObject)
                .build();
    }

    @Override
    protected QueryEntity<JSON> deleteBrowserWindowBatchBody(JSONObject params) {
        if (!params.containsKey("ids")) {
            throw new IllegalArgumentException("id must be provided");
        }
        return QueryEntity.<JSON>builder()
                .body(params)
                .contentPath("/browser/delete")
                .resultStrHandler(JSONArray::parseObject)
                .build();
    }

    @Override
    protected QueryEntity<JSON> browserWindowDetailBody(JSONObject params) {
        if (!params.containsKey("id")) {
            throw new IllegalArgumentException("id must be provided");
        }
        return QueryEntity.<JSON>builder()
                .body(params)
                .contentPath("/browser/detail")
                .resultStrHandler(JSONArray::parseObject)
                .build();
    }

    @Override
    protected QueryEntity<JSON> boundsBrowserWindowBody(JSONObject params) {
        return QueryEntity.<JSON>builder()
                .body(params)
                .contentPath("/windowbounds")
                .resultStrHandler(JSONArray::parseObject)
                .build();
    }

    @Override
    protected QueryEntity<JSON> autoFlexWindowBody() {
        return QueryEntity.<JSON>builder()
                .contentPath("/windowbounds/flexable")
                .resultStrHandler(JSONArray::parseObject)
                .build();
    }

    @Override
    protected QueryEntity<JSON> updateWindowGroupBody(JSONObject params) {
        if (!params.containsKey("groupId")) {
            throw new IllegalArgumentException("groupId must be provided");
        }
        if (!params.containsKey("browserIds")) {
            throw new IllegalArgumentException("browserIds must be provided");
        }
        return QueryEntity.<JSON>builder()
                .body(params)
                .contentPath("/browser/group/update")
                .resultStrHandler(JSONArray::parseObject)
                .build();
    }

    @Override
    protected QueryEntity<JSON> updateWindowRemarkBody(JSONObject params) {
        if (!params.containsKey("remark")) {
            throw new IllegalArgumentException("remark must be provided");
        }
        if (!params.containsKey("browserIds")) {
            throw new IllegalArgumentException("browserIds must be provided");
        }
        return QueryEntity.<JSON>builder()
                .body(params)
                .contentPath("/browser/remark/update")
                .resultStrHandler(JSONArray::parseObject)
                .build();
    }

    @Override
    protected QueryEntity<JSON> proxyCheckOutBody(JSONObject params) {
        if (!params.containsKey("host")) {
            throw new IllegalArgumentException("host must be provided");
        }
        if (!params.containsKey("port")) {
            throw new IllegalArgumentException("port must be provided");
        }
        return QueryEntity.<JSON>builder()
                .body(params)
                .contentPath("/checkagent")
                .resultStrHandler(JSONArray::parseObject)
                .build();
    }

    @Override
    protected QueryEntity<JSON> displayListQueryBody() {
        return QueryEntity.<JSON>builder().contentPath("/checkagent").resultStrHandler(JSONArray::parseObject).build();
    }

    /**
     * 自适应排列选择序号
     *
     * @param seqList seqList
     * @return JSONObject
     */
    public JSONObject flexAbleWindowBounds(List<Integer> seqList) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("seqlist", seqList);
        QueryEntity<JSON> queryEntity = QueryEntity.<JSON>builder()
                .body(jsonObject)
                .contentPath("/windowbounds/flexable")
                .resultStrHandler(JSONObject::parseObject).build();

        CompletableFuture<String> request = request(queryEntity);
        String str = null;
        try {
            str = request.get();
            return (JSONObject) queryEntity.getResultStrHandler().apply(str);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 重置浏览器关闭状态
     *
     * @param id id
     * @return JSON
     */
    public JSON resetCloseStatus(String id) {
        try {
            JSONObject params = new JSONObject();
            params.put("id", id);
            QueryEntity<JSON> queryEntity = QueryEntity.<JSON>builder()
                    .body(params).contentPath("/browser/closing/reset")
                    .resultStrHandler(JSONArray::parseArray)
                    .build();
            CompletableFuture<String> request = request(queryEntity);
            String str = request.get();
            return queryEntity.getResultStrHandler().apply(str);
        } catch (InterruptedException | ExecutionException e) {
            throw new BrowserRequestException("reset closing status error", e);
        }
    }

    /**
     * 仿真输入，将剪切板内容输入到已聚焦的输入框
     *
     * @param browserId browserId
     * @param url       url
     * @return JSON
     */
    public JSON autoPaste(String browserId, String url) {
        try {
            JSONObject params = new JSONObject();
            params.put("browserId", browserId);
            params.put("url", url);
            QueryEntity<JSON> queryEntity = QueryEntity.<JSON>builder()
                    .body(params).contentPath("/autopaste")
                    .resultStrHandler(JSONArray::parseArray)
                    .build();
            CompletableFuture<String> request = request(queryEntity);
            String str = request.get();
            return queryEntity.getResultStrHandler().apply(str);
        } catch (InterruptedException | ExecutionException e) {
            throw new BrowserRequestException("autopaste error", e);
        }
    }

    /**
     * 根据序列号启动
     *
     * @param fingerSeq fingerSeq
     * @return String debugger address
     */
    public String startWebDriverBySeq(Integer fingerSeq) {
        JSONObject params = new JSONObject();
        params.put("page", 0);
        params.put("pageSize", 1);
        params.put("seq", fingerSeq);
        JSONObject pageQueryResult = (JSONObject) pageQueryWindow(params);
        String fingerId = pageQueryResult.getJSONObject("data").getJSONArray("list")
                .getJSONObject(0).getString("id");
        JSONObject openParams = new JSONObject();
        openParams.put("id", fingerId);
        openParams.put("args", new ArrayList<>());
        openParams.put("queue", true);
        JSONObject openResult = (JSONObject) openBrowserWindow(openParams);

        return openResult.getJSONObject("data").getString("http");
    }
}
