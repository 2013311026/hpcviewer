package edu.rice.cs.hpc.viewer.scope;

import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

import edu.rice.cs.hpc.data.experiment.Experiment;
import edu.rice.cs.hpc.data.experiment.metric.BaseMetric;
import edu.rice.cs.hpc.data.experiment.scope.RootScope;
import edu.rice.cs.hpc.data.experiment.scope.RootScopeType;
import edu.rice.cs.hpc.data.experiment.scope.visitors.FilterScopeVisitor;


/**
 * 
 *
 */
abstract public class BaseScopeView  extends AbstractBaseScopeView {
	
    //======================================================
    // ................ ATTRIBUTES..........................
    //======================================================

    //======================================================
    // ................ METHODS  ..........................
    //======================================================
	
    /// ---------------------------------------------
    /// filter feature
    /// ---------------------------------------------
    
    /****
     * enable/disable filter
     * 
     * @param isEnabled
     */
	protected void enableFilter(boolean isEnabled)
    {
    	if (treeViewer.getTree().isDisposed())
    		return;
    	
    	Experiment experiment = getExperiment();
    	if (experiment == null || myRootScope == null)
    		return;
    	
		RootScopeType rootType = myRootScope.getType();
		
		// reassign root scope
		myRootScope = experiment.getRootScope(rootType);
		
		// update the content of the view
		refreshTree(myRootScope);
		
        // ------------------------------------------------------------
    	// check the status of filter. 
        // if the filter may incur misleading information, we should warn users
        // ------------------------------------------------------------
        checkFilterStatus(experiment);
    }
    

	
    //======================================================
    // ................ UPDATE ............................
    //======================================================
    
	/*
	 * (non-Javadoc)
	 * @see edu.rice.cs.hpc.viewer.scope.AbstractBaseScopeView#updateDisplay()
	 */
	public void updateDisplay() 
	{
		// return immediately when there's no database or the view is closed (disposed)
        if (database == null || treeViewer == null || treeViewer.getTree().isDisposed())
        	return;
        
        // ------------------------------------------------------------
        // Tell children to update the content with the new database
        // ------------------------------------------------------------
        final Experiment myExperiment = database.getExperiment();        
        this.updateDatabase(myExperiment);

        // Update root scope
        if (myRootScope != null && myRootScope.getChildCount() > 0) {
            treeViewer.setInput(myRootScope);
            treeViewer.setChildCount(myRootScope, myRootScope.getChildCount());
            
            this.objViewActions.updateContent(getExperiment(), myRootScope);

            // FIXME: For unknown reason, the updateContent method above does not resize the column automatically,
            // so we need to do it here, manually ... sigh
            this.objViewActions.resizeColumns();	// resize the column to fit all metrics
        	
            // Laks 2009.03.17: select the first scope
            TreeItem objItem = this.treeViewer.getTree().getItem(1);
            this.treeViewer.getTree().setSelection(objItem);
            // reset the button
            this.objViewActions.checkNodeButtons();
            
            // ------------------------------------------------------------
        	// check the status of filter. 
            // if the filter may incur misleading information, we should warn users
            // ------------------------------------------------------------
            checkFilterStatus(myExperiment);
        } else {
        	// empty experiment data (it should be a warning instead of an error. The error should be on the profile side).
        	// this.objViewActions.showErrorMessage("Warning: empty database.");
        }
   	}

	/*
	 * (non-Javadoc)
	 * @see edu.rice.cs.hpc.viewer.scope.AbstractBaseScopeView#initTableColumns()
	 */
	protected void initTableColumns(boolean keepColumnStatus) {
		
        if (treeViewer != null) {
        	Tree tree = treeViewer.getTree();
        	if (tree != null && !tree.isDisposed())
        	{
        		initTableColumns(tree, keepColumnStatus);
        	}
        }
	}

