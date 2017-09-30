import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

class BlockMultiplier extends RecursiveAction {
  private static final int SEQUENTIAL_THRESHOLD = 100;
  private int [][] matrixA, matrixB, result;

  BlockMultiplier(int [][]matrixA, int[][] matrixB, int[][] result) {
    this.matrixA = matrixA;
    this.matrixB = matrixB;
    this.result = result;
  }

  private void computeDirectly() {
    for(int i = 0; i < matrixA.length; i++) {
      for (int j = 0; j < matrixB[0].length; j++) {
        int sum = 0;

        for (int k = 0; k < matrixB.length; k++)
          sum += matrixA[i][k] * matrixB[k][j];

        result[i][j] = sum;
        sum = 0;
      }
    }
  }

  @Override
  public void compute() {
    int len = matrixA.length;

    if(len <= SEQUENTIAL_THRESHOLD) {
      computeDirectly();
      return;
    }
    else if(len != matrixA[0].length || len != matrixB.length || len != matrixB[0].length) {
      int max_len = len;

      if(max_len < matrixA[0].length)
        max_len = matrixA[0].length;

      if(max_len < matrixB.length)
        max_len = matrixB.length;

      if(max_len < matrixB[0].length)
        max_len = matrixB[0].length;

      int[][] matrixA_temp, matrixB_temp, result_temp;
      matrixA_temp = new int[max_len][max_len];
      matrixB_temp = new int[max_len][max_len];
      result_temp = new int[max_len][max_len];

      for(int i=0; i<matrixA.length; i++)
        for(int j=0; j<matrixA[0].length; j++)
          matrixA_temp[i][j] = matrixA[i][j];

      for(int i=0; i<matrixB.length; i++)
        for(int j=0; j<matrixB[0].length; j++)
          matrixB_temp[i][j] = matrixB[i][j];

      new BlockMultiplier(matrixA_temp, matrixB_temp, result_temp).compute();

      for(int i=0; i<result.length; i++)
        for(int j=0; j<result[0].length; j++)
          result[i][j] = result_temp[i][j];

      return;
    }
    else if(len%2 != 0) {
      int[][] matrixA_temp, matrixB_temp, result_temp;
      matrixA_temp = new int[len+1][len+1];
      matrixB_temp = new int[len+1][len+1];
      result_temp = new int[len+1][len+1];

      for(int i=0; i<len; i++) {
        for(int j=0; j<len; j++) {
          matrixA_temp[i][j] = matrixA[i][j];
          matrixB_temp[i][j] = matrixB[i][j];
        }
      }

      BlockMultiplier bm = new BlockMultiplier(matrixA_temp, matrixB_temp, result_temp);
      bm.compute();
      for(int i=0; i<len; i++)
        for(int j=0; j<len; j++)
          result[i][j] = result_temp[i][j];
      return;
    }

    int [][] A11 = new int[len/2][len/2];
    int [][] A12 = new int[len/2][len/2];
    int [][] A21 = new int[len/2][len/2];
    int [][] A22 = new int[len/2][len/2];

    int [][] B11 = new int[len/2][len/2];
    int [][] B12 = new int[len/2][len/2];
    int [][] B21 = new int[len/2][len/2];
    int [][] B22 = new int[len/2][len/2];

    partition(matrixA, A11, 0, 0);
    partition(matrixA, A12, 0, len/2);
    partition(matrixA, A21, len/2, 0);
    partition(matrixA, A22, len/2, len/2);

    partition(matrixB, B11, 0, 0);
    partition(matrixB, B12, 0, len/2);
    partition(matrixB, B21, len/2, 0);
    partition(matrixB, B22, len/2, len/2);

    int [][] M1 = new int[len/2][len/2];
    int [][] M2 = new int[len/2][len/2];
    int [][] M3 = new int[len/2][len/2];
    int [][] M4 = new int[len/2][len/2];
    int [][] M5 = new int[len/2][len/2];
    int [][] M6 = new int[len/2][len/2];
    int [][] M7 = new int[len/2][len/2];

    BlockMultiplier bm1 = new BlockMultiplier(add(A11, A22), add(B11, B22), M1);
    bm1.fork();
    BlockMultiplier bm2 = new BlockMultiplier(add(A21, A22), B11, M2);
    bm2.fork();
    BlockMultiplier bm3 = new BlockMultiplier(A11, sub(B12, B22), M3);
    bm3.fork();
    BlockMultiplier bm4 = new BlockMultiplier(A22, sub(B21, B11), M4);
    bm4.fork();
    BlockMultiplier bm5 = new BlockMultiplier(add(A11, A12), B22, M5);
    bm5.fork();
    BlockMultiplier bm6 = new BlockMultiplier(sub(A21, A11), add(B11, B12), M6);
    bm6.fork();
    BlockMultiplier bm7 = new BlockMultiplier(sub(A12, A22), add(B21, B22), M7);
    bm7.compute();

    bm1.join();
    bm2.join();
    bm3.join();
    bm4.join();
    bm5.join();
    bm6.join();

    int [][] C11 = add(sub(add(M1, M4), M5), M7);
    int [][] C12 = add(M3, M5);
    int [][] C21 = add(M2, M4);
    int [][] C22 = add(add(sub(M1, M2), M3), M6);

    copy(C11, result, 0, 0);
    copy(C12, result, 0, len/2);
    copy(C21, result, len/2, 0);
    copy(C22, result, len/2, len/2);
  }

