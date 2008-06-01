package de.unisb.cs.st.javaslicer.tracer.classRepresentation;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class ReadMethod {

    private final ArrayList<Instruction> instructions = new ArrayList<Instruction>();
    private final ReadClass readClass;
    private final String name;
    private final String desc;
    private final int instructionNumberStart;
    private int instructionNumberEnd;

    public ReadMethod(final ReadClass readClass, final String name, final String desc, final int instructionNumberStart) {
        this.readClass = readClass;
        this.name = name;
        this.desc = desc;
        this.instructionNumberStart = instructionNumberStart;
    }

    public int addInstruction(final Instruction instruction) {
        this.instructions.add(instruction);
        return this.instructions.size()-1;
    }

    public void ready() {
        this.instructions.trimToSize();
    }

    public ArrayList<Instruction> getInstructions() {
        return this.instructions;
    }

    public ReadClass getReadClass() {
        return this.readClass;
    }

    public String getName() {
        return this.name;
    }

    public String getDesc() {
        return this.desc;
    }

    public int getInstructionNumberStart() {
        return this.instructionNumberStart;
    }

    public int getInstructionNumberEnd() {
        return this.instructionNumberEnd;
    }

    public void setInstructionNumberEnd(final int instructionNumberEnd) {
        this.instructionNumberEnd = instructionNumberEnd;
    }

    public void writeOut(final ObjectOutputStream out) throws IOException {
        out.writeUTF(this.name);
        out.writeUTF(this.desc);
        out.writeInt(this.instructionNumberEnd);
        out.writeInt(this.instructionNumberStart);
        out.writeInt(this.instructions.size());
        for (final Instruction instr: this.instructions)
            instr.writeOut(out);
    }

    public static ReadMethod readFrom(final ObjectInputStream in, final ReadClass readClass) throws IOException {
        final String name = in.readUTF();
        final String desc = in.readUTF();
        final int instructionNumberStart = in.readInt();
        final int instructionNumberEnd = in.readInt();
        final ReadMethod rm = new ReadMethod(readClass, name, desc, instructionNumberStart);
        rm.setInstructionNumberEnd(instructionNumberEnd);
        int numInstr = in.readInt();
        rm.instructions.ensureCapacity(numInstr);
        while (numInstr-- > 0)
            try {
                rm.instructions.add((Instruction)in.readObject());
            } catch (final ClassNotFoundException e) {
                throw new RuntimeException(e);
            }

        return rm;
    }

}
