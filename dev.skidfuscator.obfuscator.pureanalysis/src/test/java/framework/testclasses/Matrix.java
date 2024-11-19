package framework.testclasses;

import framework.Pure;

public class Matrix {
    private final double[][] data;
    
    @Pure(description = "Creates new matrix",
         because = {"Initializes internal state", "No external effects"})
    public static Matrix create(int rows, int cols) {
        return new Matrix(rows, cols);
    }

    private Matrix(int rows, int cols) {
        this.data = new double[rows][cols];
    }

    @Pure(description = "Pure matrix multiplication",
         because = {"Creates new matrix", "No modification of inputs"})
    public static Matrix multiply(Matrix a, Matrix b) {
        if (a.data[0].length != b.data.length) {
            throw new IllegalArgumentException("Invalid dimensions");
        }

        Matrix result = new Matrix(a.data.length, b.data[0].length);
        for (int i = 0; i < a.data.length; i++) {
            for (int j = 0; j < b.data[0].length; j++) {
                for (int k = 0; k < a.data[0].length; k++) {
                    result.data[i][j] += a.data[i][k] * b.data[k][j];
                }
            }
        }
        return result;
    }

    @Pure(description = "Creates transposed matrix",
         because = {"Returns new matrix", "Original unchanged"})
    public Matrix transpose() {
        Matrix result = new Matrix(data[0].length, data.length);
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[0].length; j++) {
                result.data[j][i] = data[i][j];
            }
        }
        return result;
    }
}