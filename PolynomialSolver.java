import java.math.BigInteger;
import java.util.*;

class PolynomialSolver {

    int n, k;
    Map<Integer, Share> shares;

    PolynomialSolver(int n, int k, Map<Integer, Share> shares) {
        this.n = n;
        this.k = k;
        this.shares = shares;
    }

    // Evaluate polynomial at x using BigInteger
    private BigInteger evaluate(BigInteger[] coeffs, int x) {
        BigInteger res = BigInteger.ZERO;
        BigInteger pow = BigInteger.ONE;
        BigInteger X = BigInteger.valueOf(x);

        for (BigInteger c : coeffs) {
            res = res.add(c.multiply(pow));
            pow = pow.multiply(X);
        }
        return res;
    }

    // Interpolate polynomial using Gaussian elimination on k points
    private BigInteger[] interpolate(List<Map.Entry<Integer, BigInteger>> points) {
        int size = points.size();
        BigInteger[][] A = new BigInteger[size][size];
        BigInteger[] b = new BigInteger[size];

        for (int i = 0; i < size; i++) {
            int x = points.get(i).getKey();
            BigInteger y = points.get(i).getValue();
            BigInteger pow = BigInteger.ONE;
            for (int j = 0; j < size; j++) {
                A[i][j] = pow;
                pow = pow.multiply(BigInteger.valueOf(x));
            }
            b[i] = y;
        }

        // Gaussian elimination (with BigInteger)
        for (int i = 0; i < size; i++) {
            // normalize row
            BigInteger div = A[i][i];
            for (int j = i; j < size; j++) A[i][j] = A[i][j].divide(div);
            b[i] = b[i].divide(div);

            // eliminate
            for (int r = 0; r < size; r++) {
                if (r != i) {
                    BigInteger factor = A[r][i];
                    for (int c = i; c < size; c++) {
                        A[r][c] = A[r][c].subtract(factor.multiply(A[i][c]));
                    }
                    b[r] = b[r].subtract(factor.multiply(b[i]));
                }
            }
        }
        return b; // coefficients
    }

    // Detect incorrect points
    public List<Map.Entry<Integer, BigInteger>> findIncorrectPoints() {
        List<Map.Entry<Integer, Share>> allPoints = new ArrayList<>(shares.entrySet());

        // Take first k points
        List<Map.Entry<Integer, BigInteger>> sample = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            Map.Entry<Integer, Share> entry = allPoints.get(i);
            sample.add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue().actual));
        }

        // Interpolate polynomial
        BigInteger[] coeffs = interpolate(sample);

        // Validate all points
        List<Map.Entry<Integer, BigInteger>> badPoints = new ArrayList<>();
        for (Map.Entry<Integer, Share> entry : allPoints) {
            BigInteger expected = evaluate(coeffs, entry.getKey());
            if (!expected.equals(entry.getValue().actual)) {
                badPoints.add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue().actual));
            }
        }
        return badPoints;
    }

    // Lagrange interpolation with BigInteger
    public BigInteger lagrangeInterpolation(Map<Integer, Share> subset) {
        BigInteger secret = BigInteger.ZERO;

        List<Integer> xs = new ArrayList<>(subset.keySet());

        for (int i = 0; i < xs.size(); i++) {
            int xi = xs.get(i);
            BigInteger yi = subset.get(xi).actual;

            BigInteger num = BigInteger.ONE;
            BigInteger den = BigInteger.ONE;

            for (int j = 0; j < xs.size(); j++) {
                if (i == j) continue;
                int xj = xs.get(j);

                num = num.multiply(BigInteger.valueOf(-xj)); // (0 - xj)
                den = den.multiply(BigInteger.valueOf(xi - xj));
            }

            BigInteger term = yi.multiply(num).divide(den); // exact division since it's Shamir scheme
            secret = secret.add(term);
        }

        return secret;
    }

    // Try all subsets of size k
    public BigInteger findSecret() {
        List<Integer> keys = new ArrayList<>(shares.keySet());
        List<BigInteger> results = new ArrayList<>();

        combine(keys, 0, new ArrayList<>(), results);

        // Majority voting
        Map<BigInteger, Integer> freq = new HashMap<>();
        for (BigInteger res : results) {
            freq.put(res, freq.getOrDefault(res, 0) + 1);
        }

        BigInteger correctSecret = null;
        int maxFreq = 0;
        for (Map.Entry<BigInteger, Integer> entry : freq.entrySet()) {
            if (entry.getValue() > maxFreq) {
                correctSecret = entry.getKey();
                maxFreq = entry.getValue();
            }
        }

        System.out.println("Correct Secret = " + correctSecret);
        return correctSecret;
    }

    private void combine(List<Integer> arr, int idx, List<Integer> path, List<BigInteger> results) {
        if (path.size() == k) {
            Map<Integer, Share> subset = new HashMap<>();
            for (int key : path) {
                subset.put(key, shares.get(key));
            }
            results.add(lagrangeInterpolation(subset));
            return;
        }
        if (idx == arr.size()) return;

        path.add(arr.get(idx));
        combine(arr, idx + 1, path, results);
        path.remove(path.size() - 1);
        combine(arr, idx + 1, path, results);
    }
}
