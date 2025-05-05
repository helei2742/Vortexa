package cn.com.vortexa.common.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = -1028302839238238923L;

    private Integer page = 1;

    private Integer limit = 5;

    private Map<String, Object> filterMap;
}
