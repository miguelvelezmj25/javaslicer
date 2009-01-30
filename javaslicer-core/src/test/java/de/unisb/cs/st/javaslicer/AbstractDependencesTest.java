package de.unisb.cs.st.javaslicer;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;
import de.hammacher.util.Diff;
import de.hammacher.util.DiffPrint;
import de.hammacher.util.Diff.change;
import de.unisb.cs.st.javaslicer.AbstractDependencesTest.Dependence.Type;
import de.unisb.cs.st.javaslicer.common.classRepresentation.Instruction.InstructionInstance;
import de.unisb.cs.st.javaslicer.dependenceAnalysis.DependencesExtractor;
import de.unisb.cs.st.javaslicer.dependenceAnalysis.DependencesVisitorAdapter;
import de.unisb.cs.st.javaslicer.dependenceAnalysis.VisitorCapabilities;
import de.unisb.cs.st.javaslicer.traceResult.ThreadId;
import de.unisb.cs.st.javaslicer.traceResult.TraceResult;
import de.unisb.cs.st.javaslicer.variables.StackEntry;
import de.unisb.cs.st.javaslicer.variables.Variable;


public abstract class AbstractDependencesTest {

    public static class Dependence implements Comparable<Dependence> {
        public static enum Type { RAW, WAR }

        String from;
        String to;
        Type type;
        public Dependence(final String from, final String to, final Type type) {
            super();
            this.from = from;
            this.to = to;
            this.type = type;
        }
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((this.from == null) ? 0 : this.from.hashCode());
            result = prime * result + ((this.to == null) ? 0 : this.to.hashCode());
            result = prime * result + ((this.type == null) ? 0 : this.type.hashCode());
            return result;
        }
        @Override
        public boolean equals(final Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final Dependence other = (Dependence) obj;
            if (!this.from.equals(other.from))
                return false;
            if (!this.to.equals(other.to))
                return false;
            if (!this.type.equals(other.type))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return this.type + " from " + this.from + " to " + this.to;
        }
        public int compareTo(final Dependence o) {
            int cmp = this.from.compareTo(o.from);
            if (cmp == 0)
                cmp = this.to.compareTo(o.to);
            if (cmp == 0)
                cmp = this.type.compareTo(o.type);
            return cmp;
        }

    }


    public static class StringArrDepVisitor extends DependencesVisitorAdapter {

        private final Set<Dependence> dependences;
        private final InstructionFilter instrFilter;

        public StringArrDepVisitor(final InstructionFilter instrFilter, final Set<Dependence> dependences) {
            this.instrFilter = instrFilter;
            this.dependences = dependences;
        }

        @Override
        public void visitDataDependence(final InstructionInstance from, final InstructionInstance to,
                final Variable var, final DataDependenceType type) {
            if (var instanceof StackEntry)
                return;
            if (!this.instrFilter.filterInstance(from) || !this.instrFilter.filterInstance(to))
                return;

            final String fromStr = from.getInstruction().getMethod().getReadClass().getSource()
                + ":" + from.getInstruction().getLineNumber();
            final String toStr = to.getInstruction().getMethod().getReadClass().getSource()
                + ":" + to.getInstruction().getLineNumber();
            final Type depType = type == DataDependenceType.READ_AFTER_WRITE ? Dependence.Type.RAW : Dependence.Type.WAR;
            this.dependences.add(new Dependence(fromStr, toStr, depType));
        }

    }

    protected static interface InstructionFilter {

        boolean filterInstance(InstructionInstance inst);

    }

    protected void compareDependences(final Dependence[] expectedDependences,
            final String traceFilename, final String threadName, final InstructionFilter instrFilter) throws IOException, URISyntaxException {
        final File traceFile = new File(AbstractSlicingTest.class.getResource(traceFilename).toURI());

        final TraceResult res = TraceResult.readFrom(traceFile);

        ThreadId threadId = null;
        for (final ThreadId t: res.getThreads()) {
            if (threadName.equals(t.getThreadName())) {
                    threadId = t;
                    break;
            }
        }

        assertTrue("Thread not found", threadId != null);

        final Set<Dependence> dependences = new HashSet<Dependence>();
        final DependencesExtractor extr = new DependencesExtractor(res);
        extr.registerVisitor(new StringArrDepVisitor(instrFilter, dependences), VisitorCapabilities.DATA_DEPENDENCES_ALL);
        extr.processBackwardTrace(threadId);
        final Dependence[] computetedDependences = dependences.toArray(new Dependence[dependences.size()]);

        Arrays.sort(expectedDependences);
        Arrays.sort(computetedDependences);

        final Diff differ = new Diff(expectedDependences, computetedDependences);
        final change diff = differ.diff_2(false);
        if (diff == null)
            return;

        final StringWriter output = new StringWriter();
        output.append("Slice differs from expected slice:").append(System.getProperty("line.separator"));

        if (expectedDependences.length != computetedDependences.length) {
            output.append("Expected " + expectedDependences.length +
                " entries, got " + computetedDependences.length + "." +
                System.getProperty("line.separator"));
        }

        final DiffPrint.Base diffPrinter = new DiffPrint.Base(expectedDependences, computetedDependences) {
            @Override
            protected void print_hunk(final change hunk) {
                /* Determine range of line numbers involved in each file. */
                analyze_hunk(hunk);
                if (this.deletes == 0 && this.inserts == 0)
                    return;

                /* Print the lines that were expected but did not occur. */
                if (this.deletes != 0)
                    for (int i = this.first0; i <= this.last0; i++) {
                        final Dependence exp = (Dependence) this.file0[i];
                        print_1_line("- ", exp);
                    }

                /* Print the lines that the second file has. */
                if (this.inserts != 0)
                    for (int i = this.first1; i <= this.last1; i++) {
                        final Dependence exp = (Dependence) this.file1[i];
                        print_1_line("+ ", exp);
                    }
            }
        };
        diffPrinter.setOutput(output);
        diffPrinter.print_script(diff);

        Assert.fail(output.toString());

    }

}