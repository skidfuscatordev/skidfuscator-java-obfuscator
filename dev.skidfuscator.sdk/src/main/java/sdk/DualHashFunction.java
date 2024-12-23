package sdk;

// An internal helper class for casting LongTupleHashFunction as LongHashFunction

abstract class DualHashFunction extends LongTupleHashFunction {
    private static final long serialVersionUID = 0L;

    private transient final int resultLength = newResultArray().length;
    private void checkResult(final long[] result) {
        if (null == result) {
            throw new NullPointerException();
        }
        if (result.length < resultLength) {
            throw new IllegalArgumentException("The input result array has not enough space!");
        }
    }

    protected abstract long dualHashLong(long input, long[] result);
    @Override
    public void hashLong(final long input, final long[] result) {
        checkResult(result);
        dualHashLong(input, result);
    }

    protected abstract long dualHashInt(int input, long[] result);
    @Override
    public void hashInt(final int input, final long[] result) {
        checkResult(result);
        dualHashInt(input, result);
    }

    protected abstract long dualHashShort(short input, long[] result);
    @Override
    public void hashShort(final short input, final long[] result) {
        checkResult(result);
        dualHashShort(input, result);
    }

    protected abstract long dualHashChar(char input, long[] result);
    @Override
    public void hashChar(final char input, final long[] result) {
        checkResult(result);
        dualHashChar(input, result);
    }

    protected abstract long dualHashByte(byte input, long[] result);
    @Override
    public void hashByte(final byte input, final long[] result) {
        checkResult(result);
        dualHashByte(input, result);
    }

    protected abstract long dualHashVoid(long[] result);
    @Override
    public void hashVoid(final long[] result) {
        checkResult(result);
        dualHashVoid(result);
    }

    protected abstract <T> long dualHash(T input, Access<T> access, long off, long len, long[] result);
    @Override
    public <T> void hash(final T input, final Access<T> access, final long off, final long len, final long[] result) {
        checkResult(result);
        dualHash(input, access, off, len, result);
    }
    @Override
    public <T> long[] hash(final T input, final Access<T> access, final long off, final long len) {
        final long[] result = newResultArray();
        dualHash(input, access, off, len, result);
        return result;
    }

    // LongTupleHashFunction and LongHashFunction are stateless objects after construction
    // so cache the LongHashFunction instance.
    private transient final LongHashFunction longHashFunction = new LongHashFunction() {
        @Override
        public long hashLong(final long input) {
            return dualHashLong(input, null);
        }

        @Override
        public long hashInt(final int input) {
            return dualHashInt(input, null);
        }

        @Override
        public long hashShort(final short input) {
            return dualHashShort(input, null);
        }

        @Override
        public long hashChar(final char input) {
            return dualHashChar(input, null);
        }

        @Override
        public long hashByte(final byte input) {
            return dualHashByte(input, null);
        }

        @Override
        public long hashVoid() {
            return dualHashVoid(null);
        }

        @Override
        public <T> long hash(final T input, final Access<T> access, final long off, final long len) {
            return dualHash(input, access, off, len, null);
        }
    };

    protected LongHashFunction asLongHashFunction() {
        return longHashFunction;
    }
}
