package cn.com.vortexa.nameserver.pool;


import cn.com.vortexa.nameserver.constant.NameserverSystemConstants;
import cn.com.vortexa.nameserver.dto.RemotingCommand;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;
import java.util.Random;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;


public class RemotingCommandPool {
    private static final int POOL_INIT_SIZE; // 对象池的大小
    private static final int POOL_MAX_SIZE; // 对象池最大的大小
    private static final AtomicIntegerFieldUpdater<RemotingCommandPool> COUNT_UPDATER;
    private static final RemotingCommandPool INSTANCE;
    private static final Random random = new Random();

    private final PriorityBlockingQueue<PooledRemotingCommand> pool;
    private volatile int currentCount = 0;

    static {
        POOL_INIT_SIZE = NameserverSystemConstants.MESSAGE_OBJ_POOL_INIT_SIZE;
        POOL_MAX_SIZE = NameserverSystemConstants.MESSAGE_OBJ_POOL_MAX_SIZE;

        INSTANCE = new RemotingCommandPool(new PriorityBlockingQueue<PooledRemotingCommand>(POOL_INIT_SIZE,
                (m1,m2)->Boolean.compare(m1.busy, m2.busy)));

        COUNT_UPDATER = AtomicIntegerFieldUpdater.newUpdater(RemotingCommandPool.class,
                "currentCount");
    }

    private RemotingCommandPool(PriorityBlockingQueue<PooledRemotingCommand> pool) {
        this.pool = pool;
    }


    public static RemotingCommand getObject() {
        PooledRemotingCommand poll = INSTANCE.pool.poll();

        if(poll == null || poll.isBusy()) {
            poll = new PooledRemotingCommand();
        }
        poll.setBusy(true);

        int currentCount = COUNT_UPDATER.get(INSTANCE);
        if(currentCount < POOL_INIT_SIZE) {
            INSTANCE.pool.offer(poll);
            COUNT_UPDATER.incrementAndGet(INSTANCE);
        } else if(currentCount > POOL_INIT_SIZE && currentCount < POOL_MAX_SIZE) {
            if(random.nextBoolean()) {
                INSTANCE.pool.offer(poll);
                COUNT_UPDATER.incrementAndGet(INSTANCE);
            }
        }

        return poll;
    }

    public static void returnObject(RemotingCommand remotingCommand) {
        if(remotingCommand instanceof PooledRemotingCommand) {
            ((PooledRemotingCommand) remotingCommand).clear();
            COUNT_UPDATER.decrementAndGet(INSTANCE);
        }
    }
}


@Setter
@Getter
class PooledRemotingCommand extends RemotingCommand {
    boolean busy;

    public PooledRemotingCommand() {
        super();
        busy = false;
    }

    public void release() {

        RemotingCommandPool.returnObject(this);
    }

    public void clear() {
        super.clear();
        this.busy = false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PooledRemotingCommand that)) return false;
        if (!super.equals(o)) return false;
        return busy == that.busy;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), busy);
    }
}
