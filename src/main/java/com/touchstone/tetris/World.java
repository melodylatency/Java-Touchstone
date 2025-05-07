package com.touchstone.tetris;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.image.Image;

import java.util.*;

public class World{
	private double x, y;
	private int width, height;
	private List<Tetris> tetrominoes = new ArrayList<>();

	public World(double x, double y, int w, int h){
		this.x = x;
		this.y = y;
		this.width = w;
		this.height = h;
	}

	public List<Tetris> getTetrises(){
		return this.tetrominoes;
	}

	public double getX(){
		return this.x;
	}

	public double getY(){
		return this.y;
	}

	public void setX(double v){
		this.x = v;
	}

	public void setY(double v){
		this.y = v;
	}

	public int getWidth(){
		return this.width;
	}

	public int getHeight(){
		return this.height;
	}

	private boolean pointExists(int x, int y){
		for (int i = 0; i < this.tetrominoes.size(); i++){
			Tetris t = this.tetrominoes.get(i);
			if (t.getAbsoluteFromShape(x, y)){
				return true;
			}
		}

		return false;
	}

	public void checkLines(){
		int rowsCleared = 0;
		for (int y = 0; y < this.height; y++){
			boolean completed = true;
			for (int x = 0; x < this.width; x++){
				boolean exists = pointExists(x, y);
				if (!exists){
					completed = false;
					break;
				}
			}

			if (completed){
				for (int i = 0; i < this.tetrominoes.size(); i++){
					Tetris t = this.tetrominoes.get(i);
					t.removeRow(y);
					if (t.getMinY() < y){
						if (t.getMaxY() > y){
							t.partialFall();
						} else {
							t.fall();	
						}
					}
				}
				rowsCleared++;
			}
		}

		switch (rowsCleared){
			case 1:
				MainApplication.score += 100;
				MainApplication.audio.get("clear.wav").play();
				break;
			case 2:
				MainApplication.score += 300;
				MainApplication.audio.get("clear.wav").play();
				break;
			case 3:
				MainApplication.score += 500;
				MainApplication.audio.get("clear.wav").play();
				break;
			case 4:
				MainApplication.score += 800;
				MainApplication.audio.get("tetris.wav").play();
				break;
		}
	}

	public void update(){	
		for (int i = 0; i < this.tetrominoes.size(); i++){
			Tetris t = this.tetrominoes.get(i);
			t.update();
		}
	}

	public void render(GraphicsContext gc){
		gc.save();
		gc.translate(this.x, this.y);

		for (int i = 0; i < this.tetrominoes.size(); i++){
			Tetris t = this.tetrominoes.get(i);
			t.render(gc);
		}
		gc.restore();

		gc.setStroke(Color.BLACK);
		gc.setLineWidth(3);
		gc.strokeRect(this.x, this.y, Tetris.SIZE*this.width, Tetris.SIZE*this.height);
	}
}