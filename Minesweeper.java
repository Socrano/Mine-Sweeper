import tester.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import javalib.impworld.*;
import javalib.worldimages.*;
import java.awt.Color;


// represents the world where all the components are drawn on
class GameWorld extends World {
  int gridLength;
  int gridWidth;
  int numMines;
  int flagNum;
  ArrayList<ArrayList<IGamePiece>> grid;
  Random rand;

  // default constructor
  GameWorld(int gridLength, int gridWidth, int numMines) {
    this.gridLength = gridLength;
    this.gridWidth = gridWidth;
    this.numMines = numMines;
    this.flagNum = 0;
    this.grid = new ArrayList<ArrayList<IGamePiece>>();
    this.rand = new Random();

    this.initCells();
    this.placeMines();
    this.updateNeighbors();
  }

  // convenience constructor
  GameWorld(int gridLength, int gridWidth, int numMines, int seed) {
    this.gridLength = gridLength;
    this.gridWidth = gridWidth;
    this.numMines = numMines;
    this.flagNum = 0;
    this.grid = new ArrayList<ArrayList<IGamePiece>>();
    this.rand = new Random(seed);
  }
  
  /* TEMPLATE
   * Fields:
   * ... this.gridLength ...                                     --int
   * ... this.gridWidth ...                                      --int
   * ... this.numMines ...                                       --int
   * ... this.grid ...                                           --ArrayList<ArrayList<IGamePiece>>
   * ... this.rand ...                                           --Random
   * ... this.isGameOver ...                                     --boolean
   * Methods:
   * ... this.initCells() ...                                    --void
   * ... this.placeMines() ...                                   --void
   * ... this.updateNeighbors() ...                              --void
   * ... this.checkOutOfBounds(int n) ...                        --int
   * ... this.makeScene() ...                                    --WorldScene
   * ... this.onMouseClicked(Posn pos, String buttonName) ...    --void
   * ... this.floodFill(int rowIdx, int colIdx) ...              --void
   * ... this.lastScene(String msg) ...                          --WorldScene
   */

  // initializes all gridLength*gridWidth cells in this.grid
  void initCells() {
    for (int r = 0; r < this.gridLength; r++) {
      ArrayList<IGamePiece> row = new ArrayList<IGamePiece>();
      for (int c = 0; c < this.gridWidth; c++) {
        row.add(new Cell());
      }
      this.grid.add(row);
    }
  }

  // randomly places mines in this.grid
  void placeMines() {
    ArrayList<Mine> mines = new ArrayList<Mine>();
    while (mines.size() < this.numMines) {
      int r = rand.nextInt(this.gridLength);
      int c = rand.nextInt(this.gridWidth);
      Mine m = new Mine(r, c);
      if (mines.size() == 0) {
        mines.add(m);
      }
      else {
        boolean result = false;
        for (Mine mine : mines) {
          result = result || mine.sameMine(m);
        }
        if (!result) {
          mines.add(m);
        }
      }
    }
    for (Mine mine : mines) {
      grid.get(mine.rowNum).set(mine.colNum, mine);
    }
  }

  // updates the list of neighbors for each cell
  void updateNeighbors() {
    for (int r = 0; r < grid.size(); r++) {
      for (int c = 0; c < grid.get(r).size(); c++) {
        int startRow = this.checkOutOfBounds(r - 1, this.grid.size());
        int endRow = this.checkOutOfBounds(r + 1, this.grid.size());
        int startCol = this.checkOutOfBounds(c - 1, this.grid.get(r).size());
        int endCol = this.checkOutOfBounds(c + 1, this.grid.get(r).size());
        for (int row = startRow; row <= endRow; row++) {
          for (int col = startCol; col <= endCol; col++) {
            if (row == r && col == c) {
              continue;
            }
            grid.get(r).get(c).addNeighbor(grid.get(row).get(col));
          }
        }
        grid.get(r).get(c).countNeighbor();
      }
    }
  }

  // prevents the given number to be OutOfBounds in the 2D array
  int checkOutOfBounds(int num, int size) {
    if (num < 0) {
      return 0;
    }
    else if (num >= size) {
      return size - 1;
    }
    return num;
  }

