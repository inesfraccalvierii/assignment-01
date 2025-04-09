package pcd.ass01.view;

import pcd.ass01.model.BoidsModel;
import pcd.ass01.controller.SequentialBoids.BoidsSimulator;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.awt.*;
import java.util.Hashtable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class BoidsView implements ChangeListener {

	private JFrame frame;
	private BoidsPanel boidsPanel;
	private JSlider cohesionSlider, separationSlider, alignmentSlider;
	private BoidsModel model;
	private int width, height;
	private JButton startButton, pauseResumeButton, stopButton;
	private JTextField boidInputField;
	private boolean isPaused = false;
	private BoidsSimulator boidsSimulator;

	public BoidsView(BoidsModel model, int width, int height, BoidsSimulator simulator) {
		this.model = model;
		this.width = width;
		this.height = height;
		this.boidsSimulator = simulator;

		frame = new JFrame("Boids Simulation");
		frame.setSize(width, height);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JPanel cp = new JPanel();
		LayoutManager layout = new BorderLayout();
		cp.setLayout(layout);

		boidsPanel = new BoidsPanel(this, model);
		cp.add(BorderLayout.CENTER, boidsPanel);

		JPanel slidersPanel = new JPanel();

		cohesionSlider = makeSlider();
		separationSlider = makeSlider();
		alignmentSlider = makeSlider();

		slidersPanel.add(new JLabel("Separation"));
		slidersPanel.add(separationSlider);
		slidersPanel.add(new JLabel("Alignment"));
		slidersPanel.add(alignmentSlider);
		slidersPanel.add(new JLabel("Cohesion"));
		slidersPanel.add(cohesionSlider);

		cp.add(BorderLayout.SOUTH, slidersPanel);

		JPanel buttonsPanel = new JPanel();
		boidInputField = new JTextField(5);
		startButton = new JButton("Start");
		pauseResumeButton = new JButton("Pause");
		stopButton = new JButton("Stop");

		startButton.setEnabled(false);
		pauseResumeButton.setEnabled(false);
		stopButton.setEnabled(false);

		boidInputField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				String text = boidInputField.getText().trim();
				startButton.setEnabled(!text.isEmpty());
			}
		});

		startButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int numBoids;
				try {
					numBoids = Integer.parseInt(boidInputField.getText().trim());
					if (numBoids > 0) {
						boidsSimulator.setNumberOfBoids(numBoids);
						new Thread(() -> boidsSimulator.startSimulation()).start();
						boidInputField.setEnabled(false);
						startButton.setEnabled(false);
						pauseResumeButton.setEnabled(true);
						stopButton.setEnabled(true);
					}
				} catch (NumberFormatException ex) {
					JOptionPane.showMessageDialog(frame, "Please enter a valid number.", "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		pauseResumeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (isPaused) {
					boidsSimulator.resumeSimulation();
					pauseResumeButton.setText("Pause");
				} else {
					boidsSimulator.pauseSimulation();
					pauseResumeButton.setText("Resume");
				}
				isPaused = !isPaused;
			}
		});

		stopButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				boidsSimulator.stopSimulation();
				boidInputField.setEnabled(true);
				startButton.setEnabled(true);
				pauseResumeButton.setEnabled(false);
				stopButton.setEnabled(false);

				isPaused = false;
				pauseResumeButton.setText("Pause");
			}
		});

		buttonsPanel.add(new JLabel("Number of Boids: "));
		buttonsPanel.add(boidInputField);
		buttonsPanel.add(startButton);
		buttonsPanel.add(pauseResumeButton);
		buttonsPanel.add(stopButton);

		cp.add(BorderLayout.NORTH, buttonsPanel);

		frame.setContentPane(cp);
		frame.setVisible(true);
	}

	private JSlider makeSlider() {
		var slider = new JSlider(JSlider.HORIZONTAL, 0, 20, 10);
		slider.setMajorTickSpacing(10);
		slider.setMinorTickSpacing(1);
		slider.setPaintTicks(true);
		slider.setPaintLabels(true);
		Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
		labelTable.put(0, new JLabel("0"));
		labelTable.put(10, new JLabel("1"));
		labelTable.put(20, new JLabel("2"));
		slider.setLabelTable(labelTable);
		slider.setPaintLabels(true);
		slider.addChangeListener(this);
		return slider;
	}

	public void update(int frameRate) {
		boidsPanel.setFrameRate(frameRate);
		boidsPanel.repaint();
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == separationSlider) {
			var val = separationSlider.getValue();
			model.setSeparationWeight(0.1 * val);
		} else if (e.getSource() == cohesionSlider) {
			var val = cohesionSlider.getValue();
			model.setCohesionWeight(0.1 * val);
		} else {
			var val = alignmentSlider.getValue();
			model.setAlignmentWeight(0.1 * val);
		}
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}
}
