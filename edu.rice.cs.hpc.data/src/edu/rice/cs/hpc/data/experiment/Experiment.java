//////////////////////////////////////////////////////////////////////////
//																		//
//	Experiment.java														//
//																		//
//	experiment.Experiment -- an open HPCView experiment					//
//	Last edited: January 15, 2002 at 12:37 am							//
//																		//
//	(c) Copyright 2002 Rice University. All rights reserved.			//
//																		//
//////////////////////////////////////////////////////////////////////////




package edu.rice.cs.hpc.data.experiment;


import edu.rice.cs.hpc.data.experiment.metric.*;
import edu.rice.cs.hpc.data.experiment.scope.*;
import edu.rice.cs.hpc.data.experiment.scope.filters.*;
import edu.rice.cs.hpc.data.experiment.scope.visitors.*;
import edu.rice.cs.hpc.data.filter.IFilterData;
import edu.rice.cs.hpc.data.util.IUserData;

import java.io.File;

//////////////////////////////////////////////////////////////////////////
//	CLASS EXPERIMENT													//
//////////////////////////////////////////////////////////////////////////

/**
 *
 * An HPCView experiment and its data.
 *
 */


public class Experiment extends BaseExperimentWithMetrics
{
	static public enum ExperimentOpenFlag {TREE_CCT_ONLY, TREE_ALL};
	
	// thread level database
	private MetricRaw[] metrics_raw;
	private boolean mergedDatabase = false;
	private ExperimentOpenFlag flag;

	//////////////////////////////////////////////////////////////////////////
	//	PERSISTENCE															//
	//////////////////////////////////////////////////////////////////////////


	//////////////////////////////////////////////////////////////////////////
	// Case for merged database
	//////////////////////////////////////////////////////////////////////////
	
	public void setMergedDatabase(boolean flag)
	{
		mergedDatabase = flag;
	}
	
	public boolean isMergedDatabase()
	{
		return mergedDatabase;
	}
	
	//////////////////////////////////////////////////////////////////////////
	// File opening															//
	//////////////////////////////////////////////////////////////////////////

	/*
	 * (non-Javadoc)
	 * @see edu.rice.cs.hpc.data.experiment.BaseExperiment#open(java.io.File, edu.rice.cs.hpc.data.util.IUserData, boolean)
	 */
	public void open(File fileExperiment, IUserData<String, String> userData, ExperimentOpenFlag flag)
			throws	Exception
	{
		this.flag = flag;
		super.open(fileExperiment, userData, true);
	}

	//////////////////////////////////////////////////////////////////////////
	// Postprocessing														//
	//////////////////////////////////////////////////////////////////////////
	protected void accumulateMetricsFromKids(Scope target, Scope source, MetricValuePropagationFilter filter) {
		int nkids = source.getSubscopeCount();
		for (int i = 0; i < nkids; i++) {
			Scope child = source.getSubscope(i);
			if (child instanceof LoopScope) {
				accumulateMetricsFromKids(target, child, filter);
			}
			target.accumulateMetrics(child, filter, this.getMetricCount());
		}
	}

	protected void copyMetric(Scope target, Scope source, int src_i, int targ_i, MetricValuePropagationFilter filter) {
		if (filter.doPropagation(source, target, src_i, targ_i)) {
			MetricValue mv = source.getMetricValue(src_i);
			if (mv != MetricValue.NONE && Float.compare(MetricValue.getValue(mv), 0.0f)!=0) {
				target.setMetricValue(targ_i, mv);
			}
		}
	}

	/*************************************************************************
	 *	Adds a new scope subtree to the scope tree (& scope list)
	 ************************************************************************/
	public void beginScope(Scope scope)
	{
		Scope top = this.getRootScope();
		top.addSubscope(scope);
		scope.setParentScope(top);
	}

	/***
	 * Preparing the tree for caller view. Since we will create the tree dynamically,
	 * 	we need to create at least the root. All the children will be created by
	 * 	createCallersView() method.
	 * 
	 * @param callingContextViewRootScope
	 * @return
	 */
	protected RootScope prepareCallersView(Scope callingContextViewRootScope)
	{
		RootScope callersViewRootScope = new RootScope(this, "Callers View", RootScopeType.CallerTree);
		beginScope(callersViewRootScope);
		return callersViewRootScope;
	}
	
