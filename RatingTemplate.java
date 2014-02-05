package hec.data.cwmsRating;

import static hec.data.cwmsRating.RatingConst.SEPARATOR1;
import static hec.data.cwmsRating.RatingConst.SEPARATOR2;
import static hec.data.cwmsRating.RatingConst.SEPARATOR3;
import static hec.util.TextUtil.join;
import static hec.util.TextUtil.split;
import hec.data.DataSetException;
import hec.data.RatingException;
import hec.data.cwmsRating.RatingConst.RatingMethod;
import hec.data.cwmsRating.io.RatingTemplateContainer;
import hec.data.rating.IRatingTemplate;
import hec.data.rating.JDomRatingTemplate;

import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Types;
import java.util.Arrays;

import rma.lang.Modifiable;

/**
 * Implements CWMS-style rating template.  Holds information about parameters of rating objects.
 * 
 * @author Mike Perryman
 */
public class RatingTemplate implements Modifiable
{
	
	/**
	 * The templateVersion text of the rating template
	 */
	private String templateVersion = null;
	/**
	 * The name(s) of the independent parameter(s)
	 */
	private String[] indParameters = null;
	/**
	 * The rating behavior for when the value to be rated is in the range of the independent values of the rating.
	 * Not used for ExpressionRating objects. One method for each independent parameter.
	 */
	private RatingMethod[] inRangeMethods = null;
	/**
	 * The rating behavior for when the value to be rated sorts to a position before the first independent value of the rating.
	 * Not used for ExpressionRating objects. One method for each independent parameter.
	 */
	private RatingMethod[] outRangeLowMethods = null;
	/**
	 * The rating behavior for when the value to be rated sorts to a position after the last independent value of the rating.
	 * Not used for ExpressionRating objects. One method for each independent parameter.
	 */
	private RatingMethod[] outRangeHighMethods = null;
	/**
	 * The name of the dependent parameter
	 */
	private String depParameter = null;
	/**
	 * Descriptive text about the rating template
	 */
	private String description = null;
	/**
	 * The number of independent parameters for ratings associated with this template
	 */
	private int indParamCount = 0;
	/**
	 * Generates a new RatingTemplate object from a CWMS database connection
	 * @param conn The connection to a CWMS database
	 * @param officeId The identifier of the office owning the rating. If null, the office associated with the connect user is used. 
	 * @param templateId The rating template identifier
	 * @throws RatingException
	 */
	public static RatingTemplate fromDatabase(
			Connection conn, 
			String officeId, 
			String templateId)
			throws RatingException {
		try {
			String sql = 
					"begin "                                  +
					   "cwms_rating.retrieve_templates_xml("   +
					      "p_templates        => :1,"         +
					      "p_template_id_mask => :2,"         +
					      "p_office_id_mask   => :3);"        +
					"end;";
			CallableStatement stmt = conn.prepareCall(sql);
			stmt.registerOutParameter(1, Types.CLOB);
			stmt.setString(2, templateId);
			if (officeId == null) {
				stmt.setNull(3, Types.VARCHAR);
			}
			else {
				stmt.setString(3, officeId);
			}
			stmt.execute();
			Clob clob = stmt.getClob(1);
			stmt.close();
			if (clob.length() > Integer.MAX_VALUE) {
				throw new RatingException("CLOB too long.");
			}
			String xmlText = clob.getSubString(1, (int)clob.length());
			return new RatingTemplate(RatingTemplateContainer.fromXml(xmlText));
		}
		catch (Throwable t) {
			if (t instanceof RatingException) throw (RatingException)t;
			throw new RatingException(t);
		}
	}

