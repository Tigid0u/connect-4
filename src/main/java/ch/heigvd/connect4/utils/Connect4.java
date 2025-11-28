package ch.heigvd.connect4.utils;

public class Connect4 {
  /**
   * Enum qui permet à la méthode play de renvoyer un état
   */
  public enum TurnResult {
    WIN,
    NOTHING,
    DRAW
  }

  private final int maxColumns;
  private final int maxRows;
  private final int[][] grid; // Colonne de gauche à droite, ligne de bas en haut

  /**
   * Constructeur de la classe Connect4 qui prend comme paramètre la taille de la
   * grille désirée
   * et instancie cette grille pour que le jeu puisse démarrer.
   * Les paramètres doivent être au minimum égal 4, car sinon impossible de jouer
   * 
   * @param maxColumns Le maximum des colonnes voulu
   * @param maxRows    Le maximum des lignes voulu
   */
  public Connect4(int maxColumns, int maxRows) {
    if (maxColumns < 4 || maxRows < 4) {
      throw new IllegalArgumentException("La taille de la grille doit être au minimum 4x4");
    }
    this.maxColumns = maxColumns;
    this.maxRows = maxRows;
    grid = new int[maxColumns][maxRows];
  }

  /**
   * Méthode qui permet de tester si le coup voulu est légal ou pas.
   * Permet de savoir s'il est en dehors de la grille, ou si la colonne voulue est
   * déjà pleine.
   * 
   * @param column Index de la colonne
   * @return retourne true si le coup est valide
   */
  public boolean checkInput(int column) {
    return column >= 0 && column < maxColumns && grid[column][maxRows - 1] == 0; // dernière condition, permet de savoir
                                                                                 // si la colonne est pleine
  }

  /**
   * Méthode qui permet de jouer un coup pour un joueur donné.
   * 
   * @param column La colonne à jouer
   * @param player Le joueur qui joue, soit 1, soit 2
   * @return WIN : Indique que le joueur qui vient de placer la pièce a gagné
   *         NOTHING : Indique qu'il n'y a rien à signaler, le jeu continue de
   *         manière normale
   *         DRAW : Indique que la grille est pleine et qu'il n'y a aucun
   *         vainqueur
   */
  public TurnResult play(int column, int player) {
    if (player != 1 && player != 2) {
      throw new IllegalArgumentException("Player must be 1 or 2");
    }
    if (!checkInput(column)) {
      throw new IllegalArgumentException("Column not valid or column is full");
    }
    int row = 0;
    for (int i = 0; i < maxRows; ++i) {
      if (grid[column][i] == 0) {
        grid[column][i] = player;
        row = i;
        break;
      }
    }
    if (checkWin(column, row, player)) {
      return TurnResult.WIN;
    }
    if (gridIsFull()) {
      return TurnResult.DRAW;
    }
    return TurnResult.NOTHING;

  }

  /**
   * Méthode qui permet de checker s'il y a une victoire à partir d'une position
   * donnée
   * dans le tableau ainsi que du joueur voulu.
   * 
   * @param column Index de la colonne ou la pièce a été joué
   * @param row    Index de la ligne ou la pièce a été joué
   * @param player Le joueur à qui appartient la pièce jouée
   * @return Retourne true si le joueur a gagné
   */
  private boolean checkWin(int column, int row, int player) {
    int[][] directions = {
        { 0, 1 }, // Haut, bas
        { 1, 0 }, // Gauche, droite
        { 1, 1 }, // Diagonale bas gauche vers haut droite
        { 1, -1 } // Diagonale haut gauche vers bas droit
    };

    for (int[] direction : directions) {
      int count = 1;
      // Pour la colonne et la ligne, on ajoute déjà une fois la direction, car la
      // case ou la pièce a été posé, est déjà compté
      count += countDirection(column + direction[0], row + direction[1], direction[0], direction[1], player);
      // Il faut aller dans les 2 sens pour couvrir toutes les directions.
      count += countDirection(column - direction[0], row - direction[1], -direction[0], -direction[1], player);

      if (count >= 4) {
        return true;
      }
    }
    return false;
  }

  /**
   * Méthode qui compte le nombre de pièces d'un joueur donné dans une direction
   * donné
   * jusqu'à rencontrer une pièce d'un joueur adverse ou jusqu'à arriver à la
   * limite de la grille
   * 
   * @param column Index de la colonne de départ
   * @param row    Index de la ligne de départ
   * @param d1     Direction pour la colonne
   * @param d2     Direction pour la ligne
   * @param player Joueur à qui doit appartenir les pièces
   * @return Retourne le nombre de pièces
   */
  private int countDirection(int column, int row, int d1, int d2, int player) {
    int count = 0;
    while (column >= 0 && column < maxColumns && row >= 0 && row < maxRows) {
      if (grid[column][row] == player) {
        ++count;
        column += d1;
        row += d2;
      } else {
        break;
      }
    }
    return count;
  }

  /**
   * Méthode qui permet de tester si la grille est pleine
   * 
   * @return Retourne true si la grille est pleine
   */
  private boolean gridIsFull() {
    for (int i = 0; i < maxColumns; ++i) {
      if (grid[i][maxRows - 1] == 0) {
        return false;
      }
    }
    return true;
  }

  /**
   * Méthode qui permet d'avoir la grille du jeu, qui contient l'état du jeu en
   * cours
   * avec comme valeurs : 0 (case vide), 1 (joueur 1), 2 (joueur 2).
   * 
   * @return Retrourne une copie du tableau 2D grid
   */
  public int[][] getGrid() {
    int[][] copy = new int[maxColumns][maxRows];
    for (int i = 0; i < maxColumns; ++i) {
      System.arraycopy(grid[i], 0, copy[i], 0, maxRows); // Ligne proposée par intelliJ à la place d'un autre for
    }
    return copy;
  }
}
