// Sudoku.java

package com.restlesswarrior.sudoku;

import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import javax.swing.*;

/** A GUI for displaying progress solving a Sudoku board. */
public class Sudoku extends JPanel implements ActionListener, Runnable {

  // -- Constants --

  private static final int[] MASKS = {
    0x0000, 0x0001, 0x0002, 0x0004, 0x0008,
    0x0010, 0x0020, 0x0040, 0x0080, 0x0100
  };


  // -- Fields --

  private Board board;
  private JLabel[][] labels;
  private JButton deduce, deduceAll, solve;


  // -- Constructor --

  public Sudoku(Board board) {
    super();
    this.board = board;
    labels = new JLabel[9][9];
    GridLayout grid = new GridLayout(9, 9);
    setLayout(grid);
    Font font = new Font("serif", 0, 20);
    for (int r=0; r<9; r++) {
      for (int c=0; c<9; c++) {
        int v = board.values[r][c];
        labels[r][c] = new JLabel();
        labels[r][c].setFont(font);
        labels[r][c].setForeground(Color.blue);
        if (v != 0) labels[r][c].setText(" " + v);
        add(labels[r][c]);
      }
    }
    deduce = new JButton("Deduce one");
    deduce.setActionCommand("deduce");
    deduce.addActionListener(this);
    deduceAll = new JButton("Deduce all");
    deduceAll.setActionCommand("deduceAll");
    deduceAll.addActionListener(this);
    solve = new JButton("Solve");
    solve.setActionCommand("solve");
    solve.addActionListener(this);
  }


  // -- Sudoku methods --

  /** Updates labels to match deduced values. */
  public void resync() {
    for (int r=0; r<9; r++) {
      for (int c=0; c<9; c++) {
        int v = board.values[r][c];
        if (v == 0) continue;
        boolean last = r == board.lastR && c == board.lastC;
        Color color = last ? Color.red : Color.blue;
        labels[r][c].setForeground(color);
        labels[r][c].setText(" " + v);
      }
    }
  }


  // -- Component methods --

  /** Paints the Sudoku board with black dividing lines. */
  public void paint(Graphics g) {
    super.paint(g);
    Dimension size = getSize();
    int w = size.width, h = size.height;

    g.setColor(Color.black);
    for (int q=1; q<9; q++) {
      int x = w * q / 9 - 4;
      int y = h * q / 9 - 4;
      g.drawLine(x, 0, x, h);
      g.drawLine(0, y, w, y);
      if (q % 3 == 0) {
        g.drawLine(x - 1, 0, x - 1, h);
        g.drawLine(x + 1, 0, x + 1, h);
        g.drawLine(0, y - 1, w, y - 1);
        g.drawLine(0, y + 1, w, y + 1);
      }
    }
  }


  // -- ActionListener methods --

  /** Handles button presses. */
  public void actionPerformed(ActionEvent e) {
    String cmd = e.getActionCommand();
    final boolean doSolve = "solve".equals(cmd);
    final boolean doAll = "deduceAll".equals(cmd);
    final Sudoku sudoku = this;
    new Thread() {
      public void run() {
        solve.setEnabled(false);
        deduceAll.setEnabled(false);
        deduce.setEnabled(false);
        if (doSolve) {
          Vector solutions = board.solve(false);
          if (solutions != null) board = (Board) solutions.elementAt(0);
        }
        else board.deduce(doAll ? 81 : 1);
        SwingUtilities.invokeLater(sudoku); // redraw
        solve.setEnabled(true);
        deduceAll.setEnabled(true);
        deduce.setEnabled(true);
        if (doSolve) solve.requestFocus();
        else if (doAll) deduceAll.requestFocus();
        else deduce.requestFocus();
      }
    }.start();
  }


  // -- Runnable methods --

  /** Updates labels and repaints the board. */
  public void run() {
    resync();
    repaint();
  }



  // -- Main method --

  public static void main(String[] args) throws Exception {
    if (args.length < 1) {
      System.out.println("Usage: java Sudoku board.txt [board2.txt ...]");
      System.exit(1);
    }

    if (args.length < 2) {
      // create GUI
      Sudoku sudoku = new Sudoku(Board.makeBoard(args[0]));
      JFrame frame = new JFrame("Sudoku Solver");
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      JPanel pane = new JPanel();
      frame.setContentPane(sudoku);
      frame.setContentPane(pane);
      pane.setLayout(new BorderLayout());
      pane.add(sudoku, BorderLayout.CENTER);
      JPanel buttons = new JPanel();
      buttons.setLayout(new BorderLayout());
      buttons.add(sudoku.deduce, BorderLayout.WEST);
      buttons.add(sudoku.solve, BorderLayout.CENTER);
      buttons.add(sudoku.deduceAll, BorderLayout.EAST);
      pane.add(buttons, BorderLayout.SOUTH);
      frame.setBounds(200, 200, 300, 300);
      frame.show();
    }
    else {
      int num = 100;
      System.out.println("Using " + num + " trials per board.");
      long[] total = new long[args.length];
      for (int f=0; f<args.length; f++) {
        // average solution time over many trials
        total[f] = 0;
        Board board = Board.makeBoard(args[f]);
        System.out.print(args[f] + ": ");
        Vector solutions = null;
        int numSolutions = 0;
        for (int i=0; i<=num; i++) {
          long start = System.currentTimeMillis();
          solutions = board.solve(i == 0);
          long end = System.currentTimeMillis();
          if (solutions == null) {
            System.out.println("No solution");
            break;
          }
          if (i == 0) numSolutions = solutions.size();
          else total[f] += end - start;
          if (i % 10 == 1) System.out.print(".");
        }
        if (solutions == null) {
          total[f] = -1; // no solution
          continue;
        }
        double time = (double) total[f] / num;
        boolean easier = total[0] > total[f];
        long ratio;
        if (easier) {
          ratio = total[f] == 0 ? -1 :
            (total[0] + (total[f] / 2)) / total[f];
        }
        else {
          ratio = total[0] == 0 ? -2 :
            (total[f] + (total[0] / 2)) / total[0];
        }
        if (numSolutions > 1) {
          System.out.print(" [" + numSolutions + " solutions]");
        }
        System.out.print(" " + time + " ms");
        if (f > 0) {
          if (total[0] == -1) {
            // first board has no solutions
          }
          else if (ratio == 1 || (total[0] == 0 && total[f] == 0)) {
            System.out.println(" (about as hard as " + args[0] + ")");
          }
          else if (ratio == -1) { // total[f] == 0
            System.out.println(" (infinitely easier than " + args[0] + ")");
          }
          else if (ratio == -2) { // total[0] == 0
            System.out.println(" (infinitely harder than " + args[0] + ")");
          }
          else {
            System.out.println(" (" + ratio + "x " +
              (easier ? "easier" : "harder") + " than " + args[0] + ")");
          }
        }
        else System.out.println();
      }
    }
  }

}
