package org.iota.qupla;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;

public class TestConverter {
    private static final SecureRandom random = new SecureRandom();
    private static final BigInteger RADIX = BigInteger.valueOf(3);

    private static long iriTime;
    private static long newTime;

    public static void main(String[] args) {
        final byte[] hash = new byte[48];

        // run through a bunch of extremes to shake out edge conditions
        for (int i = 0; i < 256; i++) {
            Arrays.fill(hash, (byte) i);
            bin2trin(hash, false);
        }

        // large amount of random tries to shake out anomalies and average timing
        for (int i = 0; i < 100_000; i++) {
            random.nextBytes(hash);
            bin2trin(hash, false);
        }

        System.out.println("IRI: " + iriTime);
        System.out.println("New: " + newTime);
        System.out.println("Factor: " + (1.0 * iriTime / newTime));
    }

    private static void bin2trin(final byte[] hash, final boolean log) {

        if (log) {
            // show input bytes
            for (int i = 0; i < 48; i++) {
                System.out.print(" " + "0123456789abcdef".charAt((hash[i] >> 4) & 0x0f));
                System.out.print("" + "0123456789abcdef".charAt(hash[i] & 0x0f));
                if (i == 23) {
                    System.out.println();
                }
            }
            System.out.println();
        }

        // old IRI magic
        long startTime = System.nanoTime();
        final String iriTrytes = trytesFromBytesIri(hash);
        iriTime += System.nanoTime() - startTime;

        // new conversion magic
        startTime = System.nanoTime();
        final byte[] trytes = Trytes.fromBytes(hash);
        final char[] chars = new char[81];
        for (int i = 0; i < 81; i++) {
            chars[i] = "NOPQRSTUVWXYZ9ABCDEFGHIJKLM".charAt(trytes[i] + 13);
        }
        final String newTrytes = new String(chars);
        newTime += System.nanoTime() - startTime;

        if (log || !newTrytes.equals(iriTrytes)) {
            // show old and new conversion results
            System.out.println(newTrytes + ", " + startTime);
            System.out.println(iriTrytes + ", " + iriTime);
            if (!newTrytes.equals(iriTrytes)) {
                // highlight any mismatch
                System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
                System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
                System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
            }
        }
    }

    private static void tritsFromBigInt(final BigInteger value, final byte[] destination) {
        // save sign
        final int signum = value.signum();

        // work with unsigned value
        BigInteger absoluteValue = value.abs();

        //
        for (int i = 0; i < 243; i++) {
            BigInteger[] divRemainder = absoluteValue.divideAndRemainder(RADIX);
            absoluteValue = divRemainder[0];

            byte remainder = divRemainder[1].byteValue();
            if (remainder > 1) {
                remainder = -1;
                absoluteValue = absoluteValue.add(BigInteger.ONE);
            }
            destination[i] = signum < 0 ? (byte) -remainder : remainder;
        }
    }

    private static String trytesFromBytesIri(final byte[] hash) {
        final byte[] trits = new byte[243];
        final BigInteger kerl = new BigInteger(hash);
        tritsFromBigInt(kerl, trits);
        trits[242] = 0;

        final char[] trytes = new char[81];
        int next = 0;
        for (int i = 0; i < 81; i++) {
            final int t0 = trits[next];
            final int t1 = trits[next + 1];
            final int t2 = trits[next + 2];
            final int tryte = t0 + t1 + (t1 << 1) + t2 + (t2 << 3);
            trytes[i] = "NOPQRSTUVWXYZ9ABCDEFGHIJKLM".charAt(tryte + 13);
            next += 3;
        }

        return new String(trytes);
    }
}
