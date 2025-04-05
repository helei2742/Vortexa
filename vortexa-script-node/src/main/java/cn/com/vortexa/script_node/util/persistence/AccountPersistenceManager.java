package cn.com.vortexa.script_node.util.persistence;


import cn.com.vortexa.common.entity.AccountContext;

import java.util.List;
import java.util.concurrent.ExecutionException;

public interface AccountPersistenceManager {

    void init();

    void persistenceAccountContexts(List<AccountContext> accountContexts);

    List<AccountContext> loadAccountContexts(Integer botId, String botKey);

    void registerPersistenceListener(List<AccountContext> targetList);

    void fillAccountInfos(List<AccountContext> accountContexts) throws ExecutionException, InterruptedException;
}
