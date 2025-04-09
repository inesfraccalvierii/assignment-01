package pcd.ass01.controller.BoidsTaskSimulator;

import pcd.ass01.model.Boid;
import pcd.ass01.model.BoidsModel;

import java.util.List;
import java.util.concurrent.CountDownLatch;

class BoidPositionUpdater implements Runnable {
    private final List<Boid> boids;
    private final BoidsModel model;
    private final CountDownLatch latch;

    public BoidPositionUpdater(List<Boid> boids, BoidsModel model, CountDownLatch latch) {
        this.boids = boids;
        this.model = model;
        this.latch = latch;
    }

    @Override
    public void run() {
        try {
            for (Boid boid : boids) {
                boid.updatePos(model);
            }
        } finally {

            latch.countDown();
        }
    }
}