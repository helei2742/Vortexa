package cn.com.helei.common.vo;

import lombok.Data;

import java.util.Map;

@Data
public class PageQuery {

    private Integer page = 1;

    private Integer limit = 5;

    private Map<String, Object> filterMap;
}