  // draws the game
  public WorldScene makeScene() {
    WorldScene background = this.getEmptyScene();
    WorldImage bg = new RectangleImage(20 * this.gridWidth, 20 * this.gridLength,
        OutlineMode.SOLID, Color.CYAN);
    for (int r = 0; r < grid.size(); r++) {
      for (int c = 0; c < grid.get(r).size(); c++) {
        bg = this.grid.get(r).get(c).drawAt(0, 0, bg);
        background.placeImageXY(bg, 20 * r + 10, 20 * c + 10);
      }
    }
    return background;
  }
 
    
  
  // detects which mouse button is being pressed, and reacts according
  // if a Mine is pressed, game over
  // if not, then the Cell will be represented as clicked
  public void onMouseClicked(Posn pos, String buttonName) {
    int rowIndex = (int) Math.ceil(pos.x / 20);
    int colIndex = (int) Math.ceil(pos.y / 20);
    IGamePiece piece = this.grid.get(rowIndex).get(colIndex);
    if (buttonName.equals("LeftButton")) {
      if (piece.isFlag()) {
        System.out.println("Are you sure you want to click that?");
      }
      else if (piece.isMine()) {
        this.endOfWorld("lost");
      }
      else if (piece.isCell()) {
        this.floodFill(rowIndex, colIndex);
      }
    }
    else if (buttonName.equals("RightButton")) {
      if (piece.isFlag()) {
        Flag f = (Flag) piece;
        if (f.isMine) {
          this.grid.get(rowIndex).set(colIndex, new Mine(rowIndex, colIndex));
          this.numMines++;
          //flagNum--;
        } else {
          this.grid.get(rowIndex).set(colIndex, new Cell(f.neighbors, f.mineNum, false));
          //flagNum--;
        }
      } else if (piece.isMine()) {
        this.grid.get(rowIndex).set(colIndex, new Flag(true));
        this.numMines--;
        //flagNum++;
      }
      else if (piece.isCell()) {
        Cell c = (Cell) piece;
        if (!c.isClicked) {
          this.grid.get(rowIndex).set(colIndex, new Flag(c.neighbors, c.mineNum));
          //flagNum++;
        }
      }
    }
    if (this.numMines == 0) {
      this.endOfWorld("win");
    }
  }
    
  // achieves the flood-fill effect
  void floodFill(int rowIdx, int colIdx) {
    Cell c = (Cell) this.grid.get(rowIdx).get(colIdx);
    if (c.isClicked) {
      return;
    } 
    if (c.mineNum == 0) {
      this.grid.get(rowIdx).set(colIdx, new Cell(c.neighbors, c.mineNum, true));
      int startRow = this.checkOutOfBounds(rowIdx - 1, this.grid.size());
      int endRow = this.checkOutOfBounds(rowIdx + 1, this.grid.size());
      int startCol = this.checkOutOfBounds(colIdx - 1, this.grid.get(rowIdx).size());
      int endCol = this.checkOutOfBounds(colIdx + 1, this.grid.get(rowIdx).size());
      for (int row = startRow; row <= endRow; row++) {
        for (int col = startCol; col <= endCol; col++) {
          if (row == rowIdx && col == colIdx) {
            continue;
          }
          else {
            if (this.grid.get(row).get(col).isCell()) {
              this.floodFill(row, col);
            }
          }
        } 
      }
    }
    else {
      this.grid.get(rowIdx).set(colIdx, new Cell(c.neighbors, c.mineNum, true));
    }
  }
  
  // ends the game if the user clicks on a Mine
  public WorldScene lastScene(String msg) {
    WorldScene background = this.getEmptyScene();
    if (msg.equals("lost")) {
      background.placeImageXY(new TextImage("You Lost :(", 24, FontStyle.BOLD, Color.RED), 150,
          150);
    }
    else if (msg.equals("win")) {
      background.placeImageXY(new TextImage("You won!!!", 24, FontStyle.BOLD, Color.GREEN), 150,
          150);
    }
    return background;
  }
}

// represents a component of the game
interface IGamePiece {
  // adds an IGamePiece into an ArrayList of IGamePieces
  void addNeighbor(IGamePiece piece);
  
  // determines whether this IGamePiece is a Cell or not
  boolean isCell();

  // determines whether this IGamePiece is a Mine or not
  boolean isMine();
  
  // determines whether this IGamePiece is a Flag or not
  boolean isFlag();

  // updates the number of mines of this IGamePiece
  void countNeighbor();

