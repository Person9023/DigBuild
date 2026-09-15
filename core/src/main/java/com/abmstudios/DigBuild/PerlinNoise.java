package com.abmstudios.DigBuild;

public class PerlinNoise {

    private final int[] permutation = new int[512];

    public PerlinNoise(long seed) {

        java.util.Random random = new java.util.Random(seed);

        int[] p = new int[256];

        for (int i = 0; i < 256; i++) {
            p[i] = i;
        }

        // Shuffle the permutation table
        for (int i = 255; i > 0; i--) {

            int j = random.nextInt(i + 1);

            int temp = p[i];
            p[i] = p[j];
            p[j] = temp;
        }

        for (int i = 0; i < 512; i++) {
            permutation[i] = p[i & 255];
        }
    }

    private double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private double lerp(double a, double b, double t) {
        return a + t * (b - a);
    }

    // 2D gradient
    private double grad(int hash, double x, double y) {

        int h = hash & 3;

        double u = (h & 1) == 0 ? x : -x;
        double v = (h & 2) == 0 ? y : -y;

        return u + v;
    }

    // 3D gradient
    private double grad3D(int hash, double x, double y, double z) {

        int h = hash & 15;

        double u = h < 8 ? x : y;

        double v;

        if (h < 4) {
            v = y;
        } else if (h == 12 || h == 14) {
            v = x;
        } else {
            v = z;
        }

        return ((h & 1) == 0 ? u : -u)
            + ((h & 2) == 0 ? v : -v);
    }

    // Existing 2D noise
    public double noise(double x, double y) {

        int X = (int) Math.floor(x) & 255;
        int Y = (int) Math.floor(y) & 255;

        x -= Math.floor(x);
        y -= Math.floor(y);

        double u = fade(x);
        double v = fade(y);

        int aa = permutation[permutation[X] + Y];
        int ab = permutation[permutation[X] + Y + 1];
        int ba = permutation[permutation[X + 1] + Y];
        int bb = permutation[permutation[X + 1] + Y + 1];

        double x1 = lerp(
            grad(aa, x, y),
            grad(ba, x - 1, y),
            u
        );

        double x2 = lerp(
            grad(ab, x, y - 1),
            grad(bb, x - 1, y - 1),
            u
        );

        return lerp(x1, x2, v);
    }


    public double octaveNoise(
        double x,
        double y,
        int octaves,
        double persistence,
        double lacunarity
    ) {
        double total = 0.0;
        double amplitude = 1.0;
        double frequency = 1.0;

        double maxValue = 0.0;

        for (int i = 0; i < octaves; i++) {
            total += noise(x * frequency, y * frequency) * amplitude;

            maxValue += amplitude;

            amplitude *= persistence;
            frequency *= lacunarity;
        }

        return total / maxValue;
    }


    // New 3D noise
    public double noise(double x, double y, double z) {

        int X = (int) Math.floor(x) & 255;
        int Y = (int) Math.floor(y) & 255;
        int Z = (int) Math.floor(z) & 255;

        x -= Math.floor(x);
        y -= Math.floor(y);
        z -= Math.floor(z);

        double u = fade(x);
        double v = fade(y);
        double w = fade(z);

        int aaa = permutation[
            permutation[
                permutation[X] + Y
                ] + Z
            ];

        int aba = permutation[
            permutation[
                permutation[X] + Y + 1
                ] + Z
            ];

        int aab = permutation[
            permutation[
                permutation[X] + Y
                ] + Z + 1
            ];

        int abb = permutation[
            permutation[
                permutation[X] + Y + 1
                ] + Z + 1
            ];

        int baa = permutation[
            permutation[
                permutation[X + 1] + Y
                ] + Z
            ];

        int bba = permutation[
            permutation[
                permutation[X + 1] + Y + 1
                ] + Z
            ];

        int bab = permutation[
            permutation[
                permutation[X + 1] + Y
                ] + Z + 1
            ];

        int bbb = permutation[
            permutation[
                permutation[X + 1] + Y + 1
                ] + Z + 1
            ];

        double x1 = lerp(
            grad3D(aaa, x, y, z),
            grad3D(baa, x - 1, y, z),
            u
        );

        double x2 = lerp(
            grad3D(aba, x, y - 1, z),
            grad3D(bba, x - 1, y - 1, z),
            u
        );

        double y1 = lerp(x1, x2, v);

        double x3 = lerp(
            grad3D(aab, x, y, z - 1),
            grad3D(bab, x - 1, y, z - 1),
            u
        );

        double x4 = lerp(
            grad3D(abb, x, y - 1, z - 1),
            grad3D(bbb, x - 1, y - 1, z - 1),
            u
        );

        double y2 = lerp(x3, x4, v);

        return lerp(y1, y2, w);
    }
}