  private int [][] add(int [][] matrixA, int [][] matrixB) {
    int len = matrixA.length;

    int [][] result = new int[len][len];

    for(int i=0; i<len; i++)
      for(int j=0; j<len; j++)
        result[i][j] = matrixA[i][j] + matrixB[i][j];

    return result;
  }

  private int [][] sub(int [][] matrixA, int [][] matrixB) {
    int len = matrixA.length;

    int [][] result = new int[len][len];

    for(int i=0; i<len; i++)
      for(int j=0; j<len; j++)
        result[i][j] = matrixA[i][j] - matrixB[i][j];

    return result;
  }

  private void partition(int[][] source, int[][] target, int source_i, int source_j) {
    for(int i = 0; i<target.length; i++)
      for(int j = 0; j<target.length; j++)
        target[i][j] = source[source_i+i][source_j+j];
  }

  private void copy(int[][] source, int[][] target, int target_i, int target_j) {
    for(int i = 0; i<source.length; i++)
      for(int j = 0; j<source.length; j++)
        target[target_i+i][target_j+j] = source[i][j];
  }
}

class AdvancedAproach {
  private static int [][] matrixA, matrixB, result;
  private static int mA_cols, mA_rows, mB_cols, mB_rows;

  public static void main(String[] args) {
    mA_cols = Integer.parseInt(args[0]);
    mA_rows = Integer.parseInt(args[1]);
    mB_cols = Integer.parseInt(args[2]);
    mB_rows = Integer.parseInt(args[3]);

    matrixA = new int[mA_rows][mA_cols];
    matrixB = new int[mB_rows][mB_cols];

    if(mA_cols != mB_rows) {
      System.out.println("Error: Number of columns of the matrix A must be equal to the number of rows in matrix B.");
      System.exit(1);
    }

    result = new int[mA_rows][mB_cols];

    System.out.printf("Start reading in matrices: %s%n", (System.currentTimeMillis() / 10L));

    fillMatrix(matrixA, "matrixA_nums");
    fillMatrix(matrixB, "matrixB_nums");

    System.out.printf("Start multiplication: %s%n", (System.currentTimeMillis() / 10L));
    ForkJoinPool pool = new ForkJoinPool();
    pool.invoke(new BlockMultiplier(matrixA, matrixB, result));
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
