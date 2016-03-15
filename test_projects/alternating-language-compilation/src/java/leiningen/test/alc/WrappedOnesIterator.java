package leiningen.test.alc;

import java.util.Iterator;

/**
 * Does what it says on the tin: Takes an iterator of Long,
 * and wraps it with ones, i.e. there is a one before and
 * after every element returned by the wrapped iterator.
 */
public class WrappedOnesIterator implements Iterator<Long> {
    private Iterator<Long> origIterator;
    private int modThreeCounter = 0;

    /**
     * @param orig Original iterator to wrap
     */
    public WrappedOnesIterator(Iterator<Long> orig) {
        origIterator = orig;
    }

    public boolean hasNext() {
        return modThreeCounter == 2 || origIterator.hasNext();
    }

    public Long next() {
        Long ret = 0L;
        if (modThreeCounter == 1) {
            ret = origIterator.next();
        } else {
            ret = 1L;
        }
        modThreeCounter += 1;
        modThreeCounter %= 3;
        return ret;
    }

    /**
     * Unsupported on this Iterator.
     * @throws UnsupportedOperationException
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
