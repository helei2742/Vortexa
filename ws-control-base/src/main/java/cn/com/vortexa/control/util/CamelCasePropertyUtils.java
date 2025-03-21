package cn.com.vortexa.control.util;

import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.introspector.PropertyUtils;

import java.util.Locale;

public class CamelCasePropertyUtils extends PropertyUtils {
    @Override
    public Property getProperty(Class<?> type, String name) {
        String camelCaseName = toCamelCase(name);
        return super.getProperty(type, camelCaseName);
    }

    private String toCamelCase(String name) {
        String[] parts = name.split("-");
        StringBuilder camelCase = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            camelCase.append(parts[i].substring(0, 1).toUpperCase(Locale.ROOT))
                    .append(parts[i].substring(1));
        }
        return camelCase.toString();
    }
}
