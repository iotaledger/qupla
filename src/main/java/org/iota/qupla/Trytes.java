package org.iota.qupla;

public class Trytes {
    private static final Trytes[] powersOf2 = new Trytes[384];

    private int length;
    public short[] trytes = new short[82]; // 82 to not have to check for overflows when carry happens
    private long[] quads = new long[20]; // Java specific, to hold converted copy of first 80 bytes

    static {
        // calculate the first 384 powers of 2 in trinary, encoded as trytes

        Trytes prev = new Trytes();
        prev.length = 1;
        prev.trytes[0] = 1;
        prev.quads[0] = 1;
        powersOf2[0] = prev;

        for (int i = 1; i < 384; i++) {
            final Trytes next = new Trytes();

            // add double the value of the previous one
            for (int j = 0; j < prev.length; j++) {
                int value = (prev.trytes[j] << 1) + next.trytes[j];
                if (value >= 27) {
                    value -= 27;
                    next.trytes[j + 1]++;
                }

                next.trytes[j] = (short) value;
            }

            // length is same as previous one except when carry happened
            next.length = prev.length;
            if (next.trytes[next.length] > 0) {
                next.length++;
            }

            // wrap bytes into longs for future quadruple addition
            // (specific to Java because a tryte array cannot be accessed as a long array through casting)
            next.trytesToQuads();

            // pre-calculate index of last used quad
            prev.length = prev.length < 77 ? ((prev.length + 3) >> 2) - 1 : 19;

            powersOf2[i] = next;
            prev = next;
        }
    }

    public static byte[] fromBytes(byte[] hash) {
        // this algorithm uses the pre-calculated powers of 2 table
        // it takes 1 bit at a time from the 384-bit hash value
        // and adds the corresponding power of 2 to the result when the bit is 1

        // make sure 384-bit input value is positive
        // note: input value has MSB first
        final boolean negative = (hash[0] & 0x80) != 0;
        if (negative) {
            // negate 48-byte value, starting at LSB
            int carry = 1;
            for (int i = 47; i >= 0; i--) {
                carry += ~hash[i] & 0xff;
                hash[i] = (byte) carry;
                carry >>= 8;
            }
        }

        final Trytes result = new Trytes();
        int bitNr = 0;
        // walk from lSB to MSB
        for (int i = 47; i >= 0; i--) {
            // take next byte and add the corresponding 8 powers of 2 without carry between trytes
            // note that this will not overflow a tryte (max is 384 * 26 == 9984),
            // or become negative, and even has room to spare for carry-over
            byte bits = hash[i];
            for (int b = 0; b < 8; b++) {
                // only add power of 2 when bit is 1
                if ((bits & 1) != 0) {
                    // add precalculated 2^bitNr to result using quadruple addition
                    result.addPower(bitNr);
                }

                // go for next bit and power of 2
                bits >>= 1;
                bitNr++;
            }
        }

        // unwrap trytes from longs after quadruple additions
        // (specific to Java because a short array cannot be accessed as a long array through casting)
        result.quadsToTrytes();

        final byte[] bytes = new byte[81];

        // handle carry between trytes after all power of 2 additions that were
        // guaranteed to have no carry between trytes and to be positive numbers
        int tryte = result.trytes[0];
        for (int i = 0; i < 81; i++) {
            // add overflow to next tryte
            int next = result.trytes[i + 1] + tryte / 27;

            // keep tryte without overflow
            tryte %= 27;

            // convert unbalanced ternary tryte to balanced ternary
            if (tryte > 13) {
                tryte -= 27;
                next++;
            }

            // if we negated the binary input data we need to negate the ternary result
            bytes[i] = (byte) (negative ? -tryte : tryte);

            tryte = next;
        }

        // clear trit 243
        final int v = bytes[80];
        bytes[80] = (byte) (v < -4 ? v + 9 : v > 4 ? v - 9 : v);

        // conversion is now complete, next step would be to convert the result
        // to the type you need, be it tryte String, tryte bytes, or trit bytes
        return bytes;
    }

    private void addPower(final int exponent) {
        // treat the tryte array as a 64-bit long integer array
        // thereby doing quadruple adds (4 trytes in a single add)

        final Trytes power = powersOf2[exponent];

        // note that in Java we have to copy between tryte and long arrays and vice versa
        // at strategic points in the code before doing this
        // languages that support proper casting could use the following C equivalent instead
        // uint64_t * lhs = (uit64_t *) &trytes[0];
        // uint64_t * rhs = (uit64_t *) &power.trytes[0];
        final long[] lhs = quads;
        final long[] rhs = power.quads;

        // 20x quadruple add, handles 80 trytes
        // this loop can be 100% parallelized on hardware
        // maybe even unroll loop for a percent extra oomph?

        // strangely enough limiting additions to the non-zero ones
        // by using power.length instead of 19
        // decreases the speed of the algorithm in Java, YMMV
        for (int i = 19; i >= 0; i--) {
            lhs[i] += rhs[i];
        }

        // handle final single tryte
        trytes[80] += power.trytes[80];
    }

    private void quadsToTrytes() {
        // convert 64-bit long integer array to the equivalent tryte array

        // note that this is Java specific
        // languages that support proper casting can remove this function

        int i = 0;
        while (i < 80) {
            long value = quads[i >> 2];
            trytes[i++] = (short) value;
            value >>= 16;
            trytes[i++] = (short) value;
            value >>= 16;
            trytes[i++] = (short) value;
            trytes[i++] = (short) (value >> 16);
        }
    }

    private void trytesToQuads() {
        // convert tryte array to the equivalent 64-bit long integer array

        // note that this is Java specific
        // languages that support proper casting can remove this function

        for (int i = 0; i < 80; i += 4) {
            long value = trytes[i + 3] << 16;
            value += trytes[i + 2];
            value <<= 16;
            value += trytes[i + 1];
            value <<= 16;
            quads[i >> 2] = value + trytes[i];
        }
    }

    @Override
    public String toString() {
        return length + ": " + quads[0];
    }
}
