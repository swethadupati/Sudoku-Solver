import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Advanced Sudoku Solver
 * Features:
 * - Random puzzle generation
 * - Unique-solution verification
 * - MRV heuristic
 * - Constraint propagation through candidate calculation
 * - Animated backtracking
 * - Hint system
 * - Timer, mistakes and score
 * - Save / Load
 * - Difficulty levels
 *
 * Designed for Java 21+ (also suitable for newer JDKs such as Java 26).
 */
public class SudokuSolver extends JFrame {

    private static final int SIZE = 9;
    private static final int BOX = 3;
    private static final int EMPTY = 0;

    private final JTextField[][] cells = new JTextField[SIZE][SIZE];
    private final int[][] solution = new int[SIZE][SIZE];
    private final int[][] originalPuzzle = new int[SIZE][SIZE];

    private final Random random = new Random();

    private JComboBox<String> difficultyBox;
    private JButton newPuzzleButton, solveButton, validateButton;
    private JButton clearButton, resetButton, hintButton;
    private JButton animateButton, saveButton, loadButton;
    private JLabel timerLabel, mistakesLabel, scoreLabel, statusLabel;

    private Timer gameTimer;
    private long startTime;
    private int mistakes;
    private int hintsUsed;
    private boolean gameRunning;
    private boolean busy;

    public SudokuSolver() {
        setTitle("Advanced Sudoku Solver - MRV + Backtracking");
        setSize(820, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        buildUI();
        attachEvents();
        generateNewPuzzle();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel title = new JLabel("ADVANCED SUDOKU SOLVER", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 28));

        JLabel subtitle = new JLabel(
                "MRV + Constraint Propagation + Backtracking",
                SwingConstants.CENTER);
        subtitle.setFont(new Font("Arial", Font.PLAIN, 14));

        JPanel heading = new JPanel(new GridLayout(2, 1));
        heading.add(title);
        heading.add(subtitle);
        root.add(heading, BorderLayout.NORTH);

        JPanel gridPanel = new JPanel(new GridLayout(SIZE, SIZE));
        createGrid(gridPanel);
        root.add(gridPanel, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(5, 5));

        JPanel topControls = new JPanel(new FlowLayout());
        topControls.add(new JLabel("Difficulty:"));

        difficultyBox = new JComboBox<>(new String[]{"Easy", "Medium", "Hard"});
        topControls.add(difficultyBox);

        newPuzzleButton = new JButton("New Puzzle");
        topControls.add(newPuzzleButton);

        bottom.add(topControls, BorderLayout.NORTH);

        JPanel buttons1 = new JPanel(new FlowLayout());
        solveButton = new JButton("Solve");
        animateButton = new JButton("Animate Solve");
        validateButton = new JButton("Validate");
        hintButton = new JButton("Hint");

        buttons1.add(solveButton);
        buttons1.add(animateButton);
        buttons1.add(validateButton);
        buttons1.add(hintButton);

        JPanel buttons2 = new JPanel(new FlowLayout());
        clearButton = new JButton("Clear");
        resetButton = new JButton("Reset");
        saveButton = new JButton("Save");
        loadButton = new JButton("Load");

        buttons2.add(clearButton);
        buttons2.add(resetButton);
        buttons2.add(saveButton);
        buttons2.add(loadButton);

        JPanel allButtons = new JPanel(new GridLayout(2, 1));
        allButtons.add(buttons1);
        allButtons.add(buttons2);
        bottom.add(allButtons, BorderLayout.CENTER);

        JPanel info = new JPanel(new FlowLayout());
        timerLabel = new JLabel("Time: 00:00");
        mistakesLabel = new JLabel("Mistakes: 0");
        scoreLabel = new JLabel("Score: 0");
        statusLabel = new JLabel("Ready");

        info.add(timerLabel);
        info.add(Box.createHorizontalStrut(20));
        info.add(mistakesLabel);
        info.add(Box.createHorizontalStrut(20));
        info.add(scoreLabel);
        info.add(Box.createHorizontalStrut(20));
        info.add(statusLabel);

        bottom.add(info, BorderLayout.SOUTH);
        root.add(bottom, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private void createGrid(JPanel panel) {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                JTextField cell = new JTextField();
                cell.setHorizontalAlignment(JTextField.CENTER);
                cell.setFont(new Font("Arial", Font.BOLD, 21));

                int top = (r % 3 == 0) ? 3 : 1;
                int left = (c % 3 == 0) ? 3 : 1;
                int bottom = (r == SIZE - 1) ? 3 : 1;
                int right = (c == SIZE - 1) ? 3 : 1;

                cell.setBorder(BorderFactory.createMatteBorder(
                        top, left, bottom, right, Color.BLACK));

                final int row = r;
                final int col = c;

                cell.addKeyListener(new KeyAdapter() {
                    @Override
                    public void keyTyped(KeyEvent e) {
                        if (busy || !cell.isEditable()) {
                            e.consume();
                            return;
                        }

                        char ch = e.getKeyChar();
                        if (!Character.isDigit(ch) || ch == '0' ||
                                cell.getText().length() >= 1) {
                            e.consume();
                        }
                    }

                    @Override
                    public void keyReleased(KeyEvent e) {
                        if (busy) return;
                        if (cell.getText().isEmpty()) return;

                        int value = Character.digit(cell.getText().charAt(0), 10);
                        if (value >= 1 && value <= 9) {
                            checkUserMove(row, col, value);
                        }
                    }
                });

                cells[r][c] = cell;
                panel.add(cell);
            }
        }
    }

