package edu.rice.cs.hpc.traceAnalysis.data.tree;

import java.io.Serializable;

import edu.rice.cs.hpc.traceAnalysis.utils.TraceAnalysisUtils;

public class TraceTree implements Serializable {
	private static final long serialVersionUID = -8921129750765810938L;

	public final RootTrace root;
	
	public final long begTime;
	public final long endTime;
	
	public final long numSamples;
	public final int sampleFrequency;
	
	public TraceTree(RootTrace root, long begTime, long endTime, long numSamples) {
		this.root = root;
		this.begTime = begTime;
		this.endTime = endTime;
		this.numSamples = numSamples;
		this.sampleFrequency = (int)((endTime - begTime) / (numSamples - 1));
	}
	
	public String toString(int maxDepth) {
		return root.toString(maxDepth, sampleFrequency * TraceAnalysisUtils.traceCutoffMultiplier, 0);
	}
	
	public String printLargeDiffNodes(int maxDepth) {
		return root.printLargeDiffNodes(maxDepth, sampleFrequency * TraceAnalysisUtils.traceCutoffMultiplier, null, Long.MIN_VALUE);
	}
}
