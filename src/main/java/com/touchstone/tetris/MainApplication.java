package com.touchstone.tetris;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.canvas.*;
import javafx.scene.paint.Color;
import javafx.animation.*;
import javafx.util.Duration;
import javafx.scene.input.KeyCode;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.scene.media.*;
import javafx.scene.image.Image;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.TextInputDialog;

import java.util.*;
import java.io.*;

public class MainApplication extends Application{
	private static double WIDTH = 600;
	private static double HEIGHT = 800;
	private static final int FPS = 40;

	private World world;
	private Tetris fallingTetris;
	private Map<KeyCode, Boolean> keys = new HashMap<>();
	private volatile int fallTime;
	private Piece nextPiece = null, holdPiece;
	private boolean paused, holdDone;
	private List<Map.Entry<String, Integer>> entries;
	private Rectangle2D holdRect;
	private double lastDragPoint;
	private boolean dragging = false;

	public static volatile int score, lastScore, highscore, difficulty = 5;
	public static Map<String, AudioClip> audio = new HashMap<>();
	private static Media BACKGROUND_MUSIC;
	private static Font MAIN_FONT = Font.loadFont(MainApplication.class.getResourceAsStream("/font.ttf"), 25);
	private static int FALLING_SPEED = getSpeed();
	
	@Override
	public void start(Stage stage){
		StackPane pane = new StackPane();
		Canvas canvas = new Canvas(WIDTH, HEIGHT);
		GraphicsContext gc = canvas.getGraphicsContext2D();
		pane.getChildren().add(canvas);

		loadHighscore();
		loadAudio();
		MediaPlayer mp = new MediaPlayer(BACKGROUND_MUSIC);
		mp.setCycleCount(MediaPlayer.INDEFINITE);
		mp.play();

		canvas.setFocusTraversable(true);
		canvas.setOnKeyPressed(e -> this.keys.put(e.getCode(), true));
		canvas.setOnKeyReleased(e -> this.keys.put(e.getCode(), false));

		this.world = new World((WIDTH-10*Tetris.SIZE)*0.7, (HEIGHT-20*Tetris.SIZE)/2, 10, 20);
		createTetris(null);

		canvas.setOnMouseDragged(e -> {
			if (!this.paused){
				if (Math.abs(this.lastDragPoint-e.getX()) > 20){
					if (e.getX() > lastDragPoint){
						this.keys.put(KeyCode.RIGHT, true);
					} else {
						this.keys.put(KeyCode.LEFT, true);
					}
					this.lastDragPoint = e.getX();
					this.dragging = true;
				}
			}
		});

		Thread gameLoop = new Thread(() -> {
			while (true){
				try {
					if (!this.paused){
						this.world.update();

						if (!this.fallingTetris.isFalling()){
							this.world.checkLines();
							MainApplication.audio.get("block_landed.wav").play();
							createTetris(null);
							this.holdDone = false;
						}

						if (this.keys.getOrDefault(KeyCode.DOWN, false)){
							score += 2;
							MainApplication.audio.get("move.wav").play();
						}

						if (score > lastScore+5000){ // Every 5000 score the game gets more difficult
							lastScore = score;
							difficulty = Math.min(difficulty+1, 9);
						}
					}

					Thread.sleep(this.keys.getOrDefault(KeyCode.DOWN, false) ? FALLING_SPEED/4 : this.fallTime);
				} catch (InterruptedException ex){
					ex.printStackTrace();
				}
			}
		});
		gameLoop.setDaemon(true);
		gameLoop.start();
		
		Timeline loop = new Timeline(new KeyFrame(Duration.millis(1000.0/FPS), e -> update(gc)));
		loop.setCycleCount(Animation.INDEFINITE);
		loop.play();

		stage.widthProperty().addListener((ob, oldV, newV) -> resize((double)newV, HEIGHT, canvas));
		stage.heightProperty().addListener((ob, oldV, newV) -> resize(WIDTH, (double)newV, canvas));

		stage.setTitle("Tetris");
		stage.getIcons().add(new Image(getClass().getResourceAsStream("/icon.png")));
		stage.setScene(new Scene(pane, WIDTH, HEIGHT));
		stage.show();
	}

	private void resize(double w, double h, Canvas canvas){
		WIDTH = w;
		HEIGHT = h;
		Tetris.SIZE = (int)Math.min(WIDTH*0.05, HEIGHT*0.0375);
		MAIN_FONT = Font.loadFont(MainApplication.class.getResourceAsStream("/font.ttf"), Math.min(WIDTH*0.04, HEIGHT*0.03));
		canvas.setWidth(w);
		canvas.setHeight(h);
		this.world.setX((WIDTH-10*Tetris.SIZE)*0.5+Tetris.SIZE*2);
		this.world.setY((HEIGHT-20*Tetris.SIZE)/2);
	}

