package cn.com.vortexa.script_node.view;

import cn.com.vortexa.common.util.AnsiColor;
import cn.com.vortexa.script_node.bot.AutoLaunchBot;
import cn.com.vortexa.script_node.constants.BotStatus;
import cn.com.vortexa.script_node.view.commandMenu.CommandMenuNode;
import cn.com.vortexa.script_node.view.commandMenu.MenuNodeMethod;
import cn.hutool.core.collection.ConcurrentHashSet;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import static cn.com.vortexa.common.util.AnsiColor.CYAN;

/**
 * 命令行交互的depin机器人
 */
@Slf4j
@Getter
public abstract class CommandLineMenu {
    private final Map<String, AutoLaunchBot<?>> loadedBotMap = new ConcurrentHashMap<>();
    private final Set<String> usableBotKeySet = new ConcurrentHashSet<>();

    private final CommandMenuNode mainManu;

    private AutoLaunchBot<?> bot;

    public CommandLineMenu() {
        this.mainManu = new CommandMenuNode(
                "主菜单",
                "当前可用的Bot"
        );
    }

    /**
     * 构建command菜单
     */
    protected void buildMenuNode(CommandMenuNode mainManu) {
        usableBotKeySet.forEach(botKey -> {
            CommandMenuNode botMenu = new CommandMenuNode(
                    botKey,
                    String.format("欢迎使用[%s]-bot", botKey)
            );

            botMenu.setTittleBuilder(()->{
                BotStatus botStatus = BotStatus.NOT_LOADED;
                AutoLaunchBot<?> alb = loadedBotMap.get(botKey);
                if (alb != null) {
                    botStatus = alb.getStatus();
                }
                return botKey + " - " + AnsiColor.colorize(botStatus.name(), CYAN);
            });

            botMenu.setAction(menu ->{
                AutoLaunchBot<?> curBot = bot = loadedBotMap.getOrDefault(botKey, null);
                if (curBot != null) {
                    // 解析MenuNodeMethod注解添加菜单节点
                    for (Method method : curBot.getClass().getDeclaredMethods()) {
                        method.setAccessible(true);

                        if (method.isAnnotationPresent(MenuNodeMethod.class)) {
                            if (method.getParameterCount() > 0) {
                                throw new IllegalArgumentException("菜单方法参数数量必须为0");
                            }

                            MenuNodeMethod anno = method.getAnnotation(MenuNodeMethod.class);
                            String title = anno.title();
                            String description = anno.description();

                            CommandMenuNode menuNode = new CommandMenuNode(title, description, () -> {
                                try {
                                    return method.invoke(curBot).toString();
                                } catch (IllegalAccessException | InvocationTargetException e) {
                                    throw new RuntimeException(e);
                                }
                            });

                            botMenu.addSubMenu(menuNode);
                        }
                    }
                }

                return "current use: [%s], status:[%s]".formatted(
                        botKey,
                        curBot == null ? BotStatus.NOT_LOADED :curBot.getStatus()
                );
            });

            mainManu.addSubMenu(botMenu);
            buildBotMenuNode(botMenu, botKey);
        });
    }

    protected abstract void buildBotMenuNode(CommandMenuNode botMenu, String botKey);

    /**
     * 异步启动
     */
    private void asyncExecute(CountDownLatch startLatch) {
        Thread commandInputThread = new Thread(() -> {
            try {
                doExecute();
            } catch (Exception e) {
                log.error("启动bot发生错误", e);
            } finally {
                startLatch.countDown();
            }
        }, "script-node-cmd");
        commandInputThread.setDaemon(true);
        commandInputThread.start();
    }

    /**
     * 启动bot
     */
    public void start() {
        try {
            CountDownLatch startLatch = new CountDownLatch(1);
            //启动命令行交互的线程
            asyncExecute(startLatch);

            startLatch.await();
        } catch (Exception e) {
            throw new RuntimeException("command menu start error", e);
        }
    }

    /**
     * 运行机器人
     *
     * @throws IOException IOException
     */
    public void doExecute() throws IOException {
        //Step 1 获取输入
        CommandMenuNode mainMenuNode = getMenuNode();
        Terminal terminal = TerminalBuilder.builder().system(true).build();
        LineReader reader = LineReaderBuilder.builder().terminal(terminal).parser(new DefaultParser()).build();

        Stack<CommandMenuNode> menuNodeStack = new Stack<>();
        CommandMenuNode currentMenuNode = mainMenuNode;

        //Step 2 不断监听控制台输入
        while (true) {
            boolean inputAccept = true;
            //Step 2.1 获取输入
            String choice;
            try {
                System.out.println("\n<\n" + getInvokeActionAndMenuNodePrintStr(currentMenuNode) + "请选择>");
                choice = reader.readLine()
                        .trim();
            } catch (Exception e) {
                log.error("进入菜单节点[{}]发生异常", currentMenuNode.getTittle(), e);
                currentMenuNode = menuNodeStack.pop();
                continue;
            }

            try {
                //Step 2.2 退出
                if ("exit".equals(choice)) {
                    exitHandler();
                    break;
                }

                //Step 2.3 选择操作
                int option = Integer.parseInt(choice.trim());
                if (option == 0) {
                    //返回上一级菜单
                    if (!menuNodeStack.isEmpty()) {
                        currentMenuNode = menuNodeStack.pop();
                    }
                } else if (option > 0 && option <= currentMenuNode.getSubNodeList().size()) {
                    //进入选择的菜单
                    menuNodeStack.push(currentMenuNode);
                    currentMenuNode = currentMenuNode.getSubNodeList().get(option - 1);
                } else {
                    inputAccept = false;
                }

                //终点节点，不进入，直接返回
                if (currentMenuNode.isEnd()) {
                    System.out.println(getInvokeActionAndMenuNodePrintStr(currentMenuNode));
                    currentMenuNode = menuNodeStack.pop();
                }
            } catch (Exception e) {
                inputAccept = false;
            }

            try {
                if (!inputAccept && currentMenuNode.getResolveInput() != null) {
                    currentMenuNode.getResolveInput().accept(choice);
                }
            } catch (Exception e) {
                System.out.println("系统异常");
            }
        }
    }

    /**
     * 获取菜单， 会放入额外的固定菜单
     *
     * @return CommandMenuNode
     */
    private CommandMenuNode getMenuNode() {

        buildMenuNode(mainManu);

        return mainManu;
    }

    private String printBanner() {

        return "" + bot.printBotRuntimeInfo();
    }

    /**
     * 退出回调
     */
    protected void exitHandler() {
    }

    /**
     * 执行Action回调，获取当前菜单打印的字符串
     *
     * @param currentMenuNode currentMenuNode
     * @return String
     */
    public String getInvokeActionAndMenuNodePrintStr(CommandMenuNode currentMenuNode) {
        StringBuilder sb = new StringBuilder();
        sb.append(currentMenuNode.getDescribe()).append("\n");

        if (currentMenuNode.getAction() != null) {
            sb.append(currentMenuNode.getAction().apply(currentMenuNode)).append("\n");
        }

        if (currentMenuNode.isEnd()) {
            return sb.toString();
        }

        sb.append("选项:\n");
        List<CommandMenuNode> menuNodeList = currentMenuNode.getSubNodeList();
        for (int i = 0; i < menuNodeList.size(); i++) {
            sb.append(i + 1).append(". ").append(menuNodeList.get(i).getTittle()).append("\n");
        }

        sb.append("0. 返回上一级菜单\n");

        return sb.toString();
    }

}
