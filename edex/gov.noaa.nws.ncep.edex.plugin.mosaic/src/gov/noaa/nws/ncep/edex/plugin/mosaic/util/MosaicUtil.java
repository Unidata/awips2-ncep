package gov.noaa.nws.ncep.edex.plugin.mosaic.util;

/**
 * A series of methods to help in the processing and tiling of mosaic data.
 * 
 * @author brockwoo
 * @version 1.0
 */
public class MosaicUtil {

    private static final double TWOTOTHE52 = (double) (1L << 52);

    final static int[] table = { 0, 16, 22, 27, 32, 35, 39, 42, 45, 48, 50, 53,
            55, 57, 59, 61, 64, 65, 67, 69, 71, 73, 75, 76, 78, 80, 81, 83, 84,
            86, 87, 89, 90, 91, 93, 94, 96, 97, 98, 99, 101, 102, 103, 104,
            106, 107, 108, 109, 110, 112, 113, 114, 115, 116, 117, 118, 119,
            120, 121, 122, 123, 124, 125, 126, 128, 128, 129, 130, 131, 132,
            133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143, 144, 144,
            145, 146, 147, 148, 149, 150, 150, 151, 152, 153, 154, 155, 155,
            156, 157, 158, 159, 160, 160, 161, 162, 163, 163, 164, 165, 166,
            167, 167, 168, 169, 170, 170, 171, 172, 173, 173, 174, 175, 176,
            176, 177, 178, 178, 179, 180, 181, 181, 182, 183, 183, 184, 185,
            185, 186, 187, 187, 188, 189, 189, 190, 191, 192, 192, 193, 193,
            194, 195, 195, 196, 197, 197, 198, 199, 199, 200, 201, 201, 202,
            203, 203, 204, 204, 205, 206, 206, 207, 208, 208, 209, 209, 210,
            211, 211, 212, 212, 213, 214, 214, 215, 215, 216, 217, 217, 218,
            218, 219, 219, 220, 221, 221, 222, 222, 223, 224, 224, 225, 225,
            226, 226, 227, 227, 228, 229, 229, 230, 230, 231, 231, 232, 232,
            233, 234, 234, 235, 235, 236, 236, 237, 237, 238, 238, 239, 240,
            240, 241, 241, 242, 242, 243, 243, 244, 244, 245, 245, 246, 246,
            247, 247, 248, 248, 249, 249, 250, 250, 251, 251, 252, 252, 253,
            253, 254, 254, 255 };

    /**
     * A faster replacement for (int)(java.lang.Math.sqrt(x)). Completely
     * accurate for x < 2147483648 (i.e. 2^31)...
     */
    static int fastSqrt(int x) {
        int xn;

        if (x >= 0x10000) {
            if (x >= 0x1000000) {
                if (x >= 0x10000000) {
                    if (x >= 0x40000000) {
                        xn = table[x >> 24] << 8;
                    } else {
                        xn = table[x >> 22] << 7;
                    }
                } else {
                    if (x >= 0x4000000) {
                        xn = table[x >> 20] << 6;
                    } else {
                        xn = table[x >> 18] << 5;
                    }
                }

                xn = (xn + 1 + (x / xn)) >> 1;
                xn = (xn + 1 + (x / xn)) >> 1;
                return ((xn * xn) > x) ? --xn : xn;
            } else {
                if (x >= 0x100000) {
                    if (x >= 0x400000) {
                        xn = table[x >> 16] << 4;
                    } else {
                        xn = table[x >> 14] << 3;
                    }
                } else {
                    if (x >= 0x40000) {
                        xn = table[x >> 12] << 2;
                    } else {
                        xn = table[x >> 10] << 1;
                    }
                }

                xn = (xn + 1 + (x / xn)) >> 1;

                return ((xn * xn) > x) ? --xn : xn;
            }
        } else {
            if (x >= 0x100) {
                if (x >= 0x1000) {
                    if (x >= 0x4000) {
                        xn = (table[x >> 8]) + 1;
                    } else {
                        xn = (table[x >> 6] >> 1) + 1;
                    }
                } else {
                    if (x >= 0x400) {
                        xn = (table[x >> 4] >> 2) + 1;
                    } else {
                        xn = (table[x >> 2] >> 3) + 1;
                    }
                }

                return ((xn * xn) > x) ? --xn : xn;
            } else {
                if (x >= 0) {
                    return table[x] >> 4;
                }
            }
        }

        throw new IllegalArgumentException(
                "Attempt to take the square root of negative number");
    }

    /**
     * A quick rounding method which is much faster than the Math.round method.
     * 
     * @param a
     *            The number to round
     * @return An int representation of the rounded number
     */
    public static int fastRound(double a) {
        double dd = TWOTOTHE52 + Math.abs(a);
        int ll = (int) Double.doubleToRawLongBits(dd);
        int signMask = (int) (Double.doubleToRawLongBits(a) >> 63);
        return (ll ^ signMask) - signMask;
    }

    /**
     * A quick method to get the absolute value which is faster than Math.abs.
     * 
     * @param a
     *            The int to get the absolute value for
     * @return The absolute value for the passed in int
     */
    public static int fastAbs(int a) {
        if (a >= 0)
            return a;
        return (a * -1);
    }

    /**
     * Returns the subarray for the passed byte array.
     * 
     * @param from
     *            The full byte array to get something out of
     * @param start
     *            The start of the array to collect
     * @param size
     *            The number of elements out of the array to collect
     * @return The subarray
     */
    public static byte[] subArray(byte[] from, int start, int size) {
    	//System.out.println("In MosaicUtil subArray- raw data length=" + from.length);
    	//System.out.println("In MosaicUtil subArray- offset = " + start + " data length = " + size);
    	byte[] newArray = new byte[size];
		//copy number of bytes by subtracting offset from data length.  This is because the
    	//"size" info read out from AWC NSSL image is off by 4 bytes.
        System.arraycopy(from, start, newArray, 0, from.length-start);

        return newArray;
    }

    /**
     * Returns the subarray for the passed float array.
     * 
     * @param from
     *            The full float array to get something out of
     * @param start
     *            The start of the array to collect
     * @param size
     *            The number of elements out of the array to collect
     * @return The subarray
     */
    public static float[] subArray(float[] from, int start, int size) {
        float[] newArray = new float[size];
        System.arraycopy(from, 0, newArray, 0, size);
        return newArray;
    }

    /**
     * Returns the subarray for the passed short array.
     * 
     * @param from
     *            The full short array to get something out of
     * @param start
     *            The start of the array to collect
     * @param size
     *            The number of elements out of the array to collect
     * @return The subarray
     */
    public static short[] subArray(short[] from, int start, int size) {
        short[] newArray = new short[size];
        System.arraycopy(from, 0, newArray, 0, size);
        return newArray;
    }

}
