package cn.com.vortexa.control.dto;

import java.util.Arrays;

public class ArgsWrapper {
    private Object[] args;

    public ArgsWrapper() {
    }

    public ArgsWrapper(Object[] args) {
        this.args = args;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    @Override
    public String toString() {
        return "ArgsWrapper{" +
                "args=" + Arrays.toString(args) +
                '}';
    }
}
