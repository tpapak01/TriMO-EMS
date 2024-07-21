package jmetal.util;

import java.util.Random;

public class EuclideanDist {
    public static void main(String[] args) {
        EuclideanDist euc = new EuclideanDist();
        Random rnd = new Random();

        int N = Integer.parseInt(args[0]);

        Double[] a = new Double[N];
        Double[] b = new Double[N];

        euc.print(euc.init(a, rnd));
        euc.print(euc.init(b, rnd));
        System.out.println(euc.distance(a, b));
    }

    private Double[] init(Double[] src, Random rnd) {
        for (int i = 0; i < src.length; i++) {
            src[i] = rnd.nextDouble();
        }
        return src;
    }

    public static double distance(Double[] a, Double[] b) {
        double diff_square_sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            diff_square_sum += (a[i] - b[i]) * (a[i] - b[i]);
        }
        return Math.sqrt(diff_square_sum);
    }

    private void print(Double[] x) {
        for (int j = 0; j < x.length; j++) {
            System.out.print(" " + x[j] + " ");
        }
        System.out.println();
    }
}
