/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package test.com.sun.max.unsafe;

import com.sun.max.program.*;
import com.sun.max.unsafe.*;

public class AddressTest extends WordTestCase {

    public AddressTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(AddressTest.class);
    }

    public void test_toString() {
        String s = addressLow.toString();
        assertEquals(s, "@" + Integer.toHexString(low));

        s = address0.toString();
        assertEquals(s, "@0");

        s = addressMax.toString();
        switch (wordWidth()) {
            case BITS_64:
                assertEquals(s, "@ffffffffffffffff");
                break;
            case BITS_32:
                assertEquals(s, "@ffffffff");
                break;
            default:
                ProgramError.unknownCase();
                break;
        }
    }

    public void test_compareTo() {
        assertTrue(addressMedium.compareTo(address0) > 0);
        assertTrue(addressMedium.compareTo(addressMedium) == 0);
        assertTrue(addressMedium.compareTo(addressHigh) < 0);
        assertTrue(address0.compareTo(addressHigh) < 0);
        assertTrue(addressMax.compareTo(address0) > 0);
        assertTrue(addressMax.compareTo(addressHigh) > 0);
        assertTrue(addressMax.compareTo(addressMax) == 0);
        switch (wordWidth()) {
            case BITS_64:
                assertTrue(addressMax32.compareTo(addressMax) < 0);
                break;
            case BITS_32:
                assertTrue(addressMax32.compareTo(addressMax) == 0);
                break;
            default:
                ProgramError.unknownCase();
                break;
        }
    }

    public void testLessThan() {
        assertTrue(addressLow.lessThan(Integer.MAX_VALUE));
        assertFalse(addressMax.lessThan(Integer.MAX_VALUE));
        assertTrue(address0.lessThan(1));
        assertFalse(address0.lessThan(0));
        assertFalse(addressLow.lessThan(low));
        assertTrue(addressLow.lessThan(low + 1));
        assertTrue(address0.lessThan(-1));
        assertTrue(address0.lessThan(-low));
        switch (wordWidth()) {
            case BITS_64:
                assertFalse(addressHigh.lessThan(Integer.MAX_VALUE));
                assertTrue(addressMax32.compareTo(addressHigh) < 0);
                break;
            case BITS_32:
                assertTrue(addressHigh.lessThan(Integer.MAX_VALUE));
                assertTrue(addressMax32.compareTo(addressHigh) > 0);
                break;
            default:
                ProgramError.unknownCase();
                break;
        }
    }

    public void testEquals() {
        assertTrue(address0.equals(0));
        assertTrue(address1.equals(1));
        assertTrue(addressLow.equals(low));
        switch (wordWidth()) {
            case BITS_64:
                assertFalse(addressHigh.equals((int) high));
                break;
            case BITS_32:
                assertTrue(addressHigh.equals((int) high));
                break;
            default:
                ProgramError.unknownCase();
                break;
        }

        assertFalse(addressLow.equals(-1));
        assertFalse(addressLow.equals(-low));
        addressMax.toString();
        assertTrue(addressMax.equals(Address.fromLong(-1L)));
    }

    public void test_plus_Address() {
        assertTrue(addressMedium.plus(addressLow).toInt() == medium + low);
        assertTrue(address0.plus(address0).equals(address0));
        assertTrue(addressMax.plus(address1).toLong() == 0L);

        final long result = addressHigh.plus(addressLow).toLong();
        switch (wordWidth()) {
            case BITS_64:
                assertTrue(result == high + low);
                assertFalse(result == ((int) high + low));
                break;
            case BITS_32:
                assertFalse(result == high + low);
                assertTrue(result == ((int) high + low));
                break;
            default:
                ProgramError.unknownCase();
                break;
        }
    }

    public void test_plus_Offset() {
        assertTrue(address0.plus(offset1).equals(address1));
        assertTrue(address1.plus(offset1.negate()).equals(address0));
        assertTrue(addressMedium.plus(Offset.fromInt(low)).toInt() == medium + low);
        assertTrue(addressMedium.plus(Offset.fromInt(-low)).toInt() == medium - low);
        assertTrue(address0.plus(Offset.zero()).equals(address0));
        assertTrue(addressMax.plus(offset1).toLong() == 0L);
        assertTrue(address0.plus(offset1.negate()).equals(addressMax));

        long result = addressHigh.plus(offsetLow).toLong();
        switch (wordWidth()) {
            case BITS_64:
                assertTrue(result == high + low);
                assertFalse(result == ((int) high + low));
                break;
            case BITS_32:
                assertFalse(result == high + low);
                assertTrue(result == ((int) high + low));
                break;
            default:
                ProgramError.unknownCase();
                break;
        }
        assertTrue(addressLow.plus(offsetHigh).equals(Address.fromLong(result)));

        result = addressLow.plus(offsetHigh.negate()).toLong();
        final long difference = low - high;
        final long differenceLowBits = difference & 0xffffffffL;
        switch (wordWidth()) {
            case BITS_64:
                assertTrue(result == low - high);
                assertFalse(result == differenceLowBits);
                break;
            case BITS_32:
                assertFalse(result == low - high);
                assertTrue(result == differenceLowBits);
                break;
            default:
                ProgramError.unknownCase();
                break;
        }
    }

    public void test_plus_int() {
        assertTrue(address0.plus(1).equals(address1));
        assertTrue(address1.plus(-1).equals(address0));
        assertTrue(addressMedium.plus(low).toInt() == medium + low);
        assertTrue(addressMedium.plus(-low).toInt() == medium - low);
        assertTrue(address0.plus(0).equals(address0));

        assertTrue(addressMax.plus(1).toLong() == 0L);
        assertTrue(address0.plus(-1).equals(addressMax));

        final long result = addressHigh.plus(low).toLong();
        switch (wordWidth()) {
            case BITS_64:
                assertTrue(result == high + low);
                assertFalse(result == ((int) high + low));
                break;
            case BITS_32:
                assertFalse(result == high + low);
                assertTrue(result == ((int) high + low));
                break;
            default:
                ProgramError.unknownCase();
                break;
        }
        assertTrue(addressLow.plus((int) high).equals(Address.fromInt(low + (int) high)));
    }

    public void test_minus_Address() {
        assertTrue(address1.minus(address1).equals(address0));
        assertTrue(address0.minus(address1).equals(addressMax));
        assertTrue(addressMedium.minus(addressLow).toInt() == medium - low);
    }

    public void test_minus_Offset() {
        assertTrue(address1.minus(offset1).equals(address0));
        assertTrue(addressMedium.minus(offsetLow).toInt() == medium - low);
        assertTrue(address0.minus(offset1).equals(addressMax));
        switch (wordWidth()) {
            case BITS_64: {
                assertTrue(addressLow.minus(offsetMedium).equals(offsetLow.minus(offsetMedium)));
                break;
            }
            case BITS_32: {
                final long v = ((long) low - (long) medium) & LOW_32_BITS_MASK;
                assertTrue(addressLow.minus(offsetMedium).toLong() == v);
                break;
            }
            default: {
                ProgramError.unknownCase();
                break;
            }
        }
    }

    public void test_minus_int() {
        assertTrue(address1.minus(1).equals(address0));
        assertTrue(addressMedium.minus(low).toInt() == medium - low);
        assertTrue(addressMedium.minus(low).equals(offsetLow.negate().plus(offsetMedium)));
        assertTrue(address0.minus(1).equals(addressMax));
    }

    public void test_dividedBy() {
        try {
            addressLow.dividedBy(0);
            fail();
        } catch (ArithmeticException arithmeticException) {
        }
        try {
            addressLow.dividedBy(-1);
        } catch (ArithmeticException arithmeticException) {
        }
        assertTrue(addressLow.dividedBy(4).toInt() == low / 4);
        assertTrue(address0.dividedBy(42).toInt() == 0);
    }

    public void test_remainder() {
        try {
            addressLow.remainder(0);
            fail();
        } catch (ArithmeticException arithmeticException) {
        }
        try {
            addressLow.remainder(-1);
        } catch (ArithmeticException arithmeticException) {
        }
        for (int i = 0; i < 10; i++) {
            assertTrue(Address.fromInt(i).remainder(4) == i % 4);
        }
        assertTrue(address0.remainder(42) == 0);
    }

    public void test_isRoundedBy() {
        try {
            addressLow.isRoundedBy(0);
            fail();
        } catch (ArithmeticException arithmeticException) {
        }
        try {
            addressLow.isRoundedBy(-1);
        } catch (ArithmeticException arithmeticException) {
        }
        for (int i = 0; i < 10; i++) {
            assertTrue(Address.fromInt(i).isRoundedBy(4) == (i % 4 == 0));
        }
        assertTrue(address0.isRoundedBy(42));
    }

    private int roundedUpBy(int base, int number) {
        final int rest = number % base;
        if (rest == 0) {
            return number;
        }
        return number + base - rest;
    }

    public void test_roundedUpBy() {
        try {
            addressLow.roundedUpBy(0);
            fail();
        } catch (ArithmeticException arithmeticException) {
        }
        try {
            addressLow.roundedUpBy(-1);
        } catch (ArithmeticException arithmeticException) {
        }
        for (int i = 0; i < 20; i++) {
            assertTrue(Address.fromInt(i).roundedUpBy(8).toInt() == roundedUpBy(8, i));
        }
        assertTrue(address0.roundedUpBy(12).equals(address0));
    }

    public void test_roundedDownBy() {
        try {
            addressLow.roundedDownBy(0);
            fail();
        } catch (ArithmeticException arithmeticException) {
        }
        try {
            addressLow.roundedDownBy(-1);
        } catch (ArithmeticException arithmeticException) {
        }
        for (int i = 0; i < 20; i++) {
            assertTrue(Address.fromInt(i).roundedDownBy(8).toInt() == (i & ~7));
        }
        assertTrue(address0.roundedDownBy(12).equals(address0));
    }

    public void test_align() {
        final int n = Word.size();
        assertTrue(Address.zero().wordAligned().toInt() == 0);
        assertTrue(Address.fromInt(1).wordAligned().toInt() == n);
        assertTrue(Address.fromInt(n).wordAligned().toInt() == n);
        assertTrue(Address.fromInt(n - 1).wordAligned().toInt() == n);
        assertTrue(Address.fromInt(n / 2).wordAligned().toInt() == n);
        assertTrue(Address.fromInt(n + 1).wordAligned().toInt() == n + n);
        assertTrue(Address.fromInt(n + (n / 2)).wordAligned().toInt() == n + n);
        assertTrue(Address.fromInt(n + n).wordAligned().toInt() == n + n);
        assertTrue(Address.fromInt(n + n - 1).wordAligned().toInt() == n + n);
        assertTrue(Address.fromInt(2003 * n).wordAligned().toInt() == 2003 * n);
        assertTrue(Address.fromInt(2003 * n - 1).wordAligned().toInt() == 2003 * n);
        assertTrue(Address.fromInt(2003 * n + 1).wordAligned().toInt() == 2003 * n + n);

    }

    public void test_aligned() {
        final int n = Word.size();
        assertTrue(Address.zero().isWordAligned());
        assertFalse(Address.fromInt(1).isWordAligned());
        assertFalse(Address.fromInt(n - (n / 2)).isWordAligned());
        assertFalse(Address.fromInt(n - 1).isWordAligned());
        assertTrue(Address.fromInt(n).isWordAligned());
        assertFalse(Address.fromInt(n + 1).isWordAligned());
        assertFalse(Address.fromInt(n + (n / 2)).isWordAligned());
        assertFalse(Address.fromInt(n + n - 1).isWordAligned());
        assertTrue(Address.fromInt(n + n).isWordAligned());
        assertFalse(Address.fromInt(n + n + 1).isWordAligned());
        assertFalse(Address.fromInt(2003 * n - 1).isWordAligned());
        assertTrue(Address.fromInt(2003 * n).isWordAligned());
        assertFalse(Address.fromInt(2003 * n + 1).isWordAligned());
    }

    public void test_times_Address() {
        assertTrue(address0.times(addressHigh).equals(address0));
        assertTrue(address0.times(addressLow).toInt() == 0);
        assertTrue(address1.times(address1).toInt() == 1);
        assertTrue(addressTiny.times(addressTiny).toLong() == (long) tiny * (long) tiny);
        assertTrue(address1.times(addressHigh).equals(addressHigh));
        assertTrue(address1.times(addressLow).toInt() == addressLow.toInt());
        switch (wordWidth()) {
            case BITS_64:
                assertTrue(addressLow.times(addressLow).toLong() == (long) low * (long) low);
                assertTrue(addressMax.times(addressMax).equals(address1));
                addressHigh.toLong();
                addressHigh.times(addressHigh).toLong();

                addressMax.toLong();
                addressMax.dividedBy(2).toLong();

                assertTrue(addressHigh.times(addressHigh).lessThan(addressMax.dividedBy(2)));
                assertTrue(addressHigh.times(addressHigh).greaterThan(addressHigh));
                break;
            case BITS_32:
                assertTrue(addressLow.times(addressLow).toLong() < (long) low * (long) low);
                break;
            default:
                ProgramError.unknownCase();
                break;
        }
    }

}
