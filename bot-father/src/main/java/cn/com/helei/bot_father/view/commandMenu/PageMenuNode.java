package cn.com.helei.bot_father.view.commandMenu;


import cn.com.helei.common.dto.PageResult;
import cn.com.helei.common.util.tableprinter.CommandLineTablePrintHelper;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class PageMenuNode<T> extends CommandMenuNode {
    private static final String PREFIX_PAGE_NAME = "pre_page";

    private static final String NEXT_PAGE_NAME = "next_page";

    private static final String REFRESH_PAGE_NAME = "refresh_page";

    private static final String PAGE_KEY = "current_page";

    private static final String TOTAL_KEY = "total";

    private final Map<String, Object> params = new HashMap<>();

    private final Field[] fields;

    private final int limit;

    private List<T> cacheList;

    private boolean isQuery = true;

    public PageMenuNode(
            String tittle,
            String describe,
            BiFunction<Integer, Integer, PageResult<T>> pageInvocation,
            Class<T> tClass
    ) {
        this(tittle, describe, 20, pageInvocation, tClass);
    }

    public PageMenuNode(
            String tittle,
            String describe,
            int limit,
            BiFunction<Integer, Integer, PageResult<T>> pageInvocation,
            Class<T> tClass
    ) {
        super(tittle, describe, null);

        this.fields = tClass.getDeclaredFields();
        this.limit = limit;

        this.setAction(() -> {
            String print = "";
            if (isQuery) {
                int page = Math.max(1, (Integer) params.getOrDefault(PAGE_KEY, 1));
                PageResult<T> pageResult = pageInvocation.apply(page, limit);

                params.put(TOTAL_KEY, pageResult.getTotal());
                params.put(PAGE_KEY, pageResult.getPageNum());

                cacheList = pageResult.getList();
                isQuery = false;
                print = CommandLineTablePrintHelper.generateTableString(cacheList, tClass);
            }

            return print + printPageInfo();
        });

        this.setResolveInput(input -> {
            String[] split = input.split(":");
            if (split.length != 2) return;

            if ("detail".equals(split[0]) && cacheList != null && !cacheList.isEmpty()) {
                int row = Integer.parseInt(split[1]);
                T item = cacheList.get(row);
                try {
                    System.out.println(printItem(item));
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        this.addSubMenu(preMenuNode()).addSubMenu(nextMenuNode()).addSubMenu(refreshMenuNode());
    }


    private CommandMenuNode preMenuNode() {
        return new CommandMenuNode(
                true,
                PREFIX_PAGE_NAME,
                "",
                () -> {
                    params.compute(PAGE_KEY, (k, v) -> (int) Math.max(0, (v == null ? 0 : (int) v) - 1));
                    isQuery = true;
                    return "";
                }
        );
    }

    private CommandMenuNode nextMenuNode() {
        return new CommandMenuNode(
                true,
                NEXT_PAGE_NAME,
                "",
                () -> {
                    params.compute(PAGE_KEY, (k, v) -> {
                        Long total = (Long) params.get(TOTAL_KEY);
                        v = v == null ? 0 : v;
                        if (total == null) return (int) v + 1;

                        return (int) Math.min(total, (int) v + 1);
                    });
                    isQuery = true;
                    return "";
                }
        );
    }

    private CommandMenuNode refreshMenuNode() {
        return new CommandMenuNode(
                true,
                REFRESH_PAGE_NAME,
                "",
                () -> {
                    isQuery = true;
                    return "";
                }
        );
    }


    private String printPageInfo() {
        return "\n总数:%s, 当前页: %s, 每页大小: %s\n输入detail:row查看详情".formatted(
                params.get(TOTAL_KEY), params.get(PAGE_KEY), this.limit
        );
    }

    private String printItem(T item) throws IllegalAccessException {
        StringBuilder sb = new StringBuilder();
        for (Field field : fields) {
            field.setAccessible(true);
            sb.append(field.getName()).append(": ").append(field.get(item)).append("\n");
        }

        return sb.toString();
    }
}
