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
    private volatile boolean running = true;
    private volatile boolean stop = false;

    private long totalSimulationTime = 0;
    private int totalFrames = 0;
    private final Random random = new Random();

    public BoidsVirtualThreadSimulator(BoidsModel model) {
        super(model);
        this.model = model;
        view = Optional.empty();
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

            batch.forEach(boid -> boid.setColor(color));

            Thread.ofVirtual().start(() -> {
                try {
                    while (!stop) {
                        isPaused();
                        for (Boid boid : List.copyOf(batch)) {
                            boid.updateVelocity(model);
                        }

                        boidsLatch.countDown();
                        boidsLatch.await();

                        for (Boid boid : List.copyOf(batch)) {
                            boid.updatePos(model);
                        }

                        boidsLatch.countDown();
                        boidsLatch.await();
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
                totalSimulationTime += dtElapsed;
                totalFrames++;
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

                if (totalFrames > 0) {
                    double avgFrameTime = (double) totalSimulationTime / totalFrames;
                    System.out.println("Simulation ended.");
                    System.out.println("Total frames: " + totalFrames);
                    System.out.println("Average frame time: " + avgFrameTime + " ms");
                    System.out.println("Average FPS: " + (1000.0 / avgFrameTime));
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
