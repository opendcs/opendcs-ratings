/*
 * Copyright (c) 2021. Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved. HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 *
 */

package org.opendcs.ratings;

import hec.data.cwmsRating.io.RatingContainerXmlCompatUtil;
import hec.data.cwmsRating.io.RatingJdbcCompatUtil;
import hec.data.cwmsRating.io.RatingXmlCompatUtil;
import java.sql.Connection;
import java.util.logging.Logger;

import hec.data.DataSetException;
import hec.data.DataSetIllegalArgumentException;
import hec.data.Parameter;

import hec.data.RoundingException;
import hec.data.UsgsRounder;
import hec.data.cwmsRating.RatingConst.RatingMethod;
import hec.data.cwmsRating.io.RatingSpecContainer;
import hec.data.cwmsRating.io.RatingTemplateContainer;
import hec.data.rating.IRatingSpecification;
import hec.data.rating.JDomRatingSpecification;
import hec.util.TextUtil;

import org.w3c.dom.Node;

import static hec.data.cwmsRating.RatingConst.SEPARATOR1;
import static hec.data.cwmsRating.RatingConst.SEPARATOR2;
import static hec.data.cwmsRating.RatingConst.SEPARATOR3;
import static hec.util.TextUtil.split;
/**
 * Implements the CWMS-style rating specification
 *
 * @author Mike Perryman
 */
public class RatingSpec extends RatingTemplate {

	protected static final Logger logger = Logger.getLogger(RatingSet.class.getPackage().getName());

	/**
	 * The identifier of the location for the rating specification
	 */
	protected String locationId = null;
	/**
	 * The rating specification version
	 */
	protected String version = null;
	/**
	 * The identifier of the agency that maintains ratings associated with this specification
	 */
	protected String sourceAgencyId = null;
	/**
	 * The rating behavior to use when the time of a value to rate is within the effective date rage of active ratings
	 */
	protected RatingMethod inRangeMethod = RatingMethod.LINEAR;
	/**
	 * The rating behavior to use when the time of a value to rate is earlier than the effective date of earliest active rating
	 */
	protected RatingMethod outRangeLowMethod = RatingMethod.NEXT;
	/**
	 * The rating behavior to use when the time of a value to rate is later than the effective date of latest active rating
	 */
	protected RatingMethod outRangeHighMethod = RatingMethod.PREVIOUS;
	/**
	 * A flag specifying whether ratings associated with this specification are considered to be active. If false, this
	 * flag overrides the active flag of individual ratings associated with this specification
	 */
	protected boolean active = true;
	/**
	 * A flag specifying whether ratings associated with this specification should be automatically updated when a new
	 * rating is available from the source agency
	 */
	protected boolean autoUpdate = true;
	/**
	 * A flag specifying whether automatically updated ratings should also automatically be marked as active
	 */
	protected boolean autoActivate = false;
	/**
	 * A flag specifying whether automatically updated ratings should also automatically have any rating extensions applied
	 */
	protected boolean autoMigrateExtensions = false;
	/**
	 * USGS-style rounding specifications for each independent parameter
	 */
	protected UsgsRounder[] indRoundingSpecs = null;
	/**
	 * USGS-style rounding specification for the dependent parameter
	 */
	protected UsgsRounder depRoundingSpec = null;
	/**
	 * Descripive text about the rating specification
	 */
	protected String description = null;
	/**
	 * Tests a rating specification identifier for validity.
	 *
	 * @param ratingSpecId The rating specification identifier to test
	 * @return Whether the supplied rating specification identifier is valid
	 */
	public static boolean isValidRatingSpecId(String ratingSpecId) {
		boolean isValid = false;
		test:
		do {
			String[] parts = TextUtil.split(ratingSpecId, SEPARATOR1);
			if (parts.length != 4) break;
			for (String part : parts) {
				if (part.length() == 0) break test;
			}
			parts = TextUtil.split(parts[1].replace(SEPARATOR2, SEPARATOR3), SEPARATOR3);
			if (parts.length < 2) break;
			for (String part : parts) {
				try {new Parameter(part);}
				catch (DataSetIllegalArgumentException e) {break test;}
			}
			isValid = true;
		} while (false);
		return isValid;
	}

