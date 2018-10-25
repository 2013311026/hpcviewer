package edu.rice.cs.hpc.viewer.scope;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.graphics.Image;

//import edu.rice.cs.hpc.data.experiment.metric.DerivedMetric;
//import edu.rice.cs.hpc.data.experiment.metric.DerivedMetric;
import edu.rice.cs.hpc.data.experiment.metric.BaseMetric;
import edu.rice.cs.hpc.data.experiment.metric.MetricValue;
import edu.rice.cs.hpc.data.experiment.scope.Scope;
import edu.rice.cs.hpc.data.experiment.scope.LineScope;
import edu.rice.cs.hpc.data.experiment.scope.CallSiteScope;
import edu.rice.cs.hpc.data.util.OSValidator;
import edu.rice.cs.hpc.viewer.util.Utilities;
//======================================================
// ................ SORTING ............................
//======================================================
public class ColumnViewerSorter extends ViewerComparator {
	// direction
	public static final int ASC = 1;
	public static final int NONE = 0;	// unused: for init only
	public static final int DESC = -1;
	private int direction = 0;
	// data
	private TreeColumn column;		// column
	private TreeViewer viewer;	// viewer
	private int iColNumber;			// column position
	private BaseMetric metric;			// data for metric table
	
	/**
	 * Update the metric for this column
	 * @param newMetric
	 */
	public void setMetric(BaseMetric newMetric) {
		this.metric = newMetric;
	}
	
	/**
	 * Class to sort a column
	 * @param viewer: the table tree
	 * @param column: the column
	 * @param newMetric: the metric
	 * @param colNum: the position
	 */
	public ColumnViewerSorter(final TreeViewer viewer, TreeColumn column, BaseMetric newMetric, int colNum) {
		this.column = column;
		this.iColNumber = colNum;
		this.viewer = viewer;
		this.metric = newMetric;

		// catch event when the user sort the column on the column header
		this.column.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Object []elements = null;
				if (OSValidator.isMac()) {
					// --------------------------------------------------------------------
					//Eclipse Indigo bug on Mac OS: expanding a long call path will cause
					// SWT to slowly sort tree items. Somehow Eclipse also expands other
					// collapsed tree items as well.
					// --------------------------------------------------------------------
					// save the current expaded elements to be restored after the sort
					elements = viewer.getExpandedElements();
				}
				
				// collapse all the items
				viewer.collapseAll();
				// before sorting, we need to check if the first row is an element header 
				// something like "aggregate metrics" or zoom-in item
				Tree tree = ColumnViewerSorter.this.viewer.getTree();
				if (tree.getItemCount()==0)
					return; // no items: no need to sort
				
				TreeItem item = ColumnViewerSorter.this.viewer.getTree().getItem(0);
				Image imgItem = item.getImage(0);
				String []sText = Utilities.getTopRowItems(ColumnViewerSorter.this.viewer);
				if( ColumnViewerSorter.this.viewer.getComparator() != null ) {
					if( ColumnViewerSorter.this.viewer.getComparator() == ColumnViewerSorter.this ) {
						int tdirection = ColumnViewerSorter.this.direction;
						
						if( tdirection == ASC ) {
							setSorter(ColumnViewerSorter.this, DESC);
						} else if( tdirection == DESC ) {
							setSorter(ColumnViewerSorter.this, ASC);
						}
					} else {
						setSorter(ColumnViewerSorter.this, ASC);
					}
				} else {
					setSorter(ColumnViewerSorter.this, ASC);
				}
				// post-sorting 
				if(sText != null) {
					Utilities.insertTopRow(ColumnViewerSorter.this.viewer, imgItem, sText);
				}
				if (elements != null) {
					viewer.setExpandedElements(elements);
				}
			}

		}
		);

		//if (colNum == 1) setSorter(this, ASC); // johnmc
	}
	
	/**
	 * Sort the column according to the direction
	 * @param sorter
	 * @param direction
	 */
	public void setSorter(ColumnViewerSorter sorter, int direction) {
		// bug Eclipse no 199811 https://bugs.eclipse.org/bugs/show_bug.cgi?id=199811
		// sorting can be very slow in mac OS
		// we need to manually disable redraw before comparison and the refresh after the comparison 
		this.viewer.getTree().setRedraw(false);
		if( direction == NONE ) {
			column.getParent().setSortColumn(null);
			column.getParent().setSortDirection(SWT.NONE);
			viewer.setComparator(null);
		} else {
			column.getParent().setSortColumn(column);
			sorter.direction = direction;
			
			if( direction == ASC ) {
				column.getParent().setSortDirection(SWT.DOWN);
			} else {
				column.getParent().setSortDirection(SWT.UP);
			}
			
			if( viewer.getComparator() == sorter ) {
				//viewer.refresh(); // Laks: I don't think we need to refresh the UI now
			} else {
				viewer.setComparator(sorter);
				//viewer.refresh(); // was: johnmc. Laks: we don't need to refresh
			}
			
		}
		// bug Eclipse no 199811 https://bugs.eclipse.org/bugs/show_bug.cgi?id=199811
		// sorting can be very slow in mac OS
		// we need to manually disable redraw before comparison and the refresh after the comparison 
		this.viewer.refresh();
		this.viewer.getTree().setRedraw(true);
	}

	/**
	 * general comparison for sorting
	 */
	public int compare(Viewer viewer, Object e1, Object e2) {
		return direction * doCompare(viewer, e1, e2);
	}

	/**
	 * Compare the names of node 1 and node 2.
	 * However, if both scopes are of type CallSiteScope, compare
	 * the line numbers of the nodes
	 * @param node1
	 * @param node2
	 * @return
	 */
	private int doCompare(Scope node1, Scope node2) {
		if (node1 instanceof CallSiteScope && 
			node2 instanceof CallSiteScope) {
			CallSiteScope cs1 = (CallSiteScope) node1;
			CallSiteScope cs2 = (CallSiteScope) node2;
			LineScope ls1 = cs1.getLineScope();
			LineScope ls2 = cs2.getLineScope();
			if (ls1 != null && ls2 != null)
				return ls1.getLineNumber() - ls2.getLineNumber();
		} 
		String text1 = node1.getName();
		String text2 = node2.getName();
		return text1.compareTo(text2);
	}
	// laks: lazy comparison
	/**
	 * This method is to compare one object to another
	 * Please implement this method in the child class if necessary
	 */
	protected int doCompare(Viewer viewer, Object e1, Object e2) {
		if(e1 instanceof Scope && e2 instanceof Scope) {
			Scope node1 = (Scope) e1;
			Scope node2 = (Scope) e2;

			// dirty solution: if the column position is 0 then we sort
			// according to its element name
			// otherwise, sort according to the metric
			if(this.iColNumber==0) {
				return this.doCompare(node1, node2);
			} else {
				MetricValue mv1 = this.metric.getValue(node1); //node1.getMetricValue(this.metric); 
				MetricValue mv2 = this.metric.getValue(node2); // node2.getMetricValue(this.metric); 
				int iRet = MetricValue.compareTo(mv2, mv1);
				if(iRet != 0)
					return iRet;

				// if the two values are equal, look at the text of the tree node
				return this.doCompare(node1, node2);
			}
		}
		return 0;
	}

	

}
