package com.touchstone.tetris;

import javafx.scene.canvas.GraphicsContext;

import java.util.*;

public class Tetris{
	public static int SIZE = 30;

	private World world;
	private int x, y;
	private boolean falling = true;
	private int pieceWidth, pieceHeight;
	private boolean[] pieceShape;
	private Piece piece;
	private Tetris parent;
	private int rotation;
	private boolean empty;

	public Tetris(World world, int x, int y, Piece piece){
		this.world = world;
		this.x = x;
		this.y = y;
		this.piece = piece;
		this.pieceWidth = this.piece.getWidth();
		this.pieceHeight = this.piece.getHeight();
		this.pieceShape = new boolean[this.piece.getShape().length];
		System.arraycopy(piece.getShape(), 0, this.pieceShape, 0, this.pieceShape.length);
	}

	public boolean isFalling(){
		return this.falling;
	}

	public void setParent(Tetris t){
		this.parent = t;
	}

	public int getMinX(){
		int minX = Integer.MAX_VALUE;
		for (int y = 0; y < this.pieceHeight; y++){
			int foundX = -1;
			for (int x = 0; x < this.pieceWidth; x++){
				boolean pos = this.pieceShape[x+this.pieceWidth*y];
				if (pos){
					foundX = x;
					break;
				}
			}

			if (foundX != -1 && foundX < minX){
				minX = foundX;
			}
		}

		return this.x+minX;
	}

	public int getMaxX(){
		int maxX = Integer.MIN_VALUE;
		for (int y = 0; y < this.pieceHeight; y++){
			int foundX = -1;
			for (int x = this.pieceWidth-1; x >= 0; x--){
				boolean pos = this.pieceShape[x+this.pieceWidth*y];
				if (pos){
					foundX = x;
					break;
				}
			}

			if (foundX != -1 && foundX > maxX){
				maxX = foundX;
			}
		}

		return this.x+maxX;
	}

	public int getMinY(){
		int minY = Integer.MAX_VALUE;
		for (int x = 0; x < this.pieceWidth; x++){
			int foundY = -1;
			for (int y = 0; y < this.pieceHeight; y++){
				boolean pos = this.pieceShape[x+this.pieceWidth*y];
				if (pos){
					foundY = y;
					break;
				}
			}

			if (foundY != -1 && foundY < minY){
				minY = foundY;
			}
		}

		return this.y+minY;
	}

	public int getMaxY(){
		int maxY = Integer.MIN_VALUE;
		for (int x = 0; x < this.pieceWidth; x++){
			int foundY = -1;
			for (int y = this.pieceHeight-1; y >= 0; y--){
				boolean pos = this.pieceShape[x+this.pieceWidth*y];
				if (pos){
					foundY = y;
					break;
				}
			}

			if (foundY != -1 && foundY > maxY){
				maxY = foundY;
			}
		}

		return this.y+maxY;
	}

	public int getX(){
		return this.x;
	}

	public int getY(){
		return this.y;
	}

	public void setX(int v){
		this.x = v;
	}

	public void setY(int v){
		this.y = v;
	}

	public Piece getPiece(){
		return this.piece;
	}

	public int getRotation(){
		return this.rotation;
	}

	public void move(int n){
		if (this.falling){
			this.x += n;
			if (getMinX() < 0 || getMaxX() > this.world.getWidth()-1 || collided()){
				this.x -= n;
			}
			MainApplication.audio.get("move.wav").play();
		}
	}

	public void partialFall(){
		int space = -1;
		final int offset = getMinY()-this.y;
		for (int y = offset; y < this.pieceHeight; y++){
			boolean empty = true;
			for (int x = 0; x < this.pieceWidth; x++){
				if (this.pieceShape[x+this.pieceWidth*y]){
					empty = false;
					break;
				}
			}

			if (empty){
				space = y;
				break;
			}
		}

		if (space != -1){
			int h = this.pieceHeight-1;
			int newY = this.y+1;
			boolean[] shape = new boolean[this.pieceWidth*h];
			for (int y = 0; y < this.pieceHeight; y++){
				for (int x = 0; x < this.pieceWidth; x++){
					if (y < space){
						shape[x+this.pieceWidth*y] = this.pieceShape[x+this.pieceWidth*y];
					} else if (y > space){
						shape[x+this.pieceWidth*(y-1)] = this.pieceShape[x+this.pieceWidth*y];
					}
				}
			}
			this.y = newY;
			this.pieceHeight = h;
			this.pieceShape = shape;
		}
	}

