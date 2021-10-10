package com.raju.game;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Random;

public class GameBoard {

    public static final int ROWS = 4;
    public static final int COLS = 4;

    private final int startingTiles = 2;
    private Tile[][] board;

    private boolean dead;
    private boolean won;

    private BufferedImage gameBoard;
    private BufferedImage finalBoard;
    private int x;
    private int y;

    private int score = 0;
    private int highScore = 0;
    private Font scoreFont;

    private static int SPACING = 10;
    public static int BOARD_WIDTH = (COLS + 1) * SPACING + COLS * Tile.WIDTH;
    public static int BOARD_HEIGHT = (ROWS + 1) * SPACING + ROWS * Tile.HEIGHT;
    public static int BOARD_ARC_WIDTH = 0;
    public static int BOARD_ARC_HEIGHT = 0;

    private long elapsedMS;
    private long fastestMS;
    private long startTime;
    private String formattedTime = "00:00:000";

    private boolean hasStarted;

    //Saving
    private String saveDataPath;
    private String fileName = "SaveData";

    public GameBoard(int x, int y){
        try{
            saveDataPath = System.getProperty("user.home") + "\\Documents" + "\\2048-SaveGame";
        }
        catch (Exception e){
            e.printStackTrace();
        }

        scoreFont = Game.main.deriveFont(24f);
        this.x = x;
        this.y = y;
        board = new Tile[ROWS][COLS];
        gameBoard = new BufferedImage(BOARD_WIDTH, BOARD_HEIGHT, BufferedImage.TYPE_INT_RGB);
        finalBoard = new BufferedImage(BOARD_WIDTH, BOARD_HEIGHT, BufferedImage.TYPE_INT_RGB);
        startTime = System.nanoTime();

        loadHighScore();
        createBoardImage();
        start();
    }

