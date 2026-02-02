/*
 * Copyright (c) 2021. Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved. HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 *
 */

package org.opendcs.ratings;

import static org.opendcs.ratings.RatingConst.SEPARATOR1;
import static org.opendcs.ratings.RatingConst.SEPARATOR2;
import static org.opendcs.ratings.RatingConst.SEPARATOR3;
import static hec.util.TextUtil.join;
import static hec.util.TextUtil.split;

import hec.data.DataSetException;

import org.opendcs.ratings.RatingConst.RatingMethod;
import org.opendcs.ratings.io.RatingContainerXmlCompatUtil;
import org.opendcs.ratings.io.RatingJdbcCompatUtil;
import org.opendcs.ratings.io.RatingTemplateContainer;
import org.opendcs.ratings.io.RatingXmlCompatUtil;
import hec.data.rating.IRatingTemplate;
import hec.data.rating.JDomRatingTemplate;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Types;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import rma.lang.Modifiable;

/**
 * Implements CWMS-style rating template.  Holds information about parameters of rating objects.
 *
 * @author Mike Perryman
 */
public class RatingTemplate implements Modifiable
{

	protected static final Logger logger = Logger.getLogger(RatingSet.class.getPackage().getName());

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
	 * @throws RatingException any issues retrieving the data or processing what's returned
	 * @return the rating template in XML form
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.jdbc.RatingJdbcFactory#getRatingSpecXmlFromDatabase(Connection, String, String) instead
	 */
	public static String getXmlfromDatabase(Connection conn, String officeId, String templateId) throws RatingException {
		return RatingJdbcCompatUtil.getInstance().getTemplateXmlFromDatabase(conn, officeId, templateId);
	}

