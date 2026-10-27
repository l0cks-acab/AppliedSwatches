package com.lach.appliedswatches.client;

/** sRGB to CIELAB and colour distance. */
public final class ColorMath {
    private ColorMath() {
    }

    public static float[] lab(int rgb) {
        double r = lin((rgb >> 16) & 0xFF), g = lin((rgb >> 8) & 0xFF), b = lin(rgb & 0xFF);
        double x = (0.4124 * r + 0.3576 * g + 0.1805 * b) / 0.95047;
        double y = 0.2126 * r + 0.7152 * g + 0.0722 * b;
        double z = (0.0193 * r + 0.1192 * g + 0.9505 * b) / 1.08883;
        double fx = f(x), fy = f(y), fz = f(z);
        return new float[]{(float) (116 * fy - 16), (float) (500 * (fx - fy)), (float) (200 * (fy - fz))};
    }

    /** CIE76 delta E. */
    public static float distance(float[] a, float[] b) {
        float dl = a[0] - b[0], da = a[1] - b[1], db = a[2] - b[2];
        return (float) Math.sqrt(dl * dl + da * da + db * db);
    }

    private static double lin(int c) {
        double v = c / 255.0;
        return v <= 0.04045 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
    }

    private static double f(double t) {
        return t > 216 / 24389.0 ? Math.cbrt(t) : (24389 / 27.0 * t + 16) / 116;
    }
}
