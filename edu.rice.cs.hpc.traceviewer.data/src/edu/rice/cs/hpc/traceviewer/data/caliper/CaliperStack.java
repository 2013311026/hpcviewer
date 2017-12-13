package edu.rice.cs.hpc.traceviewer.data.caliper;

import java.util.Vector;

import edu.rice.cs.hpc.traceviewer.data.abstraction.AbstractStack;
import edu.rice.cs.hpc.traceviewer.data.caliper.stackframe.CaliperStackFrame;

/**
 * Represents the caliper stack.
 * 
 * @log
 * - 2016.7 (by Lai Wei) Class created.
 */
public class CaliperStack extends AbstractStack {
	private final Vector<CaliperStackFrame> frames;
	
	/**
	 * Initialize a caliper stack
	 * @param frames frames ordered from stack bottom (the closed side) to stack top (the open side).
	 */
	public CaliperStack(Vector<CaliperStackFrame> frames) {
		super(frames.size());
		this.frames = frames;
	}

	private CaliperStackFrame getFrameAt(int depth) {
		if (depth < 0) return null;
		if (depth >= frames.size() - 1) depth = frames.size() - 1;
		return frames.get(depth);
	}
	
	@Override
	public String getColorNameAt(int depth) {
		return getFrameAt(depth).getColorName();
	}

	@Override
	public Vector<String> getDisplayNames() {
		Vector<String> names = new Vector<String>(frames.size());
		for (int i = 0; i < frames.size(); i++) 
			names.add(frames.get(i).getDisplayName());
		return names;
	}
	
	public String toString() {
		String ret = "";
		for (int i = 0; i < frames.size(); i++)
			ret += frames.get(i).getDisplayName() + " || ";
		return ret;
	}

	@Override
	public boolean isSameInstanceAtDepth(AbstractStack other, int depth) {
		if (other instanceof CaliperStack) {
			CaliperStack otherStack = (CaliperStack)other;
			return this.getFrameAt(depth).getDisplayName().equals(
					otherStack.getFrameAt(depth).getDisplayName());
		}
		return false;
	}
}