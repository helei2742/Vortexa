package cn.com.vortexa.common.util;

public class AnsiColor {

    public static final String RESET = "\u001B[0m";

    // 前景色（文字颜色）
    public static final String BLACK   = "\u001B[30m";
    public static final String RED     = "\u001B[31m";
    public static final String GREEN   = "\u001B[32m";
    public static final String YELLOW  = "\u001B[33m";
    public static final String BLUE    = "\u001B[34m";
    public static final String PURPLE  = "\u001B[35m";
    public static final String CYAN    = "\u001B[36m";
    public static final String WHITE   = "\u001B[37m";

    // 背景色
    public static final String BG_BLACK  = "\u001B[40m";
    public static final String BG_RED    = "\u001B[41m";
    public static final String BG_GREEN  = "\u001B[42m";
    public static final String BG_YELLOW = "\u001B[43m";
    public static final String BG_BLUE   = "\u001B[44m";
    public static final String BG_PURPLE = "\u001B[45m";
    public static final String BG_CYAN   = "\u001B[46m";
    public static final String BG_WHITE  = "\u001B[47m";

    // 样式
    public static final String BOLD      = "\u001B[1m";
    public static final String UNDERLINE = "\u001B[4m";
    public static final String REVERSED  = "\u001B[7m";

    /**
     * 给字符串加上前景色
     */
    public static String colorize(String text, String color) {
        return color + text + RESET;
    }

    /**
     * 给字符串加上前景色 + 背景色 + 样式
     */
    public static String colorize(String text, String color, String bgColor, String style) {
        return style + color + bgColor + text + RESET;
    }

    public static void main(String[] args) {
        // 纯颜色
        System.out.println(colorize("红色", RED));
        System.out.println(colorize("绿色", GREEN));
        System.out.println(colorize("蓝色", BLUE));
        System.out.println(colorize("紫色", PURPLE));
        System.out.println(colorize("青色", CYAN));
        System.out.println(colorize("黄色", YELLOW));
        System.out.println(colorize("白色", WHITE));

        // 背景+颜色+样式
        System.out.println(colorize("加粗+蓝字+黄底", BLUE, BG_YELLOW, BOLD));
        System.out.println(colorize("下划线+红字+白底", RED, BG_WHITE, UNDERLINE));
        System.out.println(colorize("反色+绿色+黑底", GREEN, BG_BLACK, REVERSED));
    }
}