	private Tetris createTempTetris(){
		Tetris temp = new Tetris(this.world, 0, 0, this.fallingTetris.getPiece());
		temp.setParent(this.fallingTetris);
		for (int i = 0; i < this.fallingTetris.getRotation(); i++){
			temp.rotate();
		}
		temp.setX(this.fallingTetris.getX());
		temp.setY(this.fallingTetris.getY());
		while (temp.isFalling()){
			temp.fall();
		}

		return temp;
	}

	private void holdPiece(){
		if (this.holdDone) return;
		if (this.holdPiece != null){
			this.world.getTetrises().remove(this.fallingTetris);
			Piece p = this.holdPiece;
			this.holdPiece = this.fallingTetris.getPiece();
			createTetris(p);
			this.holdDone = true;
		} else {
			this.holdPiece = this.fallingTetris.getPiece();
			this.world.getTetrises().remove(this.fallingTetris);
			createTetris(null);
		}
	}

	private void saveHighscore(){
		File file = new File("highscore.txt");
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			writer.write(Integer.toString(highscore));
			writer.close();
		} catch (IOException ex){
			ex.printStackTrace();
		}
	}

	private void loadHighscore(){
		File file = new File("highscore.txt");
		if (file.exists()){
			try {
				BufferedReader reader = new BufferedReader(new FileReader(file));
				highscore = Integer.parseInt(reader.readLine());
				reader.close();
			} catch (IOException ex){
				ex.printStackTrace();
			}
		}
	}

	private static void loadAudio(){
		try {
			BACKGROUND_MUSIC = new Media(MainApplication.class.getResource("/audio/background.mp3").toURI().toString());
			String[] audios = new String[]{"block_landed.wav", "clear.wav", "falling.wav", "gameover.wav", "move.wav", "rotate.wav", "tetris.wav"};

			for (String a : audios){
				audio.put(a, new AudioClip(MainApplication.class.getResource("/audio/"+a).toURI().toString()));
			}
		} catch (Exception ex){
			ex.printStackTrace();
		}
	}

	private void gameOver(){
		MainApplication.audio.get("gameover.wav").play();
		this.world.getTetrises().clear();
		this.fallingTetris = null;
		this.fallTime = FALLING_SPEED;
		this.nextPiece = null;
		if (score > highscore){
			highscore = score;
			saveHighscore();
		}
		score = 0;
		createTetris(null);
	}

	private void createTetris(Piece defPiece){
		Random random = new Random();
		Piece p = this.nextPiece == null ? Piece.values()[random.nextInt(Piece.values().length)] : this.nextPiece;
		if (defPiece != null){
			p = defPiece;
		} else {
			this.nextPiece = Piece.values()[random.nextInt(Piece.values().length)];
		}

		Tetris t = new Tetris(this.world, this.world.getWidth()/2-p.getWidth()/2, 0, p);

		if (t.collided()){
			gameOver();
			return;
		}

		this.fallingTetris = t;
		this.world.getTetrises().add(t);
		this.fallTime = FALLING_SPEED;
	}

	private static int getSpeed(){
		// 100 - 1200, 5
		double t = difficulty/9.0;
		int speed = (int)Math.round((1-t)*1100+100);
		return speed;
	}
	
	private void update(GraphicsContext gc){
		gc.clearRect(0, 0, WIDTH, HEIGHT);
		gc.setFill(Color.web("#D5FDEB"));
		gc.fillRect(0, 0, WIDTH, HEIGHT);

		if (this.paused){
			// Show leaderboard
			if (this.entries != null){
				gc.setFill(Color.BLACK);
				gc.setTextAlign(TextAlignment.CENTER);
				StringBuilder builder = new StringBuilder();
				builder.append("Click to exit\n\n");
				int i = 1;
				for (Map.Entry<String, Integer> entry : this.entries){
					builder.append((i++)+". "+entry.getKey()+"    "+entry.getValue()+"\n");
					if (i == 4){
						builder.append("\n");
					} else if (i == 11) break;
				}
				gc.fillText(builder.toString(), WIDTH/2, HEIGHT*0.15);
			} else {
				gc.setFill(Color.BLACK);
				gc.setTextAlign(TextAlignment.CENTER);
				gc.fillText("Loading...\n\nClick to exit", WIDTH/2, HEIGHT*0.15);
			}

			return;
		}

		if (this.fallingTetris != null){
			if (this.keys.getOrDefault(KeyCode.RIGHT, false)){
				this.fallingTetris.move(1);
				this.keys.put(KeyCode.RIGHT, false);
			} else if (this.keys.getOrDefault(KeyCode.LEFT, false)){
				this.fallingTetris.move(-1);
				this.keys.put(KeyCode.LEFT, false);
			} else if (this.keys.getOrDefault(KeyCode.UP, false)){
				MainApplication.audio.get("rotate.wav").play();
				this.fallingTetris.rotate();
				this.keys.put(KeyCode.UP, false);
			} else if (this.keys.getOrDefault(KeyCode.SPACE, false)){
				if (this.fallingTetris.isFalling()){
					MainApplication.audio.get("falling.wav").play();
					Tetris shadow = createTempTetris();
					score += (shadow.getMinY()-this.fallingTetris.getMaxY()-1)*10;
					this.fallingTetris.stop();
					this.fallingTetris.setX(shadow.getX());
					this.fallingTetris.setY(shadow.getY());
					this.keys.put(KeyCode.SPACE, false);
				}
			} else if (this.keys.getOrDefault(KeyCode.H, false)){
				holdPiece();
				this.keys.put(KeyCode.H, false);
			}
		}

		// Difficulty
		if (this.keys.getOrDefault(KeyCode.DIGIT1, false)){
			difficulty = 1;
			FALLING_SPEED = getSpeed();
			this.fallTime = FALLING_SPEED;
			this.keys.put(KeyCode.DIGIT1, false);
		} else if (this.keys.getOrDefault(KeyCode.DIGIT2, false)){
			difficulty = 2;
			FALLING_SPEED = getSpeed();
			this.fallTime = FALLING_SPEED;
			this.keys.put(KeyCode.DIGIT2, false);
		} else if (this.keys.getOrDefault(KeyCode.DIGIT3, false)){
			difficulty = 3;
			FALLING_SPEED = getSpeed();
			this.fallTime = FALLING_SPEED;
			this.keys.put(KeyCode.DIGIT3, false);
		} else if (this.keys.getOrDefault(KeyCode.DIGIT4, false)){
			difficulty = 4;
			FALLING_SPEED = getSpeed();
			this.fallTime = FALLING_SPEED;
			this.keys.put(KeyCode.DIGIT4, false);
		} else if (this.keys.getOrDefault(KeyCode.DIGIT5, false)){
			difficulty = 5;
			FALLING_SPEED = getSpeed();
			this.fallTime = FALLING_SPEED;
			this.keys.put(KeyCode.DIGIT5, false);
		} else if (this.keys.getOrDefault(KeyCode.DIGIT6, false)){
			difficulty = 6;
			FALLING_SPEED = getSpeed();
			this.fallTime = FALLING_SPEED;
			this.keys.put(KeyCode.DIGIT6, false);
		} else if (this.keys.getOrDefault(KeyCode.DIGIT7, false)){
			difficulty = 7;
			FALLING_SPEED = getSpeed();
			this.fallTime = FALLING_SPEED;
			this.keys.put(KeyCode.DIGIT7, false);
		} else if (this.keys.getOrDefault(KeyCode.DIGIT8, false)){
			difficulty = 8;
			FALLING_SPEED = getSpeed();
			this.fallTime = FALLING_SPEED;
			this.keys.put(KeyCode.DIGIT8, false);
		} else if (this.keys.getOrDefault(KeyCode.DIGIT9, false)){
			difficulty = 9;
			FALLING_SPEED = getSpeed();
			this.fallTime = FALLING_SPEED;
			this.keys.put(KeyCode.DIGIT9, false);
		} else if (this.keys.getOrDefault(KeyCode.DIGIT0, false)){
			difficulty = 0;
			FALLING_SPEED = getSpeed();
			this.fallTime = FALLING_SPEED;
			this.keys.put(KeyCode.DIGIT0, false);
		}

		this.world.render(gc);

		if (this.fallingTetris != null && this.fallingTetris.isFalling()){
			gc.save();
			gc.translate(this.world.getX(), this.world.getY());
			createTempTetris().render(gc);
			gc.restore();
		}

		gc.setFill(Color.BLACK);
		gc.setFont(MAIN_FONT);
		gc.setTextAlign(TextAlignment.CENTER);
		gc.fillText("Score: "+score+"   Highscore: "+highscore+"   Difficulty: "+difficulty, WIDTH/2, HEIGHT*0.1);

		gc.setStroke(Color.BLACK);
		gc.setLineWidth(3);
		gc.setFill(Color.BLACK);
		double xp = this.world.getX()-Tetris.SIZE*5.5;
		gc.fillText("NEXT", xp+Tetris.SIZE*2+10, HEIGHT*0.65);
		gc.strokeRect(xp, HEIGHT*0.67, Tetris.SIZE*4.8, Tetris.SIZE*4.8);
		Tetris.render(gc, this.nextPiece, xp+Tetris.SIZE*0.4, HEIGHT*0.67+Tetris.SIZE*0.4);

		// Hold
		gc.fillText("HOLD", xp+Tetris.SIZE*2+10, HEIGHT*0.4);
		this.holdRect = new Rectangle2D(xp, HEIGHT*0.42, Tetris.SIZE*4.8, Tetris.SIZE*4.8);
		gc.strokeRect(this.holdRect.getMinX(), this.holdRect.getMinY(), this.holdRect.getWidth(), this.holdRect.getHeight());
		if (this.holdPiece != null) Tetris.render(gc, this.holdPiece, xp+Tetris.SIZE*0.4, HEIGHT*0.42+Tetris.SIZE*0.4);
	}
	
	public static void main(String[] args){
		launch(args);
	}
}