    private void createSaveData(){
        try{
            File file = new File(saveDataPath, fileName);

            FileWriter output = new FileWriter(file);
            BufferedWriter writer = new BufferedWriter(output);
            writer.write("" + 0);
            writer.newLine();
            writer.write("" + Integer.MAX_VALUE);
            writer.close();
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    private void loadHighScore(){
        try{
            File f = new File(saveDataPath, fileName);
            if(!f.isFile()){
                createSaveData();
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
            highScore = Integer.parseInt(reader.readLine());
            fastestMS = Long.parseLong(reader.readLine());
            reader.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private void setHighScore(){
        FileWriter output = null;

        try {
            File f = new File(saveDataPath, fileName);
            output = new FileWriter(f);
            BufferedWriter writer = new BufferedWriter(output);

            writer.write("" + highScore);

            writer.newLine();
            if(elapsedMS <= fastestMS && won){
                writer.write("" + elapsedMS);
            }
            else{
                writer.write("" + fastestMS);
            }

            writer.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private void createBoardImage(){
        Graphics2D g = (Graphics2D) gameBoard.getGraphics();
        g.setColor(new Color(0x776e65));
        g.fillRoundRect(0, 0, BOARD_WIDTH, BOARD_HEIGHT, BOARD_ARC_WIDTH, BOARD_ARC_HEIGHT);
        g.setColor(new Color(0xbbada0));

        for(int row = 0 ; row < ROWS ; row++){
            for(int col = 0 ; col < COLS ; col++){
                int x = SPACING + SPACING * col + Tile.WIDTH * col;
                int y = SPACING + SPACING * row + Tile.HEIGHT * row;
                g.fillRoundRect(x, y, Tile.WIDTH, Tile.HEIGHT, Tile.ARC_WIDTH, Tile.ARC_HEIGHT);
            }
        }
        g.dispose();
    }

    private void start(){
        for(int i=0 ; i < startingTiles ; i++){
            spawnRandom();
        }
    }

    private void spawnRandom(){
        Random random = new Random();
        boolean notValid = true;

        while(notValid){
            int row = random.nextInt(ROWS);
            int col = random.nextInt(COLS);
            Tile current = board[row][col];
            if(current == null){
                int value = random.nextInt(10) < 8 ? 2 : 4;
                Tile tile = new Tile(value, getTileX(col), getTileY(row));
                board[row][col] = tile;
                notValid = false;
            }
        }
    }

    public int getTileX(int col){
        return SPACING + col * Tile.WIDTH + col * SPACING;
    }

    public int getTileY(int row){
        return SPACING + row * Tile.HEIGHT + row * SPACING;
    }

    public void render(Graphics2D g){
        Graphics2D g2d = (Graphics2D) finalBoard.getGraphics();
        g2d.drawImage(gameBoard, 0, 0, null);

        for(int row = 0; row < ROWS ; row++){
            for(int col = 0 ; col < COLS ; col++){
                Tile current = board[row][col];
                if(current == null) continue;
                current.render(g2d);
            }
        }

        g.drawImage(finalBoard, x, y, null);
        g2d.dispose();

        g.setColor(Color.lightGray);
        g.setFont(scoreFont);
        g.drawString("" + score, 30, 40);
        g.setColor(Color.red);
        g.drawString("Best: " + highScore, Game.WIDTH - DrawUtils.getMessageWidth("Best: " + highScore, scoreFont, g) - 20, 40);
        g.setColor(Color.LIGHT_GRAY);
        g.drawString("Time: " + formattedTime, 30, 70);
        g.setColor(Color.red);
        g.drawString("Fastest: " + formatTime(fastestMS), Game.WIDTH - DrawUtils.getMessageWidth("Fastest: " + formatTime(fastestMS), scoreFont, g) - 20, 70);
    }

    public void update(){
        if(!won && !dead){
            if(hasStarted){
                elapsedMS = (System.nanoTime() - startTime) / 1000000;
                formattedTime = formatTime(elapsedMS);
            }
            else {
                startTime = System.nanoTime();
            }
        }

        checkKeys();

        if(score >= highScore){
            highScore = score;
        }

        for(int row = 0 ; row < ROWS ; row++){
            for(int col = 0 ; col < COLS ; col++){
                Tile current = board[row][col];
                if(current == null) continue;
                current.update();
                resetPosition(current, row, col);
                if(current.getValue() == 2048){
                    won = true;
                }
            }
        }
    }

    private String formatTime(long millis){
        String formattedTime;

        String hourFormat = "";
        int hours = (int)(millis / 3600000);
        if(hours >= 1){
            millis -= hours * 3600000;
            if(hours < 10){
                hourFormat = "0" + hours;
            }
            else {
                hourFormat = "" + hours;
            }
            hourFormat += ":";
        }

        String minuteFormat;
        int minutes = (int)(millis / 60000);
        if(minutes >= 1){
            millis -= minutes * 60000;
            if(minutes < 10){
                minuteFormat = "0" + minutes;
            }
            else {
                minuteFormat = "" + minutes;
            }
        }
        else{
            minuteFormat = "00";
        }
        minuteFormat += ":";

        String secondFormat;
        int seconds = (int)(millis / 1000);
        if(seconds >= 1){
            millis -= seconds * 1000;
            if(seconds < 10){
                secondFormat = "0" + seconds;
            }
            else {
                secondFormat = "" + seconds;
            }
        }
        else{
            secondFormat = "00";
        }
        secondFormat += ":";

        String milliFormat;
        if(millis > 99){
            milliFormat = "" + millis;
        }
        else if(millis > 9){
            milliFormat = "0" + millis;
        }
        else{
            milliFormat = "00" + millis;
        }

        formattedTime = hourFormat + minuteFormat + secondFormat + milliFormat;
        return formattedTime;
    }

    private void resetPosition(Tile current, int row, int col){
        if(current == null) return;

        int x = getTileX(col);
        int y = getTileY(row);

        int distX = current.getX() - x;
        int distY = current.getY() - y;

        if(Math.abs(distX) < Tile.SLIDE_SPEED){
            current.setX(current.getX() - distX);
        }

        if(Math.abs(distY) < Tile.SLIDE_SPEED){
            current.setY(current.getY() - distY);
        }

        if(distX < 0){
            current.setX(current.getX() + Tile.SLIDE_SPEED);
        }
        if(distY < 0){
            current.setY(current.getY() + Tile.SLIDE_SPEED);
        }
        if(distX > 0){
            current.setX(current.getX() - Tile.SLIDE_SPEED);
        }
        if(distY > 0){
            current.setY(current.getY() - Tile.SLIDE_SPEED);
        }
    }

    private boolean move(int row, int col, int horizontalDirection, int verticalDirection, Direction dir){
        boolean canMove = false;

        Tile current = board[row][col];
        if(current == null) return false;
        boolean move = true;
        int newCol = col;
        int newRow = row;
        while(move){
            newCol += horizontalDirection;
            newRow += verticalDirection;
            if(checkOutOfBounds(dir, newRow, newCol)) break;
            if(board[newRow][newCol] == null){
                board[newRow][newCol] = current;
                board[newRow - verticalDirection][newCol - horizontalDirection] = null;
                board[newRow][newCol].setSlideTo(new Point(newRow, newCol));
                canMove = true;
            }
            else if(board[newRow][newCol].getValue() == current.getValue() && board[newRow][newCol].canCombine()){
                board[newRow][newCol].setCanCombine(false);
                board[newRow][newCol].setValue(board[newRow][newCol].getValue() * 2);
                canMove = true;
                board[newRow - verticalDirection][newCol - horizontalDirection] = null;
                board[newRow][newCol].setSlideTo(new Point(newRow, newCol));
                board[newRow][newCol].setCombineAnimation(true);
                score += board[newRow][newCol].getValue();
            }
            else{
                move = false;
            }
        }

        return canMove;
    }

    private boolean checkOutOfBounds(Direction dir, int row, int col) {
        if(dir == Direction.LEFT){
            return col < 0;
        }
        else if(dir == Direction.RIGHT){
            return col > COLS - 1;
        }
        else if(dir == Direction.UP){
            return row < 0;
        }
        else if(dir == Direction.DOWN){
            return row > ROWS - 1;
        }
        return false;
    }

    private void moveTiles(Direction dir){
        boolean canMove = false;
        int horizontalDirection = 0;
        int verticalDirection = 0;

        if(dir == Direction.LEFT){
            horizontalDirection = -1;
            for(int row = 0 ; row < ROWS ; row++){
                for(int col = 0 ; col < COLS ; col++){
                    if(!canMove){
                        canMove = move(row, col, horizontalDirection, verticalDirection, dir);
                    }
                    else move(row, col, horizontalDirection, verticalDirection, dir);
                }
            }
        }

        else if(dir == Direction.RIGHT){
            horizontalDirection = 1;
            for(int row = 0 ; row < ROWS ; row++){
                for(int col = COLS - 1 ; col >= 0 ; col--){
                    if(!canMove){
                        canMove = move(row, col, horizontalDirection, verticalDirection, dir);
                    }
                    else move(row, col, horizontalDirection, verticalDirection, dir);
                }
            }
        }

        else if(dir == Direction.UP){
            verticalDirection = -1;
            for(int row = 0 ; row < ROWS ; row++){
                for(int col = 0 ; col < COLS ; col++){
                    if(!canMove){
                        canMove = move(row, col, horizontalDirection, verticalDirection, dir);
                    }
                    else move(row, col, horizontalDirection, verticalDirection, dir);
                }
            }
        }

        else if(dir == Direction.DOWN){
            verticalDirection = 1;
            for(int row = ROWS - 1 ; row >= 0 ; row--){
                for(int col = 0 ; col < COLS ; col++){
                    if(!canMove){
                        canMove = move(row, col, horizontalDirection, verticalDirection, dir);
                    }
                    else move(row, col, horizontalDirection, verticalDirection, dir);
                }
            }
        }

        else {
            System.out.println(dir + " is not a valid direction.");
        }

        for(int row = 0 ; row < ROWS ; row++){
            for(int col = 0 ; col<COLS ; col++){
                Tile current = board[row][col];
                if(current == null) continue;
                current.setCanCombine(true);
            }
        }

        if(canMove){
            spawnRandom();
            checkDead();
        }
    }

    private void checkDead(){
        for(int row = 0 ; row < ROWS ; row++){
            for(int col = 0 ; col < COLS ; col++){
                if(board[row][col] == null) return;
                if(checkSurroundingTiles(row, col, board[row][col])){
                    return;
                }
            }
        }

        dead = true;
        if(score >= highScore){
            highScore = score;
        }
        setHighScore();
    }

    private boolean checkSurroundingTiles(int row, int col, Tile current){
        if(row > 0){
            Tile check = board[row - 1][col];
            if(check == null) return true;
            if(current.getValue() == check.getValue()) return true;
        }
        if(row < ROWS - 1){
            Tile check = board[row + 1][col];
            if(check == null) return true;
            if(current.getValue() == check.getValue()) return true;
        }
        if(col > 0){
            Tile check = board[row][col - 1];
            if(check == null) return true;
            if(current.getValue() == check.getValue()) return true;
        }
        if(col < COLS - 1){
            Tile check = board[row][col + 1];
            if(check == null) return true;
            if(current.getValue() == check.getValue()) return true;
        }
        return false;
    }

    private void checkKeys(){
        if(Keyboard.typed(KeyEvent.VK_LEFT)){
            moveTiles(Direction.LEFT);
            if(!hasStarted) hasStarted = true;
        }
        if(Keyboard.typed(KeyEvent.VK_RIGHT)){
            moveTiles(Direction.RIGHT);
            if(!hasStarted) hasStarted = true;
        }
        if(Keyboard.typed(KeyEvent.VK_UP)){
            moveTiles(Direction.UP);
            if(!hasStarted) hasStarted = true;
        }
        if(Keyboard.typed(KeyEvent.VK_DOWN)){
            moveTiles(Direction.DOWN);
            if(!hasStarted) hasStarted = true;
        }
    }

}