  // draws this IGamePiece onto the background
  WorldImage drawAt(int row, int col, WorldImage background);
}

// represents a safe cell
class Cell implements IGamePiece {
  ArrayList<IGamePiece> neighbors;
  int mineNum;
  Color color;
  boolean isClicked;

  // default constructor
  Cell() {
    this.neighbors = new ArrayList<IGamePiece>();
    this.mineNum = 0;
    this.color = Color.CYAN;
    this.isClicked = false;
  }

  // convenience constructor
  Cell(ArrayList<IGamePiece> neighbors, int mineNum, boolean isClicked) {
    this.neighbors = neighbors;
    this.mineNum = mineNum;
    this.color = Color.CYAN;
    this.isClicked = isClicked;
  }
  
  /* TEMPLATE
   * Fields:
   * ... this.neighbors ...                                         --ArrayList<IGamePiece>
   * ... this.mineNum ...                                           --int
   * ... this.color ...                                             --Color
   * ... this.isClicked ...                                         --boolean
   * Methods:
   * ... this.addNeighbor(IGamePiece piece) ...                     --void
   * ... this.isCell() ...                                          --boolean
   * ... this.isMine() ...                                          --boolean
   * ... this.isFlag() ...                                          --boolean
   * ... this.countNeighbor() ...                                   --void
   * ... this.drawAt(int row, int col, WorldImage background) ...   --WorldImage       
   * ... this.diffColorNums(Integer n) ...                          --WorldImage
   */

  // adds this IGamePiece to this Cell's list of neighbors
  public void addNeighbor(IGamePiece piece) {
    this.neighbors.add(piece);
  }
  
  // returns true because this is a Cell
  public boolean isCell() {
    return true;
  }

  // returns false because this Cell is not a Mine
  public boolean isMine() {
    return false;
  }
  
  // returns false because this Cell is not a Flag
  public boolean isFlag() {
    return false;
  }

  // adds 1 to this cell's number of Mines if a respective element
  // in this Cell's list of neighbors is a Mine
  public void countNeighbor() {
    for (IGamePiece g : neighbors) {
      if (g.isMine()) {
        this.mineNum = this.mineNum + 1;
      }
    }
  }

  // draws this Cell onto the given background
  public WorldImage drawAt(int row, int col, WorldImage background) {
    if (!isClicked) {
      background = new OverlayOffsetImage(
          new RectangleImage(20, 20, OutlineMode.OUTLINE, Color.BLACK), row, col,
          new RectangleImage(20, 20, OutlineMode.SOLID, this.color));
      return background;
    }
    else {
      Integer num = this.mineNum;
      if (num != 0) {
        background = new OverlayOffsetImage(this.diffColorNums(num), row, col,
            new OverlayImage(new RectangleImage(20, 20, OutlineMode.OUTLINE, Color.BLACK),
                new RectangleImage(20, 20, OutlineMode.SOLID, Color.lightGray)));
      }
      else {
        background = new OverlayImage(new RectangleImage(20, 20, OutlineMode.OUTLINE, Color.BLACK),
            new RectangleImage(20, 20, OutlineMode.SOLID, Color.lightGray));
      }
      return background;
    }
  }

  // produce different colored text images based on the number of mines
  WorldImage diffColorNums(Integer n) {
    if (n == 1) {
      return new TextImage(n.toString(), Color.BLUE);
    }
    else if (n == 2) {
      return new TextImage(n.toString(), Color.GREEN);
    }
    else if (n == 3) {
      return new TextImage(n.toString(), Color.ORANGE);
    }
    return new TextImage(n.toString(), Color.RED);
  }
}

// represents a mine
class Mine implements IGamePiece {
  int rowNum;
  int colNum;
  boolean isClicked;

  // default constructor
  Mine(int rowNum, int colNum) {
    this.rowNum = rowNum;
    this.colNum = colNum;
    this.isClicked = false;
  }

  // Convenience constructor
  Mine(int rowNum, int colNum, boolean isClicked) {
    this.rowNum = rowNum;
    this.colNum = colNum;
    this.isClicked = isClicked;
  }

