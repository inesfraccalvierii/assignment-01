package pcd.ass01.controller.MultiThread;

import pcd.ass01.controller.SequentialBoids.BoidsSimulator;
import pcd.ass01.model.Boid;
import pcd.ass01.model.BoidsLatch;
import pcd.ass01.model.BoidsModel;
import pcd.ass01.view.BoidsView;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class BoidsMultiThreadingSimulator extends BoidsSimulator {

    private final BoidsModel model;
    private Optional<BoidsView> view;

    private static final int FRAMERATE = 60;
    private int framerate;
    private BoidsLatch boidsLatch;
    private final int boidsWorkers = Runtime.getRuntime().availableProcessors() - 1;
    private final Thread[] workerThreads;
    private volatile boolean running = true;
    private volatile boolean stop = false;


    private final Random random = new Random();

    public BoidsMultiThreadingSimulator(BoidsModel model) {
        super(model);
        this.model = model;
        view = Optional.empty();
        workerThreads = new Thread[boidsWorkers];
    }

    public void attachView(BoidsView view) {
        this.view = Optional.of(view);
    }

    public void startSimulation() {
        if (!running) {
            resumeSimulation();
            stop = false;
        }
        List<Boid> boids = model.getBoids();
        int batchSize = (int) Math.ceil((double) boids.size() / boidsWorkers);
        boidsLatch = new BoidsLatch(boidsWorkers);
        for (int i = 0; i < boidsWorkers; i++) {
            Color color = new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256));
            int start = i * batchSize;
            int end = Math.min(start + batchSize, boids.size());
            List<Boid> batch = boids.subList(start, end);
            batch.forEach(boid -> {
                boid.setColor(color);
            });
            workerThreads[i] = new Thread(() -> {
                while (!stop) {
                    isPaused();

                    for (Boid boid : List.copyOf(batch)) {
                        boid.updateVelocity(model);
                    }

                    boidsLatch.countDown();

                    try {
                        boidsLatch.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    for (Boid boid : new ArrayList<>(batch)) {
                        boid.updatePos(model);
                    }


                    boidsLatch.countDown();

                    try {
                        boidsLatch.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            });

            workerThreads[i].start();
        }

        runSimulation();
    }

    public synchronized void isPaused() {
        while (!running) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    public void runSimulation() {
        while (!stop) {
            var t0 = System.currentTimeMillis();

            if (view.isPresent()) {
                view.get().update(framerate);
                var t1 = System.currentTimeMillis();
                var dtElapsed = t1 - t0;
                var frameRatePeriod = 1000 / FRAMERATE;

                if (dtElapsed < frameRatePeriod) {
                    try {
                        Thread.sleep(frameRatePeriod - dtElapsed);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    framerate = FRAMERATE;
                } else {
                    framerate = (int) (1000 / dtElapsed);
                }
            }
        }
    }

    public void stopSimulation() {
        for (Thread thread : workerThreads) {
            thread.interrupt();
        }
        model.deleteBoids();
    }

    public synchronized void pauseSimulation() {
        running = false;
    }

    public synchronized void resumeSimulation() {
        running = true;
        notifyAll();
    }

}
