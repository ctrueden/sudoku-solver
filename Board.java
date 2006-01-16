// Board.java

package com.restlesswarrior.sudoku;

import java.io.*;
import java.util.*;

/** Represents a Sudoku board state. */
public class Board {

  // -- Constants --

  private static final int[] MASKS = {
    0x01ff, 0x0001, 0x0002, 0x0004, 0x0008,
    0x0010, 0x0020, 0x0040, 0x0080, 0x0100
  };


  // -- Fields --

  public int[][] possible;
  public int[][] values;
  public int lastR = -1, lastC = -1;


  // -- Constructors --

  /** Clones a board. */
  public Board(Board board) { this(copy(board.values), copy(board.possible)); }

  /** Constructs a board with the given values. */
  public Board(int[][] values) { this(values, null); }

  /** Constructs a board with the given values and possible values. */
  public Board(int[][] values, int[][] poss) {
    super();
    this.values = values;
    if (possible == null) {
      possible = new int[9][9];
      for (int r=0; r<9; r++) {
        for (int c=0; c<9; c++) {
          possible[r][c] = MASKS[values[r][c]];
        }
      }
    }
    else possible = poss;
  }


  // -- Board methods --

  /**
   * Deduces up to num values based on known values. No recursion.
   * @return false if the board position is untenable
   */
  public boolean deduce(int num) {
    int count = 0;
    boolean progress = true;
    while (progress && count < num) {
      progress = false;
      for (int r=0; r<9; r++) {
        for (int c=0; c<9; c++) {
          if (values[r][c] == 0) {
            // eliminate stuff in the same column
            for (int rr=0; rr<9; rr++) {
              int v = values[rr][c];
              if (v != 0) {
                if ((possible[r][c] & MASKS[v]) != 0) {
                  possible[r][c] -= MASKS[v];
                  progress = true;
                }
              }
            }

            // eliminate stuff in the same row
            for (int cc=0; cc<9; cc++) {
              int v = values[r][cc];
              if (v != 0) {
                if ((possible[r][c] & MASKS[v]) != 0) {
                  possible[r][c] -= MASKS[v];
                  progress = true;
                }
              }
            }

            // eliminate stuff in the same box
            int p = r - r % 3;
            int q = c - c % 3;
            for (int j=0; j<3; j++) {
              int rr = p + j;
              for (int i=0; i<3; i++) {
                int cc = q + i;
                int v = values[rr][cc];
                if (v != 0) {
                  if ((possible[r][c] & MASKS[v]) != 0) {
                    possible[r][c] -= MASKS[v];
                    progress = true;
                  }
                }
              }
            }

            // mark off value if only one remains
            if (progress) {
              if (possible[r][c] == 0) {
                // all possibilities eliminated -- we are hosed
                return false;
              }
              for (int v=1; v<=9; v++) {
                if (possible[r][c] == MASKS[v]) {
                  values[r][c] = v;
                  lastR = r;
                  lastC = c;
                  count++;
                  break;
                }
              }
            }
          }
          if (progress) break;
        }
        if (progress) break;
      }
    }
    return true;
  }

  /** Solves the entire puzzle, using recursion if necessary. */
  public Vector solve(boolean all) {
    // deduce all possible to get a headstart
    boolean success = deduce(81);
    if (!success) return null;

    // find "easiest" value to brute force
    int bestR = -1, bestC = -1, numOff = 10;
    for (int r=0; r<9; r++) {
      for (int c=0; c<9; c++) {
        if (values[r][c] != 0) continue; // already got this one
        // count number off for this value
        int count = 0;
        for (int q=1; q<=9; q++) {
          if ((possible[r][c] & MASKS[q]) != 0) count++;
        }
        if (count < numOff) {
          bestR = r;
          bestC = c;
          numOff = count;
        }
      }
    }
    if (bestR < 0) {
      Vector solutions = new Vector();
      solutions.add(this);
      return solutions;
    }

    // try all possible values
    Vector solutions = new Vector();
    for (int q=1; q<=9; q++) {
      if ((possible[bestR][bestC] & MASKS[q]) == 0) {
        // skip impossible value
        continue;
      }
      Board clone = new Board(this);
      clone.values[bestR][bestC] = q;
      Vector v = clone.solve(all);
      if (v != null) {
        if (!all) return v;
        for (int i=0; i<v.size(); i++) solutions.add(v.elementAt(i));
      }
    }
    return solutions.size() == 0 ? null : solutions;
  }


  // -- Utility methods --

  /** Parses board data from the given file. */
  public static Board makeBoard(String filename) throws IOException {
    int[][] values = new int[9][9];
    BufferedReader in = new BufferedReader(new FileReader(filename));
    int r = 0, c = 0;
    while (true) {
      String line = in.readLine();
      if (line == null) break;
      StringTokenizer st = new StringTokenizer(line);
      while (st.hasMoreTokens()) {
        String token = st.nextToken();
        values[r][c++] = token.equalsIgnoreCase("x") ?
          0 : Integer.parseInt(token);
        if (c == 9) { c = 0; r++; }
        if (r == 9) break;
      }
    }
    in.close();
    return new Board(values);
  }

  /** Performs a deep copy of the given array. */
  public static int[][] copy(int[][] a) {
    int[][] b = new int[a.length][];
    for (int i=0; i<a.length; i++) {
      b[i] = new int[a[i].length];
      System.arraycopy(a[i], 0, b[i], 0, a[i].length);
    }
    return b;
  }

}
