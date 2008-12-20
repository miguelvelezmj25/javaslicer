package de.unisb.cs.st.javaslicer.traceResult.traceSequences;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import de.hammacher.util.MultiplexedFileReader;
import de.hammacher.util.MultiplexedFileReader.MultiplexInputStream;
import de.unisb.cs.st.javaslicer.traceResult.traceSequences.ConstantTraceSequence.ConstantLongTraceSequence;

public class ConstantUncompressedLongTraceSequence implements ConstantLongTraceSequence {

    protected final MultiplexedFileReader file;
    protected final int streamIndex;

    public ConstantUncompressedLongTraceSequence(final MultiplexedFileReader file, final int streamIndex) {
        this.file = file;
        this.streamIndex = streamIndex;
    }

    public Iterator<Long> backwardIterator() throws IOException {
        return new BackwardIterator(this.file, this.streamIndex, 4*1024);
    }

    public ListIterator<Long> iterator() throws IOException {
        return new ForwardIterator(this.file, this.streamIndex);
    }

    public static ConstantUncompressedLongTraceSequence readFrom(final DataInput in, final MultiplexedFileReader file)
            throws IOException {
        final int streamIndex = in.readInt();
        if (!file.getStreamIds().contains(streamIndex))
            throw new IOException("corrupted data");
        return new ConstantUncompressedLongTraceSequence(file, streamIndex);
    }

    private static class BackwardIterator implements Iterator<Long> {

        private long offset;
        private final long[] buf;
        private int bufPos;
        private final MultiplexInputStream inputStream;
        private final DataInputStream dataIn;

        public BackwardIterator(final MultiplexedFileReader file, final int streamIndex, final int bufSize) throws IOException {
            this.inputStream = file.getInputStream(streamIndex);
            final long numLongs = this.inputStream.getDataLength()/8;
            if (numLongs * 8 != this.inputStream.getDataLength())
                throw new IOException("Stream's length not dividable by 8");

            long startLong = (numLongs - 1) / bufSize * bufSize;
            this.offset = startLong * 8;
            this.inputStream.seek(this.offset);
            this.dataIn = new DataInputStream(this.inputStream);
            this.buf = new long[startLong == 0 ? (int)numLongs : bufSize];
            this.bufPos = (int) (numLongs - startLong - 1);
            for (int i = 0; startLong < numLongs; ++startLong) {
                this.buf[i++] = this.dataIn.readLong();
            }
        }

        public boolean hasNext() {
            try {
                if (this.bufPos >= 0)
                    return true;
                if (this.offset == 0)
                    return false;
                this.offset -= (long)this.buf.length*8;
                this.inputStream.seek(this.offset);
                for (int i = 0; i < this.buf.length; ++i) {
                    this.buf[i] = this.dataIn.readLong();
                }
                this.bufPos = this.buf.length - 1;
                return true;
            } catch (final IOException e) {
                close();
                return false;
            }
        }

        public Long next() {
            if (!hasNext())
                throw new NoSuchElementException();
            return this.buf[this.bufPos--];
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        public void close() {
            this.bufPos = -1;
            this.offset = 0;
            this.inputStream.close();
        }
    }

    private static class ForwardIterator implements ListIterator<Long> {

        private final MultiplexInputStream inputStream;
        private final DataInputStream dataIn;

        public ForwardIterator(final MultiplexedFileReader file, final int streamIndex) throws IOException {
            this.inputStream = file.getInputStream(streamIndex);
            this.dataIn = new DataInputStream(this.inputStream);
        }

        public boolean hasNext() {
            try {
                return !this.inputStream.isEOF();
            } catch (final IOException e) {
                return false;
            }
        }

        public Long next() {
            if (!hasNext())
                throw new NoSuchElementException();
            try {
                return this.dataIn.readLong();
            } catch (final IOException e) {
                throw new NoSuchElementException(e.toString());
            }
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        public void add(final Long e) {
            throw new UnsupportedOperationException();
        }

        public boolean hasPrevious() {
            return this.inputStream.getPosition() != 0;
        }

        public int nextIndex() {
            return (int) Math.min(Integer.MAX_VALUE, this.inputStream.getPosition() / 8);
        }

        public Long previous() {
            if (!hasPrevious())
                throw new NoSuchElementException();
            try {
                final long seekPos = this.inputStream.getPosition()-8;
                this.inputStream.seek(seekPos);
                final long ret = this.dataIn.readLong();
                this.inputStream.seek(seekPos);
                return ret;
            } catch (final IOException e) {
                throw new NoSuchElementException(e.toString());
            }
        }

        public int previousIndex() {
            return (int) Math.min(Integer.MAX_VALUE, (this.inputStream.getPosition() / 8)-1);
        }

        public void set(final Long e) {
            throw new UnsupportedOperationException();
        }
    }

}