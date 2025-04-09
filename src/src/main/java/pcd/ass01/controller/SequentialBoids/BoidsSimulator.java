package pcd.ass01.controller.SequentialBoids;

import pcd.ass01.model.Boid;
import pcd.ass01.model.BoidsModel;
import pcd.ass01.view.BoidsView;

import java.util.Optional;

public class BoidsSimulator {

    private BoidsModel model;
    private Optional<BoidsView> view;
    
    private static final int FRAMERATE = 60;
    private int framerate;

    private boolean stop = false;

    public BoidsSimulator(BoidsModel model) {
        this.model = model;
        view = Optional.empty();
    }

    public void attachView(BoidsView view) {
    	this.view = Optional.of(view);
    }

    public void startSimulation() {
        runSimulation();
    }
    public void runSimulation() {
    	while (!stop) {
            var t0 = System.currentTimeMillis();
    		var boids = model.getBoids();
    		/*
    		for (Boid boid : boids) {
                boid.update(model);
            }
            */
    		
    		/* 
    		 * Improved correctness: first update velocities...
    		 */
    		for (Boid boid : boids) {
                boid.updateVelocity(model);
            }

    		/* z
    		 * ..then update positions
    		 */
    		for (Boid boid : boids) {
                boid.updatePos(model);
            }

            
    		if (view.isPresent()) {
            	view.get().update(framerate);
            	var t1 = System.currentTimeMillis();
                var dtElapsed = t1 - t0;
                var framratePeriod = 1000/FRAMERATE;
                
                if (dtElapsed < framratePeriod) {		
                	try {
                		Thread.sleep(framratePeriod - dtElapsed);
                	} catch (Exception ex) {}
                	framerate = FRAMERATE;
                } else {
                	framerate = (int) (1000/dtElapsed);
                }
    		}
            
    	}
    }

    public void stopSimulation() {
        this.stop = true;
        model.deleteBoids();
    }

    public void pauseSimulation() {
        this.stop = true;
    }

    public void resumeSimulation() {
        this.stop = false;
    }

    public void setNumberOfBoids(int nBoids){
        model.createBoids(nBoids);
    }
}