  /* TEMPLATE
   * Fields:
   * ... this.rowNum ...                                                  --int
   * ... this.colNum ...                                                  --int
   * ... this.isClicked ...                                               --boolean
   * Methods:
   * ... this.addNeighbor(IGamePiece piece) ...                           --void
   * ... this.isCell() ...                                                --boolean
   * ... this.isMine() ...                                                --boolean
   * ... this.isFlag() ...                                                --boolean
   * ... this.countNeighbor() ...                                         --void
   * ... this.drawAt(int row, int col, WorldImage background) ...         --WorldImage
   * ... this.sameMine(Mine m) ...                                        --boolean
   */
  
  // doesn't do anything because this Mine doesn't have a list of neighbors
  public void addNeighbor(IGamePiece piece) {
    return;
  }
  
  // returns false because this is not a Cell
  public boolean isCell() {
    return false;
  }

  // returns true because this is a Mine
  public boolean isMine() {
    return true;
  }
  
  // returns false because this is not a Flag
  public boolean isFlag() {
    return false;
  }

  // doesn't do anything because it doesn't have a list of neighbors
  // nor a number of Mines
  public void countNeighbor() {
    return;
  }

  // draws this Mine onto the given background
  public WorldImage drawAt(int row, int col, WorldImage background) {
    if (!isClicked) {
      background = new OverlayOffsetImage(
          new RectangleImage(20, 20, OutlineMode.OUTLINE, Color.BLACK), row, col,
          new RectangleImage(20, 20, OutlineMode.SOLID, Color.CYAN));
      return background;
    }
    else {
      background = new OverlayOffsetImage(new CircleImage(5, OutlineMode.SOLID, Color.RED), row,
          col, new OverlayImage(new RectangleImage(20, 20, OutlineMode.OUTLINE, Color.BLACK),
              new RectangleImage(20, 20, OutlineMode.SOLID, Color.CYAN)));
      return background;
    }
  }

  // returns if this Mine is same as that Mine
  boolean sameMine(Mine m) {
    return this.rowNum == m.rowNum && this.colNum == m.colNum;
  }
}

// represents a flag
class Flag implements IGamePiece {
  ArrayList<IGamePiece> neighbors;
  int mineNum;
  boolean isMine;

  // default constructor
  Flag(boolean isMine) {
    this.isMine = isMine;
  }
  
  // convenience constructor
  Flag(ArrayList<IGamePiece> neighbors, int mineNum) {
    this.neighbors = neighbors;
    this.mineNum = mineNum;
    this.isMine = false;
  }
  
  /* TEMPLATE
   * Fields:
   * ... this.isMine ...                                                       --boolean
   * Methods:
   * ... this.addNeighbor(IGamePiece piece) ...                                --void       
   * ... this.isCell() ...                                                     --boolean
   * ... this.isMine() ...                                                     --boolean
   * ... this.isFlag() ...                                                     --boolean
   * ... this.countNeighbor() ...                                              --void
   * ... this.WorldImage drawAt(int row, int col, WorldImage background) ...   --WorldImage
   */

  // doesn't do anything because this Mine doesn't have a list of neighbors
  public void addNeighbor(IGamePiece piece) {
    return;
  }
  
  // returns false because this is not a Cell
  public boolean isCell() {
    return false;
  }

  // returns if this flag is sitting on a Mine or not
  public boolean isMine() {
    return this.isMine;
  }
  
  // returns true because this is a Flag
  public boolean isFlag() {
    return true;
  }

  // doesn't do anything because it doesn't have a list of neighbors
  // nor a number of Mines
  public void countNeighbor() {
    return;
  }

  // draws this Flag onto the given background
  public WorldImage drawAt(int row, int col, WorldImage background) {
    background = new OverlayOffsetImage(
        new EquilateralTriangleImage(10, OutlineMode.SOLID, Color.YELLOW), row, col,
        new OverlayImage(new RectangleImage(20, 20, OutlineMode.OUTLINE, Color.BLACK),
            new RectangleImage(20, 20, OutlineMode.SOLID, Color.CYAN)));
    return background;
  }
}

// runs the game
class RunMinesweeper {
  IGamePiece c1 = new Cell();
  IGamePiece m1 = new Mine(1, 1);
  IGamePiece m2 = new Mine(2, 2, true);
  IGamePiece f1 = new Flag(true);
  IGamePiece f2 = new Flag(false);
  IGamePiece c2 = new Cell(new ArrayList<IGamePiece>(Arrays.asList(c1, m1, m2, f1, f2)), 3, true);