	/**
	 * Disabled public zero-arg constructor
	 */
	protected RatingTemplate() {}
	/**
	 * Package Constructor
	 * @param officeId The office that owns the template
	 * @param templateId The CWMS rating template identifier
	 * @param inRangeMethods The specified rating behavior for when the value to rate 
	 *        is in the range of independent values, one for each independent parameter 
	 * @param outRangeLowMethods The specified rating behavior for when the value to rate 
	 *        is less than the smallest independent value, one for each independent parameter
	 * @param outRangeHighMethods The specified rating behavior for when the value to rate 
	 *        is greater than the largest independent value, one for each independent parameter
	 * @param description The description of the template
	 * @throws RatingException
	 */
	public RatingTemplate(
			String officeId,
			String templateId,
			RatingMethod[] inRangeMethods,
			RatingMethod[] outRangeLowMethods,
			RatingMethod[] outRangeHighMethods,
			String description) throws RatingException {
		this.officeId = officeId;
		setTemplateId(templateId);
		setInRangeMethods(inRangeMethods);
		setOutRangeLowMethods(outRangeLowMethods);
		setOutRangeHighMethods(outRangeHighMethods);
		this.description = description;
	}
	/**
	 * Protected constructor from RatingTemplateContainer 
	 * @param rtc The RatingTemplateContainer to initialize from
	 * @throws RatingException
	 */
	public RatingTemplate(RatingTemplateContainer rtc) throws RatingException {
		setData(rtc);
	}
	/**
	 * Retrieves the parameters identifier portion of the template
	 * @return The parameters identifier portion
	 */
	public String getParametersId() {
		
		StringBuilder sb = new StringBuilder(indParameters[0]);
		for (int i = 1; i < indParameters.length; ++i) sb.append(SEPARATOR3).append(indParameters[i]);
		sb.append(SEPARATOR2).append(depParameter);
		return sb.toString();
	}
	/**
	 * Sets the parameters identifier portion of the rating template
	 * @param parametersId The parameters identifier portion of the rating template
	 * @throws RatingException
	 */
	public void setParametersId(String parametersId) throws RatingException {
		String[]  parts = split(parametersId, SEPARATOR2, "L"); 
		if (parts.length != 2) throw new RatingException("Invalid parameters identifier: " + parametersId);
		depParameter = parts[1];
		indParameters = split(parts[0], SEPARATOR3, "L");
		indParamCount = indParameters.length;
	}
	/**
	 * Returns the independent + dependent parameters
	 * @return The independent + dependent parameters
	 */
	public String[] getParameters() {
		String[] parameters = Arrays.copyOf(indParameters, indParamCount+1);
		parameters[indParamCount] = depParameter;
		return parameters;
	}
	/**
	 * Retrieves the number of independent parameters for ratings using this template.
	 * @return The number of independent parameters for this template
	 */
	public int getIndParamCount() {
		return indParamCount;
	}
	/**
	 * Retrieves the rating behaviors for when the value to be rated is in the range of the independent values of the rating.
	 * Not used for ExpressionRating objects. One method for each independent parameter.
	 * @return the in-range rating behaviors
	 */
	public RatingMethod[] getInRangeMethods() {
		return inRangeMethods;
	}
	/**
	 * Retrieves the rating behaviors for when the value to be rated is in the range of the independent values of the rating.
	 * Not used for ExpressionRating objects. One method for each independent parameter.
	 * @param inRangeMethods the in-range rating behaviors
	 */
	public void setInRangeMethods(RatingMethod[] inRangeMethods) throws RatingException {
		if (inRangeMethods.length != indParamCount) {
			throw new RatingException("Number of in-range rating methods is not the same as the number of independent parameters.");
		}
		this.inRangeMethods = inRangeMethods;
	}
	/**
	 * Retrieves the rating behaviors for when the value to be rated sorts to a position before the first independent values of the rating.
	 * Not used for ExpressionRating objects. One method for each independent parameter.
	 * @return the out-of-range-low rating behaviors
	 */
	public RatingMethod[] getOutRangeLowMethods() {
		return outRangeLowMethods;
	}
	/**
	 * Sets the rating behaviors for when the value to be rated sorts to a position before the first independent values of the rating.
	 * Not used for ExpressionRating objects. One method for each independent parameter.
	 * @param outRangeLowMethods the out-of-range-low rating behaviors
	 */
	public void setOutRangeLowMethods(RatingMethod[] outRangeLowMethods) throws RatingException {
		if (outRangeLowMethods.length != indParamCount) {
			throw new RatingException("Number of out-of-range low rating methods is not the same as the number of independent parameters.");
		}
		this.outRangeLowMethods = outRangeLowMethods;
	}
	/**
	 * Retrieves the rating behaviors for when the value to be rated sorts to a position after the last independent values of the rating.
	 * Not used for ExpressionRating objects. One method for each independent parameter.
	 * @return the out-of-range-high rating behaviors
	 */
	public RatingMethod[] getOutRangeHighMethods() {
		return outRangeHighMethods;
	}
	/**
	 * Sets the rating behaviors for when the value to be rated sorts to a position after the last independent values of the rating.
	 * Not used for ExpressionRating objects. One method for each independent parameter.
	 * @param outRangeHighMethods the out-of-range-high rating behaviors
	 */
	public void setOutRangeHighMethods(
			RatingMethod[] outRangeHighMethods) throws RatingException {
		if (outRangeHighMethods.length != indParamCount) {
			throw new RatingException("Number of out-of-range high rating methods is not the same as the number of independent parameters.");
		}
		this.outRangeHighMethods = outRangeHighMethods;
	}
	/** Retrieves the entire rating template identifier
	 * @return The rating template identifier
	 */
	public String getTemplateId() {
		StringBuilder sb = new StringBuilder(getParametersId());
		sb.append(SEPARATOR1).append(templateVersion);
		return sb.toString();
	}
	/**
	 * Sets the rating template identifier
	 * @param templateId The rating template identifier
	 * @throws RatingException
	 */
	public void setTemplateId(String templateId) throws RatingException {
		String[] parts = split(templateId, SEPARATOR1, "L");
		if (parts.length != 2) throw new RatingException("Invalid template identifier: " + templateId);
		setParametersId(parts[0]);
		templateVersion = parts[1];
	}
	/**
	 * Retrieves the templateVersion portion of the rating template identifier
	 * @return The templateVersion portion of the rating template identifier
	 */
	public String getVersion() {
		return templateVersion;
	}
	/**
	 * Sets the templateVersion portion of the rating template identifier
	 * @param templateVersion The templateVersion portion of the rating template identifier
	 */
	public void setVersion(String version) {
		this.templateVersion = version;
	}
	/**
	 * Retrieves an array of the independent parameters for ratings using this template
	 * @return An array of independent parameters for this template
	 */
	public String[] getIndParameters() {
		return Arrays.copyOf(indParameters, indParameters.length);
	}
	/**
	 * Sets the independent parameters used by ratings using this template
	 * @param indParameters The independent parameters for this template
	 */
	public void setIndParameters(String[] indParameters) {
		this.indParameters = Arrays.copyOf(indParameters, indParameters.length);
		indParamCount = this.indParameters.length;
	}
	/**
	 * Retrieves the dependent parameter for ratings using this template
	 * @return The dependent parameter for this template
	 */
	public String getDepParameter() {
		return depParameter;
	}
	/**
	 * Sets the dependent parameter for ratings using this template
	 * @param depParameter The dependent parameter for this template
	 */
	public void setDepParameter(String depParameter) {
		this.depParameter = depParameter;
	}
	/**
	 * Retrieves the description for this template
	 * @return The description for this template
	 */
	public String getDescription() {
		return description;
	}
	/**
	 * Sets the description for this template
	 * @param description The description for this template
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	/**
	 * Retrieves a RatingTemplateContainer containing the data of this object. 
	 * @return The RatingTemplateContainer
	 */
	public RatingTemplateContainer getData() {
		RatingTemplateContainer rtc = new RatingTemplateContainer();
		getData(rtc);
		return rtc;
	}
	/**
	 * Sets the data from this object from a RatingTemplateContainer
	 * @param rtc The RatingTemplateContainer with the data
	 * @throws RatingException
	 */
	public void setData(RatingTemplateContainer rtc) throws RatingException {
		setData(rtc, false);
	}
	/**
	 * Sets the data from this object from a RatingTemplateContainer
	 * @param rtc The RatingTemplateContainer with the data
	 * @throws RatingException
	 */
	public void setData(RatingTemplateContainer rtc, boolean allowNulls) throws RatingException {
		int indParamCount = 0;
		RatingMethod[] inRangeMethods = null;
		RatingMethod[] outRangeLowMethods = null;
		RatingMethod[] outRangeHighMethods = null;
		if (!allowNulls && rtc.officeId == null) {
			throw new RatingException("RatingTemplateContainer has no office identifier.");
		}
		this.officeId = rtc.officeId;
		if (!allowNulls || rtc.templateId != null) {
			String[] parts = split(rtc.templateId, SEPARATOR1, "L");
			if (parts.length != 2) {
				throw new RatingException("RatingTemplateContainer has invalid rating template identifier.");
			}
			parts = split(parts[0], SEPARATOR2, "L");
			if (parts.length != 2) {
				throw new RatingException("RatingTemplateContainer has invalid rating template identifier.");
			}
			parts = split(parts[0], SEPARATOR3, "L");
			indParamCount = parts.length;
		}
		if (!allowNulls || (rtc.indParams != null && rtc.depParam != null)) {
			if(!String.format("%s;%s", join(SEPARATOR3, rtc.indParams), rtc.depParam).equals(rtc.parametersId)) {
				throw new RatingException("RatingTemplateContainer parameters  not consistent with parameters identifier.");
			}
			if (rtc.templateVersion != null) {
				if(!String.format("%s;%s.%s", join(SEPARATOR3, rtc.indParams), rtc.depParam, rtc.templateVersion).equals(rtc.templateId)) {
					throw new RatingException("RatingTemplateContainer parameters and/or templateVersion are not consistent with template identifier.");
				}
			}
		}
		if (!allowNulls || (rtc.inRangeMethods != null && rtc.outRangeLowMethods != null && rtc.outRangeHighMethods != null)) {
			if (rtc.inRangeMethods.length != indParamCount || rtc.outRangeLowMethods.length != indParamCount || rtc.outRangeHighMethods.length != indParamCount) {
				throw new RatingException("RatingTemplateContainer has inconsistent number of independent parameters");
			}
			inRangeMethods = new RatingMethod[indParamCount];
			outRangeLowMethods = new RatingMethod[indParamCount];
			outRangeHighMethods = new RatingMethod[indParamCount];
			for (int i = 0; i < indParamCount; ++i) {
				inRangeMethods[i] = RatingMethod.fromString(rtc.inRangeMethods[i]);
				outRangeLowMethods[i] = RatingMethod.fromString(rtc.outRangeLowMethods[i]);
				outRangeHighMethods[i] = RatingMethod.fromString(rtc.outRangeHighMethods[i]);
			}
		}
		setTemplateId(rtc.templateId);
		description = rtc.templateDescription;
		this.indParamCount = indParamCount;
		this.inRangeMethods = inRangeMethods;
		this.outRangeLowMethods = outRangeLowMethods;
		this.outRangeHighMethods = outRangeHighMethods;
	}
	/**
	 * Fills a specified RatingTemplateContainer object with information from this rating template.
	 * @param rtc The RatingTemplateConainer object to fill
	 */
	protected void getData(RatingTemplateContainer rtc) {
		rtc.officeId = officeId;
		rtc.templateId = getTemplateId();
		String[] parts = split(rtc.templateId, SEPARATOR1, "L");
		rtc.parametersId = parts[0];
		rtc.templateVersion = parts[1];
		parts = split(rtc.parametersId, SEPARATOR2, "L");
		rtc.indParams = split(parts[0], SEPARATOR3, "L");
		rtc.depParam = parts[1];
		rtc.inRangeMethods = new String[indParamCount];
		rtc.outRangeLowMethods = new String[indParamCount];
		rtc.outRangeHighMethods = new String[indParamCount];
		for (int i = 0; i < indParamCount; ++i) {
			rtc.inRangeMethods[i] = inRangeMethods == null || inRangeMethods[i] == null ? null : inRangeMethods[i].toString();
			rtc.outRangeLowMethods[i] = outRangeLowMethods == null || outRangeLowMethods[i] == null ? null : outRangeLowMethods[i].toString();
			rtc.outRangeHighMethods[i] = outRangeHighMethods == null || outRangeHighMethods[i] == null ? null : outRangeHighMethods[i].toString();
		}
		rtc.templateDescription = description;
	}
	/**
	 * Generates an XML document fragment from this rating specification. 
	 * @param indent The character(s) for each level of indentation
	 * @param level The base indentation level for the document fragment
	 * @return The XML document fragment
	 */
	public String toXmlString(CharSequence indent, int level) {
		return getData().toTemplateXml(indent, level);
	}
	
