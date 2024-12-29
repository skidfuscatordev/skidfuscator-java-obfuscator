package framework.testclasses;

import framework.Pure;

public class PolynomialFunction {
    private final double[] coefficients;

    @Pure(description = "Creates polynomial from coefficients",
         because = {"Copies input array", "Internal state initialization"})
    public static PolynomialFunction fromCoefficients(double... coeffs) {
        double[] copy = new double[coeffs.length];
        System.arraycopy(coeffs, 0, copy, 0, coeffs.length);
        return new PolynomialFunction(copy);
    }

    private PolynomialFunction(double[] coefficients) {
        this.coefficients = coefficients;
    }

    @Pure(description = "Evaluates polynomial at point",
         because = {"Pure computation", "No state changes"})
    public double evaluate(double x) {
        double result = 0;
        for (int i = coefficients.length - 1; i >= 0; i--) {
            result = result * x + coefficients[i];
        }
        return result;
    }

    @Pure(description = "Computes derivative polynomial",
         because = {"Creates new polynomial", "Pure computation"})
    public PolynomialFunction derivative() {
        if (coefficients.length <= 1) return new PolynomialFunction(new double[0]);
        
        double[] derivCoeffs = new double[coefficients.length - 1];
        for (int i = 0; i < derivCoeffs.length; i++) {
            derivCoeffs[i] = coefficients[i + 1] * (i + 1);
        }
        return new PolynomialFunction(derivCoeffs);
    }

    @Pure(description = "Adds two polynomials",
         because = {"Creates new polynomial", "No input modification"})
    public static PolynomialFunction add(PolynomialFunction p1, PolynomialFunction p2) {
        int maxLength = Math.max(p1.coefficients.length, p2.coefficients.length);
        double[] result = new double[maxLength];
        
        for (int i = 0; i < maxLength; i++) {
            double c1 = i < p1.coefficients.length ? p1.coefficients[i] : 0;
            double c2 = i < p2.coefficients.length ? p2.coefficients[i] : 0;
            result[i] = c1 + c2;
        }
        
        return new PolynomialFunction(result);
    }
}