package pcd.ass01.model;

public class BoidsLatch implements Latch{

    private int count;
    public BoidsLatch(int n) {
        this.count = n;
    }

    @Override
    public synchronized void await() throws InterruptedException {
        while (count > 0){
            wait();
        }
    }

    @Override
    public synchronized void countDown() {
        count--;
        if(count == 0){
            notifyAll();
        }
    }
}
