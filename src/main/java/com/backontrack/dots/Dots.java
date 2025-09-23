package com.backontrack.dots;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.backontrack.dots.Constants.*;

public class Dots extends Application {

    private Canvas canvas;
    private GridPane root;
    private GridPane menu;
    private Label title = new Label();
    private Label inputPlayerName = new Label();
    private TextField playerName = new TextField();
    private Button submit = new Button();
    private Button newGame = new Button();
    private Button exit = new Button();
    private Player player = new Player("Player");
    //private Player computer = new Player("Computer");

    private Random temporaryRandom = new Random();

    private int[][] grid = new int[GRID_SIZE_Y][GRID_SIZE_X]; //0 - empty, 1 - player, 2 - computer
    private int initialCol;
    private int initialRow;
    private int selectedRow = -1;
    private int selectedCol = -1;

    private final DotMap dotMap = new DotMap();
    PolygonDetector polygonDetector = new PolygonDetector();


    @Override
    public void start(Stage primaryStage) throws InterruptedException{
        canvas = new Canvas(GRID_SIZE_X * CELL_SIZE + 2 * PADDING, GRID_SIZE_X * CELL_SIZE + 2 * PADDING);

        title.setTranslateX(50);
        title.setTranslateY(50);
        title.setText("D O T S");
        Font titleFont = new Font(45);
        title.setFont(titleFont);
        title.setTextFill(Color.BLUE);

        inputPlayerName.setTranslateX(50);
        inputPlayerName.setTranslateY(270);
        inputPlayerName.setText("Your name: " + player.getPlayerName());

        playerName.setTranslateX(50);
        playerName.setTranslateY(320);
        playerName.setText("Input your name.");
        playerName.setPrefWidth(150);
        playerName.setOnMouseClicked((e) -> {
            playerName.clear();
        });

        submit.setText("Submit");
        submit.setTranslateX(50);
        submit.setTranslateY(350);
        submit.setPrefSize(80, 19);
        submit.setOnAction((e) -> {
            player.setPlayerName(playerName.getText());
            playerName.setText("");
//            rounds = 0;
//            wins = 0;
//            roundsAndWinsCount.setText("Rounds: " + rounds + ". Wins: " + wins + ". Result: " + result() + "%.");
            playerName.setText("");
            inputPlayerName.setText("Your name: " + player.getPlayerName());
        });

        newGame.setText("NEW GAME"); //TODO
        newGame.setTranslateX(50);
        newGame.setTranslateY(180);
        newGame.setPrefSize(100, 19);
        newGame.setOnAction((e) -> {
//            for (int i = 0; i < 9; i++) {
//                buttons.get(i).setDisable(false);
//                buttons.get(i).setGraphic(null);
//                buttons.get(i).setId("");
//                removed.removeAll(removed);
//                status.setText("");
//                canvas.toBack();
//                gc.clearRect(0, 0, 530, 530);
//            }
        });

        exit.setText("EXIT");
        exit.setTranslateX(50);
        exit.setTranslateY(500);
        exit.setPrefSize(100, 19);
        exit.setOnAction((e) -> {
            Platform.exit();
            System.exit(0);
        });

        drawGrid();

        canvas.setOnMouseClicked(this::handleMouseClick);



        menu = new GridPane();
        menu.setAlignment(Pos.TOP_CENTER);
        //menu.setPadding(new Insets(0, 0, 0, 0));
        menu.setHgap(5);
        menu.setVgap(5);
        menu.add(title, 2, 0);
        menu.add(inputPlayerName, 2, 0);
        menu.add(playerName, 2, 0);
        menu.add(submit, 2, 0);
        menu.add(newGame, 2, 0);
        menu.add(exit, 2, 0);

        root = new GridPane();
        root.add(canvas, 0, 0);
        root.add(menu, 1, 0);

        root.getColumnConstraints().add(new ColumnConstraints(canvas.getWidth() + 100));
        root.getColumnConstraints().add(new ColumnConstraints(200));

        Scene scene = new Scene(root, 1550, 870, Color.WHITE);

        primaryStage.setTitle("Dots");
        primaryStage.setScene(scene);

        dotMap.createDotMap();

        primaryStage.show();
    }

