import java.io.*;
import java.math.BigInteger;
import java.util.*;

public class Solution {
    public static void main(String[] args) throws Exception {
        // Read JSON file into string
        BufferedReader br = new BufferedReader(new FileReader("test1.json"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line.trim());
        }
        br.close();

        String json = sb.toString();

        // Extract n and k
        int n = extractInt(json, "\"n\"");
        int k = extractInt(json, "\"k\"");

        // Extract shares
        Map<Integer, Share> shares = new HashMap<>();
        String sharePattern = "\"(\\d+)\"\\s*:\\s*\\{\\s*\"base\"\\s*:\\s*\"?(\\d+)\"?,\\s*\"value\"\\s*:\\s*\"(\\w+)\"\\s*\\}";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(sharePattern).matcher(json);
        while (matcher.find()) {
            int index = Integer.parseInt(matcher.group(1));
            int base = Integer.parseInt(matcher.group(2));
            String value = matcher.group(3);
            Share s = new Share(base, value);
            shares.put(index, s);
        }

        // Polynomial solver with BigInteger
        PolynomialSolver solver = new PolynomialSolver(n, k, shares);
        BigInteger secret = solver.findSecret();

        // System.out.println("Secret = " + secret);

        List<Map.Entry<Integer, BigInteger>> badPoints = solver.findIncorrectPoints();
        if (badPoints.isEmpty()) {
            System.out.println("All points are correct.");
        } else {
            System.out.println("Incorrect points found:");
            for (Map.Entry<Integer, BigInteger> bad : badPoints) {
                System.out.println("x = " + bad.getKey() + ", y = " + bad.getValue());
            }
        }
    }

    static int extractInt(String json, String key) {
        int idx = json.indexOf(key);
        if (idx == -1) return -1;
        int colon = json.indexOf(":", idx);
        int comma = json.indexOf(",", colon);
        if (comma == -1) comma = json.indexOf("}", colon);
        return Integer.parseInt(json.substring(colon + 1, comma).replaceAll("[^0-9]", ""));
    }
}

class Share {
    int base;
    String value;
    BigInteger actual;

    Share(int base, String value) {
        this.base = base;
        this.value = value;
        this.actual = new BigInteger(value, base);
    }

    @Override
    public String toString() {
        return "Base: " + base + ", Value: " + value + ", Actual: " + actual;
    }
}
