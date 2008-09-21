package de.unisb.cs.st.javaslicer.tracer.traceResult.traceSequences;

import java.io.BufferedInputStream;
import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;

import de.unisb.cs.st.javaslicer.tracer.util.EmptyIterator;
import de.unisb.cs.st.javaslicer.tracer.util.MultiplexedFileReader;
import de.unisb.cs.st.javaslicer.tracer.util.OptimizedDataInputStream;
import de.unisb.cs.st.javaslicer.tracer.util.MultiplexedFileReader.MultiplexInputStream;

public class ConstantSwitchingLongTraceSequence extends ConstantLongTraceSequence {

    protected final MultiplexedFileReader file;
    protected final boolean gzipped;
    protected final int streamIndex;

    public ConstantSwitchingLongTraceSequence(final MultiplexedFileReader file, final boolean gzipped, final int streamIndex) {
        this.file = file;
        this.gzipped = gzipped;
        this.streamIndex = streamIndex;
    }

    @Override
    public Iterator<Long> backwardIterator() {
        try {
            return this.gzipped ? new GZippedBackwardIterator(this.file, this.streamIndex)
                : new NoGzipBackwardIterator(this.file, this.streamIndex);
        } catch (final IOException e) {
            return new EmptyIterator<Long>();
        }
    }

    public static ConstantSwitchingLongTraceSequence readFrom(final DataInput in, final MultiplexedFileReader file)
            throws IOException {
        final boolean gzipped = in.readBoolean();
        final int streamIndex = in.readInt();
        if (!file.getStreamIds().contains(streamIndex))
            throw new IOException("corrupted data");
        return new ConstantSwitchingLongTraceSequence(file, gzipped, streamIndex);
    }

    private static class GZippedBackwardIterator implements Iterator<Long> {

        private final MultiplexInputStream multiplexedStream;
        private final OptimizedDataInputStream dataIn;
        private boolean error;
        private final PushbackInputStream pushBackInput;

        public GZippedBackwardIterator(final MultiplexedFileReader file, final int streamIndex) throws IOException {
            this.multiplexedStream = file.getInputStream(streamIndex);
            final InputStream gzipStream = new BufferedInputStream(new GZIPInputStream(this.multiplexedStream, 512), 512);
            this.pushBackInput = new PushbackInputStream(gzipStream, 1);
            this.dataIn = new OptimizedDataInputStream(this.pushBackInput, true);
        }

        public boolean hasNext() {
            if (this.error)
                return false;
            int read;
            try {
                if ((read = this.pushBackInput.read()) != -1) {
                    this.pushBackInput.unread(read);
                    return true;
                }
                return false;
            } catch (final IOException e) {
                this.error = true;
                return false;
            }
        }

        public Long next() {
            try {
                return this.dataIn.readLong();
            } catch (final IOException e) {
                this.error = true;
                return null;
            }
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

    private static class NoGzipBackwardIterator implements Iterator<Long> {

        private final MultiplexInputStream multiplexedStream;
        private final OptimizedDataInputStream dataIn;
        private boolean error;

        public NoGzipBackwardIterator(final MultiplexedFileReader file, final int streamIndex) throws IOException {
            this.multiplexedStream = file.getInputStream(streamIndex);
            this.dataIn = new OptimizedDataInputStream(this.multiplexedStream, true);
        }

        public boolean hasNext() {
            if (this.error)
                return false;
            try {
                return !this.multiplexedStream.isEOF();
            } catch (final IOException e) {
                this.error = true;
                return false;
            }
        }

        public Long next() {
            try {
                return this.dataIn.readLong();
            } catch (final IOException e) {
                this.error = true;
                return null;
            }
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

}
