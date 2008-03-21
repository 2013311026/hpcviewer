package edu.rice.cs.hpc.data.experiment.scope;

/*public enum ScopeVisitType {
	PreVisit, PostVisit
}*/

/*public class ScopeVisitType {
	
	public final static int PreVisit = 0;
	public final static int PostVisit = 1;
	
	public int value;
	
	public ScopeVisitType(int v) { value = v; };
	public boolean isPreVisit() { return value == PreVisit; }
	public boolean isPostVisit() { return value == PostVisit; }
}*/

public class RootScopeType {
	public final static RootScopeType Flat = new RootScopeType("Flat");
	public final static RootScopeType CallTree = new RootScopeType("CallTree");
	public final static RootScopeType Invisible = new RootScopeType("Invisible");
	public String toString() { return value; }
	
	private String value;
	private RootScopeType(String value) { this.value = value; };
}