package cn.com.vortexa.common.vo;

import lombok.Data;

import java.util.List;

@Data
public class BotBindVO {

    private Integer botId;

    private String botKey;

    private List<Integer> bindAccountBaseInfoList;
}
