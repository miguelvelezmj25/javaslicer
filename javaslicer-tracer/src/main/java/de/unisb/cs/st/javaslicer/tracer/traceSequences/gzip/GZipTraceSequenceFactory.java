package de.unisb.cs.st.javaslicer.tracer.traceSequences.gzip;

import java.io.IOException;
import java.io.OutputStream;

import de.unisb.cs.st.javaslicer.common.TraceSequenceTypes;
import de.unisb.cs.st.javaslicer.common.TraceSequenceTypes.Type;
import de.unisb.cs.st.javaslicer.tracer.ThreadTracer;
import de.unisb.cs.st.javaslicer.tracer.Tracer;
import de.unisb.cs.st.javaslicer.tracer.traceSequences.TraceSequence;
import de.unisb.cs.st.javaslicer.tracer.traceSequences.TraceSequenceFactory;

public class GZipTraceSequenceFactory implements TraceSequenceFactory, TraceSequenceFactory.PerThread {

    public TraceSequence createTraceSequence(final Type type, final Tracer tracer) {
        switch (type) {
        case INTEGER:
            return new GZipIntegerTraceSequence(tracer);
        case LONG:
            return new GZipLongTraceSequence(tracer);
        default:
            assert false;
            return null;
        }
    }

    @Override
    public void finish() {
        // nop
    }

    @Override
    public PerThread forThreadTracer(final ThreadTracer tt) {
        return this;
    }

    @Override
    public void writeOut(final OutputStream out) throws IOException {
        out.write(TraceSequenceTypes.FORMAT_GZIP);
    }

    @Override
    public boolean shouldAutoFlushFile() {
        return false;
    }

}