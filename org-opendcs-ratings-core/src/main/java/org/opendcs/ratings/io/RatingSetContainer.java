/*
 * Copyright (c) 2021. Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved. HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 *
 */

package org.opendcs.ratings.io;


import hec.io.DataContainer;
import hec.io.DataContainerTransformer;
import hec.io.HecIoException;
import mil.army.usace.hec.metadata.VerticalDatum;
import mil.army.usace.hec.metadata.VerticalDatumContainer;
import mil.army.usace.hec.metadata.VerticalDatumException;
import org.opendcs.ratings.RatingException;
import org.opendcs.ratings.RatingSetFactory;
import org.w3c.dom.Element;

/**
 * Data container class for RatingSet
 * @author Mike Perryman
 */
public class RatingSetContainer implements VerticalDatum, DataContainerTransformer
{
	/**
	 * Contains the rating specification
	 */
	public RatingSpecContainer ratingSpecContainer = null;
	/**
	 * Contains the individual ratings
	 */
	public AbstractRatingContainer[] abstractRatingContainers = null;

	public RatingSetStateContainer state = null;
	/**
	 * Public empty Constructor
	 */
	public RatingSetContainer() {}
	/**
	 * Public constructor from a DOM Element.
	 * @param ratingElement The DOM Element. The document (root) node is expected to be
	 *        &lt;ratings&gt;, which is expected to have one or more &lt;rating&gt; or &lt;usgs-stream-rating&gt; child nodes, all of the same
	 *        rating specification.  Appropriate &lt;rating-template&gt; and &lt;rating-spec&gt; nodes are required for the rating set;
	 *        any other template and specification nodes are ignored.
	 * @deprecated use mil.army.usace.hec.cwms.rating.io.xml.RatingSetXmlParser#createRatingSetContainerFromXml(String) instead
	 *
	 */
	@Deprecated
	public RatingSetContainer(Element ratingElement) throws RatingException {
		populateFromXml(ratingElement);
	}
	/**
	 * Public constructor from an XML instance.
	 * @param xmlText The XML instance to construct the RatingSet object from. The document (root) node is expected to be
	 *        &lt;ratings&gt;, which is expected to have one or more &lt;rating&gt; or &lt;usgs-stream-rating&gt; child nodes, all of the same
	 *        rating specification.  Appropriate &lt;rating-template&gt; and &lt;rating-spec&gt; nodes are required for the rating set;
	 *        any other template and specification nodes are ignored.
	 * @throws RatingException on error
	 * @deprecated use mil.army.usace.hec.cwms.rating.io.xml.RatingSetXmlParser#createRatingSetContainerFromXml(String) instead
	 */
	@Deprecated
	public RatingSetContainer(String xmlText) throws RatingException {
		populateFromXml(xmlText);
	}
	/**
	 * Populates this RatingSetContainer object from a DOM Element.
	 * @param ratingElement The DOM Element. The document (root) node is expected to be
	 *        &lt;ratings&gt;, which is expected to have one or more &lt;rating&gt; or &lt;usgs-stream-rating&gt; child nodes, all of the same
	 *        rating specification.  Appropriate &lt;rating-template&gt; and &lt;rating-spec&gt; nodes are required for the rating set;
	 *        any other template and specification nodes are ignored.
	 * @deprecated use mil.army.usace.hec.cwms.rating.io.xml.RatingSetXmlParser#createRatingSetContainerFromXml(String) instead
	 */
	@Deprecated
	public void populateFromXml(Element ratingElement) throws RatingException {
		RatingContainerXmlCompatUtil service = RatingContainerXmlCompatUtil.getInstance();
		RatingSetContainer rsc = service.createRatingSetContainer(ratingElement);
		ratingSpecContainer = rsc.ratingSpecContainer;
		abstractRatingContainers = rsc.abstractRatingContainers;
	}
	/**
	 * Populates this RatingSetContainer object from an XML instance.
	 * @param xmlText The XML instance to construct the RatingSet object from. The document (root) node is expected to be
	 *        &lt;ratings&gt;, which is expected to have one or more &lt;rating&gt; or &lt;usgs-stream-rating&gt; child nodes, all of the same
	 *        rating specification.  Appropriate &lt;rating-template&gt; and &lt;rating-spec&gt; nodes are required for the rating set;
	 *        any other template and specification nodes are ignored.
	 * @throws RatingException on error
	 * @deprecated use mil.army.usace.hec.cwms.rating.io.xml.RatingSetXmlParser#createRatingSetContainerFromXml(String) instead
	 */
	@Deprecated
	public void populateFromXml(String xmlText) throws RatingException {
		RatingContainerXmlCompatUtil service = RatingContainerXmlCompatUtil.getInstance();
		RatingSetContainer rsc = service.createRatingSetContainer(xmlText);
		ratingSpecContainer = rsc.ratingSpecContainer;
		abstractRatingContainers = rsc.abstractRatingContainers;
	}

