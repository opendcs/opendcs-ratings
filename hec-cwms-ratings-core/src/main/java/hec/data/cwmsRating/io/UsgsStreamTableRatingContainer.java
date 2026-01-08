/*
 * Copyright (c) 2021. Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved. HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 *
 */

package hec.data.cwmsRating.io;


import hec.data.cwmsRating.AbstractRating;
import hec.data.cwmsRating.UsgsStreamTableRating;
import mil.army.usace.hec.metadata.VerticalDatumException;

import org.jdom.Element;
/**
 * Data container class for UsgsStreamTableRating data
 * @author Mike Perryman
 */
public class UsgsStreamTableRatingContainer extends TableRatingContainer {

	/**
	 * The dated shift adjustments
	 */
	public RatingSetContainer shifts = null;
	/**
	 * The logarithmic interpolation offsets
	 */
	public TableRatingContainer offsets = null;

	/**
	 * Public empty constructor
	 */
	public UsgsStreamTableRatingContainer() {}
	/**
	 * Public constructor from a jdom element
	 * @param ratingElement The jdom element
	 * @throws RatingException
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingXmlFactory#usgsStreamTableRatingContainer(Element) instead
	 */
	@Deprecated
	public UsgsStreamTableRatingContainer(Element ratingElement) throws RatingException {
		populateFromXml(ratingElement);
	}
	/**
	 * Public constructor from an XML snippet
	 * @param xmlText The XML snippet
	 * @throws RatingException
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingXmlFactory#usgsStreamTableRatingContainer(String) instead
	 */
	@Deprecated
	public UsgsStreamTableRatingContainer(String xmlText) throws RatingException {
		populateFromXml(xmlText);
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.io.TableRatingContainer#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		boolean result = obj == this;
		if(!result) {
			do {
				if (obj == null || obj.getClass() != getClass()) break;
				if (!super.equals(obj)) break;
				UsgsStreamTableRatingContainer other = (UsgsStreamTableRatingContainer)obj;
				if ((other.shifts == null) != (shifts == null)) break;
				if (shifts != null) {
					if (!other.shifts.equals(shifts)) break;
				}
				if ((other.offsets == null) != (offsets == null)) break;
				if (offsets != null) {
					if (!other.offsets.equals(offsets)) break;
				}
				result = true;
			} while(false);
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.io.TableRatingContainer#hashCode()
	 */
	@Override
	public int hashCode() {
		return getClass().getName().hashCode()
				+ super.hashCode()
				+ 3 * (offsets == null ? 1 : offsets.hashCode())
				+ 5 * (shifts == null ? 1 : shifts.hashCode());
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.RatingTableContainer#clone(hec.data.cwmsRating.RatingContainer)
	 */
	@Override
	public void clone(AbstractRatingContainer other) {
		if (!(other instanceof UsgsStreamTableRatingContainer)) {
			throw new IllegalArgumentException("Clone-to object must be a UsgsStreamTableRatingContainer.");
		}
		super.clone(other);
		UsgsStreamTableRatingContainer ustrc = (UsgsStreamTableRatingContainer)other;
		ustrc.shifts = shifts == null ? null : shifts.clone();
		ustrc.offsets = offsets == null ? null : (TableRatingContainer)offsets.clone();
	}

	@Override
	public AbstractRatingContainer getInstance() {
		return new UsgsStreamTableRatingContainer();
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.io.TableRatingContainer#newRating()
	 */
	@Override
	public AbstractRating newRating() throws RatingException {
		UsgsStreamTableRating rating = new UsgsStreamTableRating(this);
		return rating;
	}
	/**
	 * Populates the UsgsStreamRatingContainer object from a jdom element
	 * @param ratingElement the jdom element
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingXmlFactory#usgsStreamTableRatingContainer(Element) instead
	 */
	@Deprecated
	public void populateFromXml(Element ratingElement) throws RatingException {
		RatingContainerXmlCompatUtil service = RatingContainerXmlCompatUtil.getInstance();
		UsgsStreamTableRatingContainer usgsStreamTableRatingContainer = service.createUsgsStreamTableRatingContainer(ratingElement);
		usgsStreamTableRatingContainer.clone(this);
	}
	/**
	 * Populates the UsgsStreamRatingContainer object from an XML snippet
	 * @param xmlText the XML snippet
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingXmlFactory#usgsStreamTableRatingContainer(String) instead
	 */
	@Deprecated
	public void populateFromXml(String xmlText) throws RatingException {
		RatingContainerXmlCompatUtil service = RatingContainerXmlCompatUtil.getInstance();
		UsgsStreamTableRatingContainer usgsStreamTableRatingContainer = service.createUsgsStreamTableRatingContainer(xmlText);
		usgsStreamTableRatingContainer.clone(this);
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.io.TableRatingContainer#toNativeVerticalDatum()
	 */
	@Override
	public boolean toNativeVerticalDatum() throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		boolean change = false;
		if (getParamsAndUnits()[1][0].startsWith("Elev")) {
			double offset = vdc.getCurrentOffset();
			if (offset != 0.) {
				change = true;
				try {
					addOffset(0, -offset);
					if (offsets != null) {
						offsets.addOffset(0, -offset);
					}
					if (shifts != null && shifts.abstractRatingContainers != null)
						for (int i = 0; i < shifts.abstractRatingContainers.length; ++i) {
							shifts.abstractRatingContainers[i].addOffset(0, -offset);
					}
				}
				catch (RatingException e) {
					throw new VerticalDatumException(e);
				}
			}
			vdc.toNativeVerticalDatum();
		}
		return change;
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.io.TableRatingContainer#toNGVD29()
	 */
	@Override
	public boolean toNGVD29() throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		boolean change = false;
		String[][] paramsAndUnits = getParamsAndUnits();
		String[] params = paramsAndUnits[0];
		String[] units  = paramsAndUnits[1];
		if (params[0].startsWith("Elev")) {
			if (!vdc.getCurrentVerticalDatum().equals("NGVD29")) {
				change = true;
				double offset1 = vdc.getNGVD29Offset(units[0]);
				double offset2 = vdc.getCurrentOffset(units[0]);
				try {
					addOffset(0, offset1 - offset2);
					if (offsets != null) {
						offsets.addOffset(0, offset1 - offset2);
					}
					if (shifts != null && shifts.abstractRatingContainers != null)
						for (int i = 0; i < shifts.abstractRatingContainers.length; ++i) {
							shifts.abstractRatingContainers[i].addOffset(0, offset1 - offset2);
					}
				}
				catch (RatingException e) {
					throw new VerticalDatumException(e);
				}
			}
			vdc.toNGVD29();
		}
		return change;
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.io.TableRatingContainer#toNAVD88()
	 */
	@Override
	public boolean toNAVD88() throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		boolean change = false;
		String[][] paramsAndUnits = getParamsAndUnits();
		String[] params = paramsAndUnits[0];
		String[] units  = paramsAndUnits[1];
		if (params[0].startsWith("Elev")) {
			if (!vdc.getCurrentVerticalDatum().equals("NAVD88")) {
				change = true;
				double offset1 = vdc.getNAVD88Offset(units[0]);
				double offset2 = vdc.getCurrentOffset(units[0]);
				try {
					addOffset(0, offset1 - offset2);
					if (offsets != null) {
						offsets.addOffset(0, offset1 - offset2);
					}
					if (shifts != null && shifts.abstractRatingContainers != null)
						for (int i = 0; i < shifts.abstractRatingContainers.length; ++i) {
							shifts.abstractRatingContainers[i].addOffset(0, offset1 - offset2);
					}
				}
				catch (RatingException e) {
					throw new VerticalDatumException(e);
				}
			}
			vdc.toNAVD88();
		}
		return change;
	}

	/**
	 *
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingXmlFactory#toXml(UsgsStreamTableRatingContainer, CharSequence, int) instead
	 */
	@Override
	public String toXml(CharSequence indent) {
		return toXml(indent, 0);
	}

	/**
	 *
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingXmlFactory#toXml(UsgsStreamTableRatingContainer, CharSequence, int) instead
	 */
	@Override
	public String toXml(CharSequence indent, int level) {
		RatingContainerXmlCompatUtil service = RatingContainerXmlCompatUtil.getInstance();
		return service.toXml(this, indent, level);
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.io.TableRatingContainer#addOffset(int, double)
	 */
	@Override
	public void addOffset(int paramNum, double offset) throws RatingException {
		super.addOffset(paramNum, offset);
		if (paramNum == 0) {
			if (offsets != null) {
				offsets.addOffset(paramNum, offset);
			}
			if (shifts != null) {
				shifts.addOffset(paramNum, offset);
			}
		}
	}
}
