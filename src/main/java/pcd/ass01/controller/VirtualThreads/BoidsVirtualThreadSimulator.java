package pcd.ass01.controller.VirtualThreads;

import pcd.ass01.controller.SequentialBoids.BoidsSimulator;
import pcd.ass01.model.Boid;
import pcd.ass01.model.BoidsLatch;
import pcd.ass01.model.BoidsModel;
import pcd.ass01.view.BoidsView;

import java.awt.*;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class BoidsVirtualThreadSimulator extends BoidsSimulator {

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

    public BoidsVirtualThreadSimulator(BoidsModel model) {
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

            // Assign colors to boids
            batch.forEach(boid -> boid.setColor(color));

            // Start a virtual thread for each worker
            Thread.ofVirtual().start(() -> {
                try {
                    while (!stop) {
                        isPaused();

                        // Update velocities
                        for (Boid boid : List.copyOf(batch)) {
                            boid.updateVelocity(model);
                        }

                        boidsLatch.countDown(); // Let the main thread know velocities have been updated
                        boidsLatch.await(); // Wait for all workers to reach this point

                        // Update positions
                        for (Boid boid : List.copyOf(batch)) {
                            boid.updatePos(model);
                        }

                        boidsLatch.countDown(); // Indicate that position updates are done
                        boidsLatch.await(); // Wait for all workers to reach this point
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        runSimulation();
    }

    private synchronized void isPaused() {
        while (!running) {
            try {
                wait();
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
        stop = true;
        running = false;
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