	public void fall(){
		this.y += 1;

		if (collided()){
			this.y -= 1;
			this.falling = false;
		}

		if (getMaxY() >= this.world.getHeight()-1){
			this.falling = false;
		}
	}

	public void rotate(){
		if (this.falling){
			int w = this.pieceHeight;
			int h = this.pieceWidth;

			// Fix position
			if (this.x+w > this.world.getWidth()){
				this.x = this.world.getWidth()-w;
			}
			if (this.x < 0) this.x = 0;
			if (this.y+h >this.world.getHeight()){
				this.y = this.world.getHeight()-h;
			}

			boolean[] shape = new boolean[w*h];
			int newY = 0;
			for (int x = 0; x < this.pieceWidth; x++){
				int newX = 0;
				for (int y = this.pieceHeight-1; y >= 0; y--){
					shape[newX+w*newY] = this.pieceShape[x+this.pieceWidth*y];
					newX++;
				}
				newY++;
			}

			int backupW = this.pieceWidth;
			int backupH = this.pieceHeight;
			boolean[] backupShape = this.pieceShape;
			this.pieceWidth = w;
			this.pieceHeight = h;
			this.pieceShape = shape;

			if (collided()){
				this.pieceWidth = backupW;
				this.pieceHeight = backupH;
				this.pieceShape = backupShape;
			}

			this.rotation = (this.rotation+1)%4;
		}
	}

	public void update(){
		if (this.falling){
			fall();
		}

		if (!this.empty){
			this.empty = isEmpty();
		} else {
			this.world.getTetrises().remove(this);
		}
	}

	private boolean isEmpty(){
		for (int x = 0; x < this.pieceWidth; x++){
			for (int y = 0; y < this.pieceHeight; y++){
				boolean e = this.pieceShape[x+this.pieceWidth*y];
				if (e) return false;
			}
		}

		return true;
	}

	public boolean collided(){
		List<Tetris> tetrominoes = this.world.getTetrises();
		for (int i = 0; i < tetrominoes.size(); i++){
			Tetris t = tetrominoes.get(i);
			if (t != this && t != this.parent && !t.empty){
				for (int x = 0; x < this.pieceWidth; x++){
					for (int y = 0; y < this.pieceHeight; y++){
						boolean thisSquare = this.getAbsoluteFromShape(this.x+x, this.y+y);
						boolean otherSquare = t.getAbsoluteFromShape(this.x+x, this.y+y);
						if (thisSquare && otherSquare){
							return true;
						}
					}
				}
			}
		}

		return false;
	}

	public void removeRow(int ry){
		ry = ry-this.y;
		if (ry >= 0 && ry < this.pieceHeight){
			for (int x = 0; x < this.pieceWidth; x++){
				this.pieceShape[x+this.pieceWidth*ry] = false;
			}
		}
	}

	public boolean getAbsoluteFromShape(int x, int y){
		if (x < this.x || y < this.y || x >= this.x+this.pieceWidth || y >= this.y+this.pieceHeight) return false;

		return this.pieceShape[(x-this.x)+this.pieceWidth*(y-this.y)];
	}

	public void stop(){
		this.falling = false;
	}

	public void render(GraphicsContext gc){
		if (this.parent != null) gc.setGlobalAlpha(0.4);
		for (int x = 0; x < this.pieceWidth; x++){
			for (int y = 0; y < this.pieceHeight; y++){
				boolean show = this.pieceShape[x+this.pieceWidth*y];
				if (show){
					gc.drawImage(this.piece.getImage(), (this.x+x)*SIZE, (this.y+y)*SIZE, SIZE, SIZE);
				}
			}
		}
		gc.setGlobalAlpha(1);
	}

	public static void render(GraphicsContext gc, Piece piece, double px, double py){
		for (int x = 0; x < piece.getWidth(); x++){
			for (int y = 0; y < piece.getHeight(); y++){
				boolean show = piece.getShape()[x+piece.getWidth()*y];
				if (show){
					gc.drawImage(piece.getImage(), px+x*SIZE, py+y*SIZE, SIZE, SIZE);
				}
			}
		}
	}
}