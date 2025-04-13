package pcd.ass01.model;

public class BoidsLatch implements Latch{
    private final int parties;
    private int count;
    private int initial;
    private int generation = 0;

    public BoidsLatch(int n) {
        this.count = n;
        this.initial = n;
        this.parties = n;
    }

    @Override
    public synchronized void await() throws InterruptedException {
        int currentGen = generation;

        count--;
        if (count == 0) {
            // Ultimo thread: resetta per la prossima generazione e notifica tutti
            generation++;
            count = parties;
            notifyAll();
        } else {
            // Attende che il ciclo venga completato
            while (generation == currentGen) {
                wait();
            }
        }
    }


    @Override
    public synchronized void countDown() {
        count--;
        if (count == 0) {
            generation++;
            count = initial;
            notifyAll();
        }
    }
}