	/**
	 * Creates another RatingSetContainer object and fills it with with data from this one
	 */
	public RatingSetContainer clone() {
		RatingSetContainer rsc  = abstractRatingContainers == null ? new ReferenceRatingContainer() : new RatingSetContainer();
		if (state != null) {
			rsc.state = (RatingSetStateContainer)state.clone();
		}
		rsc.ratingSpecContainer = ratingSpecContainer == null ? null : ratingSpecContainer.clone();
		if (abstractRatingContainers != null) {
			rsc.abstractRatingContainers = new AbstractRatingContainer[abstractRatingContainers.length];
			for (int i = 0; i < abstractRatingContainers.length; ++i) {
				rsc.abstractRatingContainers[i] = abstractRatingContainers[i].clone();
			}
		}
		return rsc;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		try {
			return String.format(
					"%s - %d ratings",
					ratingSpecContainer == null ? "<NULL>" : ratingSpecContainer.toString(),
					abstractRatingContainers == null ? 0 : abstractRatingContainers.length);
		}
		catch (Throwable t) {
			return this.toString();
		}
	}

	/**
	 *
	 * @return xml representation of this RatingSetContainer
	 * @deprecated use mil.army.usace.hec.cwms.rating.io.xml.RatingSetXmlParser#toXml(RatingSetContainer, CharSequence, int, boolean, boolean) instead
	 */
	@Deprecated
	public String toXml(CharSequence indent) {
		return toXml(indent, 0);
	}

	/**
	 *
	 * @return xml representation of this RatingSetContainer
	 * @deprecated use mil.army.usace.hec.cwms.rating.io.xml.RatingSetXmlParser#toXml(RatingSetContainer, CharSequence, int, boolean, boolean) instead
	 */
	@Deprecated
	public String toXml(CharSequence indent, int level) {
		return toXml(indent, level, true);
	}

	/**
	 *
	 * @return xml representation of this RatingSetContainer
	 * @deprecated use mil.army.usace.hec.cwms.rating.io.xml.RatingSetXmlParser#toXml(RatingSetContainer, CharSequence, int, boolean, boolean) instead
	 */
	@Deprecated
	public String toXml(CharSequence indent, int level, boolean includeTemplate) {
		return toXml(indent, level, includeTemplate, true);
	}

