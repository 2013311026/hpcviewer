/**
 * 
 */
package edu.rice.cs.hpc.data.experiment.metric;

import edu.rice.cs.hpc.data.experiment.metric.format.IMetricValueFormat;
import edu.rice.cs.hpc.data.experiment.metric.format.MetricValueFormatFactory;
import edu.rice.cs.hpc.data.experiment.metric.format.MetricValuePredefinedFormat;
import edu.rice.cs.hpc.data.experiment.scope.IMetricScope;
import edu.rice.cs.hpc.data.experiment.scope.Scope;

/********************************************************************
 * Basic class for metric description
 *
 ********************************************************************/
public abstract class BaseMetric {

	//-------------------------------------------------------------------------------
	// CONSTANTS
	//-------------------------------------------------------------------------------

	static final public int PARTNER_UNKNOWN = -1;

	final private String EMPTY_SUFFIX = "   ";

	//-------------------------------------------------------------------------------
	// DATA STRUCTURE
	//-------------------------------------------------------------------------------

	/** Valid types of Annotations to be used with metric values */
	static public enum AnnotationType { NONE, PERCENT, PROCESS };
	
	//-------------------------------------------------------------------------------
	// DATA
	//-------------------------------------------------------------------------------
 
	/** The short name of this metric, used within an experiment's XML file. 
	 *  We shouldn't change the short name as it's assigned by hpcprof to compute
	 *  derived incremental metric */
	protected String shortName;

	/** The native (target OS toolset) name of this metric. */
	protected String nativeName;

	/** The user-visible name of this metric. */
	protected String displayName;

	/** Whether this metric should be displayed. */
	protected boolean displayed;

	/** The type of annotation that should be displayed with this metric (percent or process number). */
	protected AnnotationType annotationType = AnnotationType.NONE;

	/** The index of this metric in its experiment's metric list. */
	protected int index;
	// partner of the metric. If the metric is exclusive, then its partner is the inclusive one
	protected int partner_index;

	/** The display format to be used for this metric. */
	protected IMetricValueFormat displayFormat;

	protected MetricType metricType;

	protected double  sampleperiod;

	protected MetricValue rootValue;

	private char unit;
	
	//-------------------------------------------------------------------------------
	// CONSTRUCTOR
	//-------------------------------------------------------------------------------


	/*************************************************************************
	 * 
	 * @param sID : Unique ID of the metric
	 * @param sDisplayName : the name of the title
	 * @param displayed : will metric be displayed ?
	 * @param format : format of the display
	 * @param annotationType : show the percent or process number ?
	 * @param index : index in the table
	 * @param partner_index : index of the partner metric. 
	 * 		IT HAS TO BE NEGATIVE IF IT DOESNT HAVE A PARTNER !!
	 * @param type : type of the metric
	 *************************************************************************/
	public BaseMetric(String sID, String sDisplayName, boolean displayed, String format, 
			AnnotationType annotationType, int index, int partner_index, MetricType type) 
	{
		// in case of instantiation from duplicate() method, we need to make sure there is
		//	no double empty suffix
		if (sDisplayName.endsWith(EMPTY_SUFFIX))
			this.displayName = sDisplayName;
		else
			this.displayName = sDisplayName + EMPTY_SUFFIX; // johnmc - hack to leave enough room for ascending/descending triangle;
		
		this.displayed = displayed;
		this.annotationType = annotationType;
		this.index = index;
		this.partner_index = partner_index;
		
		// format
		if (format == null) {
			displayFormat = getFormatBasedOnAnnotation(annotationType);
		} else {
			this.displayFormat = new MetricValuePredefinedFormat(format);
		}

		this.unit = '0';
		this.shortName = sID;

		this.metricType = type;
	}

	//-------------------------------------------------------------------------------
	// METHODS
	//-------------------------------------------------------------------------------
	/*************************************************************************
	 *	Sets the metric's index.
	 ************************************************************************/

	public void setIndex(int index)
	{
		this.index = index;
	}

	/*************************************************************************
	 *	Returns the metric's index.
	 ************************************************************************/

	public int getIndex()
	{
		return this.index;
	}

	
	/*****
	 * get the partner metric index
	 * 
	 * @return the index partner, negative if it has no partner
	 */
	public int getPartner() {
		return partner_index;
	}
	

	public void setPartner(int ei)
	{
		this.partner_index = ei;
	}


	//=================================================================================
	//		ACCESS TO METRIC
	//=================================================================================

	/*************************************************************************
	 *	Returns the metric's short (internal) name.
	 ************************************************************************/
	public String getShortName()
	{
		return this.shortName;
	}

	/*************************************************************************
	 * 
	 * @param newName
	 *************************************************************************/
	public void setShortName(String newName)
	{
		this.shortName = newName;
	}

	/*************************************************************************
	 *	Returns the metric's native (target OS toolset) name.
	 ************************************************************************/
	public String getNativeName()
	{
		return this.nativeName;
	}

	/*************************************************************************
	 *	Returns the metric's user-visible name.
	 ************************************************************************/
	public String getDisplayName()
	{
		return this.displayName;
	}	

	/*************************************************************************
	 * set the new display name
	 * @param name
	 *************************************************************************/
	public void setDisplayName(String name)
	{
		displayName = name;
	}	

