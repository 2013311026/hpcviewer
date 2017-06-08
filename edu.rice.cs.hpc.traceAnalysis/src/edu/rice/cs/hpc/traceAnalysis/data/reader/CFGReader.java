package edu.rice.cs.hpc.traceAnalysis.data.reader;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import com.alexmerz.graphviz.ParseException;
import com.alexmerz.graphviz.Parser;
import com.alexmerz.graphviz.objects.Edge;
import com.alexmerz.graphviz.objects.Graph;
import com.alexmerz.graphviz.objects.Node;

import edu.rice.cs.hpc.traceAnalysis.data.cfg.CFGCall;
import edu.rice.cs.hpc.traceAnalysis.data.cfg.CFGFunc;
import edu.rice.cs.hpc.traceAnalysis.data.cfg.CFGLoop;
import edu.rice.cs.hpc.traceAnalysis.data.cfg.CFGNode;

public class CFGReader {
	final HashMap<Long, CFGFunc> CFGFuncMap = new HashMap<Long, CFGFunc>();
	final HashMap<Long, CFGLoop> CFGLoopMap = new HashMap<Long, CFGLoop>();
	public final String filename;
	
	public CFGReader(String filename) {
		this.filename = filename;
	}
	
	public String toString() {
		String s = "";
		for (CFGFunc f : CFGFuncMap.values())
			s += f + "\n";
		return s;
	}
	
	public boolean read(PrintStream objError) {
		FileReader r = null;
		Parser p = new Parser();
		
		try {
			r = new FileReader(filename);
			p.parse(r);
		} catch (FileNotFoundException e) {
			objError.println("CFG graphviz file open error!");
			return false;
		} catch (ParseException e) {
			objError.println("CFG graphviz file parse error!");
			return false;
		}
		
		Graph root = p.getGraphs().get(0);
		for (Graph g : root.getSubgraphs())
			processGraph(g);
		
		return true;
	}
	
	private void processGraph(Graph g) {
		String gid = g.getId().getId();
		String label = g.getAttribute("label");

		// handle subgraphs
		for (Graph sub : g.getSubgraphs())
			processGraph(sub);
		
		ArrayList<Edge> edgeList = g.getEdges();
		if (edgeList.size() == 0) {
			String[] split = gid.split("_");
			long addr = Long.decode(split[2]);
			if (split[1].equals("func"))
				CFGFuncMap.put(addr, new CFGFunc(addr, label, new CFGNode[0], null));
			else
				CFGLoopMap.put(addr, new CFGLoop(addr, label, new CFGNode[0], null));
			return;
		}
		
		// Get a list of nodes and accumulate indegree for each node.
		// g.getNodes(false) seems not to work as it returns nodes from subgraphs.
		// As a result, we get a list of nodes by enumerating edges.
		
		HashMap<Node, Integer> nodeIndegree = new HashMap<Node, Integer>();
		HashMap<Node, Integer> nodeDist = new HashMap<Node, Integer>();
		
		for (Edge e : edgeList) {
			Node n = e.getSource().getNode();
			if (!nodeIndegree.containsKey(n)) {
				nodeIndegree.put(n, 0);
				nodeDist.put(n, Integer.MAX_VALUE);
			}
			
			n = e.getTarget().getNode();
			if (!nodeIndegree.containsKey(n)) {
				nodeIndegree.put(n, 0);
				nodeDist.put(n, Integer.MAX_VALUE);
			}
			
			nodeIndegree.put(n, nodeIndegree.get(n)+1);
		}
		
		// remove the "begin" node
		Node beginNode = null;
		nodeIndegree.entrySet();
		
		// find the "begin" node
		for (Entry<Node, Integer> e : nodeIndegree.entrySet())
			if (e.getValue() == 0) {
				beginNode = e.getKey();
				break;
			}
		// update other nodes
		for (Edge e : edgeList) 
			if (e.getSource().getNode() == beginNode) {
				Node n = e.getTarget().getNode();
				nodeIndegree.put(n, nodeIndegree.get(n)-1);
				nodeDist.put(n, 1);
			}
		
		nodeIndegree.remove(beginNode);
		nodeDist.remove(beginNode);
		
		ArrayList<Node> nodeList = new ArrayList<Node>();
		
		while (nodeIndegree.size() > 0) {
			Node nextNode = null;
			for (Entry<Node, Integer> e : nodeIndegree.entrySet())
				if (e.getValue() == 0) {
					nextNode = e.getKey();
					break;
				}
			if (nextNode == null) {
				int minDist = Integer.MAX_VALUE;
				for (Entry<Node, Integer> e : nodeDist.entrySet())
					if (e.getValue() < minDist) {
						minDist = e.getValue();
						nextNode = e.getKey();
					}
			}

			for (Edge e : edgeList) 
				if (e.getSource().getNode() == nextNode) {
					Node n = e.getTarget().getNode();
					if (nodeIndegree.containsKey(n)) {
						nodeIndegree.put(n, nodeIndegree.get(n)-1);
						nodeDist.put(n, Math.min(nodeDist.get(n), nodeDist.get(nextNode)+1));
					}
				}
			
			nodeList.add(nextNode);
			nodeIndegree.remove(nextNode);
			nodeDist.remove(nextNode);
		}
		
		CFGNode nodes[] = new CFGNode[nodeList.size()];
		for (int i = 0; i < nodeList.size(); i++) {
			String[] split = nodeList.get(i).getId().getId().split("_");
			assert(split[1].subSequence(0, 2).equals("0x"));
			long addr = Long.decode(split[1]);
			if (split[0].equals("call"))
				nodes[i] = new CFGCall(addr);
			else
				nodes[i] = CFGLoopMap.get(addr);
		}
		
		String[] split = gid.split("_");
		assert(split[2].subSequence(0, 2).equals("0x"));
		long addr = Long.decode(split[2]);
		if (split[1].equals("func"))
			CFGFuncMap.put(addr, new CFGFunc(addr, label, nodes, null));
		else
			CFGLoopMap.put(addr, new CFGLoop(addr, label, nodes, null));
	}
}