	/**
	 *
	 * @return xml representation of this RatingSetContainer
	 * @deprecated use mil.army.usace.hec.cwms.rating.io.xml.RatingSetXmlParser#toXml(RatingSetContainer, CharSequence, int, boolean, boolean) instead
	 */
	@Deprecated
	public String toXml(CharSequence indent, int level, boolean includeTemplate, boolean includeEmptyTableRatings) {
		return RatingContainerXmlCompatUtil.getInstance().toXml(this, indent, level, includeTemplate, includeEmptyTableRatings);
	}
	/**
	 * Add the specified offset to the values of the specified parameter.
	 * @param paramNum Specifies which parameter to add the offset to. 0, 1 = first, second independent, etc... -1 = dependent parameter
	 * @param offset The offset to add
	 * @throws RatingException if not implemented for a specific rating container type or if paramNum is invalid for this container
	 */
	public void addOffset(int paramNum, double offset) throws RatingException {
        for (AbstractRatingContainer abstractRatingContainer : abstractRatingContainers) {
            abstractRatingContainer.addOffset(paramNum, offset);
        }
	}
	/**
	 * Returns whether this object has any vertical datum info
	 * @return whether this object has any vertical datum info
	 */
	public boolean hasVerticalDatum() {
		boolean hasVerticalDatum = false;
		if (abstractRatingContainers != null) {
            for (AbstractRatingContainer abstractRatingContainer : abstractRatingContainers) {
                if (abstractRatingContainer.hasVerticalDatum()) {
                    hasVerticalDatum = true;
                    break;
                }
            }
		}
		return hasVerticalDatum;
	}
	/**
	 * Returns whether any included vertical datum info is consistent
	 * @return true if there are zero or one vertical datum containers, or if multiple vertical
	 * datum container specify the same information and false if there are multiple vertical datum
	 * containers with different information
	 */
	public boolean isVerticalDatumInfoConsistent() {
		boolean isConsistent = true;
		if (abstractRatingContainers != null && abstractRatingContainers.length > 1) {
			VerticalDatumContainer vdc = null;
            for (AbstractRatingContainer abstractRatingContainer : abstractRatingContainers) {
                if (abstractRatingContainer.vdc != null) {
                    if (vdc == null) {
                        vdc = abstractRatingContainer.vdc;
                    } else {
                        if (!abstractRatingContainer.vdc.equals(vdc)) {
                            isConsistent = false;
                            break;
                        }
                    }
                }
            }
		}
		return isConsistent;
	}
	/**
	 * Ensures that every rating has the same vertical datum info (possibly none)
	 * @throws VerticalDatumException if multiple vertical datums are encountered
	 */
	public VerticalDatumContainer normalizeVerticalDatumInfo() throws VerticalDatumException {
		VerticalDatumContainer vdc = null;
		if (abstractRatingContainers != null && abstractRatingContainers.length > 1) {

            for (AbstractRatingContainer abstractRatingContainer : abstractRatingContainers) {
                if (abstractRatingContainer.vdc != null) {
                    vdc = abstractRatingContainer.vdc;
                    break;
                }
            }
            for (AbstractRatingContainer abstractRatingContainer : abstractRatingContainers) {
                if (abstractRatingContainer.vdc == null) {
                    if (vdc != null) {
                        throw new VerticalDatumException("Object contains multiple vertical datums");
                    }
                } else if (!abstractRatingContainer.vdc.equals(vdc)) {
                    throw new VerticalDatumException("Object contains multiple vertical datums");
                }
            }
		}
		return vdc;
	}
	/* (non-Javadoc)
	 * @see mil.army.usace.hec.metadata.VerticalDatum#getNativeVerticalDatum()
	 */
	@Override
	public String getNativeVerticalDatum() throws VerticalDatumException {
		normalizeVerticalDatumInfo();
		if (abstractRatingContainers == null || abstractRatingContainers.length == 0) {
			throw new VerticalDatumException("Object does not have vertical datum information");
		}
		return abstractRatingContainers[0].getNativeVerticalDatum();
	}
	/* (non-Javadoc)
	 * @see mil.army.usace.hec.metadata.VerticalDatum#getCurrentVerticalDatum()
	 */
	@Override
	public String getCurrentVerticalDatum() throws VerticalDatumException {
		normalizeVerticalDatumInfo();
		if (abstractRatingContainers == null || abstractRatingContainers.length == 0) {
			throw new VerticalDatumException("Object does not have vertical datum information");
		}
		return abstractRatingContainers[0].getCurrentVerticalDatum();
	}
	/* (non-Javadoc)
	 * @see mil.army.usace.hec.metadata.VerticalDatum#isCurrentVerticalDatumEstimated()
	 */
	@Override
	public boolean isCurrentVerticalDatumEstimated() throws VerticalDatumException {
		normalizeVerticalDatumInfo();
		if (abstractRatingContainers == null || abstractRatingContainers.length == 0) {
			throw new VerticalDatumException("Object does not have vertical datum information");
		}
		return abstractRatingContainers[0].isCurrentVerticalDatumEstimated();
	}
	/* (non-Javadoc)
	 * @see mil.army.usace.hec.metadata.VerticalDatum#toNativeVerticalDatum()
	 */
	@Override
	public boolean toNativeVerticalDatum() throws VerticalDatumException {
		normalizeVerticalDatumInfo();
		if (abstractRatingContainers == null || abstractRatingContainers.length == 0) {
			throw new VerticalDatumException("Object does not have vertical datum information");
		}
		boolean change = false;
        for (AbstractRatingContainer abstractRatingContainer : abstractRatingContainers) {
            if (abstractRatingContainer.toNativeVerticalDatum()) {
                change = true;
            }
        }
		return change;
	}
	/* (non-Javadoc)
	 * @see mil.army.usace.hec.metadata.VerticalDatum#toNGVD29()
	 */
	@Override
	public boolean toNGVD29() throws VerticalDatumException {
		normalizeVerticalDatumInfo();
		if (abstractRatingContainers == null || abstractRatingContainers.length == 0) {
			throw new VerticalDatumException("Object does not have vertical datum information");
		}
		boolean change = false;
        for (AbstractRatingContainer abstractRatingContainer : abstractRatingContainers) {
            if (abstractRatingContainer.toNGVD29()) {
                change = true;
            }
        }
		return change;
	}
	/* (non-Javadoc)
	 * @see mil.army.usace.hec.metadata.VerticalDatum#toNAVD88()
	 */
	@Override
	public boolean toNAVD88() throws VerticalDatumException {
		normalizeVerticalDatumInfo();
		if (abstractRatingContainers == null || abstractRatingContainers.length == 0) {
			throw new VerticalDatumException("Object does not have vertical datum information");
		}
		boolean change = false;
        for (AbstractRatingContainer abstractRatingContainer : abstractRatingContainers) {
            if (abstractRatingContainer.toNAVD88()) {
                change = true;
            }
        }
		return change;
	}
	/* (non-Javadoc)
	 * @see mil.army.usace.hec.metadata.VerticalDatum#toVerticalDatum(java.lang.String)
	 */
	@Override
	public boolean toVerticalDatum(String datum) throws VerticalDatumException {
		normalizeVerticalDatumInfo();
		if (abstractRatingContainers == null || abstractRatingContainers.length == 0) {
			throw new VerticalDatumException("Object does not have vertical datum information");
		}
		boolean change = false;
        for (AbstractRatingContainer abstractRatingContainer : abstractRatingContainers) {
            if (abstractRatingContainer.toVerticalDatum(datum)) {
                change = true;
            }
        }
		return change;
	}
	/* (non-Javadoc)
	 * @see mil.army.usace.hec.metadata.VerticalDatum#forceVerticalDatum(java.lang.String)
	 */
	@Override
	public boolean forceVerticalDatum(String datum) throws VerticalDatumException {
		normalizeVerticalDatumInfo();
		if (abstractRatingContainers == null || abstractRatingContainers.length == 0) {
			throw new VerticalDatumException("Object does not have vertical datum information");
		}
		boolean change = false;
        for (AbstractRatingContainer abstractRatingContainer : abstractRatingContainers) {
            if (abstractRatingContainer.forceVerticalDatum(datum)) {
                change = true;
            }
        }
		return change;
	}
	/* (non-Javadoc)
	 * @see mil.army.usace.hec.metadata.VerticalDatum#getCurrentOffset()
	 */
	@Override
	public double getCurrentOffset() throws VerticalDatumException {
		normalizeVerticalDatumInfo();
		if (abstractRatingContainers == null || abstractRatingContainers.length == 0) {
			throw new VerticalDatumException("Object does not have vertical datum information");
		}
		return abstractRatingContainers[0].getCurrentOffset();
	}
	/* (non-Javadoc)
	 * @see mil.army.usace.hec.metadata.VerticalDatum#getCurrentOffset(java.lang.String)
	 */
	@Override
	public double getCurrentOffset(String unit) throws VerticalDatumException {
		normalizeVerticalDatumInfo();
		if (abstractRatingContainers == null || abstractRatingContainers.length == 0) {
			throw new VerticalDatumException("Object does not have vertical datum information");
		}
		return abstractRatingContainers[0].getCurrentOffset(unit);
	}
	/* (non-Javadoc)
	 * @see mil.army.usace.hec.metadata.VerticalDatum#getNGVD29Offset()
	 */
	@Override
	public double getNGVD29Offset() throws VerticalDatumException {
		normalizeVerticalDatumInfo();
		if (abstractRatingContainers == null || abstractRatingContainers.length == 0) {
			throw new VerticalDatumException("Object does not have vertical datum information");
		}
		return abstractRatingContainers[0].getNGVD29Offset();
	}
	/* (non-Javadoc)
	 * @see mil.army.usace.hec.metadata.VerticalDatum#getNGVD29Offset(java.lang.String)
	 */
	@Override
	public double getNGVD29Offset(String unit) throws VerticalDatumException {
		normalizeVerticalDatumInfo();
		if (abstractRatingContainers == null || abstractRatingContainers.length == 0) {
			throw new VerticalDatumException("Object does not have vertical datum information");
		}
		return abstractRatingContainers[0].getNGVD29Offset(unit);
	}
	/* (non-Javadoc)
	 * @see mil.army.usace.hec.metadata.VerticalDatum#getNAVD88Offset()
	 */
	@Override
	public double getNAVD88Offset() throws VerticalDatumException {
		normalizeVerticalDatumInfo();
		if (abstractRatingContainers == null || abstractRatingContainers.length == 0) {
			throw new VerticalDatumException("Object does not have vertical datum information");
		}
		return abstractRatingContainers[0].getNAVD88Offset();
	}
	/* (non-Javadoc)
	 * @see mil.army.usace.hec.metadata.VerticalDatum#getNAVD88Offset(java.lang.String)
	 */
	@Override
	public double getNAVD88Offset(String unit) throws VerticalDatumException {
		normalizeVerticalDatumInfo();
		if (abstractRatingContainers == null || abstractRatingContainers.length == 0) {
			throw new VerticalDatumException("Object does not have vertical datum information");
		}
		return abstractRatingContainers[0].getNAVD88Offset(unit);
	}
	/* (non-Javadoc)
	 * @see mil.army.usace.hec.metadata.VerticalDatum#isNGVD29OffsetEstimated()
	 */
	@Override
	public boolean isNGVD29OffsetEstimated() throws VerticalDatumException {
		normalizeVerticalDatumInfo();
		if (abstractRatingContainers == null || abstractRatingContainers.length == 0) {
			throw new VerticalDatumException("Object does not have vertical datum information");
		}
		return abstractRatingContainers[0].isNGVD29OffsetEstimated();
	}
	/* (non-Javadoc)
	 * @see mil.army.usace.hec.metadata.VerticalDatum#isNAVD88OffsetEstimated()
	 */
	@Override
	public boolean isNAVD88OffsetEstimated() throws VerticalDatumException {
		normalizeVerticalDatumInfo();
		if (abstractRatingContainers == null || abstractRatingContainers.length == 0) {
			throw new VerticalDatumException("Object does not have vertical datum information");
		}
		return abstractRatingContainers[0].isNAVD88OffsetEstimated();
	}
	/* (non-Javadoc)
	 * @see mil.army.usace.hec.metadata.VerticalDatum#getVerticalDatumInfo()
	 */
	@Override
	public String getVerticalDatumInfo() throws VerticalDatumException {
		normalizeVerticalDatumInfo();
		if (abstractRatingContainers == null || abstractRatingContainers.length == 0) {
			throw new VerticalDatumException("Object does not have vertical datum information");
		}
		return abstractRatingContainers[0].getVerticalDatumInfo();
	}
	/* (non-Javadoc)
	 * @see mil.army.usace.hec.metadata.VerticalDatum#setVerticalDatumInfo(java.lang.String)
	 */
	@Override
	public void setVerticalDatumInfo(String xmlStr) throws VerticalDatumException {
		if (abstractRatingContainers == null || abstractRatingContainers.length == 0) {
			throw new VerticalDatumException("Object does not have vertical datum information");
		}
        for (AbstractRatingContainer abstractRatingContainer : abstractRatingContainers) {
            abstractRatingContainer.setVerticalDatumInfo(xmlStr);
        }
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		boolean result = obj == this;
		if (!result) {
			test:
				do {
					if (obj == null || obj.getClass() != getClass()) break;
					RatingSetContainer other = (RatingSetContainer)obj;
					if ((other.ratingSpecContainer == null) != (ratingSpecContainer == null)) break;
					if (ratingSpecContainer != null) {
						if (!other.ratingSpecContainer.equals(ratingSpecContainer)) break;
					}
					if ((other.abstractRatingContainers == null) != (abstractRatingContainers == null)) break;
					if (abstractRatingContainers != null) {
						if (other.abstractRatingContainers.length != abstractRatingContainers.length) break;
						for (int i = 0; i < abstractRatingContainers.length; ++i) {
							if ((abstractRatingContainers[i] == null) != (other.abstractRatingContainers[i] == null)) break test;
							if (abstractRatingContainers[i] != null) {
								if (!abstractRatingContainers[i].equals(other.abstractRatingContainers[i])) break test;
							}
						}
					}
					result = true;
				} while (false);
		}
		return result;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		int hashCode = getClass().getName().hashCode() + (ratingSpecContainer == null ? 3 : ratingSpecContainer.hashCode());
		if (abstractRatingContainers == null) {
			hashCode += 3;
		}
		else {
			hashCode += 5 * abstractRatingContainers.length;
			for (int i = 0; i < abstractRatingContainers.length; ++i) {
				hashCode += 7 * (abstractRatingContainers[i] == null ? i+1 : abstractRatingContainers[i].hashCode());
			}
		}
		if (state != null) hashCode += state.hashCode();
		return hashCode;
	}

