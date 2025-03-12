package cn.com.vortexa.bot_father.util.persistence;


import cn.com.vortexa.common.entity.AccountContext;

import java.util.List;

public interface AccountPersistenceManager {

    void init();

    void persistenceAccountContexts(List<AccountContext> accountContexts);

    List<AccountContext> loadAccountContexts(Integer botId, String botKey);

    void registerPersistenceListener(List<AccountContext> targetList);

    public void fillAccountInfo(AccountContext accountContext);
}