	/**
	 * Retrieves a RatingTemplate XML instance from a CWMS database connection
	 * @param conn The connection to a CWMS database
	 * @param officeId The identifier of the office owning the rating. If null, the office associated with the connect user is used.
	 * @param ratingSpecId The rating specification identifier
	 * @throws RatingException Any SQL errors retreiving the data
	 * @return XML string from the database
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.jdbc.RatingJdbcFactory#getRatingSpecXmlFromDatabase(Connection, String, String) instead
	 */
	@Deprecated
	public static String getXmlfromDatabase(
			Connection conn,
			String officeId,
			String ratingSpecId)
			throws RatingException {
		return RatingJdbcCompatUtil.getInstance().getSpecXmlFromDatabase(conn, officeId, ratingSpecId);
	}

	/**
	 *
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.jdbc.RatingJdbcFactory#ratingSpec(Connection, String, String) instead
	 */
	@Deprecated
	public static RatingSpec fromDatabase(Connection conn, String officeId, String ratingSpecId) throws RatingException
	{
		return RatingJdbcCompatUtil.getInstance().specFromDatabase(conn, officeId, ratingSpecId);
	}

	/**
	 * Public Constructor
	 * @param officeId The office that owns the rating specification
	 * @param ratingSpecId The rating specification identifier
	 * @param sourceAgencyId The identifier of the agency that maintains ratings using this specification
	 * @param inRangeMethod The prescribed behavior for when the time of the value to rate falls within the range of rating effective dates
	 * @param outRangeLowMethod The prescribed behavior for when the time of the value to rate falls before the earliest rating effective date
	 * @param outRangeHighMethod The prescribed behavior for when the time of the value to rate falls after the latest rating effective date
	 * @param active Specifies whether to utilize any ratings using this specification
	 * @param autoUpdate Specifies whether ratings using this specification should be automatically loaded when new ratings are available
	 * @param autoActivate Specifies whether ratings using this specification should be automatically activated when new ratings are available
	 * @param autoMigrateExtensions Specifies whether existing should be automatically applied to ratings using this specification when new ratings are loaded
	 * @param indRoundingSpecs The USGS-style rounding specifications for each independent parameter
	 * @param depRoundingSpec The USGS-style rounding specifications for the dependent parameter
	 * @param description The description of this rating specification
	 * @throws RatingException
	 * @throws RoundingException
	 */
	public RatingSpec(
			String officeId,
			String ratingSpecId,
			String sourceAgencyId,
			String inRangeMethod,
			String outRangeLowMethod,
			String outRangeHighMethod,
			boolean active,
			boolean autoUpdate,
			boolean autoActivate,
			boolean autoMigrateExtensions,
			String[] indRoundingSpecs,
			String depRoundingSpec,
			String description) throws RatingException, RoundingException {
		setOfficeId(officeId);
		String[] parts = split(ratingSpecId, SEPARATOR1, "L");
		if (parts.length != 4) throw new RatingException("Invalid rating specification: " + ratingSpecId);
		setLocationId(parts[0]);
		super.setParametersId(parts[1]);
		super.setVersion(parts[2]);
		setVersion(parts[3]);
		setSourceAgencyId(sourceAgencyId);
		setDescription(description);
		setInRangeMethod(RatingMethod.fromString(inRangeMethod));
		setOutRangeLowMethod(RatingMethod.fromString(outRangeLowMethod));
		setOutRangeHighMethod(RatingMethod.fromString(outRangeHighMethod));
		setActive(active);
		setAutoUpdate(autoUpdate);
		setAutoActivate(autoActivate);
		setAutoMigrateExtensions(autoMigrateExtensions);
		setIndRoundingSpecs(indRoundingSpecs);
		setDepRoundingSpec(depRoundingSpec);
	}
	/**
	 * Public Constructor
	 */
	public RatingSpec() {}
	/**
	 * Public Constructor from RatingSpecContainer
	 * @param rsc The RatingSpecContainer object to initialize from
	 * @throws RatingException
	 */
	public RatingSpec(RatingSpecContainer rsc) throws RatingException {
		setData(rsc);
	}
	/**
	 * Public constructor from the CWMS database
	 * @param conn The connection to a CWMS database
	 * @param officeId The identifier of the office owning the rating. If null, the office associated with the connect user is used.
	 * @param ratingSpecId The rating specification identifier
	 * @throws RatingException
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.jdbc.RatingJdbcFactory#ratingSpec(Connection, String, String) instead
	 */
	@Deprecated
	public RatingSpec (
			Connection conn,
			String officeId,
			String ratingSpecId)
			throws RatingException {
		setData(conn, officeId, ratingSpecId);
	}
	/**
	 * Public constructor from XML nodes
	 * @param templateNode The template XML node
	 * @param specNode The specification XML node
	 * @throws RatingException
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingSpecXmlFactory#ratingSpec(Node, Node) instead.
	 */
	@Deprecated
	public RatingSpec(Node templateNode, Node specNode) throws RatingException {
		setData(new RatingSpec(templateNode, specNode).getData());
	}

