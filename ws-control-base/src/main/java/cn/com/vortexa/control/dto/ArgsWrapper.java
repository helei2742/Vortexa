package cn.com.vortexa.control.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;

@Setter
@Getter
public class ArgsWrapper {
    private Object[] args;

    public ArgsWrapper() {
    }

    public ArgsWrapper(Object[] args) {
        this.args = args;
    }

    @Override
    public String toString() {
        return "ArgsWrapper{" +
                "args=" + Arrays.toString(args) +
                '}';
    }
}
