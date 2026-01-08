/*
 * Copyright (c) 2021. Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved. HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 *
 */

package hec.data.cwmsRating.io;


import hec.data.cwmsRating.AbstractRating;
import hec.data.cwmsRating.RatingConst;
import hec.data.cwmsRating.VirtualRating;
import hec.util.TextUtil;
import java.util.Objects;
import mil.army.usace.hec.metadata.VerticalDatum;
import mil.army.usace.hec.metadata.VerticalDatumContainer;
import mil.army.usace.hec.metadata.VerticalDatumException;

import static hec.lang.Const.UNDEFINED_TIME;

/**
 * Container for AbstractRating data
 *
 * @author Mike Perryman
 */
public class AbstractRatingContainer implements VerticalDatum, Comparable<AbstractRatingContainer> {
	/**
	 * Office that owns the rating
	 */
	public String officeId = null;
	/**
	 * CWMS-style rating specification identifier
	 */
	public String ratingSpecId = null;
	/**
	 * CWMS-style rating units identifier
	 */
	public String unitsId = null;
	/**
	 * Flag specifying whether the rating is marked as active
	 */
	public boolean active = false;
	/**
	 * The earliest date/time that the rating is in effect
	 */
	public long effectiveDateMillis = UNDEFINED_TIME;
	/**
	 * The time to begin transition (interpolation) from the previous rating to this one.
	 * If undefined, transition from the previous rating effective date.
	 */
	public long transitionStartDateMillis = UNDEFINED_TIME;
	/**
	 * The date/time that the rating was loaded into the database
	 */
	public long createDateMillis = UNDEFINED_TIME;
	/**
	 * Text description of the rating
	 */
	public String description = null;
	/**
	 * Vertical datum info if this rating has any.
	 */
	protected VerticalDatumContainer vdc = null;

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		AbstractRatingContainer that = (AbstractRatingContainer) o;
		return active == that.active && effectiveDateMillis == that.effectiveDateMillis &&
			transitionStartDateMillis == that.transitionStartDateMillis &&
			createDateMillis == that.createDateMillis && Objects.equals(officeId, that.officeId) &&
			Objects.equals(ratingSpecId, that.ratingSpecId) && Objects.equals(unitsId, that.unitsId) &&
			Objects.equals(description, that.description) && Objects.equals(vdc, that.vdc);
	}

	@Override
	public int hashCode() {
		return Objects.hash(officeId, ratingSpecId, unitsId, active, effectiveDateMillis, transitionStartDateMillis, createDateMillis, description,
			vdc);
	}

	/**
	 * Fills another AbstractRatingContainer object with information from this one
	 * @param other The AbstractRatingContainer object to fill
	 */
	public void clone(AbstractRatingContainer other) {
		other.officeId = officeId;
		other.ratingSpecId = ratingSpecId;
		if(!(other instanceof VirtualRatingContainer)) {
			other.unitsId = unitsId;
		}
		other.active = active;
		other.effectiveDateMillis = effectiveDateMillis;
		other.transitionStartDateMillis = transitionStartDateMillis;
		other.createDateMillis = createDateMillis;
		other.description = description;
		if (vdc != null) {
			other.vdc = vdc.clone();
		}
	}
	/**
	 * Returns a new AbstractRatingContainer cloned from this one.
	 */
	public AbstractRatingContainer clone() {
		AbstractRatingContainer other = getInstance();
		clone(other);
		return other;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		try {
			return String.format("%s/%s", officeId, ratingSpecId);
		}
		catch (Throwable t) {
			return super.toString();
		}
	}
	/**
	 * Returns whether this object has any vertical datum info
	 * @return
	 */
	public boolean hasVerticalDatum() {
		return this.vdc != null;
	}
	/* (non-Javadoc)
	 * @see hec.data.VerticalDatum#getNativeVerticalDatum()
	 */
	@Override
	public String getNativeVerticalDatum() throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		return vdc.getNativeVerticalDatum();
	}
	/* (non-Javadoc)
	 * @see hec.data.VerticalDatum#getCurrentVerticalDatum()
	 */
	@Override
	public String getCurrentVerticalDatum() throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		return vdc.getCurrentVerticalDatum();
	}
	/* (non-Javadoc)
	 * @see hec.data.VerticalDatum#isCurrentVerticalDatumEstimated()
	 */
	@Override
	public boolean isCurrentVerticalDatumEstimated() throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		return vdc.isCurrentVerticalDatumEstimated();
	}
	/* (non-Javadoc)
	 * @see hec.data.VerticalDatum#toNativeVerticalDatum()
	 */
	@Override
	public boolean toNativeVerticalDatum() throws VerticalDatumException {
		throw new VerticalDatumException("Method is not implemented for " + this.getClass().getName());
	}
	/* (non-Javadoc)
	 * @see hec.data.VerticalDatum#toNGVD29()
	 */
	@Override
	public boolean toNGVD29() throws VerticalDatumException {
		throw new VerticalDatumException("Method is not implemented for " + this.getClass().getName());
	}
	/* (non-Javadoc)
	 * @see hec.data.VerticalDatum#toNAVD88()
	 */
	@Override
	public boolean toNAVD88() throws VerticalDatumException {
		throw new VerticalDatumException("Method is not implemented for " + this.getClass().getName());
	}
	/* (non-Javadoc)
	 * @see hec.data.VerticalDatum#toVerticalDatum(java.lang.String)
	 */
	@Override
	public boolean toVerticalDatum(String datum) throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		boolean change = false;
		if (!vdc.getCurrentVerticalDatum().equalsIgnoreCase(datum)) {
			switch (datum.toUpperCase()) {
			case "NGVD29" :
				change = toNGVD29();
				break;
			case "NAVD88" :
				change = toNAVD88();
				break;
			case "NATIVE" :
				change = toNativeVerticalDatum();
				break;
			case "LOCAL" :
				if (!vdc.nativeDatum.equals("LOCAL")) {
					throw new VerticalDatumException("Object does not have LOCAL vertical datum");
				}
				change = toNativeVerticalDatum();
				break;
			default :
				if (!(vdc.nativeDatum.equals("LOCAL") && TextUtil.equals(datum, vdc.localDatumName))) {
					throw new VerticalDatumException("Unexpected datum: " + datum);
				}
				change = toNativeVerticalDatum();
				break;
			}
		}
		return change;
	}
	/* (non-Javadoc)
	 * @see hec.data.VerticalDatum#forceVerticalDatum(java.lang.String)
	 */
	@Override
	public boolean forceVerticalDatum(String datum) throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		datum = datum.trim().toUpperCase();
		boolean change = false;
		if (change) {
			switch (datum) {
			case "NGVD29" :
			case "NAVD88" :
				change = !vdc.currentDatum.equals(datum);
				vdc.currentDatum = datum;
				break;
			case "NATIVE" :
				change = !vdc.currentDatum.equals(vdc.getNativeVerticalDatum());
				vdc.currentDatum = vdc.getNativeVerticalDatum();
				break;
			case "LOCAL" :
				if (!vdc.nativeDatum.equals("LOCAL")) {
					throw new VerticalDatumException("Object does not have LOCAL vertical datum");
				}
				change = !vdc.currentDatum.equals(vdc.localDatumName);
				vdc.currentDatum = vdc.localDatumName;
				break;
			default :
				if (!(vdc.nativeDatum.equals("LOCAL") && TextUtil.equals(datum, vdc.localDatumName))) {
					throw new VerticalDatumException("Unexpected datum: " + datum);
				}
				change = !vdc.currentDatum.equals(vdc.getNativeVerticalDatum());
				vdc.currentDatum = vdc.getNativeVerticalDatum();
				break;
			}
		}
		return change;
	}
	/* (non-Javadoc)
	 * @see hec.data.VerticalDatum#getCurrentOffset()
	 */
	@Override
	public double getCurrentOffset() throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		return vdc.getCurrentOffset();
	}
	/* (non-Javadoc)
	 * @see hec.data.VerticalDatum#getCurrentOffset(java.lang.String)
	 */
	@Override
	public double getCurrentOffset(String unit) throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		return vdc.getCurrentOffset(unit);
	}
	/* (non-Javadoc)
	 * @see hec.data.VerticalDatum#getNGVD29Offset()
	 */
	@Override
	public double getNGVD29Offset() throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		return vdc.getNGVD29Offset();
	}
	/* (non-Javadoc)
	 * @see hec.data.VerticalDatum#getNGVD29Offset(java.lang.String)
	 */
	@Override
	public double getNGVD29Offset(String unit) throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		return vdc.getNGVD29Offset(unit);
	}
	/* (non-Javadoc)
	 * @see hec.data.VerticalDatum#getNAVD88Offset()
	 */
	@Override
	public double getNAVD88Offset() throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		return vdc.getNAVD88Offset();
	}
	/* (non-Javadoc)
	 * @see hec.data.VerticalDatum#getNAVD88Offset(java.lang.String)
	 */
	@Override
	public double getNAVD88Offset(String unit) throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		return vdc.getNAVD88Offset(unit);
	}
	/* (non-Javadoc)
	 * @see hec.data.VerticalDatum#isNGVD29OffsetEstimated()
	 */
	@Override
	public boolean isNGVD29OffsetEstimated() throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		return vdc.isNGVD29OffsetEstimated();
	}
	/* (non-Javadoc)
	 * @see hec.data.VerticalDatum#isNAVD88OffsetEstimated()
	 */
	@Override
	public boolean isNAVD88OffsetEstimated() throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		return vdc.isNAVD88OffsetEstimated();
	}
	/* (non-Javadoc)
	 * @see hec.data.VerticalDatum#getVerticalDatumInfo()
	 */
	@Override
	public String getVerticalDatumInfo() throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		return vdc.getVerticalDatumInfo();
	}
	/* (non-Javadoc)
	 * @see hec.data.VerticalDatum#setVerticalDatumInfo(java.lang.String)
	 */
	@Override
	public void setVerticalDatumInfo(String xmlStr) throws VerticalDatumException {
		if (vdc == null) {
			vdc = new VerticalDatumContainer(xmlStr);
		}
		else {
			vdc.setVerticalDatumInfo(xmlStr);
		}
	}
	/**
	 * Intended to be overridden to allow sub-classes to return empty instances for cloning.
	 * @return
	 */
	public AbstractRatingContainer getInstance() {
		return new AbstractRatingContainer();
	}

	public AbstractRating newRating() throws RatingException {
		throw new RatingException("Cannot call newRating() on AbstractRatingContainer class");
	}

	/**
	 * Constructs an AbstractRatingContainer from the first &lt;rating&gt; or &lt;usgs-stream-rating&gt; element in an XML string or null if no such element is found.
	 * @param xmlStr The XML string
	 * @return The RatingTemplateContainer object
	 * @throws RatingException
	 * @deprecated use mil.army.usace.hec.cwms.rating.io.xml.RatingXmlFactory#abstractRatingContainer(String) instead
	 */
	@Deprecated
	public static AbstractRatingContainer buildFromXml(String xmlStr) throws RatingException {
		RatingContainerXmlCompatUtil service = RatingContainerXmlCompatUtil.getInstance();
		return service.createAbstractRatingContainer(xmlStr);
	}

	/**
	 * Generates an XML string from this object. The subclass overrides should normally be called instead of this.
	 * @param indent The amount to indent each level
	 * @return the generated XML
	 * @deprecated use mil.army.usace.hec.cwms.rating.io.xml.RatingXmlFactory#abstractRatingContainer(AbstractRatingContainer, CharSequence, int) instead
	 */
	@Deprecated
	public String toXml(CharSequence indent) {
		return toXml(indent, 0);
	}
	/**
	 * Generates an XML string from this object. The subclass overrides should normally be called instead of this.
	 * @param indent The amount to indent each level
	 * @param level The initial level of indentation
	 * @return the generated XML
	 * @deprecated use mil.army.usace.hec.cwms.rating.io.xml.RatingXmlFactory#abstractRatingContainer(AbstractRatingContainer, CharSequence, int) instead
	 */
	@Deprecated
	public String toXml(CharSequence indent, int level) {
		return RatingContainerXmlCompatUtil.getInstance().toXml(this, indent, level);
	}
	/**
	 * Add the specified offset to the values of the specified parameter.
	 * @param paramNum Specifies which parameter to add the offset to. 0, 1 = first, second independent, etc... -1 = dependent parameter
	 * @param offset The offset to add
	 * @throws RatingException if not implemented for a specific rating container type or if paramNum is invalid for this container
	 */
	public void addOffset(int paramNum, double offset) throws RatingException {
		throw new RatingException("This method is not implemented for " + this.getClass().getName());
	}
	/**
	 * Retrieves the independent and dependent parameters as well as their units
	 * @return the parameters and units in a 2-deep array
	 */
	protected String[][] getParamsAndUnits() {
		String[][] paramsAndUnits = new String[2][];
		String parametersId = TextUtil.split(ratingSpecId, RatingConst.SEPARATOR1)[1];
		paramsAndUnits[0] = TextUtil.split(
				TextUtil.replaceAll(
					parametersId,
					RatingConst.SEPARATOR2,
					RatingConst.SEPARATOR3),
				RatingConst.SEPARATOR3);
		paramsAndUnits[1] = TextUtil.split(
				TextUtil.replaceAll(
					unitsId,
					RatingConst.SEPARATOR2,
					RatingConst.SEPARATOR3),
				RatingConst.SEPARATOR3);
		return paramsAndUnits;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(AbstractRatingContainer o) {
		int result;
		result = toString().compareTo(o.toString());
		if (result == 0) {
			result = (int)(Math.signum(effectiveDateMillis - o.effectiveDateMillis));
		}
		return result;
	}

	/**
	 * Returns the VerticalDatumContainer
	 * @return
	 */
	@Override
	public VerticalDatumContainer getVerticalDatumContainer() {
		return vdc;
	}

	/**
	 * Sets the VerticalDatumContainer
	 * @param vdc
	 */
	public void setVerticalDatumContainer(VerticalDatumContainer vdc) {
		this.vdc = vdc;
	}
}
