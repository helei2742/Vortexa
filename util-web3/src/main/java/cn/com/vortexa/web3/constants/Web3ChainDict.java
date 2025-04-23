package cn.com.vortexa.web3.constants;

import cn.com.vortexa.common.util.YamlConfigLoadUtil;
import cn.com.vortexa.web3.dto.Web3ChainInfo;
import lombok.Data;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author h30069248
 * @since 2025/4/23 10:27
 */
@Data
public class Web3ChainDict {
    public static final String RESOURCE_FILE_NAME = "chain-info.yaml";
    public static final List<String> PREFIX = List.of("vortexa", "web3");
    public static final Web3ChainDict INSTANCE;

    static {
        try (InputStream is = Web3ChainDict.class.getClassLoader().getResourceAsStream(RESOURCE_FILE_NAME)){
            INSTANCE = YamlConfigLoadUtil.load(RESOURCE_FILE_NAME, is, PREFIX, Web3ChainDict.class);
            if (INSTANCE.chainInfo != null) {
                INSTANCE.name2ChainInfoMap = INSTANCE.chainInfo.stream().collect(Collectors.toMap(Web3ChainInfo::getName, w->w));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Web3ChainInfo> chainInfo;

    private Map<String, Web3ChainInfo> name2ChainInfoMap;

    public Web3ChainInfo getChainInfo(String name) {
        return name2ChainInfoMap.get(name);
    }

    public static void main(String[] args) {
        System.out.println(INSTANCE);
    }
}
