package cn.com.vortexa.control.dto;


/**
 * @author helei
 * @since 2025-03-15
 */
public class ResponseFuture {

    private volatile boolean isDone = false;
    private Object res = null;

    public synchronized boolean setResponse(Object res) {
        if(isDone) {
            return false;
        }
        this.res = res;
        this.notifyAll();
        return isDone = true;
    }

    public synchronized Object getResponse() throws InterruptedException {
        while (res == null) {
            this.wait();
        }
        return res;
    }
}