    private void drawGrid() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.setLineWidth(1);

        //Draw grid
        gc.setStroke(Color.LIGHTGRAY);
        for (int i = 0; i < GRID_SIZE_X; i++) {
            int pos = i * CELL_SIZE + PADDING;
            gc.strokeLine(pos, PADDING + CELL_SIZE, pos, GRID_SIZE_Y * CELL_SIZE + (double) PADDING / 2);
        }

        for (int i = 0; i < GRID_SIZE_Y - 1; i++) {
            int pos = i * CELL_SIZE + PADDING + CELL_SIZE;
            gc.strokeLine(PADDING, pos, GRID_SIZE_X * CELL_SIZE + (double) PADDING / 2, pos);
        }

        //Draw dots
        for (int row = 0; row < GRID_SIZE_Y; row++) {
            for (int col = 0; col < GRID_SIZE_X; col++) {
                if (grid[row][col] == PLAYER) {
                    drawDot(col, row, PLAYERS_COLOR);
                } else if (grid[row][col] == COMPUTER) {
                    drawDot(col, row, COMPUTERS_COLOR);
                }
            }
        }

        //Draw lines
        gc.setLineWidth(3);

        int row1;
        int col1;

        for (Map.Entry<Dot, Set<Dot>> entry : dotMap.getMapOfPlayerAttributedDots().entrySet()) {
            row1 = entry.getKey().getRow();
            col1 = entry.getKey().getCol();
            for (Dot dot : entry.getValue()) {
                int row2 = dot.getRow();
                int col2 = dot.getCol();
                if (grid[row1][col1] == PLAYER && grid[row2][col2] == PLAYER) {
                    gc.setStroke(PLAYERS_COLOR);
                    gc.strokeLine(col1 * CELL_SIZE + 2 * CELL_SIZE,
                            row1 * CELL_SIZE + 2 * CELL_SIZE,
                            col2 * CELL_SIZE + 2 * CELL_SIZE,
                            row2 * CELL_SIZE + 2 * CELL_SIZE
                    );
                }
            }
        }