    private void attachEvents() {
        newPuzzleButton.addActionListener(e -> generateNewPuzzle());
        solveButton.addActionListener(e -> solveInstant());
        animateButton.addActionListener(e -> animateSolve());
        validateButton.addActionListener(e -> validateSudoku());
        hintButton.addActionListener(e -> giveHint());
        clearButton.addActionListener(e -> clearUserCells());
        resetButton.addActionListener(e -> resetPuzzle());
        saveButton.addActionListener(e -> savePuzzle());
        loadButton.addActionListener(e -> loadPuzzle());

        gameTimer = new Timer(1000, e -> updateTimer());
    }

    // -------------------- PUZZLE GENERATION --------------------

    private void generateNewPuzzle() {
        if (busy) return;

        busy = true;
        statusLabel.setText("Generating unique puzzle...");

        SwingWorker<int[][], Void> worker = new SwingWorker<>() {
            @Override
            protected int[][] doInBackground() {
                int[][] generated = new int[SIZE][SIZE];
                generateFullSolution(generated);

                int[][] puzzle = copyBoard(generated);
                int targetRemoved = getRemovalCount();
                createUniquePuzzle(puzzle, targetRemoved);

                return puzzle;
            }

            @Override
            protected void done() {
                try {
                    int[][] puzzle = get();

                    int[][] solved = copyBoard(puzzle);
                    if (!solveWithMRV(solved)) {
                        throw new IllegalStateException("Generated puzzle could not be solved.");
                    }
                    copyInto(solution, solved);

                    copyInto(originalPuzzle, puzzle);
                    displayPuzzle(puzzle);

                    mistakes = 0;
                    hintsUsed = 0;
                    updateStats();
                    startGameTimer();

                    statusLabel.setText("New puzzle ready");
                } catch (Exception ex) {
                    statusLabel.setText("Generation failed");
                    JOptionPane.showMessageDialog(
                    		SudokuSolver.this,
                            "Could not generate the puzzle.\n" + ex.getMessage(),
                            "Generation Error",
                            JOptionPane.ERROR_MESSAGE);
                } finally {
                    busy = false;
                }
            }
        };

        worker.execute();
    }

