package edu.rice.cs.hpc.traceAnalysis.data.tree;

public class IteratedLoopTrace extends AbstractTraceNode {
	private static final long serialVersionUID = 5359891394567113682L;
	
	protected final RawLoopTrace rawLoop;
	
	public IteratedLoopTrace(RawLoopTrace rawLoop) {
		super(rawLoop.ID, rawLoop.name, rawLoop.depth, rawLoop.cfgGraph, rawLoop.addrNode);
		this.traceTime = rawLoop.traceTime;
		this.rawLoop = (RawLoopTrace)rawLoop.duplicate();
	}
	
	protected IteratedLoopTrace(IteratedLoopTrace other) {
		super(other);
		this.rawLoop = other.rawLoop;
	}
	
	public void setDepth(int depth) {
		super.setDepth(depth);
		rawLoop.setDepth(depth);
	}
	
	public AbstractTreeNode duplicate() {
		return new IteratedLoopTrace(this);
	}
	
	public AbstractTreeNode voidDuplicate() {
		return new IteratedLoopTrace((RawLoopTrace)this.rawLoop.voidDuplicate());
	}
	
	public String toString(int maxDepth, long durationCutoff, int weight) {
		return "        L" + super.toString(maxDepth+1, durationCutoff, weight);
	}
}