  void initialTestCondition() {
    this.c1 = new Cell();
    this.m1 = new Mine(1, 1);
    this.m2 = new Mine(2, 2, true);
    this.f1 = new Flag(true);
    this.f2 = new Flag(false);
    this.c2 = new Cell(
        new ArrayList<IGamePiece>(Arrays.asList(this.c1, this.m1, this.m2, this.f1, this.f2)), 3,
        true);
  }

  // runs the Minesweeper game
  void testGame(Tester t) {
    GameWorld game = new GameWorld(15, 15, 2);
    game.bigBang(300, 300);
  }

  // tests the initCells method
  void testInitCells(Tester t) {
    GameWorld game = new GameWorld(5, 5, 5, 1);
    // Test 1: show that there's nothing in grid
    t.checkExpect(game.grid.size(), 0);
    // initializes the world with Cells
    game.initCells();
    // Test 2: show that the grid is now initialized with the proper amount of cells
    t.checkExpect(game.grid.size() * game.grid.get(0).size(), game.gridLength * game.gridWidth);
  }

  // tests the placeMines method
  void testPlaceMines(Tester t) {
    GameWorld game = new GameWorld(5, 5, 5, 1);
    // initializes the world with Cells
    game.initCells();
    // Test 1: check that there are no Mines in the grid
    int count = 0;
    for (int r = 0; r < game.grid.size(); r++) {
      for (int c = 0; c < game.grid.get(r).size(); c++) {
        if (game.grid.get(r).get(c).isMine()) {
          count++;
        }
      }
    }
    t.checkExpect(count, 0);
    // place the mines randomly into the grid
    game.placeMines();
    // Test 2: there should now be grid.numMines mines in the grid, regardless of
    // location
    for (int r = 0; r < game.grid.size(); r++) {
      for (int c = 0; c < game.grid.get(r).size(); c++) {
        if (game.grid.get(r).get(c).isMine()) {
          count++;
        }
      }
    }
    t.checkExpect(count, game.numMines);
  }

  // tests the updateNeighbors method
  void testUpdateNeighbors(Tester t) {
    GameWorld game = new GameWorld(2, 2, 0, 1);
    // initializes the world with Cells
    game.initCells();
    // Test 1: the list of neighbors for each cell now should be empty
    for (int r = 0; r < game.grid.size(); r++) {
      for (int c = 0; c < game.grid.get(r).size(); c++) {
        Cell cell = (Cell) game.grid.get(r).get(c);
        t.checkExpect(cell.neighbors.size(), 0);
      }
    }
    // update the neighbors
    game.updateNeighbors();
    // Test 2: because the grid is a 2x2 square with no Mines, each cell should have
    // 3 neighbors
    for (int r = 0; r < game.grid.size(); r++) {
      for (int c = 0; c < game.grid.get(r).size(); c++) {
        Cell cell = (Cell) game.grid.get(r).get(c);
        t.checkExpect(cell.neighbors.size(), 3);
      }
    }
  }

  // tests the CheckOutOfBounds method
  void testCheckOutOfBounds(Tester t) {
    GameWorld game = new GameWorld(5, 5, 5, 1);
    // initialize the world with Cells
    game.initCells();
    t.checkExpect(game.checkOutOfBounds(3, game.grid.size()), 3);
    t.checkExpect(game.checkOutOfBounds(0, game.grid.size()), 0);
    t.checkExpect(game.checkOutOfBounds(-1, game.grid.size()), 0);
    t.checkExpect(game.checkOutOfBounds(5, game.grid.size()), 4);
    t.checkExpect(game.checkOutOfBounds(4, game.grid.size()), 4);
  }

  
  //test method lastScene
  void testLastScene(Tester t) {
    GameWorld game = new GameWorld(2, 2, 1, 1);
    WorldScene background1 = game.getEmptyScene();
    background1.placeImageXY(
        new TextImage("You win", Color.BLACK), 150, 150);
    t.checkExpect(game.lastScene("You win"), background1);
    WorldScene background2 = game.getEmptyScene();
    background2.placeImageXY(
        new TextImage("You lost", Color.BLACK), 150, 150);
    t.checkExpect(game.lastScene("You lost"), background2);
  }

