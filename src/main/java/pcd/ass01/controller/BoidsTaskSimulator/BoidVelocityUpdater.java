package pcd.ass01.controller.BoidsTaskSimulator;

import pcd.ass01.model.Boid;
import pcd.ass01.model.BoidsModel;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public class BoidVelocityUpdater implements Runnable {
    private final List<Boid> batch;
    private final BoidsModel model;
    private final CountDownLatch latch;

    public BoidVelocityUpdater(List<Boid> batch, BoidsModel model, CountDownLatch latch) {
        this.batch = batch;
        this.model = model;
        this.latch = latch;
    }

    @Override
    public void run() {
        try {
            for (Boid boid : batch) {
                boid.updateVelocity(model);
            }
        } finally {
            latch.countDown();
        }
    }
}