        for (Map.Entry<Dot, Set<Dot>> entry : dotMap.getMapOfComputerAttributedDots().entrySet()) {
            row1 = entry.getKey().getRow();
            col1 = entry.getKey().getCol();

            for (Dot dot : entry.getValue()) {
                int row2 = dot.getRow();
                int col2 = dot.getCol();
                if (grid[row1][col1] == COMPUTER && grid[row2][col2] == COMPUTER) {
                    gc.setStroke(COMPUTERS_COLOR);
                    gc.strokeLine(col1 * CELL_SIZE + 2 * CELL_SIZE,
                            row1 * CELL_SIZE + 2 * CELL_SIZE,
                            col2 * CELL_SIZE + 2 * CELL_SIZE,
                            row2 * CELL_SIZE + 2 * CELL_SIZE
                    );
                }
            }
        }
    }

    private void drawDot(int col, int row, Color color) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double x = col * CELL_SIZE + PADDING;
        double y = row * CELL_SIZE + PADDING;

        gc.setFill(color);
        gc.fillOval(x - DOT_RADIUS, y - DOT_RADIUS, DOT_RADIUS * 2, DOT_RADIUS * 2);
    }

    private void handleMouseClick(MouseEvent event) {
        int col = (int) ((event.getX() - PADDING + (double) CELL_SIZE / 2) / CELL_SIZE);
        int row = (int) ((event.getY() - PADDING + (double) CELL_SIZE / 2) / CELL_SIZE);

        if (col < 0 || col >= GRID_SIZE_X || row < 0 || row >= GRID_SIZE_Y) {
            return;
        }

        if (grid[row][col] == 0) {
            //Empty cell: place a dot
            grid[row][col] = PLAYER;
            Dot dot = dotMap.getDot(row, col);
            dot.setPlayer(PLAYER);
            dotMap.attributeDotToPlayer(dot);
            selectedRow = -1;
            selectedCol = -1;

            polygonDetector.updateBlockedDots(dotMap);

            if (polygonDetector.playerHasPossiblePolygon(dotMap)) {
                System.out.println("Player can connect");
                try {
                TimeUnit.MILLISECONDS.sleep(1000);}
                catch (InterruptedException e) {
                    System.out.println(e);
                }
            }

            computersMove(row, col, 3);

        } else if (grid[row][col] == PLAYER) {
            //Clicked on own existing dot
            if (selectedRow == -1 && selectedCol == -1) {
                //First dot selected
                selectedRow = row;
                selectedCol = col;
                initialRow = row;
                initialCol = col;
                System.out.printf("First move: %d, %d selected.", row, col);
            } else if (row == initialRow && col == initialCol) {
                //Last dot selected - draw line
                if (Math.abs(selectedRow - row) <= 1 && Math.abs(selectedCol - col) <= 1) {
                    drawLineBetweenDots(selectedRow, selectedCol, row, col, PLAYER);
                    dotMap.addConnection(dotMap.getDot(selectedRow, selectedCol), dotMap.getDot(row, col));
                    selectedRow = -1;
                    selectedCol = -1;
                    System.out.printf("Last move: %d, %d selected.", row, col);
                }
            } else {
                //Next dot selected - draw line
                if (Math.abs(selectedRow - row) <= 1 && Math.abs(selectedCol - col) <= 1) {
                    drawLineBetweenDots(selectedRow, selectedCol, row, col, PLAYER);
                    dotMap.addConnection(dotMap.getDot(selectedRow, selectedCol), dotMap.getDot(row, col));
                    selectedRow = row;
                    selectedCol = col;
                    System.out.printf("Following move: %d, %d selected.", row, col);
                }
            }
        }

        drawGrid();

    }

    private void drawLineBetweenDots(int row1, int col1, int row2, int col2, int player) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        Color lineColor = (player == PLAYER) ? PLAYERS_COLOR : COMPUTERS_COLOR;

        double y1 = row1 * CELL_SIZE + PADDING;
        double x1 = col1 * CELL_SIZE + PADDING;
        double y2 = row2 * CELL_SIZE + PADDING;
        double x2 = col2 + CELL_SIZE + PADDING;

        gc.setStroke(lineColor);
        gc.setLineWidth(3);
        gc.strokeLine(x1, y1, x2, y2);
    }

    private void computersMove(int lastRow, int lastCol, int difficulty) {
        int row = lastRow;
        int col = lastCol;
        boolean isComputerMoveValid = false;

        while (!isComputerMoveValid) {

            int computerRow = -1;
            int computerCol = -1;

            if (null != dotMap.getAnyPlayersDotAlone()) {
                computerRow = dotMap.getAnyPlayersDotAlone().getRow();
                computerCol = dotMap.getAnyPlayersDotAlone().getCol();

                System.out.printf("\nAlone Dot found: %d, %d\n", computerRow, computerCol);
            } else if (dotMap.isAnyDotAttributedByComputer()) {
                for (Map.Entry<Dot, Set<Dot>> entry : dotMap.getMapOfComputerAttributedDots().entrySet()) {
                    row = entry.getKey().getRow();
                    col = entry.getKey().getCol();

                    System.out.println(entry.getKey());

                    Map<Dot, Dot> temporaryBestDotMap = new HashMap<>();

                    temporaryBestDotMap.put(dotMap.getDot(row + 2, col), dotMap.getDot(row + 1, col));
                    temporaryBestDotMap.put(dotMap.getDot(row - 2, col), dotMap.getDot(row - 1, col));
                    temporaryBestDotMap.put(dotMap.getDot(row, col + 2), dotMap.getDot(row, col + 1));
                    temporaryBestDotMap.put(dotMap.getDot(row, col - 2), dotMap.getDot(row, col - 1));
                    temporaryBestDotMap.put(dotMap.getDot(row + 2, col + 2), dotMap.getDot(row + 1, col + 1));
                    temporaryBestDotMap.put(dotMap.getDot(row + 2, col - 2), dotMap.getDot(row + 1, col - 1));
                    temporaryBestDotMap.put(dotMap.getDot(row - 2, col + 2), dotMap.getDot(row - 1, col + 1));
                    temporaryBestDotMap.put(dotMap.getDot(row - 2, col - 2), dotMap.getDot(row - 1, col - 1));

                    Dot temporaryDot = temporaryBestDotMap.entrySet().stream()
                            .filter(k -> k.getKey().getPlayer() == COMPUTER && k.getValue().getPlayer() == 0)
                            .filter(k -> dotMap.isDotConnectedToAnother(k.getValue()))
                            .map(Map.Entry::getValue)
                            .findFirst()
                            .orElse(null);

                    System.out.println("TEMPORARY DOT");
                    System.out.println(temporaryDot);
                    System.out.println();

                    if (temporaryDot != null && temporaryDot.getPlayer() == 0) {
                        computerRow = temporaryDot.getRow();
                        computerCol = temporaryDot.getCol();
                        System.out.printf("Close dot suitable for connection is found: Dot: %d, %d.", computerRow, computerCol);
                        System.out.println("Grid[row][col]: " + grid[computerRow][computerCol]);
                    } else {
                        System.out.println("No luck");
                        System.out.println("Random close position is being found.");

                        boolean isDotAvailable = false;
                        int counter = 0;

                        while (!isDotAvailable) {
                            System.out.println("Trying to find an available dot");
                            if (counter < 10) {
                                System.out.println("Counter: " + counter);
                                int[] positions = {-3, -2, -1, 0, 1, 2, 3};
                                computerRow = row + positions[temporaryRandom.nextInt(positions.length)];
                                computerCol = col + positions[temporaryRandom.nextInt(positions.length)];

                                counter++;

                            } else {
                                System.out.println("10 tries: find any available dot.");
                                Dot availableDot = dotMap.findFirstAvailableDot();
                                computerRow = availableDot.getRow();
                                computerCol = availableDot.getCol();
                            }

                            if (dotMap.getDot(computerRow, computerCol).getPlayer() == 0) {
                                isDotAvailable = true;
                                System.out.println("Available dot found.");
                            }
                        }
                    }
                }
            } else {
                System.out.println("Random close position is being found.");
                int[] positions = {-1, 0, 1};
                computerRow = row + positions[temporaryRandom.nextInt(positions.length)];
                computerCol = col + positions[temporaryRandom.nextInt(positions.length)];
            }

            if (computerCol >= 0 && computerCol < GRID_SIZE_X && computerRow > 0 && computerRow < GRID_SIZE_Y && grid[computerRow][computerCol] == 0) {
                System.out.println("Dot is available.");
                grid[computerRow][computerCol] = COMPUTER;
                dotMap.attributeDotToComputer(dotMap.getDot(computerRow, computerCol));

                isComputerMoveValid = true;

                System.out.println("Trying to connect");
                tryConnectEverythingToNearest();
                tryToFindAClosedPolygon();
                System.out.println("End of try");
                drawGrid();
            } else {
                if (grid[computerRow][computerCol] != 0) {
                    System.out.printf("Computer's move. Position already used [row %d, col %d].", computerRow, computerCol);
                    System.out.println("Player: " + dotMap.getDot(computerRow, computerCol).getPlayer());
                    //randomTimeoutForResponse();
                } else {
                    System.out.printf("Computer's move. Index out of bounds [row %d, col %d].", computerRow, computerCol);
                }
            }
        }

        System.out.println("COMPUTER'S DOT MAP WITH CONNECTIONS");
        System.out.println(dotMap.getMapOfComputerAttributedDots());
    }

    private void tryToFindAClosedPolygon() {

        Map<Dot, Dot> closedPolygonDots = polygonDetector.findFirstPolygonForComputer(dotMap);

       if (closedPolygonDots.isEmpty()) {
           System.out.println("There are no closed connections of dots.");
       } else {
           System.out.println("Closed polygon found");
           for (Map.Entry<Dot, Dot> entry: closedPolygonDots.entrySet()) {
               int row1 = entry.getKey().getRow();
               int col1 = entry.getKey().getCol();
               int row2 = entry.getValue().getRow();
               int col2 = entry.getValue().getCol();

               drawLineBetweenDots(row1, col1, row2, col2, COMPUTER);

               polygonDetector.updateBlockedDots(dotMap);

               //dotMap.addConnection(entry.getKey(), entry.getValue());
               //System.out.printf("CONNECTIONS BETWEEN DOT1: %d, %d and DOT2: %d, %d added. Draw lines between them. \n", row1, col1, row2, col2);
           }
       }

       drawGrid();


    }

    private void tryConnectEverythingToNearest() {

        if (!dotMap.isAnyDotAttributedByComputer()) {
            return;
        } else {
            System.out.println("There are dots attributed by computer.");
        }

        System.out.println("Map of Computer Dots");
        System.out.println(dotMap.getMapOfComputerAttributedDots());

        for (Map.Entry<Dot, Set<Dot>> entry : dotMap.getMapOfComputerAttributedDots().entrySet()) {
            if (entry.getKey().getPlayer() == COMPUTER) {
                System.out.println("Dot suitable for connection found.");
                int row = entry.getKey().getRow();
                int col = entry.getKey().getCol();
                int bestRow = -1;
                int bestCol = -1;

                for (int r = 0; r < GRID_SIZE_Y; r++) {
                    for (int c = 0; c < GRID_SIZE_X; c++) {
                        if ((r != row || c != col) && grid[r][c] == COMPUTER) {
                            int distX = Math.abs(col - c);
                            int distY = Math.abs(row - r);
                            if (distX <= 1 && distY <= 1) {
                                bestRow = r;
                                bestCol = c;
                            }
                        }
                    }
                }

                if (bestRow != -1 && bestCol != -1) {

                    //drawLineBetweenDots(row, col, bestRow, bestCol, COMPUTER);

                    System.out.println("Creating dots for connection");
                    Dot dot1 = dotMap.getDot(row, col);
                    Dot dot2 = dotMap.getDot(bestRow, bestCol);

                    dotMap.addConnection(dot1, dot2);
                    System.out.printf("CONNECTIONS BETWEEN DOT1: %d, %d and DOT2: %d, %d added.\n", dot1.getRow(), dot1.getCol(), dot2.getRow(), dot2.getCol());
                    System.out.printf("Draw line between: dot1: col %d, row %d and dot2: bestCol %d, bestRow %d", col, row, bestCol, bestRow);
                }
            }
        }

        drawGrid();

        for (Map.Entry<Dot, Set<Dot>> entry1 : dotMap.entrySet()) {
            if (!entry1.getValue().isEmpty()) {
                System.out.println("Dot Map:" + entry1);
            }
        }
    }

    private void randomTimeoutForResponse() {
        int randomTimeoutForResponse = temporaryRandom.nextInt(1000);
        try {
            System.out.println(randomTimeoutForResponse);
            TimeUnit.MILLISECONDS.sleep(randomTimeoutForResponse);
        } catch (InterruptedException e) {
            System.out.println(e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}