  // tests the addNeighbor and countNeighbor method
  void testAddAndCountNeighbor(Tester t) {
    Cell cell = new Cell();
    Mine mine = new Mine(5, 5);
    Flag flag = new Flag(false);
    // Test 1: check the list of neighbors for cell is empty
    t.checkExpect(cell.neighbors.size(), 0);
    // update the list of neighbors with new IGamePieces
    cell.addNeighbor(mine);
    cell.addNeighbor(new Mine(5, 3));
    cell.addNeighbor(new Cell());
    cell.addNeighbor(new Flag(false));
    // Test 2: check that the list of neighbors is now size 4
    t.checkExpect(cell.neighbors.size(), 4);
    // Test 3: check that the countNeighbor method returns 2 mines as expected
    cell.countNeighbor();
    t.checkExpect(cell.mineNum, 2);
  }

  // tests the isMine method
  void testIsMine(Tester t) {
    Cell cell = new Cell();
    Mine mine = new Mine(5, 5);
    Flag flag = new Flag(false);
    t.checkExpect(cell.isMine(), false);
    t.checkExpect(mine.isMine(), true);
    t.checkExpect(flag.isMine(), false);
  }

  // tests the drawAt method in each IGamePiece
  void testDrawAt(Tester t) {
    this.initialTestCondition();
    t.checkExpect(
        f1.drawAt(10, 10, new EquilateralTriangleImage(10, OutlineMode.SOLID, Color.YELLOW)),
        new OverlayOffsetImage(
            new EquilateralTriangleImage(10, OutlineMode.SOLID, Color.YELLOW), 10, 10,
            new OverlayImage(new RectangleImage(20, 20, OutlineMode.OUTLINE, Color.BLACK),
                new RectangleImage(20, 20, OutlineMode.SOLID, Color.CYAN))));
    t.checkExpect(m1.drawAt(10, 10, new RectangleImage(20, 20, OutlineMode.OUTLINE, Color.BLACK)),
        new OverlayOffsetImage(
            new RectangleImage(20, 20, OutlineMode.OUTLINE, Color.BLACK), 10, 10,
            new RectangleImage(20, 20, OutlineMode.SOLID, Color.CYAN)));
    t.checkExpect(m2.drawAt(10, 10, new RectangleImage(20, 20, OutlineMode.OUTLINE, Color.BLACK)),
        new OverlayOffsetImage(new CircleImage(5, OutlineMode.SOLID, Color.RED), 10, 10,
            new OverlayImage(new RectangleImage(20, 20, OutlineMode.OUTLINE, Color.BLACK),
                new RectangleImage(20, 20, OutlineMode.SOLID, Color.CYAN))));
    t.checkExpect(c1.drawAt(10, 10, new RectangleImage(20, 20, OutlineMode.OUTLINE, Color.BLACK)),
        new OverlayOffsetImage(new RectangleImage(20, 20, OutlineMode.OUTLINE, Color.BLACK), 10, 10,
            new RectangleImage(20, 20, OutlineMode.SOLID, Color.CYAN)));
    Integer n = 3;
    t.checkExpect(c2.drawAt(10, 10, new RectangleImage(20, 20, OutlineMode.OUTLINE, Color.BLACK)),
        new OverlayOffsetImage(new TextImage(n.toString(), Color.ORANGE), 10, 10,
            new OverlayImage(new RectangleImage(20, 20, OutlineMode.OUTLINE, Color.BLACK),
                new RectangleImage(20, 20, OutlineMode.SOLID, Color.lightGray))));
  }

  // tests the sameMine method
  void testSameMine(Tester t) {
    this.initialTestCondition();
    Mine mt1 = new Mine(1, 1);
    Mine mt2 = new Mine(2, 2, true);
    t.checkExpect(mt1.sameMine(new Mine(1, 1)), true);
    t.checkExpect(mt1.sameMine(mt2), false);
  }

  // tests the diffColorNums method
  void testDiffColorNums(Tester t) {
    this.initialTestCondition();
    Cell c = new Cell();
    Integer n1 = 1;
    Integer n2 = 2;
    Integer n3 = 3;
    Integer n4 = 4;
    t.checkExpect(c.diffColorNums(n1), new TextImage(n1.toString(), Color.BLUE));
    t.checkExpect(c.diffColorNums(n2), new TextImage(n2.toString(), Color.GREEN));
    t.checkExpect(c.diffColorNums(n3), new TextImage(n3.toString(), Color.ORANGE));
    t.checkExpect(c.diffColorNums(n4), new TextImage(n4.toString(), Color.RED));
  }
}
