package cn.com.vortexa.example.lens;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class MazeSolver extends JPanel {
    private final int[][] walls;
    private final int cellSize = 40;
    private final int padding = 20;
    private final List<Point> path = new ArrayList<>(); // 存储路径点

    private static final int UP = 1, RIGHT = 2, DOWN = 4, LEFT = 8;

    public MazeSolver(int[][] walls) {
        this.walls = walls;
        setPreferredSize(new Dimension(walls[0].length * cellSize + padding * 2,
                walls.length * cellSize + padding * 2));
        solveMaze(); // 计算路径
    }

    // 迷宫寻路 (BFS)
    private void solveMaze() {
        int rows = walls.length, cols = walls[0].length;
        boolean[][] visited = new boolean[rows][cols];
        Map<Point, Point> parent = new HashMap<>(); // 记录路径

        Queue<Point> queue = new LinkedList<>();
        Point start = new Point(0, 0);
        Point end = new Point(cols - 1, rows - 1);
        queue.add(start);
        visited[0][0] = true;

        // BFS 搜索
        while (!queue.isEmpty()) {
            Point cur = queue.poll();
            int r = cur.y, c = cur.x;
            if (cur.equals(end)) break; // 找到终点

            // 方向: 上、右、下、左
            int[][] directions = {{-1, 0, UP}, {0, 1, RIGHT}, {1, 0, DOWN}, {0, -1, LEFT}};
            for (int[] d : directions) {
                int nr = r + d[0], nc = c + d[1], dir = d[2];

                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols
                        && !visited[nr][nc] && (walls[r][c] & dir) == 0) {
                    queue.add(new Point(nc, nr));
                    visited[nr][nc] = true;
                    parent.put(new Point(nc, nr), cur);
                }
            }
        }

        // 生成路径
        Point step = end;
        while (parent.containsKey(step)) {
            path.add(step);
            step = parent.get(step);
        }
        path.add(start);
        Collections.reverse(path); // 逆序得到正确路径
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.BLACK);

        for (int row = 0; row < walls.length; row++) {
            for (int col = 0; col < walls[row].length; col++) {
                int x = padding + col * cellSize;
                int y = padding + row * cellSize;
                int cell = walls[row][col];

                if ((cell & UP) != 0) g.drawLine(x, y, x + cellSize, y);
                if ((cell & RIGHT) != 0) g.drawLine(x + cellSize, y, x + cellSize, y + cellSize);
                if ((cell & DOWN) != 0) g.drawLine(x, y + cellSize, x + cellSize, y + cellSize);
                if ((cell & LEFT) != 0) g.drawLine(x, y, x, y + cellSize);
            }
        }

        // 绘制路径
        g.setColor(Color.RED);
        for (int i = 0; i < path.size() - 1; i++) {
            Point p1 = path.get(i), p2 = path.get(i + 1);
            int x1 = padding + p1.x * cellSize + cellSize / 2;
            int y1 = padding + p1.y * cellSize + cellSize / 2;
            int x2 = padding + p2.x * cellSize + cellSize / 2;
            int y2 = padding + p2.y * cellSize + cellSize / 2;
            g.drawLine(x1, y1, x2, y2);
        }
    }

    public static void main(String[] args) {
        int[][] walls = {
                {2, 12, 4, 2, 10, 8, 6, 12, 2, 10, 8, 6, 10, 10, 8},
                {2, 13, 1, 0, 4, 4, 3, 11, 10, 8, 6, 13, 2, 8, 0},
                {0, 7, 10, 10, 13, 5, 0, 4, 2, 12, 1, 7, 8, 6, 12},
                {2, 11, 14, 14, 9, 5, 4, 7, 12, 3, 12, 1, 4, 5, 5},
                {0, 4, 1, 1, 6, 11, 13, 1, 3, 8, 5, 0, 5, 5, 1},
                {6, 11, 14, 10, 13, 4, 1, 2, 8, 0, 3, 14, 13, 5, 4},
                {1, 0, 1, 6, 11, 9, 6, 10, 10, 14, 8, 7, 11, 9, 5},
                {6, 10, 8, 7, 10, 10, 11, 14, 12, 7, 12, 5, 4, 0, 1},
                {1, 2, 8, 7, 10, 14, 10, 11, 13, 5, 1, 5, 7, 10, 12},
                {6, 10, 8, 1, 6, 13, 2, 8, 5, 3, 10, 13, 1, 0, 5},
                {5, 6, 10, 14, 11, 9, 6, 12, 3, 14, 8, 1, 2, 10, 9},
                {1, 1, 0, 3, 8, 0, 1, 1, 0, 1, 2, 10, 10, 8, 0}
        };

        JFrame frame = new JFrame("Maze Solver");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new MazeSolver(walls));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