	/***
	 * create callers view
	 * @param callingContextViewRootScope
	 * @return
	 */
	public RootScope createCallersView(Scope callingContextViewRootScope, RootScope callersViewRootScope)
	{
		EmptyMetricValuePropagationFilter filter = new EmptyMetricValuePropagationFilter();

		CallersViewScopeVisitor csv = new CallersViewScopeVisitor(this, callersViewRootScope, 
					getMetricCount(), false, filter);
		callingContextViewRootScope.dfsVisitScopeTree(csv);

		// bug fix 2008.10.21 : we don't need to recompute the aggregate metrics here. Just copy it from the CCT
		//	This will solve the problem where there is only nested loops in the programs
		callersViewRootScope.accumulateMetrics(callingContextViewRootScope, filter, this.getMetricCount());
		
		return callersViewRootScope;
	}

	/******
	 * Preparing the tree of the flat view. This method will create just the root.
	 * <br/>The caller is responsible to call {@link createFlatView} method to generate
	 * the real tree.
	 * 
	 * @param cctRootScope : CCT root
	 * @return the root of flat tree
	 */
	private RootScope prepareFlatView(Scope cctRootScope) 
	{
		RootScope flatRootScope = new RootScope(this, "Flat View", RootScopeType.Flat);
		beginScope(flatRootScope);
		return flatRootScope;
	}
	
	/***
	 * Create a flat tree
	 * 
	 * @param callingContextViewRootScope : the original CCT 
	 * @return the root scope of flat tree
	 */
	public RootScope createFlatView(Scope callingContextViewRootScope, RootScope flatViewRootScope)
	{
		FlatViewScopeVisitor fv = new FlatViewScopeVisitor(this, (RootScope) flatViewRootScope);

		callingContextViewRootScope.dfsVisitScopeTree(fv);

		EmptyMetricValuePropagationFilter filter = new EmptyMetricValuePropagationFilter();
		flatViewRootScope.accumulateMetrics(callingContextViewRootScope, filter	, getMetricCount());
		return flatViewRootScope;
	}



	private void addInclusiveMetrics(Scope scope, MetricValuePropagationFilter filter)
	{
		InclusiveMetricsScopeVisitor isv = new InclusiveMetricsScopeVisitor(this, filter);
		scope.dfsVisitScopeTree(isv);
	}

	private void computeExclusiveMetrics(Scope scope) {
		ExclusiveCallingContextVisitor visitor = new ExclusiveCallingContextVisitor(this);
		scope.dfsVisitScopeTree(visitor);
	}

	protected void copyMetricsToPartner(Scope scope, MetricType sourceType, MetricValuePropagationFilter filter) {
		for (int i = 0; i< this.getMetricCount(); i++) {
			BaseMetric metric = this.getMetric(i);
			// Laksono 2009.12.11: aggregate metric doesn't have partner
			if (metric instanceof Metric) {
				if (metric.getMetricType() == sourceType) {
					// get the partner index (if the metric exclusive, its partner is inclusive)
					int partner = metric.getPartner(); 
					copyMetric(scope, scope, i, partner, filter);
				}
			} else if (metric instanceof AggregateMetric) {
				if (metric.getMetricType() == MetricType.EXCLUSIVE ) {
					int partner = ((AggregateMetric)metric).getPartner();
					String partner_id = String.valueOf(partner);
					BaseMetric partner_metric = this.getMetric( partner_id );
					// case for old database: no partner information
					if (partner_metric != null) {
						MetricValue partner_value = scope.getMetricValue( partner_metric );
						scope.setMetricValue( i, partner_value);
					}
				}
			}
		}
	}

	protected void addPercents(Scope scope, RootScope totalScope)
	{	
		PercentScopeVisitor psv = new PercentScopeVisitor(this.getMetricCount(), totalScope);
		scope.dfsVisitScopeTree(psv);
	}



	/*****
	 * return a tree root
	 * @return
	 */
	public RootScope getCallerTreeRoot() {

		for (Object node: getRootScope().getChildren()) {
			Scope scope = (Scope) node;
			if ( (scope instanceof RootScope) && 
					((RootScope)scope).getType()==RootScopeType.CallerTree )
				return (RootScope) scope;
		}

		return null;
	}

