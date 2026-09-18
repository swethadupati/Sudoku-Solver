import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;


/**
 * Advanced Sudoku Solver
 *
 * Supported Sudoku sizes:
 * 6 x 6  -> 2 x 3 boxes
 * 9 x 9  -> 3 x 3 boxes
 * 16 x 16 -> 4 x 4 boxes
 *
 * Features:
 * - Random puzzle generation
 * - Multiple difficulty levels
 * - Unique-solution verification
 * - MRV based solving
 * - Candidate checking
 * - Backtracking
 * - Animated solving
 * - Hints
 * - Validation
 * - Timer
 * - Score
 * - Mistake counter
 * - Save / Load
 */
public class SudokuSolver extends JFrame {

    private static final long serialVersionUID = 1L;

    // ------------------------------------------------------------
    // Sudoku configuration
    // ------------------------------------------------------------

    private int SIZE = 9;
    private int BOX_ROWS = 3;
    private int BOX_COLS = 3;

    // ------------------------------------------------------------
    // GUI
    // ------------------------------------------------------------

    private JPanel boardPanel;

    private JTextField[][] cells;

    private JComboBox<String> sizeBox;
    private JComboBox<String> difficultyBox;

    private JButton newPuzzleButton;
    private JButton solveButton;
    private JButton animateButton;
    private JButton hintButton;
    private JButton validateButton;
    private JButton saveButton;
    private JButton loadButton;
    private JButton clearButton;

    private JLabel timerLabel;
    private JLabel mistakesLabel;
    private JLabel hintsLabel;
    private JLabel scoreLabel;
    private JLabel statusLabel;

    // ------------------------------------------------------------
    // Sudoku data
    // ------------------------------------------------------------

    private int[][] originalPuzzle;
    private int[][] solution;

    // ------------------------------------------------------------
    // Game information
    // ------------------------------------------------------------

    private int mistakes = 0;
    private int hints = 0;

    private boolean gameRunning = false;
    private boolean gameBusy = false;

    private long startTime = 0;
    private long finalElapsedTime = 0;

    private javax.swing.Timer gameTimer;

    private final Random random = new Random();

    // ------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------

    public SudokuSolver() {

        setTitle("Advanced Sudoku Solver");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setLayout(new BorderLayout(10, 10));

        createTopPanel();

        boardPanel = new JPanel();
        boardPanel.setBackground(Color.WHITE);

        add(boardPanel, BorderLayout.CENTER);

        createBottomPanel();

        updateBoardConfiguration();
        createGrid();

        setMinimumSize(new Dimension(700, 750));
        setSize(900, 850);
        setLocationRelativeTo(null);

        generateNewPuzzle();
    }

    // ============================================================
    // TOP PANEL
    // ============================================================

