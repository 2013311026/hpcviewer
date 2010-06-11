package edu.rice.cs.hpc.viewer.scope;

import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.widgets.TreeItem;

import edu.rice.cs.hpc.data.experiment.scope.RootScope;
import edu.rice.cs.hpc.data.experiment.scope.Scope;

/**
 * 
 * @author laksonoadhianto
 *
 */
abstract public class BaseScopeView  extends AbstractBaseScopeView {

	
    //======================================================
    // ................ ATTRIBUTES..........................
    //======================================================
	
	protected boolean hasThreadsLevelData = false;
	private TreeViewerColumn []colMetrics;

    

    //======================================================
    // ................ UPDATE ............................
    //======================================================
    
	/**
	 * Update the content of the tree view when a new experiment is loaded
	 */
	protected void updateDisplay() {
        if (myExperiment == null)
        	return;
        
        // refresh the content with new database
        this.updateDatabase(myExperiment);
        
        int iColCount = this.treeViewer.getTree().getColumnCount();
        if(iColCount>1) {
        	// remove the metric columns blindly
        	// TODO we need to have a more elegant solution here
        	for(int i=1;i<iColCount;i++) {
        		this.treeViewer.getTree().getColumn(1).dispose();
        	}
        }
        // prepare the data for the sorter class for tree
        sorterTreeColumn.setMetric(myExperiment.getMetric(0));

        // force garbage collector to remove the old data
        this.colMetrics = null;
        int nbMetrics = myExperiment.getMetricCount();
        boolean status[] = new boolean[nbMetrics];
        // dirty solution to update titles
        this.colMetrics = new TreeViewerColumn[nbMetrics];
        {
            // Update metric title labels
            String[] titles = new String[nbMetrics+1];
            titles[0] = "Scope";	// unused element. Already defined
            // add table column for each metric
        	for (int i=0; i<nbMetrics; i++)
        	{
        		titles[i+1] = myExperiment.getMetric(i).getDisplayName();	// get the title
        		colMetrics[i] = this.treeViewer.addTreeColumn(myExperiment.getMetric(i), (i==0));
        		status[i] = myExperiment.getMetric(i).getDisplayed();
        	}
            treeViewer.setColumnProperties(titles); // do we need this ??
        }
        
        // Update root scope
        Scope.Node nodeRootScope = myRootScope.getTreeNode();
        if (nodeRootScope.getChildCount() > 0) {
            treeViewer.setInput(myRootScope.getTreeNode());
            // update the window title
            this.getSite().getShell().setText("hpcviewer: "+myExperiment.getName());
            
            // update the root scope of the actions !
            this.objViewActions.updateContent(this.myExperiment, (RootScope)this.myRootScope, colMetrics);
            // FIXME: For unknown reason, the updateContent method above does not resize the column automatically,
            // so we need to do it here, manually ... sigh
            this.objViewActions.resizeColumns();	// resize the column to fit all metrics
        	this.objViewActions.objActionsGUI.setColumnsStatus(status);
        	
            // Laks 2009.03.17: select the first scope
            TreeItem objItem = this.treeViewer.getTree().getItem(1);
            this.treeViewer.getTree().setSelection(objItem);
            // reset the button
            this.objViewActions.checkNodeButtons();
        } else {
        	// empty experiment data (it should be a warning instead of an error. The error should be on the profile side).
        	this.objViewActions.showErrorMessage("Warning: empty database.");
        }
   	}


}