	/*************************************************************************
	 *	Returns whether the metric should be displayed.
	 ************************************************************************/	
	public boolean getDisplayed()
	{
		return this.displayed;
	}

	/*************************************************************************
	 * Set display flag (true=to be displayed)
	 * @param d, the flag
	 *************************************************************************/
	public void setDisplayed(boolean d)
	{
		this.displayed = d;
	}

	/*************************************************************************
	 *	Returns the type of annotation a metric's display should include (percent, process number, others ??).
	 ************************************************************************/
	public AnnotationType getAnnotationType()
	{
		return this.annotationType;
	}

	public void setAnnotationType( AnnotationType annType ) 
	{
		annotationType = annType;
		displayFormat = getFormatBasedOnAnnotation(annotationType);
	}
	
	/**
	 * Return the text to display based on the value of the scope
	 * @param scope
	 * @return
	 */
	public String getMetricTextValue(Scope scope) {
		MetricValue mv = this.getValue(scope);
		return this.getMetricTextValue(mv);
	}

	/*************************************************************************
	 * Return the text to display based on the metric value
	 * @param mv: the value of a metric
	 *************************************************************************/
	public String getMetricTextValue(MetricValue mv) {
		
		if (mv == null)
			return null;
		
		String sText;
		
		// enforce bounds for presentation
		if (mv.value > 9.99e99) mv.value = Float.POSITIVE_INFINITY;
		else if (mv.value < -9.99e99) mv.value = Float.NEGATIVE_INFINITY;
		else if (Math.abs(mv.value) < 1.00e-99)  mv.value = (float) 0.0;
		
		// if not a special case, convert the number to a string
		if (Float.compare(mv.value, 0.0f) == 0 || mv == MetricValue.NONE || !MetricValue.isAvailable(mv) ) sText = "";
		else if (Float.compare(mv.value, Float.POSITIVE_INFINITY)==0) sText = "Infinity";
		else if (Float.compare(mv.value, Float.NEGATIVE_INFINITY)==0) sText = "-Infinity";
		else if (Float.isNaN(mv.value)) sText = "NaN";
		else {
			sText = getDisplayFormat().format(mv);
		}
		
		return sText;
	}

	/*************************************************************************
	 * Sets the metric's display format.
	 * @param format
	 *************************************************************************/
	public void setDisplayFormat( IMetricValueFormat format )
	{
		this.displayFormat = format;
	}

	/*************************************************************************
	 *	Returns the metric's display format.
	 ************************************************************************/
	public IMetricValueFormat getDisplayFormat()
	{
		return this.displayFormat;
	}
	
	/*************************************************************************
	 *	MISC
	 ************************************************************************/
	public MetricType getMetricType()
	{
		return this.metricType;
	}

	/*************************************************************************
	 * 
	 * @param objType
	 *************************************************************************/
	public void setMetricType( MetricType objType ) 
	{
		this.metricType = objType;
	}
	/*************************************************************************
	 * Laks: need an interface to update the sample period due to change in DTD
	 * @param s
	 *************************************************************************/
	public void setSamplePeriod(String s) {
		this.sampleperiod = this.convertSamplePeriode(s);
	}

	/*************************************************************************
	 * 
	 * @param sUnit
	 *************************************************************************/
	public void setUnit (String sUnit) {
		this.unit = sUnit.charAt(0);
		if (this.isUnitEvent())
			this.sampleperiod = 1.0;
	}

	/*************************************************************************
	 * Return the sample period
	 * @return
	 *************************************************************************/
	public double getSamplePeriod()
	{
		return this.sampleperiod;
	}

	//=================================================================================
	//		ABSTRACT METHODS
	//=================================================================================
	/*************************************************************************
	 * method to return the value of a given scope. To be implemented by derived class.
	 * @param s : scope of the metric value
	 * @return a metric value
	 *************************************************************************/
	abstract public MetricValue getValue(IMetricScope s);
	
	abstract public MetricValue getRawValue(IMetricScope s);

	/***
	 * Method to duplicate itself (cloning)
	 * @return
	 */
	abstract public BaseMetric duplicate();


	//=================================================================================
	//		UTILITY METHODS
	//=================================================================================

	/**
	 * Verify if the unit is an event or not
	 * @return
	 */
	private boolean isUnitEvent() {
		return this.unit == 'e';
	}
	
	private IMetricValueFormat getFormatBasedOnAnnotation(AnnotationType annotationType) {
		
		IMetricValueFormat displayFormat;
		if (annotationType == AnnotationType.PERCENT) {
			displayFormat = MetricValueFormatFactory.createFormatPercent();
		} else if (annotationType == AnnotationType.PROCESS) {
			displayFormat = MetricValueFormatFactory.createFormatProcess(); 
		} else {
			displayFormat = MetricValueFormatFactory.createFormatDefault(); 
		}
		return displayFormat;
	}

	/**
	 * convert the input sample period into a double, depending of the unit
	 * @param sPeriod
	 * @return
	 */
	protected double convertSamplePeriode( String sPeriod ) {
		if (this.isUnitEvent())
			return 1.0;

		double period = 1.0;
		if (sPeriod != null && sPeriod.length()>0) {
			try {
				period = Double.parseDouble(sPeriod);
			} catch (java.lang.NumberFormatException e) {
				System.err.println("The sample period is incorrect :" + sPeriod);
			}
		}
		return period;
	}

}
