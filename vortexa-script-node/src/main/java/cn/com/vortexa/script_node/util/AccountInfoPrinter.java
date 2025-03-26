package cn.com.vortexa.script_node.util;


import cn.com.vortexa.common.util.tableprinter.CommandLineTablePrintHelper;
import cn.com.vortexa.common.dto.ConnectStatusInfo;
import cn.com.vortexa.common.dto.account.AccountPrintDto;
import cn.com.vortexa.common.entity.AccountContext;
import cn.com.vortexa.common.entity.BrowserEnv;
import cn.com.vortexa.common.entity.ProxyInfo;
import cn.com.vortexa.common.entity.RewordInfo;

import java.util.List;

public class AccountInfoPrinter {

    /**
     * 打印账号列表
     *
     * @return String
     */
    public static String printAccountList(List<AccountContext> accountContexts) {

        StringBuilder sb = new StringBuilder();

        sb.append(" 账户列表\n");

        List<AccountPrintDto> list = accountContexts.stream().map(accountContext -> {
            ProxyInfo proxy = accountContext.getProxy();
            BrowserEnv browserEnv = accountContext.getBrowserEnv();

            return AccountPrintDto
                    .builder()
                    .id(accountContext.getAccountBaseInfo().getId())
                    .name(accountContext.getName())
                    .proxyInfo(proxy == null ? "NO_PROXY" : proxy.getId() + "-" + proxy.generateAddressStr())
                    .browserEnvInfo(String.valueOf(browserEnv == null ? "NO_ENV" : browserEnv.getId()))
                    .signUp(accountContext.isSignUp())
                    .build();
        }).toList();

        sb.append(CommandLineTablePrintHelper.generateTableString(list, AccountPrintDto.class)).append("\n");

        return sb.toString();
    }

    /**
     * 打印账户连接情况
     *
     * @return String
     */
    public static String printAccountConnectStatusList(List<AccountContext> accountContexts) {
        StringBuilder sb = new StringBuilder();

        sb.append(" 账号链接状态列表:\n");

        List<ConnectStatusInfo> list = accountContexts.stream()
                .map(AccountContext::getConnectStatusInfo).toList();

        sb.append(CommandLineTablePrintHelper.generateTableString(list, ConnectStatusInfo.class)).append("\n");

        return sb.toString();
    }

    /**
     * 打印账号收益
     *
     * @return String
     */
    public static String printAccountReward(List<AccountContext> accountContexts) {
        StringBuilder sb = new StringBuilder();

        sb.append(" 收益列表:\n");

        List<RewordInfo> list = accountContexts.stream()
                .map(accountContext -> accountContext.getRewordInfo().newInstance()).toList();

        sb.append(CommandLineTablePrintHelper.generateTableString(list, RewordInfo.class)).append("\n");

        return sb.toString();
    }

}
