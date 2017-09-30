import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class SimpleMultiplier implements Runnable {
  private static int [][] matrixA, matrixB, result;
  private int rowIndex;

  public SimpleMultiplier(int [][]matrixA, int rowIndex, int[][] matrixB, int[][] result) {
    this.matrixA = matrixA;
    this.rowIndex = rowIndex;
    this.matrixB = matrixB;
    this.result = result;
  }

  @Override
  public void run() {
    for (int i = 0; i < matrixB[0].length; i++) {
      int sum = 0;

      for (int j = 0; j < matrixB.length; j++)
        sum += matrixA[rowIndex][j] * matrixB[j][i];

      result[rowIndex][i] = sum;
    }
  }
}

class SimpleAproach {
  private static int [][] matrixA, matrixB, result;
  private static int mA_cols, mA_rows, mB_cols, mB_rows;

  public static void main(String[] args) {
    mA_cols = Integer.parseInt(args[0]);
    mA_rows = Integer.parseInt(args[1]);
    mB_cols = Integer.parseInt(args[2]);
    mB_rows = Integer.parseInt(args[3]);

    if(mA_cols != mB_rows) {
      System.out.println("Error: Number of columns of the matrix A must be equal to the number of rows in matrix B.");
      System.exit(1);
    }

    matrixA = new int[mA_rows][mA_cols];
    matrixB = new int[mB_rows][mB_cols];
    result = new int[mA_rows][mB_cols];

    System.out.printf("Start reading in matrices: %s%n", (System.currentTimeMillis() / 10L));

    fillMatrix(matrixA, "matrixA_nums");
    fillMatrix(matrixB, "matrixB_nums");

    System.out.printf("Start multiplication: %s%n", (System.currentTimeMillis() / 10L));

    ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()*4);

    for(int i = 0; i < mA_rows; i++)
      executor.execute(new SimpleMultiplier(matrixA, i, matrixB, result));

    executor.shutdown();
    while(!executor.isTerminated()) {}
  }

  private static void fillMatrix(int[][] matrix, String file) {
        Scanner scanner = null;

        try {
            scanner = new Scanner(new File(file));
        } catch (FileNotFoundException e) {}

        for(int i = 0; i < matrix.length; i++)
          for(int j = 0; j < matrix[i].length; j++)
            matrix[i][j] = scanner.nextInt();
    }
}