    /*
     * Creates a puzzle while preserving a single solution.
     * The method tries random cell removals and accepts a removal only
     * when countSolutions() reports exactly one solution.
     */
    private void createUniquePuzzle(int[][] puzzle, int targetRemoved) {
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < SIZE * SIZE; i++) positions.add(i);
        Collections.shuffle(positions, random);

        int removed = 0;

        for (int position : positions) {
            if (removed >= targetRemoved) break;

            int r = position / SIZE;
            int c = position % SIZE;

            int backup = puzzle[r][c];
            puzzle[r][c] = EMPTY;

            int[][] test = copyBoard(puzzle);
            if (countSolutions(test, 2) == 1) {
                removed++;
            } else {
                puzzle[r][c] = backup;
            }
        }
    }

    private int getRemovalCount() {
        String difficulty = (String) difficultyBox.getSelectedItem();
        if ("Easy".equals(difficulty)) return 35;
        if ("Medium".equals(difficulty)) return 45;
        return 52;
    }

    private boolean generateFullSolution(int[][] board) {

        int[] empty = findEmptyCellFirst(board);

        // No empty cells means the board is completely solved
        if (empty == null) {
            return true;
        }

        int row = empty[0];
        int col = empty[1];

        int[] numbers = shuffledNumbers();

        for (int num : numbers) {

            if (isSafe(board, row, col, num)) {

                board[row][col] = num;

                // Recursively try to solve the remaining cells
                if (generateFullSolution(board)) {
                    return true;
                }

                // Backtrack
                board[row][col] = 0;
            }
        }

        // No number worked for this cell
        return false;
    }

    private int[] shuffledNumbers() {
        int[] numbers = {1,2,3,4,5,6,7,8,9};
        for (int i = numbers.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int t = numbers[i];
            numbers[i] = numbers[j];
            numbers[j] = t;
        }
        return numbers;
    }

    /*
     * Counts solutions, stopping at 'limit'.
     * This is used to verify uniqueness without exploring the entire
     * solution space after two solutions have been found.
     */
    private int countSolutions(int[][] board, int limit) {
        if (limit <= 0) return 0;

        int[] cell = findBestCell(board);
        if (cell == null) return 1;

        int row = cell[0];
        int col = cell[1];
        int count = 0;

        for (int num : candidates(board, row, col)) {
            board[row][col] = num;
            count += countSolutions(board, limit - count);
            board[row][col] = EMPTY;

            if (count >= limit) return count;
        }

        return count;
    }

    // -------------------- MRV + CONSTRAINT PROPAGATION --------------------

    private boolean solveWithMRV(int[][] board) {
        int[] cell = findBestCell(board);

        if (cell == null) return true;

        int row = cell[0];
        int col = cell[1];

        List<Integer> possible = candidates(board, row, col);

        for (int num : possible) {
            board[row][col] = num;

            if (solveWithMRV(board)) return true;

            board[row][col] = EMPTY;
        }

        return false;
    }

    /*
     * MRV: select the empty cell having the fewest legal candidates.
     * This is a heuristic that reduces branching compared with selecting
     * the first empty cell.
     */
    private int[] findBestCell(int[][] board) {
        int bestRow = -1;
        int bestCol = -1;
        int bestCount = Integer.MAX_VALUE;

        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (board[r][c] == EMPTY) {
                    int count = candidates(board, r, c).size();

                    if (count < bestCount) {
                        bestCount = count;
                        bestRow = r;
                        bestCol = c;

                        if (bestCount == 1) {
                            return new int[]{bestRow, bestCol};
                        }
                    }
                }
            }
        }

        return bestRow == -1 ? null : new int[]{bestRow, bestCol};
    }

    /*
     * Constraint propagation is implemented through candidate-set
     * calculation: row, column and box values are excluded before a
     * candidate is considered by the recursive search.
     */
    private List<Integer> candidates(int[][] board, int row, int col) {
        boolean[] used = new boolean[SIZE + 1];

        for (int c = 0; c < SIZE; c++) {
            int v = board[row][c];
            if (v != EMPTY) used[v] = true;
        }

        for (int r = 0; r < SIZE; r++) {
            int v = board[r][col];
            if (v != EMPTY) used[v] = true;
        }

        int startRow = row - row % BOX;
        int startCol = col - col % BOX;

        for (int r = startRow; r < startRow + BOX; r++) {
            for (int c = startCol; c < startCol + BOX; c++) {
                int v = board[r][c];
                if (v != EMPTY) used[v] = true;
            }
        }

        List<Integer> result = new ArrayList<>();
        for (int n = 1; n <= 9; n++) {
            if (!used[n]) result.add(n);
        }

        Collections.shuffle(result, random);
        return result;
    }

    private boolean isSafe(int[][] board, int row, int col, int num) {
        for (int c = 0; c < SIZE; c++) {
            if (board[row][c] == num) return false;
        }

        for (int r = 0; r < SIZE; r++) {
            if (board[r][col] == num) return false;
        }

        int startRow = row - row % BOX;
        int startCol = col - col % BOX;

        for (int r = startRow; r < startRow + BOX; r++) {
            for (int c = startCol; c < startCol + BOX; c++) {
                if (board[r][c] == num) return false;
            }
        }

        return true;
    }

    // -------------------- SOLVE / ANIMATION --------------------

    private void solveInstant() {
        if (busy) return;

        int[][] grid = getGridFromUI();
        if (grid == null) return;

        if (!isInitialBoardValid(grid)) {
            showError("Invalid Sudoku. Duplicate numbers were found.");
            return;
        }

        busy = true;
        statusLabel.setText("Solving with MRV + backtracking...");

        SwingWorker<int[][], Void> worker = new SwingWorker<>() {
            @Override
            protected int[][] doInBackground() {
                int[][] work = copyBoard(grid);
                return solveWithMRV(work) ? work : null;
            }

            @Override
            protected void done() {
                try {
                    int[][] solved = get();

                    if (solved == null) {
                        showError("No solution exists for the current board.");
                        return;
                    }

                    displaySolvedGrid(solved);
                    copyInto(solution, solved);
                    statusLabel.setText("Solved successfully");
                    gameRunning = false;
                    gameTimer.stop();
                    updateScore();

                    JOptionPane.showMessageDialog(
                    		SudokuSolver.this,
                            "Sudoku solved using MRV + backtracking!",
                            "Solved",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    showError("Solver error: " + ex.getMessage());
                } finally {
                    busy = false;
                }
            }
        };

        worker.execute();
    }

    private void animateSolve() {
        if (busy) return;

        int[][] grid = getGridFromUI();
        if (grid == null) return;

        if (!isInitialBoardValid(grid)) {
            showError("Invalid Sudoku. Fix duplicate values first.");
            return;
        }

        busy = true;
        statusLabel.setText("Animating backtracking...");

        List<SolveStep> steps = new ArrayList<>();
        int[][] work = copyBoard(grid);

        if (!collectSolveSteps(work, steps)) {
            busy = false;
            showError("No solution exists for the current board.");
            return;
        }

        animateSteps(steps, 0);
    }

    private boolean collectSolveSteps(int[][] board, List<SolveStep> steps) {
        int[] cell = findBestCell(board);
        if (cell == null) return true;

        int row = cell[0];
        int col = cell[1];

        for (int num : candidates(board, row, col)) {
            board[row][col] = num;
            steps.add(new SolveStep(row, col, num, false));

            if (collectSolveSteps(board, steps)) return true;

            board[row][col] = EMPTY;
            steps.add(new SolveStep(row, col, EMPTY, true));
        }

        return false;
    }

    private void animateSteps(List<SolveStep> steps, int index) {
        if (index >= steps.size()) {
            busy = false;
            statusLabel.setText("Animation complete");
            gameRunning = false;
            gameTimer.stop();
            return;
        }

        SolveStep step = steps.get(index);
        JTextField cell = cells[step.row][step.col];

        if (step.backtrack) {
            cell.setText("");
        } else {
            cell.setText(String.valueOf(step.value));
        }

        cell.setForeground(step.backtrack ? Color.RED : new Color(0, 100, 200));

        Timer delay = new Timer(25, e -> {
            ((Timer)e.getSource()).stop();
            animateSteps(steps, index + 1);
        });
        delay.setRepeats(false);
        delay.start();
    }

    private static class SolveStep {
        int row, col, value;
        boolean backtrack;

        SolveStep(int row, int col, int value, boolean backtrack) {
            this.row = row;
            this.col = col;
            this.value = value;
            this.backtrack = backtrack;
        }
    }

    // -------------------- HINT / VALIDATION --------------------

    private void giveHint() {
        if (busy) return;

        int[][] grid = getGridFromUI();
        if (grid == null) return;

        if (!isInitialBoardValid(grid)) {
            showError("Fix duplicate values before using a hint.");
            return;
        }

        int[][] solved = copyBoard(grid);
        if (!solveWithMRV(solved)) {
            showError("No solution exists for the current board.");
            return;
        }

        List<int[]> emptyCells = new ArrayList<>();

        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (grid[r][c] == EMPTY) {
                    emptyCells.add(new int[]{r, c});
                }
            }
        }

        if (emptyCells.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this, "There are no empty cells.", "Hint",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int[] chosen = emptyCells.get(random.nextInt(emptyCells.size()));
        int r = chosen[0];
        int c = chosen[1];

        cells[r][c].setText(String.valueOf(solved[r][c]));
        cells[r][c].setForeground(new Color(0, 130, 70));

        hintsUsed++;
        updateScore();
        statusLabel.setText("Hint used at row " + (r + 1) + ", column " + (c + 1));
    }

    private void validateSudoku() {
        if (busy) return;

        int[][] grid = getGridFromUI();
        if (grid == null) return;

        if (!isInitialBoardValid(grid)) {
            mistakes++;
            updateStats();
            showError("Invalid Sudoku. Duplicate values found.");
            return;
        }

        boolean complete = true;
        for (int[] row : grid) {
            for (int value : row) {
                if (value == EMPTY) {
                    complete = false;
                    break;
                }
            }
        }

        if (complete) {
            if (boardsEqual(grid, solution)) {
                gameRunning = false;
                gameTimer.stop();
                updateScore();
                JOptionPane.showMessageDialog(
                        this,
                        "Congratulations! Puzzle completed correctly.",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                statusLabel.setText("Completed!");
            } else {
                mistakes++;
                updateStats();
                showError("The board is full but the solution is incorrect.");
            }
        } else {
            JOptionPane.showMessageDialog(
                    this,
                    "The entered Sudoku is valid so far.",
                    "Valid",
                    JOptionPane.INFORMATION_MESSAGE);
            statusLabel.setText("No conflicts found");
        }
    }

    private boolean isInitialBoardValid(int[][] grid) {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                int num = grid[r][c];
                if (num == EMPTY) continue;

                grid[r][c] = EMPTY;
                boolean safe = isSafe(grid, r, c, num);
                grid[r][c] = num;

                if (!safe) return false;
            }
        }
        return true;
    }

    private void checkUserMove(int row, int col, int value) {
        if (originalPuzzle[row][col] != EMPTY) return;

        int[][] grid = getGridFromUI();
        if (grid == null) return;

        grid[row][col] = EMPTY;

        if (!isSafe(grid, row, col, value)) {
            mistakes++;
            cells[row][col].setBackground(new Color(255, 210, 210));
            updateStats();

            Timer t = new Timer(400, e -> {
                cells[row][col].setBackground(Color.WHITE);
                ((Timer)e.getSource()).stop();
            });
            t.setRepeats(false);
            t.start();
        } else {
            cells[row][col].setBackground(Color.WHITE);
        }

        updateScore();
    }

    // -------------------- BOARD / UI OPERATIONS --------------------

    private int[][] getGridFromUI() {
        int[][] grid = new int[SIZE][SIZE];

        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                String text = cells[r][c].getText().trim();

                if (text.isEmpty()) {
                    grid[r][c] = EMPTY;
                } else {
                    try {
                        int value = Integer.parseInt(text);
                        if (value < 1 || value > 9) {
                            showError("Only numbers 1–9 are allowed.");
                            return null;
                        }
                        grid[r][c] = value;
                    } catch (NumberFormatException ex) {
                        showError("Only numbers 1–9 are allowed.");
                        return null;
                    }
                }
            }
        }

        return grid;
    }

    private void displayPuzzle(int[][] puzzle) {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                int value = puzzle[r][c];

                if (value == EMPTY) {
                    cells[r][c].setText("");
                    cells[r][c].setEditable(true);
                    cells[r][c].setForeground(Color.BLUE);
                    cells[r][c].setBackground(Color.WHITE);
                } else {
                    cells[r][c].setText(String.valueOf(value));
                    cells[r][c].setEditable(false);
                    cells[r][c].setForeground(Color.BLACK);
                    cells[r][c].setBackground(new Color(225, 225, 225));
                }
            }
        }
    }

    private void displaySolvedGrid(int[][] board) {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                cells[r][c].setText(String.valueOf(board[r][c]));
                cells[r][c].setEditable(false);

                if (originalPuzzle[r][c] == EMPTY) {
                    cells[r][c].setForeground(new Color(0, 100, 200));
                } else {
                    cells[r][c].setForeground(Color.BLACK);
                }
            }
        }
    }

    private void clearUserCells() {
        if (busy) return;

        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (originalPuzzle[r][c] == EMPTY) {
                    cells[r][c].setText("");
                    cells[r][c].setEditable(true);
                    cells[r][c].setForeground(Color.BLUE);
                    cells[r][c].setBackground(Color.WHITE);
                }
            }
        }

        statusLabel.setText("User entries cleared");
    }

    private void resetPuzzle() {
        if (busy) return;

        displayPuzzle(originalPuzzle);
        mistakes = 0;
        hintsUsed = 0;
        updateStats();
        startGameTimer();
        statusLabel.setText("Puzzle reset");
    }

    // -------------------- SAVE / LOAD --------------------

    private void savePuzzle() {
        if (busy) return;

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Sudoku Puzzle");

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();

        try (PrintWriter out = new PrintWriter(new FileWriter(file))) {
            out.println("ADVANCED_SUDOKU_V2");
            out.println(difficultyBox.getSelectedItem());
            out.println(mistakes);
            out.println(hintsUsed);
            out.println(System.currentTimeMillis() - startTime);

            int[][] grid = getGridFromUI();
            if (grid == null) return;

            for (int r = 0; r < SIZE; r++) {
                for (int c = 0; c < SIZE; c++) {
                    out.print(grid[r][c]);
                    if (c < SIZE - 1) out.print(" ");
                }
                out.println();
            }

            out.println("ORIGINAL");
            for (int r = 0; r < SIZE; r++) {
                for (int c = 0; c < SIZE; c++) {
                    out.print(originalPuzzle[r][c]);
                    if (c < SIZE - 1) out.print(" ");
                }
                out.println();
            }

            out.println("SOLUTION");
            for (int r = 0; r < SIZE; r++) {
                for (int c = 0; c < SIZE; c++) {
                    out.print(solution[r][c]);
                    if (c < SIZE - 1) out.print(" ");
                }
                out.println();
            }

            statusLabel.setText("Puzzle saved");
        } catch (IOException ex) {
            showError("Could not save puzzle: " + ex.getMessage());
        }
    }

    private void loadPuzzle() {
        if (busy) return;

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Load Sudoku Puzzle");

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();

        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            if (!"ADVANCED_SUDOKU_V2".equals(in.readLine())) {
                throw new IOException("Unsupported puzzle file.");
            }

            String difficulty = in.readLine();
            mistakes = Integer.parseInt(in.readLine());
            hintsUsed = Integer.parseInt(in.readLine());
            long elapsed = Long.parseLong(in.readLine());

            int[][] grid = readBoard(in);
            String marker = in.readLine();
            if (!"ORIGINAL".equals(marker)) throw new IOException("Missing ORIGINAL section.");

            int[][] original = readBoard(in);
            marker = in.readLine();
            if (!"SOLUTION".equals(marker)) throw new IOException("Missing SOLUTION section.");

            int[][] loadedSolution = readBoard(in);

            difficultyBox.setSelectedItem(difficulty);
            copyInto(originalPuzzle, original);
            copyInto(solution, loadedSolution);
            displayPuzzle(grid);

            startTime = System.currentTimeMillis() - elapsed;
            gameRunning = true;
            gameTimer.start();
            updateStats();

            statusLabel.setText("Puzzle loaded");
        } catch (Exception ex) {
            showError("Could not load puzzle: " + ex.getMessage());
        }
    }

    private int[][] readBoard(BufferedReader in) throws IOException {
        int[][] board = new int[SIZE][SIZE];

        for (int r = 0; r < SIZE; r++) {
            String line = in.readLine();
            if (line == null) throw new IOException("Unexpected end of file.");

            String[] parts = line.trim().split("\\s+");
            if (parts.length != SIZE) throw new IOException("Invalid board row.");

            for (int c = 0; c < SIZE; c++) {
                board[r][c] = Integer.parseInt(parts[c]);
                if (board[r][c] < 0 || board[r][c] > 9) {
                    throw new IOException("Invalid cell value.");
                }
            }
        }

        return board;
    }

    // -------------------- TIMER / SCORE --------------------

    private void startGameTimer() {
        startTime = System.currentTimeMillis();
        gameRunning = true;
        gameTimer.start();
        updateTimer();
    }

    private void updateTimer() {
        if (!gameRunning) return;

        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        long minutes = elapsed / 60;
        long seconds = elapsed % 60;

        timerLabel.setText(String.format("Time: %02d:%02d", minutes, seconds));
        updateScore();
    }

    private void updateStats() {
        mistakesLabel.setText("Mistakes: " + mistakes);
        updateScore();
    }

    private void updateScore() {
        long elapsed = gameRunning
                ? (System.currentTimeMillis() - startTime) / 1000
                : 0;

        int difficultyBonus = switch ((String) difficultyBox.getSelectedItem()) {
            case "Easy" -> 100;
            case "Medium" -> 200;
            default -> 300;
        };

        int score = Math.max(
                0,
                difficultyBonus
                        - mistakes * 20
                        - hintsUsed * 15
                        - (int)(elapsed / 10)
        );

        scoreLabel.setText("Score: " + score);
    }

    // -------------------- UTILITY --------------------

    private int[] findEmptyCellFirst(int[][] board) {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (board[r][c] == EMPTY) return new int[]{r, c};
            }
        }
        return null;
    }

    private int[][] copyBoard(int[][] source) {
        int[][] copy = new int[SIZE][SIZE];
        for (int r = 0; r < SIZE; r++) {
            System.arraycopy(source[r], 0, copy[r], 0, SIZE);
        }
        return copy;
    }

    private void copyInto(int[][] target, int[][] source) {
        for (int r = 0; r < SIZE; r++) {
            System.arraycopy(source[r], 0, target[r], 0, SIZE);
        }
    }

    private boolean boardsEqual(int[][] a, int[][] b) {
        for (int r = 0; r < SIZE; r++) {
            if (!Arrays.equals(a[r], b[r])) return false;
        }
        return true;
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
                this, message, "Sudoku", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SudokuSolver app = new SudokuSolver();
            app.setVisible(true);
        });
    }
}