	/***
	 * check if the filter incurs omitted scopes or not
	 * 
	 * @param myExperiment : the current experiment
	 */
	private void checkFilterStatus(Experiment myExperiment) 
	{
    	if (myExperiment != null) {
    		int filterStatus = myExperiment.getFilterStatus();
    		switch (filterStatus) {
    			case FilterScopeVisitor.STATUS_FAKE_PROCEDURE:
    				objViewActions.showWarningMessage("Warning: the result of filter may incur incorrect information in Callers View and Flat View.");
    				break;
    			case FilterScopeVisitor.STATUS_OK:
    	    		int filtered = myExperiment.getNumberOfFilteredScopes();
    	    		if (filtered>0) {
    	    			// show the information how many scopes matched with the filer
    	    			// this is important to warn users that filtering may hide some scopes 
    	    			// that can be useful for analysis.
        	    		String msg = "At least there ";
        	    		if (filtered == 1) {
        	    			msg += "is one scope";
        	    		}  else {
        	    			msg += "are " + filtered + " scopes";
        	    		}
    	    			objViewActions.showInfoMessage(msg + " matched with the filter.");
    	    		}
	    			break;
    		}
    	}

	}
	
	/******
	 * The same version as {@link BaseScopeView.initTableColumns} but without
	 * 	worrying if the tree has been disposed or not.
	 * 
	 * @param tree
	 * @param keepColumnStatus
	 */
	private void initTableColumns(Tree tree, boolean keepColumnStatus) 
	{
        final Experiment myExperiment = database.getExperiment();
        final int numMetric			  = myExperiment.getMetricCount();

        int iColCount = tree.getColumnCount();
        boolean status[] = new boolean[numMetric];

        if(iColCount>1) {
        	TreeColumn []columns = tree.getColumns();
        	
        	// this is Eclipse Indigo bug: when a column is disposed, the next column will have
        	//	zero as its width. Somehow they didn't preserve the width of the columns.
        	// Hence, we have to retrieve the information of column width before the dispose action
        	for(int i=1;i<iColCount;i++) {        		
        		// bug fix: for callers view activation, we have to reserve the current status
        		if (keepColumnStatus && i-1<status.length) {
        			int width = columns[i].getWidth();
        			status[i-1] = (width > 0);
        		}
        	}
        	
        	// remove the metric columns blindly
        	// TODO we need to have a more elegant solution here
        	for(int i=1;i<iColCount;i++) {
        		TreeColumn column = columns[i]; //treeViewer.getTree().getColumn(1);
        		column.dispose();
        	}
        }
        // prepare the data for the sorter class for tree
        sorterTreeColumn.setMetric(myExperiment.getMetric(0));

        // dirty solution to update titles
        TreeViewerColumn []colMetrics = new TreeViewerColumn[numMetric];
        {
            // Update metric title labels
            String[] titles = new String[numMetric+1];
            titles[0] = "Scope";	// unused element. Already defined
            // add table column for each metric
        	for (int i=0; i<numMetric; i++)
        	{
        		final BaseMetric metric = myExperiment.getMetric(i);
        		if (metric != null) {
            		titles[i+1] = metric.getDisplayName();	// get the title
            		colMetrics[i] = this.treeViewer.addTreeColumn(metric, (i==0));
            		
            		// bug fix: for view initialization, we need to reset the status of hide/view
            		if (!keepColumnStatus) {
                		status[i] = metric.getDisplayed();
            		}
        		}
        	}
            treeViewer.setColumnProperties(titles); // do we need this ??
        }
        // update the root scope of the actions !
        this.objViewActions.updateContent(myExperiment, this.myRootScope);
    	this.objViewActions.objActionsGUI.setColumnsStatus(status);

	}
    /**
     * Tell children to update the content with the new database
     * @param new_database
     */
    abstract protected void updateDatabase(Experiment new_database);
    
    /***
     * Method to be implemented by the child class.<br/>
     * This method is called when a filter is applied, and the view needs
     * to be refreshed with the new root tree.
     * 
     * @param root : the new root tree
     */
    abstract protected void refreshTree(RootScope root);

}