	/**
	 * Post-processing for CCT:
	 * <p>
	 * <ol><li>Step 1: normalizing CCT view
	 *  <ul><li> normalize line scope, which means to add the cost of line scope into call site scope
	 *  	<li> compute inclusive metrics for I
	 *  	<li> compute inclusive metrics for X
	 *  </li>
	 *  </ul>
	 *  <li>Step 2: create call view (if enabled) and flat view
	 *  <li>Step 3: finalize metrics for cct and flat view (callers view metric will be finalized dynamically)
	 * </ol></p>
	 * @param callerView : flag whether to compute caller view (if true) or not.
	 */
	private void postprocess(boolean callerView) {
		if (this.rootScope.getSubscopeCount() <= 0) return;
		// Get first scope subtree: CCT or Flat
		Scope firstSubTree = this.rootScope.getSubscope(0);
		if (!(firstSubTree instanceof RootScope)) return;
		RootScopeType firstRootType = ((RootScope)firstSubTree).getType();

		if (firstRootType.equals(RootScopeType.CallingContextTree)) {
			// accumulate, create views, percents, etc
			Scope callingContextViewRootScope = firstSubTree;

			//----------------------------------------------------------------------------------------------
			// Inclusive metrics
			//----------------------------------------------------------------------------------------------
			if (inclusiveNeeded()) {
				// TODO: if the metric is a derived metric then DO NOT do this process !
				InclusiveOnlyMetricPropagationFilter rootInclProp = new InclusiveOnlyMetricPropagationFilter(this);
				addInclusiveMetrics(callingContextViewRootScope, rootInclProp);
				computeExclusiveMetrics(callingContextViewRootScope);
			}

			EmptyMetricValuePropagationFilter emptyFilter = new EmptyMetricValuePropagationFilter();
			copyMetricsToPartner(callingContextViewRootScope, MetricType.INCLUSIVE, emptyFilter);

			//----------------------------------------------------------------------------------------------
			// Callers View
			//----------------------------------------------------------------------------------------------
			// since we create the caller tree lazily, there is no harm to create the root in the beginning
			prepareCallersView(callingContextViewRootScope);

			//----------------------------------------------------------------------------------------------
			// Flat View
			//----------------------------------------------------------------------------------------------
			// While creating the root of flat tree, we attribute the cost for procedure scopes
			// One the tree has been created, we compute the inclusive cost for other scopes
			prepareFlatView(callingContextViewRootScope);

		} else if (firstRootType.equals(RootScopeType.Flat)) {
			addPercents(firstSubTree, (RootScope) firstSubTree);
		} else {
			// ignore; do no nothing
		}
	}


	/**
	 * Check if an inclusive computation is needed or not
	 * <br/>we need to compute inclusive metrics if the metric is a raw metric (or its kinds)
	 * @return true if inclusive computation is needed
	 */
	private boolean inclusiveNeeded() {
		boolean isNeeded = false;
		for (int i=0; !isNeeded && i<this.getMetricCount(); i++) {
			BaseMetric m = getMetric(i);
			isNeeded = !( (m instanceof FinalMetric) || (m instanceof AggregateMetric) );
		}
		return isNeeded;
	}

	public Experiment duplicate() {

		Experiment copy 	= new Experiment();
		copy.configuration 	= configuration;
		copy.databaseRepresentation =  databaseRepresentation.duplicate();
		
		return copy;
	}


	public void setXMLExperimentFile(File file) {
		databaseRepresentation.setFile(file);
	}

	public void setMetricRaw(MetricRaw []metrics) {
		this.metrics_raw = metrics;
	}


	public BaseMetric[] getMetricRaw() {
		return this.metrics_raw;
	}



	@Override
	protected void filter_finalize(RootScope rootCCT, IFilterData filter) 
	{
		//------------------------------------------------------------------------------------------
		// removing the original root for caller tree and flat tree
		//------------------------------------------------------------------------------------------
		Scope root = getRootScope();
		int index = 0;
		while (root.getChildCount() > index)
		{
			RootScopeType type = ((RootScope)root.getChildAt(index)).getType();
			if (type != RootScopeType.CallingContextTree)
				root.remove(index);
			else
				index++;
		}
		
		//------------------------------------------------------------------------------------------
		// filtering callers tree (bottom-up):
		// check if the callers tree has been created or not. If it's been created,
		// we need to remove it and create a new filter.
		// otherwise, we do nothing, and let the viewer to create dynamically
		//------------------------------------------------------------------------------------------
		prepareCallersView(rootCCT);
		
		//------------------------------------------------------------------------------------------
		// creating the flat tree from the filtered cct tree
		//------------------------------------------------------------------------------------------
		// While creating the flat tree, we attribute the cost for procedure scopes
		// One the tree has been created, we compute the inclusive cost for other scopes
		prepareFlatView(rootCCT);
	}

	@Override
	protected void open_finalize() {
		postprocess(flag == ExperimentOpenFlag.TREE_ALL);		
	}
}
