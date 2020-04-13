package org.iota.qupla;

import java.math.BigInteger;

public class Trytes {
    private static final Trytes[] powersOf2 = new Trytes[384];

    private long[] quads = new long[21];

    static {
        // calculate the first 384 powers of 2 in trinary, encoded as base-27 trytes
        final short[] trytes = new short[84]; // round 81 up to multiple of 4
        trytes[0] = 1;
        for (int i = 0; i < 384; i++) {
            powersOf2[i] = new Trytes();
            powersOf2[i].trytesToQuads(trytes);

            // double the current power of 2 to get the next power of 2
            int carry = 0;
            for (int j = 0; j < 81; j++) {
                int value = trytes[j] * 2 + carry;
                carry = value / 27;
                trytes[j] = (short) (value % 27);
            }
        }
    }

    public static byte[] fromBytes(byte[] hash) {
        // this algorithm uses the pre-calculated powers of 2 table
        // it takes 1 bit at a time from the 384-bit hash value
        // and adds the corresponding power of 2 to the result when the bit is 1

        // make sure 384-bit input value is positive
        // note: input value has MSB first
        final boolean negative = hash[0] < 0;
        if (negative) {
            // negate 48-byte value, starting at LSB
            int carry = 1;
            for (int i = 47; i >= 0; i--) {
                carry += ~hash[i] & 0xff;
                hash[i] = (byte) carry;
                carry >>= 8;
            }
        }

        final Trytes sum = new Trytes();
        int bitNr = 0;
        // walk from lSB to MSB
        for (int i = 47; i >= 0; i--) {
            // take next byte and add the corresponding 8 powers of 2 without carry between trytes
            // note that this will not overflow a tryte (max possible value is only 384 * 26 == 9984),
            // or become negative, and even has room to spare for future carry-over
            byte bits = hash[i];
            for (int j = 0; j < 8; j++) {
                // only add power of 2 when bit is 1
                if ((bits & 1) != 0) {
                    // add precalculated 2^bitNr to result using quadruple addition
                    sum.addPower(powersOf2[bitNr].quads);
                }

                // go for next bit and power of 2
                bits >>= 1;
                bitNr++;
            }
        }

        // unwrap tryte shorts from longs after quadruple additions
        final short[] trytes = sum.quadsToTrytes();

        final byte[] bytes = new byte[81];

        // handle accumulated carry between trytes after all power of 2 additions
        // in the same loop convert from unbalanced to balanced base-27
        // and also negate the result value when necessary
       int carry = 0;
        for (int i = 0; i < 81; i++) {
            int value = trytes[i] + carry + 13;
            carry = value / 27;
            int tryte = value % 27 - 13;

            // if we negated the binary input data we need to negate the ternary result
            bytes[i] = (byte) (negative ? -tryte : tryte);
        }

        // clear trit 243
        final int v = bytes[80];
        bytes[80] = (byte) (v < -4 ? v + 9 : v > 4 ? v - 9 : v);

        return bytes;
    }

    private void addPower(final long[] rhs) {
        // 21x quadruple add, handles 84 trytes
        // this loop can be 100% parallelized on hardware
        // maybe even unroll loop for a percent extra oomph?
        for (int i = 20; i >= 0; i--) {
            quads[i] += rhs[i];
        }
    }

    private short[] quadsToTrytes() {
        // convert 64-bit long integer array to the equivalent tryte short array

        final short[] trytes = new short[84];
        int i = 0;
        while (i < 81) {
            long value = quads[i >> 2];
            trytes[i++] = (short) value;
            value >>= 16;
            trytes[i++] = (short) value;
            value >>= 16;
            trytes[i++] = (short) value;
            trytes[i++] = (short) (value >> 16);
        }

        return trytes;
    }

    private void trytesToQuads(final short[] trytes) {
        // convert tryte short array to the equivalent 64-bit long integer array
        // shift them in in reverse so we can shift them out in correct order
        int i = 83;
        while (i >= 0) {
            long value = trytes[i--] << 16;
            value += trytes[i--];
            value <<= 16;
            value += trytes[i--];
            value <<= 16;
            quads[i >> 2] = value + trytes[i--];
        }
    }

    @Override
    public String toString() {
        final short[] trytes = quadsToTrytes();
        final BigInteger b27 = new BigInteger("27");

        // find last nonzero tryte
        int last = 80;
        while (last > 0 && trytes[last] == 0) {
            last--;
        }

        // construct big integer representing this value
        BigInteger big = new BigInteger("0");
        for (int i = last; i >= 0; i--) {
            big = big.multiply(b27).add(new BigInteger("" + trytes[i]));
        }

        return big.toString();
    }
}
