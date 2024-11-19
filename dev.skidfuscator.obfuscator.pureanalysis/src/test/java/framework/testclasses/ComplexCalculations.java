package framework.testclasses;

import framework.Impure;
import framework.Pure;

public class ComplexCalculations {
    @Pure(
            description = "Matrix multiplication",
            because = {
                    "Pure mathematical operation",
                    "Creates new array for result",
                    "No side effects"
            }
    )
    public static int[][] multiplyMatrix(int[][] a, int[][] b) {
        int[][] result = new int[a.length][b[0].length];
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < b[0].length; j++) {
                for (int k = 0; k < a[0].length; k++) {
                    result[i][j] += a[i][k] * b[k][j];
                }
            }
        }
        return result;
    }

    @Impure(
            description = "Matrix operation with logging",
            because = {
                    "Logs to console",
                    "Side effect in computation",
                    "Non-deterministic output"
            }
    )
    public static int[][] multiplyMatrixWithLogging(int[][] a, int[][] b) {
        System.out.println("Starting matrix multiplication...");
        int[][] result = new int[a.length][b[0].length];
        for (int i = 0; i < a.length; i++) {
            System.out.println("Processing row " + i);
            for (int j = 0; j < b[0].length; j++) {
                for (int k = 0; k < a[0].length; k++) {
                    result[i][j] += a[i][k] * b[k][j];
                }
            }
        }
        return result;
    }
}
   