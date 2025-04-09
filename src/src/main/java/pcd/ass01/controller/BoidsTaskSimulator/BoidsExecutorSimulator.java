package pcd.ass01.controller.BoidsTaskSimulator;

import pcd.ass01.controller.SequentialBoids.BoidsSimulator;
import pcd.ass01.model.Boid;
import pcd.ass01.model.BoidsModel;
import pcd.ass01.view.BoidsView;

import java.awt.*;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.*;
public class BoidsExecutorSimulator extends BoidsSimulator {

    private final BoidsModel model;
    private Optional<BoidsView> view = Optional.empty();
    private static final int FRAMERATE = 60;
    private int framerate;
    private final int boidsWorkers = Runtime.getRuntime().availableProcessors() - 1;

    private ExecutorService velocityExecutor;
    private ExecutorService positionExecutor;

    private volatile boolean running = true;
    private volatile boolean stop = false;

    private final Random random = new Random();

    public BoidsExecutorSimulator(BoidsModel model) {
        super(model);
        this.model = model;
        this.velocityExecutor = Executors.newFixedThreadPool(boidsWorkers);
        this.positionExecutor = Executors.newFixedThreadPool(boidsWorkers);
    }

    public void attachView(BoidsView view) {
        this.view = Optional.of(view);
    }

    public void startSimulation() {
        if (!running) {
            resumeSimulation();
            stop = false;
            ensureExecutors();
        }

        List<Boid> boids = model.getBoids();
        int batchSize = (int) Math.ceil((double) boids.size() / boidsWorkers);

        for (int i = 0; i < boidsWorkers; i++) {
            int start = i * batchSize;
            int end = Math.min(start + batchSize, boids.size());
            List<Boid> batch = boids.subList(start, end);

            Color color = new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256));
            batch.forEach(boid -> boid.setColor(color));
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
        List<Boid> boids = model.getBoids();
        int batchSize = (int) Math.ceil((double) boids.size() / boidsWorkers);

        while (!stop) {
            long t0 = System.currentTimeMillis();
            isPaused();

            CountDownLatch velocityLatch = new CountDownLatch(boidsWorkers);
            for (int i = 0; i < boidsWorkers; i++) {
                int start = i * batchSize;
                int end = Math.min(start + batchSize, boids.size());
                List<Boid> batch = boids.subList(start, end);

                velocityExecutor.submit(new BoidVelocityUpdater(batch, model, velocityLatch));
            }

            try {
                velocityLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            CountDownLatch positionLatch = new CountDownLatch(boidsWorkers);
            for (int i = 0; i < boidsWorkers; i++) {
                int start = i * batchSize;
                int end = Math.min(start + batchSize, boids.size());
                List<Boid> batch = boids.subList(start, end);

                positionExecutor.submit(new BoidPositionUpdater(batch, model, positionLatch));
            }

            try {
                positionLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            view.ifPresent(v -> v.update(framerate));

            long dtElapsed = System.currentTimeMillis() - t0;
            long frameRatePeriod = 1000 / FRAMERATE;

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

    public void stopSimulation() {
        stop = true;
        running = false;
        velocityExecutor.shutdownNow();
        positionExecutor.shutdownNow();
        model.deleteBoids();
    }

    public synchronized void pauseSimulation() {
        running = false;
    }

    public synchronized void resumeSimulation() {
        running = true;
        notifyAll();
    }

    private void ensureExecutors() {
        if (velocityExecutor.isShutdown()) {
            velocityExecutor = Executors.newFixedThreadPool(boidsWorkers);
        }
        if (positionExecutor.isShutdown()) {
            positionExecutor = Executors.newFixedThreadPool(boidsWorkers);
        }
    }
}