	/**
	 *
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.jdbc.RatingJdbcFactory#ratingTemplate(Connection, String, String) instead
	 */
	@Deprecated
	public static RatingTemplate fromDatabase(Connection conn, String officeId, String templateId) throws RatingException
	{
		return RatingJdbcCompatUtil.getInstance().templateFromDatabase(conn, officeId, templateId);
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
	 * @throws RatingException any issues using the provided data
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
	 * Public constructor from RatingTemplateContainer
	 * @param rtc The RatingTemplateContainer to initialize from
	 * @throws RatingException any issues with retrieving the data
	 */
	public RatingTemplate(RatingTemplateContainer rtc) throws RatingException {
		setData(rtc);
	}
	/**
	 * Public constructor from a CWMS database connection
	 * @param conn The connection to a CWMS database
	 * @param officeId The identifier of the office owning the rating. If null, the office associated with the connect user is used.
	 * @param templateId The rating template identifier
	 * @throws RatingException any issues with retrieving the data.
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.jdbc.RatingJdbcFactory#ratingTemplate(Connection, String, String) instead
	 */
	@Deprecated
	public RatingTemplate(
			Connection conn,
			String officeId,
			String templateId)
			throws RatingException {
		setData(conn, officeId, templateId);
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
	 * @throws RatingException errors setting the parameter parts. such as
	 *    if there aren't exactly 2 parts
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
	 * @throws RatingException any errors with the input. such as the number of elements not being that same as
	 *    the independent parameter count.
	 */
	public void setInRangeMethods(RatingMethod[] inRangeMethods) throws RatingException {
		if (inRangeMethods.length != indParamCount) {
			throw new RatingException("Number of in-range rating methods is not the same as the number of independent parameters.");
		}
		for (RatingMethod m : inRangeMethods) {
			switch(m) {
			case NEAREST :
				throw new RatingException("Invalid in range template method: " + m.toString());
			default :
				break;
			}
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
	 * @throws RatingException any errors with the input. such as the number of elements not being that same as
	 *    the independent parameter count.
	 */
	public void setOutRangeLowMethods(RatingMethod[] outRangeLowMethods) throws RatingException {
		if (outRangeLowMethods.length != indParamCount) {
			throw new RatingException("Number of out-of-range low rating methods is not the same as the number of independent parameters.");
		}
		for (RatingMethod m : outRangeLowMethods) {
			switch(m) {
			case LOWER :
			case PREVIOUS :
				throw new RatingException("Invalid out of range low template method: " + m.toString());
			default :
				break;
			}
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
	 * @throws RatingException if the number of elements is not the same as the number or independent Parameters
	 *     or the METHOD is not HIGHER or NEXT
	 */
	public void setOutRangeHighMethods(
			RatingMethod[] outRangeHighMethods) throws RatingException {
		if (outRangeHighMethods.length != indParamCount) {
			throw new RatingException("Number of out-of-range high rating methods is not the same as the number of independent parameters.");
		}
		for (RatingMethod m : outRangeHighMethods) {
			switch(m) {
			case HIGHER :
			case NEXT :
				throw new RatingException("Invalid out of range high template method: " + m.toString());
			default :
				break;
			}
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
	 * @throws RatingException if the template doesn't contain two elements separated by {@value org.opendcs.ratings.RatingConst#SEPARATOR1}
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
	public void setVersion(String templateVersion) {
		this.templateVersion = templateVersion;
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
	 * Sets the data for this object from a CWMS database connection
	 * @param conn The connection to a CWMS database
	 * @param officeId The identifier of the office owning the rating. If null, the office associated with the connect user is used.
	 * @param templateId The rating template identifier
	 * @throws RatingException any issues retrieving or using the specified data.
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.jdbc.RatingJdbcFactory#ratingTemplate(Connection, String, String) instead
	 */
	@Deprecated
	public synchronized void setData (
			Connection conn,
			String officeId,
			String templateId)
			throws RatingException {
		RatingTemplate ratingTemplate = RatingJdbcCompatUtil.getInstance().templateFromDatabase(conn, officeId, templateId);
		setData(ratingTemplate.getData());
	}
	/**
	 * Sets the data from this object from an XML instance
	 * @param xmlText The XML instance
	 * @throws RatingException any errors processing the XML data.
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingSpecXmlFactory#ratingTemplate(String) instead.
	 */
	@Deprecated
	public void setData(String xmlText) throws RatingException {
		RatingContainerXmlCompatUtil service = RatingContainerXmlCompatUtil.getInstance();
		RatingTemplateContainer container = service.createRatingTemplateContainer(xmlText);
		setData(container);
	}
	/**
	 * Sets the data from this object from a RatingTemplateContainer
	 * @param rtc The RatingTemplateContainer with the data
	 * @throws RatingException any issues transferring the data
	 */
	public void setData(RatingTemplateContainer rtc) throws RatingException {
		setData(rtc, false);
	}
	/**
	 * Sets the data from this object from a RatingTemplateContainer
	 * @param rtc The RatingTemplateContainer with the data
	 * @param allowNulls whether elements can take on a default.
	 * @throws RatingException any errors transfering the data. Ex. allowsNulls is full and certain elements aren't set.
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
		if(outRangeLowMethods != null) {
			setOutRangeLowMethods(outRangeLowMethods);
		}
		if(inRangeMethods != null) {
			setInRangeMethods(inRangeMethods);
		}
		if(outRangeHighMethods != null) {
			setOutRangeHighMethods(outRangeHighMethods);
		}
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
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingSpecXmlFactory#toXml(RatingTemplate, CharSequence, int) instead.
	 */
	@Deprecated
	public String toXmlString(CharSequence indent, int level) {
		RatingContainerXmlCompatUtil service = RatingContainerXmlCompatUtil.getInstance();
		return service.toXml(getData(), indent, level);
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
    @Override
	public boolean isModified()
    {
    	return modified;
    }
    /**
     * Sets the modified state of this rating curve.
     */
    @Override
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
	public void setTemplateVersion(String templateVersion) {
		this.templateVersion = templateVersion;
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
	 * @throws RatingException any issues storing this to the database
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.jdbc.RatingJdbcFactory#store(RatingTemplate, Connection, boolean) instead.
	 */
	@Deprecated
	public void storeToDatabase(Connection conn, boolean overwriteExisting) throws RatingException {
		RatingJdbcCompatUtil.getInstance().storeToDatabase(this, conn, overwriteExisting);
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
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		return obj == this || (obj != null && obj.getClass() == getClass() && getData().equals(((RatingTemplate)obj).getData()));
	}

	/* (non-Javadoc)
	 * @see org.opendcs.ratings.AbstractRating#hashCode()
	 */
	@Override
	public int hashCode() {
		return getClass().getName().hashCode() + getData().hashCode();
	}
}
