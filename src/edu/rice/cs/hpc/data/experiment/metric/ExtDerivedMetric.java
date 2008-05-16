/**
 * 
 */
package edu.rice.cs.hpc.data.experiment.metric;

import edu.rice.cs.hpc.data.experiment.Experiment;
import edu.rice.cs.hpc.data.experiment.scope.*;
import edu.rice.cs.hpc.viewer.metric.MetricVarMap;

//math expression
import com.graphbuilder.math.*;
import com.graphbuilder.math.func.*;

/**
 * @author la5
 *
 */
public class ExtDerivedMetric extends Metric {
	static public int Counter = 0;
	private Expression expression;
	private double dRootValue = 0.0;
	private FuncMap fctMap;
	private MetricVarMap varMap;

	/**
	 * @param experiment
	 * @param shortName
	 * @param nativeName
	 * @param displayName
	 * @param displayed
	 * @param percent
	 * @param sampleperiod
	 * @param metricType
	 * @param partnerIndex
	 */
	public ExtDerivedMetric(Experiment experiment, String shortName,
			String nativeName, String displayName, boolean displayed,
			boolean percent, String sampleperiod, MetricType metricType,
			int partnerIndex) {
		super(experiment, shortName, nativeName, displayName, displayed,
				percent, sampleperiod, metricType, partnerIndex);
		// TODO Auto-generated constructor stub
	}

	public ExtDerivedMetric(RootScope scopeRoot, Expression e, String sName, boolean bPercent) {
		super(scopeRoot.getExperiment(), "EDM"+ExtDerivedMetric.Counter, "ExtDerivedMetric"+ExtDerivedMetric.Counter,
				sName, true, bPercent, ".",MetricType.DERIVED, 0);
		ExtDerivedMetric.Counter++;
		this.expression = e;
		this.fctMap = new FuncMap();
		this.fctMap.loadDefaultFunctions(); // initialize it with the default functions
		this.varMap = new MetricVarMap(scopeRoot.getExperiment().getMetrics());
		// compute the aggregate value if necessary
		if(bPercent)
			this.dRootValue = this.getDoubleValue(scopeRoot.getTreeNode().getScope());
	}

	/**
	 * Compute the value of the scope 
	 * @param scope
	 * @return
	 */
	public double getDoubleValue(Scope scope) {
		this.varMap.setScope(scope);
		return this.expression.eval(this.varMap, this.fctMap);
	}
	
	/**
	 * Retrieve the text value of the scope
	 * @param scope
	 * @return
	 */
	public String getTextValue(Scope scope) {
		MetricValue mv;
		double dVal = this.getDoubleValue(scope);
		if(this.percent && this.dRootValue != 0.0) {
			mv = new MetricValue(dVal, (double)dVal/this.dRootValue);
		} else {
			mv = new MetricValue(dVal);
		}
		return this.getDisplayFormat().format(mv);
	}
}