	/**
	 * Returns the first vertical datum container found in the array of abstract rating containers.
	 *
	 * @return NULL*
	 */
	@Override
	public VerticalDatumContainer getVerticalDatumContainer()
	{
		VerticalDatumContainer retval = null;
		for(AbstractRatingContainer ratingContainer : abstractRatingContainers)
		{
			if(ratingContainer.hasVerticalDatum())
			{
				retval = ratingContainer.getVerticalDatumContainer();
				break;
			}
		}

		return retval;
	}

	/**
	 * Sets the Vertical Datum Container on all abstract rating containers.
	 *
	 * @param vdc The Vertical Datum Container to set the rating containers to
	 */
	public void setVerticalDatumContainer(VerticalDatumContainer vdc) throws VerticalDatumException
	{
		if (abstractRatingContainers == null || abstractRatingContainers.length == 0) {
			throw new VerticalDatumException("Object does not have vertical datum information");
		}
        for (AbstractRatingContainer abstractRatingContainer : abstractRatingContainers) {
            abstractRatingContainer.setVerticalDatumContainer(vdc);
        }
	}


	@Override
	public DataContainer toDataContainer() throws HecIoException
	{
		try {
			return RatingSetFactory.ratingSet(this).getDssData();
		}
		catch(RatingException e) {
			throw new HecIoException(e);
		}
	}
}
