package org.iota.qupla;

import jota.pow.ICurl;
import jota.pow.SpongeFactory;
import jota.utils.Converter;
import jota.utils.Signing;

import java.security.SecureRandom;

public class CrackerJack {
    private static final int HASH_LENGTH = 18;
    private static final int LOOP_COUNT = 10_000_000;
    private static final SecureRandom random = new SecureRandom();
    private static final String[] bundles = {
            "9DQGIRZXRCJHGHKUKPIZEAOXPPNLPFWRDAXVURLAFDHTFGTQWHXIKLOUYAYJYTHTHDZUXQBDSJFABBIFZ",
            "9XLEYFRSWDEBLKXQXGHFTBXGDYAKVHBOPGTNLVLGQRUAJFKLDOUHQHVLJNEHKFPGBFXWRVQQAHBCFX9GA",
            "RKQE9CWIDGYERCZJPQUD9QVIJQTVOKTGEIKGFIXIHXKDCVWRBYWAJBQIHNZ9JZHUVITNIZVEZS9SKQRFC",
            "SJJNBCHTCJGYBYUJVYPJSPKJITBEIKPQNYPJEFTHUHYXLAYUZCCJVJKEXVZFUNAIEGKROKAVLVPIZIGTX",
    };
    private static final int[] sums = new int[27 * 27];
    private static final ICurl curl = SpongeFactory.create(SpongeFactory.Mode.CURLP81);
    private static final Signing signing = new Signing(curl);
    private static int[] maxBundle;

    public static void main(String[] args) {
        CrackerJack caller = new CrackerJack();
        // caller.crackerJack();
        //for (int i = 0; i < 100; i++)
        caller.analyze();
    }

    public void analyze() {

        final int[] hash = new int[HASH_LENGTH];
        final int[] maxHash = new int[HASH_LENGTH];

        int sum = makeHash(maxHash);
        while (normalize(maxHash, sum) != 7) {
            // force maxHash to start with target tryte value
            sum = makeHash(maxHash);
        }

        System.out.print("H0 :");
        printHash(maxHash);
        System.out.println();

        for (int h = 1; h < 2; h++) {
            sum = makeHash(hash);
            while (normalize(hash, sum) >= 0) {
                // do not exceed target tryte value
                sum = makeHash(hash);
            }

            System.out.print("H" + h + " :");
            printHash(hash);
            System.out.println();

            for (int i = 1; i < HASH_LENGTH; i++) {
                if (maxHash[i] < hash[i]) {
                    maxHash[i] = hash[i];
                }
            }
        }

        System.out.print("Max:");
        printHash(maxHash);
        System.out.println();

        final int[] found = new int[28];
        final int[] reject = new int[28];
        final int offset = 14;
        for (int n = 0; n < LOOP_COUNT; n++) {
            sum = makeHash(hash);
            final int bucket = normalize(hash, sum);
            boolean isGood = true;
            for (int i = 0; i < HASH_LENGTH; i++) {
                if (hash[i] > maxHash[i]) {
                    isGood = false;
                    reject[offset + bucket]++;
                    break;
                }
            }
            if (isGood) {
                found[offset + bucket]++;
            }
        }

        sum = printFound(found, reject, 13, 0, offset);
        sum += printFound(found, reject, -1, -14, offset);
        System.out.print("\nTotal found: " + sum);

        sum = 0;
        for (int i = 0; i < HASH_LENGTH; i++) {
            sum += maxHash[i];
        }
        System.out.println(",  extra fragments: " + sum + " of " + (HASH_LENGTH * 12));
    }

    private int printFound(int[] found, int[] reject, int first, int last, int offset) {
        System.out.println();
        for (int i = first; i >= last; i--) {
            final String txt = "       " + i;
            System.out.print(txt.substring(txt.length() - 6));
        }
        System.out.println();

        int sum = 0;
        for (int i = first; i >= last; i--) {
            final String txt = "       " + found[offset + i];
            sum += found[offset + i];
            System.out.print(txt.substring(txt.length() - 6));
        }
        System.out.println();

        for (int i = first; i >= last; i--) {
            String txt = "       " + ((reject[offset + i] * 10000L + 5000) / LOOP_COUNT);
            txt = txt.substring(txt.length() - 5);
            System.out.print(txt.substring(0, 3) + "." + txt.substring(3));
        }
        System.out.println();

        return sum;
    }