    private void createTopPanel() {

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));

        JLabel sizeLabel = new JLabel("Size:");

        sizeBox = new JComboBox<>(
                new String[] {
                        "6 × 6",
                        "9 × 9",
                        "16 × 16"
                }
        );

        sizeBox.setSelectedItem("9 × 9");

        JLabel difficultyLabel = new JLabel("Difficulty:");

        difficultyBox = new JComboBox<>(
                new String[] {
                        "Easy",
                        "Medium",
                        "Hard"
                }
        );

        newPuzzleButton = new JButton("New Puzzle");
        solveButton = new JButton("Solve");
        animateButton = new JButton("Animate Solve");
        hintButton = new JButton("Hint");
        validateButton = new JButton("Validate");
        clearButton = new JButton("Clear");
        saveButton = new JButton("Save");
        loadButton = new JButton("Load");

        // --------------------------------------------------------
        // IMPORTANT FIX #1
        // New Puzzle only creates a new puzzle.
        // --------------------------------------------------------

        newPuzzleButton.addActionListener(e -> generateNewPuzzle());

        // --------------------------------------------------------
        // IMPORTANT FIX #7
        // Changing size changes configuration and creates a puzzle.
        // --------------------------------------------------------

        sizeBox.addActionListener(e -> changeBoardSize());

        difficultyBox.addActionListener(e -> {

            if (!gameBusy) {
                generateNewPuzzle();
            }
        });

        solveButton.addActionListener(e -> solveInstant());

        animateButton.addActionListener(e -> animateSolve());

        hintButton.addActionListener(e -> giveHint());

        validateButton.addActionListener(e -> validateSudoku());

        clearButton.addActionListener(e -> clearUserEntries());

        saveButton.addActionListener(e -> savePuzzle());

        loadButton.addActionListener(e -> loadPuzzle());

        topPanel.add(sizeLabel);
        topPanel.add(sizeBox);

        topPanel.add(difficultyLabel);
        topPanel.add(difficultyBox);

        topPanel.add(newPuzzleButton);
        topPanel.add(solveButton);
        topPanel.add(animateButton);
        topPanel.add(hintButton);
        topPanel.add(validateButton);
        topPanel.add(clearButton);
        topPanel.add(saveButton);
        topPanel.add(loadButton);

        add(topPanel, BorderLayout.NORTH);
    }

    // ============================================================
    // BOTTOM PANEL
    // ============================================================

    private void createBottomPanel() {

        JPanel bottomPanel = new JPanel(new BorderLayout());

        JPanel informationPanel =
                new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));

        timerLabel = new JLabel("Time: 00:00");

        mistakesLabel = new JLabel("Mistakes: 0");

        hintsLabel = new JLabel("Hints: 0");

        scoreLabel = new JLabel("Score: 0");

        statusLabel = new JLabel("Ready");

        informationPanel.add(timerLabel);
        informationPanel.add(mistakesLabel);
        informationPanel.add(hintsLabel);
        informationPanel.add(scoreLabel);

        bottomPanel.add(informationPanel, BorderLayout.CENTER);

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        statusPanel.add(statusLabel);

        bottomPanel.add(statusPanel, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    // ============================================================
    // BOARD CONFIGURATION
    // ============================================================

    private void updateBoardConfiguration() {

        String selected = sizeBox.getSelectedItem().toString();

        if (selected.startsWith("6")) {

            SIZE = 6;
            BOX_ROWS = 2;
            BOX_COLS = 3;

        } else if (selected.startsWith("9")) {

            SIZE = 9;
            BOX_ROWS = 3;
            BOX_COLS = 3;

        } else if (selected.startsWith("16")) {

            SIZE = 16;
            BOX_ROWS = 4;
            BOX_COLS = 4;
        }
    }

    // ============================================================
    // CHANGE BOARD SIZE
    // ============================================================

    private void changeBoardSize() {

        if (gameBusy) {
            return;
        }

        updateBoardConfiguration();

        stopGameTimer();

        mistakes = 0;
        hints = 0;
        finalElapsedTime = 0;

        createGrid();

        generateNewPuzzle();
    }

    // ============================================================
    // CREATE GRID
    // ============================================================

    private void createGrid() {

        boardPanel.removeAll();

        boardPanel.setLayout(
                new GridLayout(SIZE, SIZE, 0, 0)
        );

        cells = new JTextField[SIZE][SIZE];

        int fontSize;

        if (SIZE == 6) {
            fontSize = 28;
        } else if (SIZE == 9) {
            fontSize = 24;
        } else {
            fontSize = 18;
        }

        Font cellFont =
                new Font("Arial", Font.BOLD, fontSize);

        for (int r = 0; r < SIZE; r++) {

            for (int c = 0; c < SIZE; c++) {

                JTextField cell = new JTextField();

                cell.setHorizontalAlignment(
                        SwingConstants.CENTER
                );

                cell.setFont(cellFont);

                cell.setBackground(Color.WHITE);

                cell.setBorder(
                        createCellBorder(r, c)
                );

                // ------------------------------------------------
                // Safer input handling
                // ------------------------------------------------

                ((javax.swing.text.AbstractDocument)
                        cell.getDocument())
                        .setDocumentFilter(new SudokuSymbolFilter());

                final int row = r;
                final int col = c;

                cell.addActionListener(
                        e -> processCell(row, col)
                );

                cell.addFocusListener(
                        new java.awt.event.FocusAdapter() {

                            @Override
                            public void focusLost(
                                    java.awt.event.FocusEvent e) {

                                processCell(row, col);
                            }
                        }
                );

                cell.addMouseListener(
                        new MouseAdapter() {

                            @Override
                            public void mouseClicked(
                                    MouseEvent e) {

                                highlightRelatedCells(row, col);
                            }
                        }
                );

                cells[r][c] = cell;

                boardPanel.add(cell);
            }
        }

        boardPanel.revalidate();
        boardPanel.repaint();
    }

    // ============================================================
    // DOCUMENT FILTER
    // ============================================================

    private class SudokuSymbolFilter extends DocumentFilter {

        @Override
        public void insertString(
                FilterBypass fb,
                int offset,
                String string,
                AttributeSet attr)
                throws BadLocationException {

            replace(fb, offset, 0, string, attr);
        }

        @Override
        public void replace(
                FilterBypass fb,
                int offset,
                int length,
                String text,
                AttributeSet attrs)
                throws BadLocationException {

            if (text == null) {
                return;
            }

            text = text.toUpperCase();

            if (text.isEmpty()) {
                fb.replace(offset, length, "", attrs);
                return;
            }

            // Only one symbol is allowed in a Sudoku cell.
            char ch = text.charAt(0);

            if (isValidSymbol(
                    String.valueOf(ch))) {

                fb.replace(
                        offset,
                        length,
                        String.valueOf(ch),
                        attrs
                );
            }
        }

        @Override
        public void remove(
                FilterBypass fb,
                int offset,
                int length)
                throws BadLocationException {

            fb.remove(offset, length);
        }
    }

    // ============================================================
    // CELL BORDER
    // ============================================================

    private Border createCellBorder(int row, int col) {

        int top =
                row % BOX_ROWS == 0 ? 3 : 1;

        int left =
                col % BOX_COLS == 0 ? 3 : 1;

        int bottom =
                row == SIZE - 1 ? 3 :
                (row + 1) % BOX_ROWS == 0 ? 3 : 1;

        int right =
                col == SIZE - 1 ? 3 :
                (col + 1) % BOX_COLS == 0 ? 3 : 1;

        return BorderFactory.createMatteBorder(
                top,
                left,
                bottom,
                right,
                Color.BLACK
        );
    }

    // ============================================================
    // PROCESS CELL
    // ============================================================

    private void processCell(int row, int col) {

        if (originalPuzzle == null ||
                cells == null) {
            return;
        }

        if (originalPuzzle[row][col] != 0) {
            return;
        }

        String text =
                cells[row][col].getText()
                        .trim()
                        .toUpperCase();

        if (text.isEmpty()) {
            return;
        }

        int value = symbolToValue(text);

        if (value < 1 || value > SIZE) {
            cells[row][col].setText("");

            mistakes++;

            updateGameInformation();

            return;
        }

        int[][] board = readBoard();

        board[row][col] = 0;

        if (!isSafe(
                board,
                row,
                col,
                value)) {

            cells[row][col].setText("");

            mistakes++;

            cells[row][col].setBackground(
                    new Color(255, 210, 210)
            );

            statusLabel.setText(
                    "Incorrect entry!"
            );

            updateGameInformation();

            return;
        }

        // --------------------------------------------------------
        // Check against official solution.
        // --------------------------------------------------------

        if (solution != null &&
                solution[row][col] != value) {

            cells[row][col].setText("");

            mistakes++;

            cells[row][col].setBackground(
                    new Color(255, 210, 210)
            );

            statusLabel.setText(
                    "Incorrect value!"
            );

            updateGameInformation();

            return;
        }

        cells[row][col].setBackground(
                Color.WHITE
        );

        updateGameInformation();

        if (isBoardComplete(board)) {
            finishGame();
        }
    }

    // ============================================================
    // HIGHLIGHT RELATED CELLS
    // ============================================================

    private void highlightRelatedCells(
            int selectedRow,
            int selectedCol) {

        if (cells == null) {
            return;
        }

        for (int r = 0; r < SIZE; r++) {

            for (int c = 0; c < SIZE; c++) {

                if (originalPuzzle != null &&
                        originalPuzzle[r][c] != 0) {

                    cells[r][c].setBackground(
                            new Color(235, 235, 235)
                    );

                } else {

                    cells[r][c].setBackground(
                            Color.WHITE
                    );
                }
            }
        }

        // Same row
        for (int c = 0; c < SIZE; c++) {
            cells[selectedRow][c].setBackground(
                    new Color(240, 240, 255)
            );
        }

        // Same column
        for (int r = 0; r < SIZE; r++) {
            cells[r][selectedCol].setBackground(
                    new Color(240, 240, 255)
            );
        }

        // Same box
        int startRow =
                selectedRow -
                selectedRow % BOX_ROWS;

        int startCol =
                selectedCol -
                selectedCol % BOX_COLS;

        for (int r = startRow;
             r < startRow + BOX_ROWS;
             r++) {

            for (int c = startCol;
                 c < startCol + BOX_COLS;
                 c++) {

                cells[r][c].setBackground(
                        new Color(235, 245, 255)
                );
            }
        }

        cells[selectedRow][selectedCol]
                .setBackground(
                        new Color(255, 245, 180)
                );
    }

    // ============================================================
    // GENERATE NEW PUZZLE
    // ============================================================

    private void generateNewPuzzle() {

        if (gameBusy) {
            return;
        }

        gameBusy = true;

        setControlsEnabled(false);

        stopGameTimer();

        mistakes = 0;
        hints = 0;
        finalElapsedTime = 0;

        updateGameInformation();

        statusLabel.setText(
                "Generating " + SIZE + "×" + SIZE + " puzzle..."
        );

        SwingWorker<int[][], Void> worker =
                new SwingWorker<int[][], Void>() {

                    private int[][] generatedSolution;
                    private int[][] generatedPuzzle;

                    @Override
                    protected int[][] doInBackground()
                            throws Exception {

                        generatedSolution =
                                generateCompleteSolution();

                        generatedPuzzle =
                                createUniquePuzzle(
                                        generatedSolution
                                );

                        return generatedPuzzle;
                    }

                    @Override
                    protected void done() {

                        try {

                            int[][] puzzle = get();

                            originalPuzzle =
                                    copyBoard(puzzle);

                            solution =
                                    copyBoard(
                                            generatedSolution
                                    );

                            displayPuzzle();

                            gameBusy = false;

                            setControlsEnabled(true);

                            statusLabel.setText(
                                    "New puzzle generated."
                            );

                            startGameTimer();

                            updateGameInformation();

                        } catch (Exception ex) {

                            gameBusy = false;

                            setControlsEnabled(true);

                            statusLabel.setText(
                                    "Puzzle generation failed."
                            );

                            JOptionPane.showMessageDialog(
                                    SudokuSolver.this,
                                    "Unable to generate puzzle:\n"
                                            + ex.getMessage(),
                                    "Generation Error",
                                    JOptionPane.ERROR_MESSAGE
                            );
                        }
                    }
                };

        worker.execute();
    }

    // ============================================================
    // FAST COMPLETE SOLUTION GENERATOR
    // ============================================================

    /**
     * Generates a valid completed Sudoku using a mathematical
     * pattern instead of random backtracking.
     *
     * This is much faster for 16 x 16 Sudoku.
     */
    private int[][] generateCompleteSolution() {

        int[][] board =
                new int[SIZE][SIZE];

        List<Integer> numbers =
                new ArrayList<>();

        for (int i = 1; i <= SIZE; i++) {
            numbers.add(i);
        }

        Collections.shuffle(numbers, random);

        for (int r = 0; r < SIZE; r++) {

            for (int c = 0; c < SIZE; c++) {

                int index =
                        patternIndex(r, c);

                board[r][c] =
                        numbers.get(index);
            }
        }

        shuffleRows(board);
        shuffleColumns(board);

        return board;
    }

    // ============================================================
    // SUDOKU PATTERN
    // ============================================================

    private int patternIndex(int row, int col) {

        return (
                row * BOX_COLS
                        + row / BOX_ROWS
                        + col
        ) % SIZE;
    }

    // ============================================================
    // SHUFFLE ROWS
    // ============================================================

    private void shuffleRows(int[][] board) {

        // Shuffle rows inside each band.
        for (int band = 0;
             band < SIZE;
             band += BOX_ROWS) {

            List<Integer> rows =
                    new ArrayList<>();

            for (int i = 0; i < BOX_ROWS; i++) {
                rows.add(band + i);
            }

            Collections.shuffle(rows, random);

            int[][] temp =
                    copyBoard(board);

            for (int i = 0; i < BOX_ROWS; i++) {

                board[band + i] =
                        temp[rows.get(i)].clone();
            }
        }

        // Shuffle the bands.
        List<Integer> bands =
                new ArrayList<>();

        for (int i = 0;
             i < SIZE / BOX_ROWS;
             i++) {

            bands.add(i);
        }

        Collections.shuffle(bands, random);

        int[][] temp =
                copyBoard(board);

        for (int band = 0;
             band < bands.size();
             band++) {

            for (int r = 0;
                 r < BOX_ROWS;
                 r++) {

                board[
                        band * BOX_ROWS + r
                ] =
                        temp[
                                bands.get(band) * BOX_ROWS + r
                        ].clone();
            }
        }
    }

    // ============================================================
    // SHUFFLE COLUMNS
    // ============================================================

    private void shuffleColumns(int[][] board) {

        // Shuffle columns inside each stack.
        for (int stack = 0;
             stack < SIZE;
             stack += BOX_COLS) {

            List<Integer> columns =
                    new ArrayList<>();

            for (int i = 0; i < BOX_COLS; i++) {
                columns.add(stack + i);
            }

            Collections.shuffle(
                    columns,
                    random
            );

            for (int r = 0; r < SIZE; r++) {

                int[] old =
                        board[r].clone();

                for (int i = 0;
                     i < BOX_COLS;
                     i++) {

                    board[r][stack + i] =
                            old[columns.get(i)];
                }
            }
        }

        // Shuffle stacks.
        List<Integer> stacks =
                new ArrayList<>();

        for (int i = 0;
             i < SIZE / BOX_COLS;
             i++) {

            stacks.add(i);
        }

        Collections.shuffle(
                stacks,
                random
        );

        int[][] temp =
                copyBoard(board);

        for (int r = 0; r < SIZE; r++) {

            for (int stack = 0;
                 stack < stacks.size();
                 stack++) {

                for (int c = 0;
                     c < BOX_COLS;
                     c++) {

                    board[r][
                            stack * BOX_COLS + c
                    ] =
                            temp[r][
                                    stacks.get(stack)
                                            * BOX_COLS + c
                            ];
                }
            }
        }
    }

    // ============================================================
    // CREATE UNIQUE PUZZLE
    // ============================================================

    private int[][] createUniquePuzzle(
            int[][] completeSolution) {

        int[][] puzzle =
                copyBoard(completeSolution);

        List<Integer> positions =
                new ArrayList<>();

        int total =
                SIZE * SIZE;

        for (int i = 0; i < total; i++) {
            positions.add(i);
        }

        Collections.shuffle(
                positions,
                random
        );

        int targetRemovals =
                getRemovalCount();

        /*
         * 16 x 16 uniqueness checking is expensive.
         * The removal count is therefore kept within a safer
         * range while still producing a challenging puzzle.
         */
        int removed = 0;

        int maxAttempts =
                targetRemovals * 2;

        int attempts = 0;

        for (int position : positions) {

            if (removed >= targetRemovals) {
                break;
            }

            if (attempts >= maxAttempts) {
                break;
            }

            attempts++;

            int row =
                    position / SIZE;

            int col =
                    position % SIZE;

            int backup =
                    puzzle[row][col];

            puzzle[row][col] = 0;

            int solutions =
                    countSolutions(
                            puzzle,
                            2
                    );

            if (solutions == 1) {

                removed++;

            } else {

                puzzle[row][col] =
                        backup;
            }
        }

        return puzzle;
    }

    // ============================================================
    // REMOVAL COUNT
    // ============================================================

    private int getRemovalCount() {

        String difficulty =
                difficultyBox
                        .getSelectedItem()
                        .toString();

        int removals;

        if (SIZE == 6) {

            if (difficulty.equals("Easy")) {
                removals = 12;
            } else if (difficulty.equals("Medium")) {
                removals = 18;
            } else {
                removals = 22;
            }

        } else if (SIZE == 9) {

            if (difficulty.equals("Easy")) {
                removals = 35;
            } else if (difficulty.equals("Medium")) {
                removals = 45;
            } else {
                removals = 52;
            }

        } else {

            /*
             * Safer values for 16 x 16.
             * Very aggressive removal makes uniqueness testing
             * extremely expensive.
             */
            if (difficulty.equals("Easy")) {
                removals = 90;
            } else if (difficulty.equals("Medium")) {
                removals = 120;
            } else {
                removals = 150;
            }
        }

        // Always leave at least SIZE clues.
        removals =
                Math.min(
                        removals,
                        SIZE * SIZE - SIZE
                );

        return removals;
    }

    // ============================================================
    // COUNT SOLUTIONS
    // ============================================================

    private int countSolutions(
            int[][] board,
            int limit) {

        int[] result = {0};

        countSolutionsRecursive(
                board,
                result,
                limit
        );

        return result[0];
    }

    private void countSolutionsRecursive(
            int[][] board,
            int[] result,
            int limit) {

        if (result[0] >= limit) {
            return;
        }

        int[] cell =
                findBestEmptyCell(board);

        if (cell == null) {

            result[0]++;
            return;
        }

        int row = cell[0];
        int col = cell[1];

        List<Integer> candidates =
                getCandidates(
                        board,
                        row,
                        col
                );

        for (int value : candidates) {

            board[row][col] =
                    value;

            countSolutionsRecursive(
                    board,
                    result,
                    limit
            );

            board[row][col] = 0;

            if (result[0] >= limit) {
                return;
            }
        }
    }

    // ============================================================
    // DISPLAY PUZZLE
    // ============================================================

    private void displayPuzzle() {

        if (cells == null) {
            return;
        }

        for (int r = 0; r < SIZE; r++) {

            for (int c = 0; c < SIZE; c++) {

                JTextField cell =
                        cells[r][c];

                int value =
                        originalPuzzle[r][c];

                if (value == 0) {

                    cell.setText("");

                    cell.setEditable(true);

                    cell.setBackground(
                            Color.WHITE
                    );

                } else {

                    cell.setText(
                            valueToSymbol(value)
                    );

                    cell.setEditable(false);

                    cell.setBackground(
                            new Color(235, 235, 235)
                    );
                }
            }
        }

        boardPanel.revalidate();
        boardPanel.repaint();
    }

    // ============================================================
    // READ BOARD
    // ============================================================

    private int[][] readBoard() {

        int[][] board =
                new int[SIZE][SIZE];

        if (cells == null) {
            return board;
        }

        for (int r = 0; r < SIZE; r++) {

            for (int c = 0; c < SIZE; c++) {

                String text =
                        cells[r][c]
                                .getText()
                                .trim()
                                .toUpperCase();

                if (!text.isEmpty()) {

                    int value =
                            symbolToValue(text);

                    if (value >= 1 &&
                            value <= SIZE) {

                        board[r][c] =
                                value;
                    }
                }
            }
        }

        return board;
    }

    // ============================================================
    // SYMBOL CONVERSION
    // ============================================================

    private String valueToSymbol(int value) {

        if (value >= 1 &&
                value <= 9) {

            return String.valueOf(value);
        }

        return String.valueOf(
                (char) ('A' + value - 10)
        );
    }

    private int symbolToValue(
            String symbol) {

        if (symbol == null ||
                symbol.isEmpty()) {

            return 0;
        }

        char ch =
                Character.toUpperCase(
                        symbol.charAt(0)
                );

        if (ch >= '1' &&
                ch <= '9') {

            return ch - '0';
        }

        if (ch >= 'A' &&
                ch <= 'G') {

            return ch - 'A' + 10;
        }

        return -1;
    }

    private boolean isValidSymbol(
            String symbol) {

        int value =
                symbolToValue(symbol);

        return value >= 1 &&
                value <= SIZE;
    }

    // ============================================================
    // IS SAFE
    // ============================================================

    private boolean isSafe(
            int[][] board,
            int row,
            int col,
            int value) {

        // Row
        for (int c = 0; c < SIZE; c++) {

            if (c != col &&
                    board[row][c] == value) {

                return false;
            }
        }

        // Column
        for (int r = 0; r < SIZE; r++) {

            if (r != row &&
                    board[r][col] == value) {

                return false;
            }
        }

        // Box
        int startRow =
                row -
                row % BOX_ROWS;

        int startCol =
                col -
                col % BOX_COLS;

        for (int r = startRow;
             r < startRow + BOX_ROWS;
             r++) {

            for (int c = startCol;
                 c < startCol + BOX_COLS;
                 c++) {

                if ((r != row ||
                        c != col) &&
                        board[r][c] == value) {

                    return false;
                }
            }
        }

        return true;
    }

    // ============================================================
    // CANDIDATES
    // ============================================================

    private List<Integer> getCandidates(
            int[][] board,
            int row,
            int col) {

        List<Integer> candidates =
                new ArrayList<>();

        for (int value = 1;
             value <= SIZE;
             value++) {

            if (isSafe(
                    board,
                    row,
                    col,
                    value)) {

                candidates.add(value);
            }
        }

        Collections.shuffle(
                candidates,
                random
        );

        return candidates;
    }

    // ============================================================
    // MRV
    // ============================================================

    private int[] findBestEmptyCell(
            int[][] board) {

        int bestRow = -1;
        int bestCol = -1;

        int bestCount =
                Integer.MAX_VALUE;

        for (int r = 0; r < SIZE; r++) {

            for (int c = 0; c < SIZE; c++) {

                if (board[r][c] == 0) {

                    int count =
                            getCandidates(
                                    board,
                                    r,
                                    c
                            ).size();

                    if (count < bestCount) {

                        bestCount = count;

                        bestRow = r;
                        bestCol = c;

                        if (count == 1) {

                            return new int[] {
                                    bestRow,
                                    bestCol
                            };
                        }
                    }
                }
            }
        }

        if (bestRow == -1) {
            return null;
        }

        return new int[] {
                bestRow,
                bestCol
        };
    }

    // ============================================================
    // MRV SOLVER
    // ============================================================

    private boolean solveWithMRV(
            int[][] board) {

        int[] cell =
                findBestEmptyCell(board);

        if (cell == null) {
            return true;
        }

        int row = cell[0];
        int col = cell[1];

        List<Integer> candidates =
                getCandidates(
                        board,
                        row,
                        col
                );

        for (int value : candidates) {

            board[row][col] =
                    value;

            if (solveWithMRV(board)) {
                return true;
            }

            board[row][col] = 0;
        }

        return false;
    }

    // ============================================================
    // SOLVE INSTANTLY
    // ============================================================

    private void solveInstant() {

        if (gameBusy) {
            return;
        }

        int[][] board =
                readBoard();

        if (!isValidBoard(board)) {

            JOptionPane.showMessageDialog(
                    this,
                    "The current Sudoku contains conflicts.",
                    "Cannot Solve",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        boolean solved =
                solveWithMRV(board);

        if (!solved) {

            JOptionPane.showMessageDialog(
                    this,
                    "No solution exists for the current entries.",
                    "No Solution",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        for (int r = 0; r < SIZE; r++) {

            for (int c = 0; c < SIZE; c++) {

                cells[r][c].setText(
                        valueToSymbol(
                                board[r][c]
                        )
                );

                cells[r][c].setBackground(
                        new Color(225, 245, 225)
                );
            }
        }

        statusLabel.setText(
                "Sudoku solved."
        );

        finishGame();
    }

    // ============================================================
    // ANIMATED SOLVE
    // ============================================================

    private void animateSolve() {

        if (gameBusy) {
            return;
        }

        int[][] board =
                readBoard();

        if (!isValidBoard(board)) {

            JOptionPane.showMessageDialog(
                    this,
                    "The current Sudoku contains conflicts.",
                    "Cannot Solve",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        gameBusy = true;

        setControlsEnabled(false);

        stopGameTimer();

        statusLabel.setText(
                "Preparing animation..."
        );

        SwingWorker<List<SolveStep>, Void> worker =
                new SwingWorker<List<SolveStep>, Void>() {

                    @Override
                    protected List<SolveStep> doInBackground() {

                        List<SolveStep> steps =
                                new ArrayList<>();

                        solveWithSteps(
                                board,
                                steps
                        );

                        return steps;
                    }

                    @Override
                    protected void done() {

                        try {

                            List<SolveStep> steps =
                                    get();

                            animateSteps(
                                    steps
                            );

                        } catch (Exception ex) {

                            gameBusy = false;

                            setControlsEnabled(true);

                            statusLabel.setText(
                                    "Animation failed."
                            );
                        }
                    }
                };

        worker.execute();
    }

    // ============================================================
    // SOLVE STEPS
    // ============================================================

    private boolean solveWithSteps(
            int[][] board,
            List<SolveStep> steps) {

        int[] cell =
                findBestEmptyCell(board);

        if (cell == null) {
            return true;
        }

        int row = cell[0];
        int col = cell[1];

        List<Integer> candidates =
                getCandidates(
                        board,
                        row,
                        col
                );

        for (int value : candidates) {

            board[row][col] =
                    value;

            steps.add(
                    new SolveStep(
                            row,
                            col,
                            value
                    )
            );

            if (solveWithSteps(
                    board,
                    steps)) {

                return true;
            }

            board[row][col] = 0;

            steps.add(
                    new SolveStep(
                            row,
                            col,
                            0
                    )
            );
        }

        return false;
    }

    // ============================================================
    // ANIMATE STEPS
    // ============================================================

    private void animateSteps(
            List<SolveStep> steps) {

        if (steps.isEmpty()) {

            gameBusy = false;

            setControlsEnabled(true);

            return;
        }

        final int[] index = {0};

        int delay;

        if (SIZE == 16) {
            delay = 5;
        } else if (SIZE == 9) {
            delay = 15;
        } else {
            delay = 30;
        }

        javax.swing.Timer animationTimer =
                new javax.swing.Timer(
                        delay,
                        null
                );

        animationTimer.addActionListener(
                e -> {

                    if (index[0] >= steps.size()) {

                        animationTimer.stop();

                        gameBusy = false;

                        setControlsEnabled(true);

                        statusLabel.setText(
                                "Animation completed."
                        );

                        finishGame();

                        return;
                    }

                    SolveStep step =
                            steps.get(index[0]++);

                    if (step.value == 0) {

                        cells[step.row][step.col]
                                .setText("");

                    } else {

                        cells[step.row][step.col]
                                .setText(
                                        valueToSymbol(
                                                step.value
                                        )
                                );
                    }

                    cells[step.row][step.col]
                            .setBackground(
                                    new Color(
                                            225,
                                            240,
                                            255
                                    )
                            );
                }
        );

        animationTimer.start();
    }

    // ============================================================
    // SOLVE STEP CLASS
    // ============================================================

    private static class SolveStep {

        int row;
        int col;
        int value;

        SolveStep(
                int row,
                int col,
                int value) {

            this.row = row;
            this.col = col;
            this.value = value;
        }
    }

    // ============================================================
    // HINT
    // ============================================================

    private void giveHint() {

        if (gameBusy) {
            return;
        }

        if (solution == null) {

            JOptionPane.showMessageDialog(
                    this,
                    "No solution is available.",
                    "Hint",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        int[][] board =
                readBoard();

        List<int[]> emptyCells =
                new ArrayList<>();

        for (int r = 0; r < SIZE; r++) {

            for (int c = 0; c < SIZE; c++) {

                if (originalPuzzle[r][c] == 0 &&
                        board[r][c] == 0) {

                    emptyCells.add(
                            new int[] {r, c}
                    );
                }
            }
        }

        if (emptyCells.isEmpty()) {

            JOptionPane.showMessageDialog(
                    this,
                    "There are no empty cells.",
                    "Hint",
                    JOptionPane.INFORMATION_MESSAGE
            );

            return;
        }

        int[] selected =
                emptyCells.get(
                        random.nextInt(
                                emptyCells.size()
                        )
                );

        int row = selected[0];
        int col = selected[1];

        cells[row][col].setText(
                valueToSymbol(
                        solution[row][col]
                )
        );

        cells[row][col].setBackground(
                new Color(
                        255,
                        250,
                        180
                )
        );

        hints++;

        statusLabel.setText(
                "Hint provided."
        );

        updateGameInformation();

        if (isBoardComplete(
                readBoard())) {

            finishGame();
        }
    }

    // ============================================================
    // VALIDATE SUDOKU
    // ============================================================

    private void validateSudoku() {

        if (gameBusy) {
            return;
        }

        int[][] board =
                readBoard();

        if (!isValidBoard(board)) {

            JOptionPane.showMessageDialog(
                    this,
                    "There are duplicate or invalid values.",
                    "Validation",
                    JOptionPane.WARNING_MESSAGE
            );

            statusLabel.setText(
                    "Validation failed."
            );

            return;
        }

        int emptyCells = 0;

        for (int r = 0; r < SIZE; r++) {

            for (int c = 0; c < SIZE; c++) {

                if (board[r][c] == 0) {
                    emptyCells++;
                }
            }
        }

        if (emptyCells > 0) {

            JOptionPane.showMessageDialog(
                    this,
                    "The current entries are valid.\n"
                            + "Empty cells remaining: "
                            + emptyCells,
                    "Validation",
                    JOptionPane.INFORMATION_MESSAGE
            );

            statusLabel.setText(
                    "Current entries are valid."
            );

            return;
        }

        if (solution != null &&
                boardsEqual(
                        board,
                        solution
                )) {

            JOptionPane.showMessageDialog(
                    this,
                    "Congratulations!\n"
                            + "The Sudoku is correct.",
                    "Validation",
                    JOptionPane.INFORMATION_MESSAGE
            );

            finishGame();

        } else {

            JOptionPane.showMessageDialog(
                    this,
                    "The Sudoku is complete but contains "
                            + "incorrect values.",
                    "Validation",
                    JOptionPane.WARNING_MESSAGE
            );
        }
    }

    // ============================================================
    // CLEAR USER ENTRIES
    // ============================================================

    private void clearUserEntries() {

        if (gameBusy ||
                originalPuzzle == null) {

            return;
        }

        for (int r = 0; r < SIZE; r++) {

            for (int c = 0; c < SIZE; c++) {

                if (originalPuzzle[r][c] == 0) {

                    cells[r][c].setText("");

                    cells[r][c].setBackground(
                            Color.WHITE
                    );
                }
            }
        }

        statusLabel.setText(
                "User entries cleared."
        );
    }

    // ============================================================
    // BOARD VALIDATION
    // ============================================================

    private boolean isValidBoard(
            int[][] board) {

        if (board == null ||
                board.length != SIZE) {

            return false;
        }

        for (int r = 0; r < SIZE; r++) {

            if (board[r].length != SIZE) {
                return false;
            }

            for (int c = 0; c < SIZE; c++) {

                int value =
                        board[r][c];

                if (value < 0 ||
                        value > SIZE) {

                    return false;
                }

                if (value != 0) {

                    board[r][c] = 0;

                    boolean safe =
                            isSafe(
                                    board,
                                    r,
                                    c,
                                    value
                            );

                    board[r][c] =
                            value;

                    if (!safe) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    // ============================================================
    // BOARD COMPLETE
    // ============================================================

    private boolean isBoardComplete(
            int[][] board) {

        for (int r = 0; r < SIZE; r++) {

            for (int c = 0; c < SIZE; c++) {

                if (board[r][c] == 0) {
                    return false;
                }
            }
        }

        return isValidBoard(board);
    }

    // ============================================================
    // FINISH GAME
    // ============================================================

    private void finishGame() {

        if (!gameRunning) {
            return;
        }

        // --------------------------------------------------------
        // IMPORTANT FIX #2
        // Store the final elapsed time BEFORE stopping the game.
        // --------------------------------------------------------

        finalElapsedTime =
                (System.currentTimeMillis()
                        - startTime) / 1000;

        gameRunning = false;

        stopGameTimer();

        updateTimer();

        updateScore();

        statusLabel.setText(
                "Game completed!"
        );

        for (int r = 0; r < SIZE; r++) {

            for (int c = 0; c < SIZE; c++) {

                cells[r][c].setBackground(
                        new Color(
                                225,
                                245,
                                225
                        )
                );
            }
        }
    }

    // ============================================================
    // TIMER
    // ============================================================

    private void startGameTimer() {

        startTime =
                System.currentTimeMillis();

        finalElapsedTime = 0;

        gameRunning = true;

        if (gameTimer != null) {
            gameTimer.stop();
        }

        // IMPORTANT FIX #4
        gameTimer =
                new javax.swing.Timer(
                        1000,
                        e -> updateTimer()
                );

        gameTimer.start();

        updateTimer();
    }

    private void stopGameTimer() {

        if (gameTimer != null) {

            gameTimer.stop();

            gameTimer = null;
        }
    }

    private long getElapsedSeconds() {

        if (gameRunning) {

            return (
                    System.currentTimeMillis()
                            - startTime
            ) / 1000;
        }

        return finalElapsedTime;
    }

    private void updateTimer() {

        long elapsed =
                getElapsedSeconds();

        long minutes =
                elapsed / 60;

        long seconds =
                elapsed % 60;

        timerLabel.setText(
                String.format(
                        "Time: %02d:%02d",
                        minutes,
                        seconds
                )
        );

        updateScore();
    }

    // ============================================================
    // SCORE
    // ============================================================

    private void updateScore() {

        String difficulty =
                difficultyBox
                        .getSelectedItem()
                        .toString();

        int difficultyBonus;

        if (difficulty.equals("Easy")) {

            difficultyBonus = 100;

        } else if (difficulty.equals("Medium")) {

            difficultyBonus = 200;

        } else {

            difficultyBonus = 300;
        }

        int sizeBonus;

        if (SIZE == 6) {

            sizeBonus = 0;

        } else if (SIZE == 9) {

            sizeBonus = 50;

        } else {

            sizeBonus = 150;
        }

        long elapsed =
                getElapsedSeconds();

        int score =
                difficultyBonus
                        + sizeBonus
                        - mistakes * 20
                        - hints * 15
                        - (int) (elapsed / 10);

        score =
                Math.max(
                        score,
                        0
                );

        scoreLabel.setText(
                "Score: " + score
        );
    }

    // ============================================================
    // GAME INFORMATION
    // ============================================================

    private void updateGameInformation() {

        mistakesLabel.setText(
                "Mistakes: " + mistakes
        );

        hintsLabel.setText(
                "Hints: " + hints
        );

        updateScore();
    }

    // ============================================================
    // ENABLE / DISABLE CONTROLS
    // ============================================================

    private void setControlsEnabled(
            boolean enabled) {

        sizeBox.setEnabled(enabled);

        difficultyBox.setEnabled(enabled);

        newPuzzleButton.setEnabled(enabled);

        solveButton.setEnabled(enabled);

        animateButton.setEnabled(enabled);

        hintButton.setEnabled(enabled);

        validateButton.setEnabled(enabled);

        clearButton.setEnabled(enabled);

        saveButton.setEnabled(enabled);

        loadButton.setEnabled(enabled);
    }

    // ============================================================
    // SAVE PUZZLE
    // ============================================================

    private void savePuzzle() {

        if (originalPuzzle == null ||
                solution == null) {

            JOptionPane.showMessageDialog(
                    this,
                    "There is no puzzle to save.",
                    "Save",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        JFileChooser chooser =
                new JFileChooser();

        chooser.setDialogTitle(
                "Save Sudoku Puzzle"
        );

        if (chooser.showSaveDialog(this)
                != JFileChooser.APPROVE_OPTION) {

            return;
        }

        File file =
                chooser.getSelectedFile();

        try (BufferedWriter writer =
                     new BufferedWriter(
                             new FileWriter(file)
                     )) {

            writer.write(
                    "ADVANCED_SUDOKU_MULTI_V3"
            );

            writer.newLine();

            writer.write(
                    "SIZE=" + SIZE
            );

            writer.newLine();

            writer.write(
                    "DIFFICULTY="
                            + difficultyBox
                            .getSelectedItem()
            );

            writer.newLine();

            writer.write(
                    "MISTAKES="
                            + mistakes
            );

            writer.newLine();

            writer.write(
                    "HINTS="
                            + hints
            );

            writer.newLine();

            writer.write(
                    "ELAPSED="
                            + getElapsedSeconds()
            );

            writer.newLine();

            writer.write(
                    "ORIGINAL"
            );

            writer.newLine();

            writeBoard(
                    writer,
                    originalPuzzle
            );

            writer.write(
                    "CURRENT"
            );

            writer.newLine();

            writeBoard(
                    writer,
                    readBoard()
            );

            writer.write(
                    "SOLUTION"
            );

            writer.newLine();

            writeBoard(
                    writer,
                    solution
            );

            JOptionPane.showMessageDialog(
                    this,
                    "Puzzle saved successfully.",
                    "Save",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (IOException ex) {

            JOptionPane.showMessageDialog(
                    this,
                    "Error saving puzzle:\n"
                            + ex.getMessage(),
                    "Save Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // ============================================================
    // WRITE BOARD
    // ============================================================

    private void writeBoard(
            BufferedWriter writer,
            int[][] board)
            throws IOException {

        for (int r = 0; r < SIZE; r++) {

            for (int c = 0; c < SIZE; c++) {

                if (c > 0) {
                    writer.write(" ");
                }

                writer.write(
                        Integer.toString(
                                board[r][c]
                        )
                );
            }

            writer.newLine();
        }
    }

    // ============================================================
    // LOAD PUZZLE
    // ============================================================

    private void loadPuzzle() {

        if (gameBusy) {
            return;
        }

        JFileChooser chooser =
                new JFileChooser();

        chooser.setDialogTitle(
                "Load Sudoku Puzzle"
        );

        if (chooser.showOpenDialog(this)
                != JFileChooser.APPROVE_OPTION) {

            return;
        }

        File file =
                chooser.getSelectedFile();

        try (BufferedReader reader =
                     new BufferedReader(
                             new FileReader(file)
                     )) {

            String header =
                    reader.readLine();

            if (!"ADVANCED_SUDOKU_MULTI_V3"
                    .equals(header)) {

                throw new IOException(
                        "Unsupported or invalid Sudoku file."
                );
            }

            String sizeLine =
                    reader.readLine();

            if (sizeLine == null ||
                    !sizeLine.startsWith("SIZE=")) {

                throw new IOException(
                        "Missing Sudoku size."
                );
            }

            int loadedSize =
                    Integer.parseInt(
                            sizeLine.substring(
                                    "SIZE=".length()
                            )
                    );

            if (loadedSize != 6 &&
                    loadedSize != 9 &&
                    loadedSize != 16) {

                throw new IOException(
                        "Unsupported Sudoku size."
                );
            }

            // Difficulty
            String difficultyLine =
                    reader.readLine();

            if (difficultyLine == null ||
                    !difficultyLine.startsWith(
                            "DIFFICULTY="
                    )) {

                throw new IOException(
                        "Missing difficulty."
                );
            }

            String loadedDifficulty =
                    difficultyLine.substring(
                            "DIFFICULTY=".length()
                    );

            // Mistakes
            String mistakesLine =
                    reader.readLine();

            if (mistakesLine == null ||
                    !mistakesLine.startsWith(
                            "MISTAKES="
                    )) {

                throw new IOException(
                        "Missing mistakes data."
                );
            }

            int loadedMistakes =
                    Integer.parseInt(
                            mistakesLine.substring(
                                    "MISTAKES=".length()
                            )
                    );

            // Hints
            String hintsLine =
                    reader.readLine();

            if (hintsLine == null ||
                    !hintsLine.startsWith(
                            "HINTS="
                    )) {

                throw new IOException(
                        "Missing hints data."
                );
            }

            int loadedHints =
                    Integer.parseInt(
                            hintsLine.substring(
                                    "HINTS=".length()
                            )
                    );

            // Elapsed time
            String elapsedLine =
                    reader.readLine();

            if (elapsedLine == null ||
                    !elapsedLine.startsWith(
                            "ELAPSED="
                    )) {

                throw new IOException(
                        "Missing elapsed time."
                );
            }

            long loadedElapsed =
                    Long.parseLong(
                            elapsedLine.substring(
                                    "ELAPSED=".length()
                            )
                    );

            // Original
            if (!"ORIGINAL".equals(
                    reader.readLine()
            )) {

                throw new IOException(
                        "Missing ORIGINAL section."
                );
            }

            int[][] loadedOriginal =
                    readSavedBoard(
                            reader,
                            loadedSize
                    );

            // Current
            if (!"CURRENT".equals(
                    reader.readLine()
            )) {

                throw new IOException(
                        "Missing CURRENT section."
                );
            }

            int[][] loadedCurrent =
                    readSavedBoard(
                            reader,
                            loadedSize
                    );

            // Solution
            if (!"SOLUTION".equals(
                    reader.readLine()
            )) {

                throw new IOException(
                        "Missing SOLUTION section."
                );
            }

            int[][] loadedSolution =
                    readSavedBoard(
                            reader,
                            loadedSize
                    );

            // ----------------------------------------------------
            // Validate loaded file
            // ----------------------------------------------------

            validateLoadedData(
                    loadedSize,
                    loadedOriginal,
                    loadedCurrent,
                    loadedSolution
            );

            // ----------------------------------------------------
            // Apply loaded configuration
            // ----------------------------------------------------

            if (loadedSize == 6) {

                sizeBox.setSelectedItem(
                        "6 × 6"
                );

            } else if (loadedSize == 9) {

                sizeBox.setSelectedItem(
                        "9 × 9"
                );

            } else {

                sizeBox.setSelectedItem(
                        "16 × 16"
                );
            }

            updateBoardConfiguration();

            createGrid();

            originalPuzzle =
                    loadedOriginal;

            solution =
                    loadedSolution;

            mistakes =
                    Math.max(
                            loadedMistakes,
                            0
                    );

            hints =
                    Math.max(
                            loadedHints,
                            0
                    );

            finalElapsedTime =
                    Math.max(
                            loadedElapsed,
                            0
                    );

            displayLoadedCurrentBoard(
                    loadedCurrent
            );

            difficultyBox.setSelectedItem(
                    loadedDifficulty
            );

            gameRunning = false;

            stopGameTimer();

            updateTimer();

            updateGameInformation();

            statusLabel.setText(
                    "Puzzle loaded successfully."
            );

        } catch (Exception ex) {

            JOptionPane.showMessageDialog(
                    this,
                    "Unable to load puzzle:\n"
                            + ex.getMessage(),
                    "Load Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // ============================================================
    // READ SAVED BOARD
    // ============================================================

    private int[][] readSavedBoard(
            BufferedReader reader,
            int size)
            throws IOException {

        int[][] board =
                new int[size][size];

        for (int r = 0; r < size; r++) {

            String line =
                    reader.readLine();

            if (line == null) {

                throw new IOException(
                        "Unexpected end of file."
                );
            }

            String[] parts =
                    line.trim().split(
                            "\\s+"
                    );

            if (parts.length != size) {

                throw new IOException(
                        "Invalid row length."
                );
            }

            for (int c = 0; c < size; c++) {

                int value =
                        Integer.parseInt(
                                parts[c]
                        );

                if (value < 0 ||
                        value > size) {

                    throw new IOException(
                            "Invalid Sudoku value."
                    );
                }

                board[r][c] =
                        value;
            }
        }

        return board;
    }

    // ============================================================
    // VALIDATE LOADED DATA
    // ============================================================

   private void validateLoadedData(
        int loadedSize,
        int[][] loadedOriginal,
        int[][] loadedCurrent,
        int[][] loadedSolution) throws IOException {

    int oldSize = SIZE;

    int oldBoxRows = BOX_ROWS;
    int oldBoxCols = BOX_COLS;

    SIZE = loadedSize;

    if (SIZE == 6) {

        BOX_ROWS = 2;
        BOX_COLS = 3;

    } else if (SIZE == 9) {

        BOX_ROWS = 3;
        BOX_COLS = 3;

    } else if (SIZE == 16) {

        BOX_ROWS = 4;
        BOX_COLS = 4;

    } else {

        throw new IOException(
                "Unsupported Sudoku size."
        );
    }

    try {

        // Validate original puzzle
        if (!isValidBoard(
                copyBoard(loadedOriginal))) {

            throw new IOException(
                    "Original puzzle is invalid."
            );
        }

        // Validate current board
        if (!isValidBoard(
                copyBoard(loadedCurrent))) {

            throw new IOException(
                    "Current puzzle is invalid."
            );
        }

        // Validate solution
        if (!isValidBoard(
                copyBoard(loadedSolution))) {

            throw new IOException(
                    "Solution is invalid."
            );
        }

        // Solution must be complete
        if (!isBoardComplete(
                loadedSolution)) {

            throw new IOException(
                    "Solution is incomplete."
            );
        }

        // Check original clues against solution
        for (int r = 0; r < SIZE; r++) {

            for (int c = 0; c < SIZE; c++) {

                // Original clue must match solution
                if (loadedOriginal[r][c] != 0 &&
                        loadedOriginal[r][c]
                                != loadedSolution[r][c]) {

                    throw new IOException(
                            "Original puzzle does not match solution."
                    );
                }

                // Original clue cannot be modified
                if (loadedOriginal[r][c] != 0 &&
                        loadedCurrent[r][c]
                                != loadedOriginal[r][c]) {

                    throw new IOException(
                            "An original Sudoku clue was modified."
                    );
                }

                // Current value must match solution
                if (loadedCurrent[r][c] != 0 &&
                        loadedCurrent[r][c]
                                != loadedSolution[r][c]) {

                    throw new IOException(
                            "Current board contains an incorrect value."
                    );
                }
            }
        }

    } finally {

        // Restore current configuration
        SIZE = oldSize;

        BOX_ROWS = oldBoxRows;
        BOX_COLS = oldBoxCols;
    }
}
    // ============================================================
    // DISPLAY LOADED CURRENT BOARD
    // ============================================================

    private void displayLoadedCurrentBoard(
            int[][] current) {

        for (int r = 0; r < SIZE; r++) {

            for (int c = 0; c < SIZE; c++) {

                int value =
                        current[r][c];

                if (value == 0) {

                    cells[r][c].setText("");

                } else {

                    cells[r][c].setText(
                            valueToSymbol(value)
                    );
                }

                if (originalPuzzle[r][c] != 0) {

                    cells[r][c].setEditable(false);

                    cells[r][c].setBackground(
                            new Color(
                                    235,
                                    235,
                                    235
                            )
                    );

                } else {

                    cells[r][c].setEditable(true);

                    cells[r][c].setBackground(
                            Color.WHITE
                    );
                }
            }
        }

        boardPanel.revalidate();
        boardPanel.repaint();
    }

    // ============================================================
    // COPY BOARD
    // ============================================================

    private int[][] copyBoard(
            int[][] board) {

        int[][] copy =
                new int[board.length][];

        for (int i = 0;
             i < board.length;
             i++) {

            copy[i] =
                    board[i].clone();
        }

        return copy;
    }

    // ============================================================
    // BOARDS EQUAL
    // ============================================================

    private boolean boardsEqual(
            int[][] first,
            int[][] second) {

        if (first == null ||
                second == null ||
                first.length != second.length) {

            return false;
        }

        for (int r = 0;
             r < first.length;
             r++) {

            if (first[r].length !=
                    second[r].length) {

                return false;
            }

            for (int c = 0;
                 c < first[r].length;
                 c++) {

                if (first[r][c] !=
                        second[r][c]) {

                    return false;
                }
            }
        }

        return true;
    }

    // ============================================================
    // MAIN METHOD
    // ============================================================

    public static void main(
            String[] args) {

        SwingUtilities.invokeLater(
                () -> {

                    try {

                        UIManager.setLookAndFeel(
                                UIManager
                                        .getSystemLookAndFeelClassName()
                        );

                    } catch (Exception ignored) {
                    }

                    SudokuSolver app =
                            new SudokuSolver();

                    app.setVisible(true);
                }
        );
    }
}