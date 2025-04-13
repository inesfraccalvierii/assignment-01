package pcd.ass01.controller.SequentialBoids;

import pcd.ass01.model.Boid;
import pcd.ass01.model.BoidsModel;
import pcd.ass01.view.BoidsView;

import java.util.Optional;

public class BoidsSimulator {
    //final static int N_BOIDS = 1500;
    private BoidsModel model;
    private Optional<BoidsView> view;
    
    private static final int FRAMERATE = 25;
    private int framerate;
    private boolean run;
    private boolean stop;
    public BoidsSimulator(BoidsModel model) {
        this.model = model;
        view = Optional.empty();
    }

    private long totalSimulationTime = 0;
    private int totalFrames = 0;

    public void attachView(BoidsView view) {
    	this.view = Optional.of(view);
    }
      
    public void runSimulation() {
        run = true;
        stop = false;
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

    		/* 
    		 * ..then update positions
    		 */
    		for (Boid boid : boids) {
                boid.updatePos(model);
            }

            
    		if (view.isPresent()) {
            	view.get().update(framerate);
            	var t1 = System.currentTimeMillis();
                var dtElapsed = t1 - t0;
                totalSimulationTime += dtElapsed;
                totalFrames++;

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

            if (totalFrames > 0) {
                double avgFrameTime = (double) totalSimulationTime / totalFrames;
                System.out.println("Simulation ended.");
                System.out.println("Total frames: " + totalFrames);
                System.out.println("Average frame time: " + avgFrameTime + " ms");
                System.out.println("Average FPS: " + (1000.0 / avgFrameTime));
            }

            while (!run){

            }
            runSimulation();
    	}
    }

    public void setNumberOfBoids(int numBoids) {
        model.createBoids(numBoids);
    }

    public void startSimulation() {
        runSimulation();
    }

    public void resumeSimulation() {
        run = true;
    }

    public void pauseSimulation() {
        run = false;
    }

    public void stopSimulation() {
        stop = true;
        model.deleteBoids();
    }
}
