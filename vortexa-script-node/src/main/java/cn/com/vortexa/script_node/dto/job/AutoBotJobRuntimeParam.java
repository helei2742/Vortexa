package cn.com.vortexa.script_node.dto.job;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.reflect.Method;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AutoBotJobRuntimeParam {

    private Object target;

    private Method method;

    private Object[] extraParams;
}