    private void printHash(int[] hash) {
        for (int i = 0; i < HASH_LENGTH; i++) {
            final String txt = "    " + hash[i];
            System.out.print(txt.substring(txt.length() - 4));
        }
    }

    private int normalize(final int[] hash, int sum) {
        // normalize hash and determine count bucket
        hash[0] -= sum;
        if (hash[0] > 12) {
            // every hash that goes to or over M
            return 13;
        }

        if (hash[0] >= -13) {
            // only this tryte needed normalizing
            return hash[0];
        }

        // handle overflow, sum can only be positive
        sum = -13 - hash[0];
        hash[0] = -13;

        // normalize next trytes until we run out of positive sum
        for (int i = 1; i < HASH_LENGTH; i++) {
            hash[i] -= sum;
            if (hash[i] >= -13) {
                break;
            }

            // handle overflow
            sum = -13 - hash[i];
            hash[i] = -13;
        }

        // overflow bucket
        return -14;
    }

    private int makeHash(final int[] hash) {
        // generate M-free random hash and pre-calculate sum
        int sum = 0;
        for (int i = 0; i < HASH_LENGTH; i++) {
            hash[i] = random.nextInt(26) - 13;
            sum += hash[i];
        }
        return sum;
    }

    public void crackerJack() {
        maxBundle = signing.normalizedBundle(bundles[0]);
        for (final String bundle : bundles) {
            display(bundle);
        }

//    for (int i = 0; i < 100000; i++)
//    {
//      final String bundle = SeedRandomGenerator.generateNewSeed();
//      display(bundle);
//    }
//
//    for (int i = 0; i < sums.length; i++)
//    {
//      System.out.println((i - 27 * 13) + ": " + sums[i]);
//    }

        System.out.println("\nTarget values:");
        for (int i = 0; i < maxBundle.length; i += 9) {
            for (int j = 0; j < 9; j++) {
                final String txt = "    " + maxBundle[i + j];
                System.out.print(txt.substring(txt.length() - 4));
            }
            System.out.println();
        }

        double prod = 1;
        System.out.println("\nFactors:");
        for (int i = 0; i < 54; i += 9) {
            for (int j = 0; j < 9; j++) {
                int factor = 14 + maxBundle[i + j];
                prod *= 27.0 / factor;
                final String txt = "    " + factor;
                System.out.print(txt.substring(txt.length() - 4));
            }
            System.out.println();
        }
        System.out.println("\nTries: " + prod);

//        maxBundle[0] = -13;
        final int[] hash = new int[27];
        int found = 10000000;
        for (int n = 0; n < 10000000; n++) {
            for (int i = 0; i < hash.length; i++) {
                hash[i] = random.nextInt(27) - 13;
            }

            normalize(hash);

            for (int i = 0; i < hash.length; i++) {
                if (hash[i] > maxBundle[i]) {
                    found--;
                    break;
                }
            }
        }
        System.out.println("\nFound: " + found);
    }

    private void normalize(int[] hash) {
        int sum = 0;
        for (int i = 0; i < hash.length; i++) {
            sum += hash[i];
        }

        // negative sum can only influence first tryte otherwise we hit maxFirst
        // use 13 for maxFirst to prevent M-bug
        if (sum <= 0) {
            hash[0] -= sum;
            return;
        }

        for (int i = 0; i < hash.length; i++) {
            hash[i] -= sum;
            if (hash[i] >= -13) {
                return;
            }
            sum = -13 - hash[i];
            hash[i] = -13;
        }
    }

    private void display(String bundle) {
        final int[] normBundle = signing.normalizedBundle(bundle);
        System.out.println("\nBundle: " + bundle);
        System.out.println("Normalized:");
        for (int i = 0; i < normBundle.length; i += 9) {
            for (int j = 0; j < 9; j++) {
                final String txt = "    " + normBundle[i + j];
                System.out.print(txt.substring(txt.length() - 4));
                if (maxBundle[i + j] < normBundle[i + j]) {
                    maxBundle[i + j] = normBundle[i + j];
                }
            }
            System.out.println();
        }

        System.out.print("Sums: ");
        for (int i = 0; i < bundle.length(); i += 27) {
            int sum = 0;
            for (int j = 0; j < 27; j++) {
                sum += Converter.value(Converter.trits("" + bundle.charAt(i + j)));
            }
            System.out.print("  " + sum);
            sums[sum + 27 * 13]++;
        }
        System.out.println();
    }
}