	/**
	 * Public constructor from XML strings
	 * @param templateXml The template XML text
	 * @param specXml The specification XML text
	 * @throws RatingException
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingSpecXmlFactory#ratingSpec(String, String) instead.
	 */
	@Deprecated
	public RatingSpec(String templateXml , String specXml) throws RatingException {
		RatingXmlCompatUtil service = RatingXmlCompatUtil.getInstance();
		RatingSpec ratingSpec = service.createRatingSpec(templateXml, specXml);
		setData(ratingSpec.getData());
	}
	/**
	 * Retrieves the identifier of the office that owns this rating specification
	 * @return The identifier of the office that owns this rating specification
	 */
	public String getOfficeId() {
		return officeId;
	}
	/**
	 * Sets the identifier of the office that owns this rating specification
	 * @param officeId The identifier of the office that owns this rating specification
	 */
	public void setOfficeId(String officeId) {
		this.officeId = officeId;
	}
	/**
	 * Retrieves the location for which ratings using this specification apply
	 * @return The location for this rating specification
	 */
	public String getLocationId() {
		return locationId;
	}
	/**
	 * Sets the location for which ratings using this specification apply
	 * @param locationId The location for this rating specification
	 */
	public void setLocationId(String locationId) {
		this.locationId = locationId;
	}
	/**
	 * Retrieves the version portion of the rating specification identifier
	 * @return The version portion of the rating specification identifier
	 */
	@Override
	public String getVersion() {
		return version;
	}
	/**
	 * Sets the version portion of the rating specification identifier
	 * @param version version portion of the rating specification identifier
	 */
	@Override
	public void setVersion(String version) {
		this.version = version;
	}
	/**
	 * Retrieves the identifier of the agency that maintains ratings using this specification
	 * @return The identifier of the agency that maintains ratings using this specification
	 */
	public String getSourceAgencyId() {
		return sourceAgencyId;
	}
	/**
	 * Sets the identifier of the agency that maintains ratings using this specification
	 * @param sourceAgencyId The identifier of the agency that maintains ratings using this specification
	 */
	public void setSourceAgencyId(String sourceAgencyId) {
		this.sourceAgencyId = sourceAgencyId;
	}
	/**
	 * Retrieves the prescribed behavior for when the time of the value to rate falls within the range of rating effective dates
	 * @return The prescribed behavior for when the time of the value to rate falls within the range of rating effective dates
	 */
	public RatingMethod getInRangeMethod() {
		return inRangeMethod;
	}
	/**
	 * Sets the prescribed behavior for when the time of the value to rate falls within the range of rating effective dates
	 * @param inRangeMethod The prescribed behavior for when the time of the value to rate falls within the range of rating effective dates
	 * @throws RatingException
	 */
	public void setInRangeMethod(RatingMethod inRangeMethod) throws RatingException {
		if (inRangeMethod == RatingMethod.NEAREST) {
			throw new RatingException("Invalid in range specification method: " + inRangeMethod);
		}
		this.inRangeMethod = inRangeMethod;
	}
	/**
	 * Retrieves the prescribed behavior for when the time of the value to rate falls before the earliest rating effective date
	 * @return The prescribed behavior for when the time of the value to rate falls before the earliest rating effective date
	 */
	public RatingMethod getOutRangeLowMethod() {
		return outRangeLowMethod;
	}
	/**
	 * Sets the prescribed behavior for when the time of the value to rate falls before the earliest rating effective date
	 * @param outRangeLowMethod The prescribed behavior for when the time of the value to rate falls before the earliest rating effective date
	 */
	public void setOutRangeLowMethod(RatingMethod outRangeLowMethod) throws RatingException {
		switch (outRangeLowMethod) {
		case PREVIOUS:
		case LOWER:
			throw new RatingException("Invalid out of range low specification method: " + outRangeLowMethod);
		default:
			break;
		}
		this.outRangeLowMethod = outRangeLowMethod;
	}
	/**
	 * Retrieves the prescribed behavior for when the time of the value to rate falls after the latest rating effective date
	 * @return The prescribed behavior for when the time of the value to rate falls after the latest rating effective date
	 */
	public RatingMethod getOutRangeHighMethod() {
		return outRangeHighMethod;
	}
	/**
	 * Sets the prescribed behavior for when the time of the value to rate falls after the latest rating effective date
	 * @param outRangeHighMethod The prescribed behavior for when the time of the value to rate falls after the latest rating effective date
	 * @throws RatingException
	 */
	public void setOutRangeHighMethod(RatingMethod outRangeHighMethod) throws RatingException {
		switch (outRangeHighMethod) {
		case NEXT:
		case HIGHER:
			throw new RatingException("Invalid out of range high specification method: " + outRangeHighMethod);
		default:
			break;
		}
		this.outRangeHighMethod = outRangeHighMethod;
	}
	/**
	 * Retrieves whether to utilize any ratings using this specification
	 * @return Whether to utilize any ratings using this specification
	 */
	public boolean isActive() {
		return active;
	}
	/**
	 * Sets whether to utilize any ratings using this specification
	 */
	public void setActive(boolean active) {
		this.active = active;
	}
	/**
	 * Retrieves whether ratings using this specification should be automatically loaded when new ratings are available
	 * @return Whether ratings using this specification should be automatically loaded when new ratings are available
	 */
	public boolean isAutoUpdate() {
		return autoUpdate;
	}
	/**
	 * Sets whether ratings using this specification should be automatically loaded when new ratings are available
	 * @param autoUpdate Whether ratings using this specification should be automatically loaded when new ratings are available
	 */
	public void setAutoUpdate(boolean autoUpdate) {
		this.autoUpdate = autoUpdate;
	}
	/**
	 * Retrieves whether ratings using this specification should be automatically activated when new ratings are available
	 * @return Whether ratings using this specification should be automatically activated when new ratings are available
	 */
	public boolean isAutoActivate() {
		return autoActivate;
	}
	/**
	 * Sets whether ratings using this specification should be automatically activated when new ratings are available
	 * @param autoActivate Whether ratings using this specification should be automatically activated when new ratings are available
	 */
	public void setAutoActivate(boolean autoActivate) {
		this.autoActivate = autoActivate;
	}
	/**
	 * Retrieves whether existing rating extensions should be automatically applied to ratings using this specification when new ratings are loaded
	 * @return Whether existing rating extensions should be automatically applied to ratings using this specification when new ratings are loaded
	 */
	public boolean isAutoMigrateExtensions() {
		return autoMigrateExtensions;
	}
	/**
	 * Sets whether existing rating extensions should be automatically applied to ratings using this specification when new ratings are loaded
	 * @param autoMigrateExtensions Whether existing rating extensions should be automatically applied to ratings using this specification when new ratings are loaded
	 */
	public void setAutoMigrateExtensions(boolean autoMigrateExtensions) {
		this.autoMigrateExtensions = autoMigrateExtensions;
	}
	/**
	 * Retrieves the rounding specifications for each of the independent parameters
	 * @return The rounding specifications for each of the independent parameters
	 * @throws RoundingException
	 */
	public UsgsRounder[] getIndRoundingSpecs() throws RoundingException {
		UsgsRounder[] specs = new UsgsRounder[indRoundingSpecs.length];
		for (int i = 0; i < indRoundingSpecs.length; ++i) specs[i] = new UsgsRounder(indRoundingSpecs[i].getRoundingSpec());
		return specs;
	}
	/**
	 * Retrieves the rounding specification strings for each of the independent parameters
	 * @return The rounding specification strings for each of the independent parameters
	 */
	public String[] getIndRoundingSpecStrings() {
		String[] roundingSpecs = new String[indRoundingSpecs.length];
		for (int i = 0; i < indRoundingSpecs.length; ++i) roundingSpecs[i] = indRoundingSpecs[i].getRoundingSpec();
		return roundingSpecs;
	}
	/**
	 * Sets rounding specifications for each of the independent parameters
	 * @param indRoundingSpecs Rounding specification strings for each of the independent parameters
	 * @throws RoundingException
	 */
	public void setIndRoundingSpecs(UsgsRounder[] indRoundingSpecs) throws RoundingException {
		this.indRoundingSpecs = new UsgsRounder[indRoundingSpecs.length];
		for (int i = 0; i < indRoundingSpecs.length; ++i) this.indRoundingSpecs[i] = new UsgsRounder(indRoundingSpecs[i].getRoundingSpec());
	}
	/**
	 * Sets rounding specifications for each of the independent parameters
	 * @param indRoundingSpecs Rounding specification strings for each of the independent parameters
	 * @throws RoundingException
	 */
	public void setIndRoundingSpecs(String[] indRoundingSpecs) throws RoundingException {
		this.indRoundingSpecs = new UsgsRounder[indRoundingSpecs.length];
		for (int i = 0; i < indRoundingSpecs.length; ++i) this.indRoundingSpecs[i] = new UsgsRounder(indRoundingSpecs[i]);
	}
	/**
	 * Retrieves the rounding specification for the dependent parameter
	 * @return The rounding specification for the dependent parameter
	 * @throws RoundingException
	 */
	public UsgsRounder getDepRoundingSpec() throws RoundingException {
		return new UsgsRounder(depRoundingSpec.getRoundingSpec());
	}
	/**
	 * Retrieves the rounding specification string for the dependent parameter
	 * @return The rounding specification string for the dependent parameter
	 */
	public String getDepRoundingSpecString() {
		return depRoundingSpec.getRoundingSpec();
	}
	/**
	 * Sets the rounding specification for the dependent parameter
	 * @param depRoundingSpec The rounding specification for the dependent parameter
	 * @throws RoundingException
	 */
	public void setDepRoundingSpec(UsgsRounder depRoundingSpec) throws RoundingException {
		this.depRoundingSpec = new UsgsRounder(depRoundingSpec.getRoundingSpec());
	}
	/**
	 * Sets the rounding specification for the dependent parameter
	 * @param depRoundingSpec The rounding specification for the dependent parameter
	 * @throws RoundingException
	 */
	public void setDepRoundingSpec(String depRoundingSpec) throws RoundingException {
		this.depRoundingSpec = new UsgsRounder(depRoundingSpec);
	}
	/**
	 * Retrieves the description of the rating specification
	 * @return The description of the rating specification
	 */
	@Override
	public synchronized String getDescription() {
		return description;
	}
	/**
	 * Sets the description of the rating specification
	 * @param description The description of the rating specification
	 */
	@Override
	public synchronized void setDescription(String description) {
		this.description = description;
	}
	/*
	 * (non-Javadoc)
	 * @see hec.data.cwmsRating.RatingTemplate#getParametersId()
	 */
	@Override
	public String getParametersId() {
		return super.getParametersId();
	}
	/*
	 * (non-Javadoc)
	 * @see hec.data.cwmsRating.RatingTemplate#setParametersId(java.lang.String)
	 */
	@Override
	public void setParametersId(String parametersId) throws RatingException {
		super.setParametersId(parametersId);
	}
	/*
	 * (non-Javadoc)
	 * @see hec.data.cwmsRating.RatingTemplate#getTemplateId()
	 */
	@Override
	public String getTemplateId() {
		return super.getTemplateId();
	}
	/*
	 * (non-Javadoc)
	 * @see hec.data.cwmsRating.RatingTemplate#setTemplateId(java.lang.String)
	 */
	@Override
	public void setTemplateId(String templateId) throws RatingException {
		super.setTemplateId(templateId);
	}
	/*
	 * (non-Javadoc)
	 * @see hec.data.cwmsRating.RatingTemplate#getIndParameters()
	 */
	@Override
	public String[] getIndParameters() {
		return super.getIndParameters();
	}
	/*
	 * (non-Javadoc)
	 * @see hec.data.cwmsRating.RatingTemplate#setIndParameters(java.lang.String[])
	 */
	@Override
	public void setIndParameters(String[] indParameters) {
		super.setIndParameters(indParameters);
	}
	/*
	 * (non-Javadoc)
	 * @see hec.data.cwmsRating.RatingTemplate#getIndParamCount()
	 */
	@Override
	public int getIndParamCount() {
		return super.getIndParamCount();
	}
	/*
	 * (non-Javadoc)
	 * @see hec.data.cwmsRating.RatingTemplate#getDepParameter()
	 */
	@Override
	public String getDepParameter() {
		return super.getDepParameter();
	}
	/*
	 * (non-Javadoc)
	 * @see hec.data.cwmsRating.RatingTemplate#setDepParameter(java.lang.String)
	 */
	@Override
	public void setDepParameter(String depParameter) {
		super.setDepParameter(depParameter);
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.RatingTemplate#getInRangeMethods()
	 */
	@Override
	public RatingMethod[] getInRangeMethods() {
		return super.getInRangeMethods();
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.RatingTemplate#setInRangeMethods(hec.data.cwmsRating.RatingConst.RatingMethod[])
	 */
	@Override
	public void setInRangeMethods(RatingMethod[] inRangeMethods)
			throws RatingException {
		super.setInRangeMethods(inRangeMethods);
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.RatingTemplate#getOutRangeLowMethods()
	 */
	@Override
	public RatingMethod[] getOutRangeLowMethods() {
		return super.getOutRangeLowMethods();
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.RatingTemplate#setOutRangeLowMethods(hec.data.cwmsRating.RatingConst.RatingMethod[])
	 */
	@Override
	public void setOutRangeLowMethods(RatingMethod[] outRangeLowMethods)
			throws RatingException {
		super.setOutRangeLowMethods(outRangeLowMethods);
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.RatingTemplate#getOutRangeHighMethods()
	 */
	@Override
	public RatingMethod[] getOutRangeHighMethods() {
		return super.getOutRangeHighMethods();
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.RatingTemplate#setOutRangeHighMethods(hec.data.cwmsRating.RatingConst.RatingMethod[])
	 */
	@Override
	public void setOutRangeHighMethods(RatingMethod[] outRangeHighMethods)
			throws RatingException {
		super.setOutRangeHighMethods(outRangeHighMethods);
	}
	/**
	 * Retrieves the rating specification identifier
	 * @return The rating specification identifier
	 */
	public String getRatingSpecId() {
		return locationId + "." + getTemplateId() + "." + version;
	}
	/**
	 * Retrieves the standard HEC-DSS pathname for this rating specification
	 * @return The standard HEC-DSS pathname for this rating specification
	 */
	public String getDssPathname() {
		return String.format("/%s/%s/%s//%s/%s/", officeId, locationId, getParametersId(), getTemplateVersion(), getVersion());
	}
	/**
	 * Retrieves a RatingSpecContainer containing the data of this object.
	 * @return The RatingSpecContainer
	 */
	@Override
	public RatingSpecContainer getData() {
		return getData(true);
	}
	/**
	 * Retrieves a RatingSpecContainer containing the data of this object.
	 * @return The RatingSpecContainer
	 */
	public RatingSpecContainer getData(boolean getTemplate) {
		RatingSpecContainer rsc = new RatingSpecContainer();
		if (getTemplate) super.getData(rsc);
		rsc.officeId = officeId;
		rsc.specOfficeId = officeId;
		rsc.templateId = getTemplateId();
		rsc.specId = locationId + "." + rsc.templateId + "." + version;
		rsc.locationId = locationId;
		rsc.specVersion = version;
		rsc.sourceAgencyId = sourceAgencyId;
		rsc.inRangeMethod = inRangeMethod.toString();
		rsc.outRangeLowMethod = outRangeLowMethod.toString();
		rsc.outRangeHighMethod = outRangeHighMethod.toString();
		rsc.active = active;
		rsc.autoUpdate = autoUpdate;
		rsc.autoActivate = autoActivate;
		rsc.autoMigrateExtensions = autoMigrateExtensions;
		rsc.indRoundingSpecs = new String[indRoundingSpecs.length];
		for (int i = 0; i < indRoundingSpecs.length; ++i) {
			rsc.indRoundingSpecs[i] = indRoundingSpecs[i].toString();
		}
		rsc.depRoundingSpec = depRoundingSpec.toString();
		rsc.specDescription = description;
		return rsc;
	}
	/**
	 * Sets the data from this object from a RatingSpecContainer
	 * @param rsc The RatingSpecContainer with the data
	 * @throws RatingException
	 */
	public void setData(RatingSpecContainer rsc) throws RatingException {
		try {
			super.setData(rsc, true);
			if (officeId == null && rsc.specOfficeId != null) {
				officeId = rsc.specOfficeId;
			}
			locationId = rsc.locationId;
			version = rsc.specVersion;
			sourceAgencyId = rsc.sourceAgencyId;
			inRangeMethod = RatingMethod.fromString(rsc.inRangeMethod);
			outRangeLowMethod = RatingMethod.fromString(rsc.outRangeLowMethod);
			outRangeHighMethod = RatingMethod.fromString(rsc.outRangeHighMethod);
			active = rsc.active;
			autoUpdate = rsc.autoUpdate;
			autoActivate = rsc.autoActivate;
			autoMigrateExtensions = rsc.autoMigrateExtensions;
			int indParamCount = rsc.indRoundingSpecs.length;
			if (indParamCount != getIndParamCount()) {
				throw new RatingException("Number of independent parameter rounding specifications does not equal number of independent parameters.");
			}
			indRoundingSpecs = new UsgsRounder[indParamCount];
			for (int i = 0; i < indParamCount; ++i) {
				indRoundingSpecs[i] = new UsgsRounder(rsc.indRoundingSpecs[i]);
			}
			depRoundingSpec = new UsgsRounder(rsc.depRoundingSpec);
			description = rsc.specDescription;
		}
		catch (Throwable t) {
			if (t instanceof RatingException) throw (RatingException)t;
			throw new RatingException(t);
		}
	}
	/**
	 * Sets the data from this object from a the database
	 * @param conn The connection to a CWMS database
	 * @param officeId The identifier of the office owning the rating. If null, the office associated with the connect user is used.
	 * @param ratingSpecId The rating specification identifier
	 * @throws RatingException
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.jdbc.RatingJdbcFactory#ratingSpec(Connection, String, String) instead
	 */
	@Deprecated
	public synchronized void setData (
			Connection conn,
			String officeId,
			String ratingSpecId)
			throws RatingException {
		RatingSpec ratingSpec = RatingJdbcCompatUtil.getInstance().specFromDatabase(conn, officeId, ratingSpecId);
		setData(ratingSpec.getData());
	}

	/**
	 * Sets the data for this object from XML Text
	 * @param templateXml The template XML text
	 * @param specXml The specification XML text
	 * @throws RatingException
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingSpecXmlFactory#ratingSpec(String, String) instead.
	 */
	@Deprecated
	public void setData(String templateXml, String specXml) throws RatingException {
		RatingXmlCompatUtil service = RatingXmlCompatUtil.getInstance();
		RatingSpecContainer container = service.createRatingSpec(templateXml, specXml).getData();
		setData(container);
	}

	/**
	 * Sets the data for this object from XML nodes
	 * @param templateNode The template XML node
	 * @param specNode The specification XML node
	 * @throws RatingException
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingSpecXmlFactory#ratingSpec(String, String) instead.
	 */
	@Deprecated
	public void setData(Node templateNode, Node specNode) throws RatingException {
		RatingXmlCompatUtil service = RatingXmlCompatUtil.getInstance();
		RatingTemplateContainer container = service.createRatingSpec(templateNode, specNode).getData();
		setData(container);
	}

	/**
	 * Generates an XML document from this rating specification containing only the
	 * &lt;rating-spec&gt; element.
	 * @param indent The character(s) for each level of indentation
	 * @return The XML document fragment
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingSpecXmlFactory#toXml(RatingSpec, CharSequence, int, boolean) instead.
	 */
	public String toSpecXml(CharSequence indent) {
		RatingContainerXmlCompatUtil service = RatingContainerXmlCompatUtil.getInstance();
		return service.toXml(getData(), indent, 0, false);
	}

	/**
	 * Generates an XML document fragment from this rating specification containing only the
	 * &lt;rating-spec&gt; element.
	 * @param indent The character(s) for each level of indentation
	 * @param level The base indentation level for the document fragment
	 * @return The XML document
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingSpecXmlFactory#toXml(RatingSpec, CharSequence, int, boolean) instead.
	 */
	public String toSpecXml(CharSequence indent, int level) {
		RatingContainerXmlCompatUtil service = RatingContainerXmlCompatUtil.getInstance();
		return service.toXml(getData(), indent, level, false);
	}

	/**
	 * Generates an XML document from this rating specification containing only the
	 * &lt;rating-template&gt; element.
	 * @param indent The character(s) for each level of indentation
	 * @return The XML document
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingSpecXmlFactory#toXml(RatingTemplate, CharSequence, int) instead.
	 */
	public String toTemplateXml(CharSequence indent) {
		RatingContainerXmlCompatUtil service = RatingContainerXmlCompatUtil.getInstance();
		return service.toXml(getData(), indent, 0);
	}

	/**
	 * Generates an XML document fragment from this rating specification containing only the
	 * &lt;rating-template&gt; element.
	 * @param indent The character(s) for each level of indentation
	 * @param level The base indentation level for the document fragment
	 * @return The XML document fragment
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingSpecXmlFactory#toXml(RatingTemplate, CharSequence, int) instead.
	 */
	public String toTemplateXml(CharSequence indent, int level) {
		RatingContainerXmlCompatUtil service = RatingContainerXmlCompatUtil.getInstance();
		return service.toXml(getData(), indent, level);
	}

	/**
	 * Generates an XML document from this rating specification containing both
	 * &lt;rating-template&gt; and &lt;rating-spec&gt; elements.
	 * @param indent The character(s) for each level of indentation
	 * @return The XML document
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingSpecXmlFactory#toXml(RatingSpec, CharSequence, int, boolean) instead.
	 */
	@Deprecated
	public String toXml(CharSequence indent) {
		RatingXmlCompatUtil service = RatingXmlCompatUtil.getInstance();
		return service.toXml(this, indent, 0, false);
	}

	/**
	 * Generates an XML document fragment from this rating specification containing both
	 * &lt;rating-template&gt; and &lt;rating-spec&gt; elements.
	 * @param indent The character(s) for each level of indentation
	 * @param level The base indentation level for the document fragment
	 * @return The XML document fragment
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingSpecXmlFactory#toXml(RatingSpec, CharSequence, int, boolean) instead.
	 */
	@Deprecated
	public String toXml(CharSequence indent, int level) {
		RatingXmlCompatUtil service = RatingXmlCompatUtil.getInstance();
		return service.toXml(this, indent, level, false);
	}

	/**
	 * Generates an XML document fragment from this rating specification.
	 * @param indent The character(s) for each level of indentation
	 * @param level The base indentation level for the document fragment
	 * @return The XML document fragment
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingSpecXmlFactory#toXml(RatingSpec, CharSequence, int, boolean) instead.
	 */
	@Deprecated
	public String toXml(CharSequence indent, int level, boolean includeTemplate) {
		RatingXmlCompatUtil service = RatingXmlCompatUtil.getInstance();
		return service.toXml(this, indent, level, includeTemplate);
	}

	/**
	 * Generates an XML document fragment from this rating specification containing both
	 * &lt;rating-template&gt; and &lt;rating-spec&gt; elements.
	 * @param indent The character(s) for each level of indentation
	 * @param level The base indentation level for the document fragment
	 * @return The XML document fragment
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingSpecXmlFactory#toXml(RatingSpec, CharSequence, int, boolean) instead.
	 */
	@Deprecated
	@Override
	public String toXmlString(CharSequence indent, int level) {
		RatingXmlCompatUtil service = RatingXmlCompatUtil.getInstance();
		return service.toXml(this, indent, level, false);
	}

	/**
	 * Generates an XML document fragment from this rating specification.
	 * @param indent The character(s) for each level of indentation
	 * @param level The base indentation level for the document fragment
	 * @return The XML document fragment
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingSpecXmlFactory#toXml(RatingSpec, CharSequence, int, boolean) instead.
	 */
	@Deprecated
	public String toXmlString(CharSequence indent, int level, boolean includeTemplate) {
		RatingXmlCompatUtil service = RatingXmlCompatUtil.getInstance();
		return service.toXml(this, indent, level, includeTemplate);
	}

	/**
	 * Returns the unique identifying parts for the rating specification.
	 *
	 * @return
	 * @throws DataSetException
	 */
	public IRatingSpecification getRatingSpecification() throws DataSetException
	{
		String officeId = getOfficeId();
		String specificationId = getRatingSpecId();
		JDomRatingSpecification specification = new JDomRatingSpecification(officeId, specificationId);
		return specification;
	}

	/**
	 * Stores the rating specification (without template) to a CWMS database
	 * @param conn The connection to the CWMS database
	 * @param overwriteExisting Flag specifying whether to overwrite any existing rating data
	 * @throws RatingException
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.jdbc.RatingJdbcFactory#store(Connection, RatingSpec, boolean, boolean) instead
	 */
	@Deprecated
	@Override
	public void storeToDatabase(Connection conn, boolean overwriteExisting) throws RatingException {
		RatingJdbcCompatUtil.getInstance().storeToDatabase(this, conn, overwriteExisting, false);
	}

	/**
	 *
	 * @param conn
	 * @param overwriteExisting
	 * @param storeTemplate
	 * @throws RatingException
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.jdbc.RatingJdbcFactory#store(Connection, RatingSpec, boolean, boolean) instead
	 */
	@Deprecated
	public void storeToDatabase(Connection conn, boolean overwriteExisting, boolean storeTemplate) throws RatingException {
		RatingJdbcCompatUtil.getInstance().storeToDatabase(this, conn, overwriteExisting, storeTemplate);
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		return obj == this || (obj != null && obj.getClass() == getClass() && getData().equals(((RatingSpec)obj).getData()));
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#hashCode()
	 */
	@Override
	public int hashCode() {
		return getClass().getName().hashCode() + getData().hashCode();
	}
}