	/**
	 * A flag to indicate that this rating curve has been modified.
	 */
	private boolean modified = false;
	/**
	 * The identifier of the office that owns the rating specification
	 */
	protected String officeId = null;
	
	/**
	 * Returns the modified state of this rating curve.
	 */
    public boolean isModified()
    {
    	return modified;
    }
    /**
     * Sets the modified state of this rating curve.
     */
    public void setModified(boolean bool)
    {
    	modified = bool;
    }
	/**
	 * Retrieves the templateVersion portion of the rating template used by this specification
	 * @return The templateVersion portion of the rating template used by this specification
	 */
	public String getTemplateVersion() {
		return templateVersion;
	}
	/**
	 * Sets the templateVersion portion of the rating template used by this specification
	 * @param templateVersion The templateVersion portion of the rating template used by this specification
	 */
	public void setTemplateVersion(String version) {
		templateVersion = version;
	}
	/**
	 * Retrieves the description portion of the rating template used by this specification
	 * @return The description portion of the rating template used by this specification
	 */
	public String getTemplateDescription() {
		return description;
	}
	/**
	 * Sets the description portion of the rating template used by this specification
	 * @param description The description portion of the rating template used by this specification
	 */
	public void setTemplateDescription(String description) {
		this.description = description;
	}
	/**
	 * Stores the rating template to a CWMS database
	 * @param conn The connection to the CWMS database
	 * @param overwriteExisting Flag specifying whether to overwrite any existing rating data
	 * @throws RatingException
	 */
	public void storeToDatabase(Connection conn, boolean overwriteExisting) throws RatingException {
		RatingSet.storeToDatabase(conn, getData().toTemplateXml(""), overwriteExisting);
	}

	/**
	 * Returns the unique identifying parts for the rating template.
	 * @return
	 * @throws DataSetException
	 */
	public IRatingTemplate getRatingTemplate() throws DataSetException
	{
		//String officeId = officeId;
		String templateId = getTemplateId();
		JDomRatingTemplate template = new JDomRatingTemplate(officeId, templateId);
		return template;
	}
}
