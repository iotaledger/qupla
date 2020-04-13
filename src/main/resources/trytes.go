package trytes

import (
	"unsafe"
)

type Trytes struct {
	length  int
	trytes  [82]int16 // 82 to not have to check for overflows when carry happens
}

var powersOf2 [384]Trytes

func init() {
    // calculate the first 384 powers of 2 in trinary, encoded as trytes

    prev := &powersOf2[0]
    prev.length = 1
    prev.trytes[0] = 1

    for i := 1; i < 384; i++ {
        next := &powersOf2[i]

        // add double the value of the previous one
        for j := 0; j < prev.length; j++ {
            value := (prev.trytes[j] << 1) + next.trytes[j]
            if (value >= 27) {
                value -= 27
                next.trytes[j + 1]++
            }

            next.trytes[j] = value
        }

        // length is same as previous one except when carry happened
        next.length = prev.length
        if next.trytes[next.length] > 0 {
            next.length++
        }

        // pre-calculate index of last used quad
        if prev.length < 77 {
            prev.length = ((prev.length + 3) >> 2) - 1;
        } else {
            prev.length = 19;
        }

        prev = next;
    }
}

func FromBytes(hash []byte) []int8 {
    // this algorithm uses the pre-calculated powers of 2 table
    // it takes 1 bit at a time from the 384 bit hash value
    // and adds the corresponding power of 2 to the result when the bit is 1

    // make sure 384-bit input value is positive
    // note: input value has MSB first
    negative := (hash[0] & 0x80) != 0
    if negative {
        // negate 48-byte value, starting at LSB
        carry := 1
        for i := 47; i >= 0; i-- {
            carry += int(^hash[i])
            hash[i] = byte(carry)
            carry >>= 8
        }
    }

    result := Trytes{}
    bitNr := 0;
    // walk from lSB to MSB
    for i := 47; i >= 0; i-- {
        // take next byte and add the corresponding 8 powers of 2 without carry between trytes
        // note that this will not overflow a tryte (max is 384 * 26 == 9984),
        // or become negative, and even has room to spare for carry-over
        bits := hash[i];
        for b := 0; b < 8; b++ {
            // only add power of 2 when bit is 1
            if (bits & 1) != 0 {
                // add precalculated 2^bitNr to result using quadruple addition
                result.addPower(bitNr)
            }

            // go for next bit and power of 2
            bits >>= 1
            bitNr++
        }
    }

    bytes := make([]int8, 81)

    // handle carry between trytes after all power of 2 additions that were
    // guaranteed to have no carry between trytes and to be positive numbers
    tryte := result.trytes[0]
    for i := 0; i < 81; i++ {
        // add overflow to next tryte
        next := result.trytes[i + 1] + tryte / 27

        // keep tryte without overflow
        tryte %= 27

        // convert unbalanced ternary tryte to balanced ternary
        if tryte > 13 {
            tryte -= 27
            next++
        }

        // if we negated the binary data we need to negate the ternary result
        if negative {
           bytes[i] = int8(-tryte)
        } else {
            bytes[i] = int8(tryte)
        }

        tryte = next
    }

    // clear trit 243
    v := bytes[80]
	if v > 4 {
		bytes[80] = v - 9
	} else if v < -4 {
		bytes[80] = v + 9
	}

    // conversion is now complete, next step would be to convert the result
    // to the type you need, be it tryte String, tryte bytes, or trit bytes
    return bytes
}

func (class Trytes) addPower(exponent int) {
    // treat the tryte array as a 64-bit long integer array
    // thereby doing quadruple adds (4 trytes in a single add)

    power := &powersOf2[exponent]

    // note that in Java we have to copy between tryte and long arrays and vice versa
    // at strategic points in the code before doing this
    // languages that support proper casting could use the following C equivalent instead
    // uint64_t * lhs = (uit64_t *) &trytes[0];
    // uint64_t * rhs = (uit64_t *) &power.trytes[0];
    lhs := *(*[]uint64) (unsafe.Pointer(&class.trytes[0]))
    rhs := *(*[]uint64) (unsafe.Pointer(&power.trytes[0]))

    // 20x quadruple add, handles 80 trytes
    // this loop can be 100% parallelized on hardware
    // maybe even unroll loop for a percent extra oomph?

    // strangely enough limiting additions to the non-zero ones
    // by using power.length instead of 19
    // decreases the speed of the algorithm in Java, YMMV
    for i := 19; i >= 0; i-- {
        lhs[i] += rhs[i]
    }

    // handle final single tryte
    class.trytes[80] += power.trytes[80]
}
