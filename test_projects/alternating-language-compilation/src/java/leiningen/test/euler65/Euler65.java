/* 
 * SPOILER ALERT!
 * This file implements a solution to Project Euler problem #65,
 * Convergents of e (https://projecteuler.net/problem=65). If you
 * intend to solve this problem on your own, don't pay too much
 * attention to what the code is doing.
 *
 * Also, this is incredibly contrived, but only to test javac
 * functionality. Don't take this as a best practice.
 */

package leiningen.test.euler65;

import leiningen.test.alc.naturalexpander.NaturalExpander;
import java.math.BigInteger;

public class Euler65 {
    public static NaturalExpander expander = new NaturalExpander(null);

    public static void main(String[] args) {
        expander.expandBy(100);
        BigInteger numerator = expander.getNumerator();
        System.out.println(String.format("The 100th expansion of e is %s/%s",
                                         numerator,
                                         expander.getDenominator()));
        int sumOfDigits = 0;
        while (numerator.compareTo(BigInteger.ZERO) == 1) {
            sumOfDigits += numerator.mod(BigInteger.TEN).longValue();
            numerator = numerator.divide(BigInteger.TEN);
        }
        System.out.println("The sum of the digits of the numerator is " +
                           sumOfDigits);
    }
}
