/*
 * Copyright (c) 2021. Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved. HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 *
 */

package org.opendcs.ratings.io;

import org.opendcs.ratings.RatingObjectDoesNotExistException;
import java.util.Arrays;

/**
 * Container class for RatingSpec objects
 *
 * @author Mike Perryman
 */
public class RatingSpecContainer extends RatingTemplateContainer {
	/**
	 * The office associated with the specification
	 */
	public String specOfficeId;
	/**
	 * The rating specification identifier
	 */
	public String specId = null;
	/**
	 * The location portion of the specification identifier
	 */
	public String locationId = null;
	/**
	 * The specification version portion of the specification identifier
	 */
	public String specVersion = null;
	/**
	 * The agency that maintains the identified rating
	 */
	public String sourceAgencyId = null;
	/**
	 * The rating method for handling dates that lie between effective dates of the ratings
	 * @see org.opendcs.ratings.RatingConst.RatingMethod
	 */
	public String inRangeMethod = null;
	/**
	 * The rating method for handling dates that are earlier than the earliest effective date of the ratings
	 * @see org.opendcs.ratings.RatingConst.RatingMethod
	 */
	public String outRangeLowMethod = null;
	/**
	 * The rating method for handling dates that are later than the latest effective date of the ratings
	 * @see org.opendcs.ratings.RatingConst.RatingMethod
	 */
	public String outRangeHighMethod = null;
	/**
	 * Specifies whether ratings under this specification are active
	 */
	public boolean active = false;
	/**
	 * Specifies whether to automatically update ratings under this specification when new ratings are available
	 */
	public boolean autoUpdate = false;
	/**
	 * Specifies whether to automatically mark ratings under this specification as active when auto-updated
	 */
	public boolean autoActivate = false;
	/**
	 * Specifies whether to automatically apply any rating extensions for ratings under this specification when auto-updated
	 */
	public boolean autoMigrateExtensions = false;
	/**
	 * Usgs-style rounding specifications, one for each independent parameter.
	 * @see "hec.data.UsgsRounder.java in hec-monolith"
	 */
	public String[] indRoundingSpecs = null;
	/**
	 * Usgs-style rounding specification for the independent parameter.
	 * @see "hec.data.UsgsRounder.java in hec-monolith"
	 */
	public String depRoundingSpec = null;
	/**
	 * The description of this rating specification
	 */
	public String specDescription = null;
	/**
	 * Public empty constructor
	 */
	public RatingSpecContainer() {}
	/**
	 * Public constructor from an XML snippet
	 * @param xmlStr The XML snippet
	 * @throws RatingObjectDoesNotExistException
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingSpecXmlFactory#ratingSpecContainer(String) instead
	 */
	@Deprecated
	public RatingSpecContainer(String xmlStr) throws RatingObjectDoesNotExistException {
		populateFromXml(xmlStr);
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
					if (!super.equals(obj)) break;
					RatingSpecContainer other = (RatingSpecContainer)obj;
					if ((other.specId == null) != (specId == null)) break;
					if (specId != null) {
						if (!other.specId.equals(specId)) break;
					}
					if ((other.officeId == null) != (officeId == null)) break;
					if (officeId != null) {
						if (!other.officeId.equals(officeId)) break;
					}
					if ((other.specVersion == null) != (specVersion == null)) break;
					if (specVersion != null) {
						if (!other.specVersion.equals(specVersion)) break;
					}
					if ((other.sourceAgencyId == null) != (sourceAgencyId == null)) break;
					if (sourceAgencyId != null) {
						if (!other.sourceAgencyId.equals(sourceAgencyId)) break;
					}
					if ((other.inRangeMethod == null) != (inRangeMethod == null)) break;
					if (inRangeMethod != null) {
						if (!other.inRangeMethod.equalsIgnoreCase(inRangeMethod)) break;
					}
					if ((other.outRangeLowMethod == null) != (outRangeLowMethod == null)) break;
					if (outRangeLowMethod != null) {
						if (!other.outRangeLowMethod.equalsIgnoreCase(outRangeLowMethod)) break;
					}
					if ((other.outRangeHighMethod == null) != (outRangeHighMethod == null)) break;
					if (outRangeHighMethod != null) {
						if (!other.outRangeHighMethod.equalsIgnoreCase(outRangeHighMethod)) break;
					}
					if (other.active != active) break;
					if (other.autoUpdate != autoUpdate) break;
					if (other.autoActivate != autoActivate) break;
					if (other.autoMigrateExtensions != autoMigrateExtensions) break;
					if ((other.indRoundingSpecs == null) != (indRoundingSpecs == null)) break;
					if (indRoundingSpecs != null) {
						if (other.indRoundingSpecs.length != indRoundingSpecs.length) break;
						for (int i = 0; i < indRoundingSpecs.length; ++i) {
							if ((other.indRoundingSpecs[i] == null) != (indRoundingSpecs[i] == null)) break test;
							if (indRoundingSpecs[i] != null) {
								if (!other.indRoundingSpecs[i].equals(indRoundingSpecs[i])) break test;
							}
						}
					}
					if (!other.depRoundingSpec.equals(depRoundingSpec)) break;
					result = true;
				} while (false);
		}
		return result;
	}
	/* (non-Javadoc)
	 * @see org.opendcs.ratings.io.RatingTemplateContainer#hashCode()
	 */
	@Override
	public int hashCode() {
		int hashCode = getClass().getName().hashCode()
				+ super.hashCode()
				+  3 * (specId == null ? 1 : specId.hashCode())
				+  5 * (officeId == null ? 1 : officeId.hashCode())
				+  7 * (specVersion == null ? 1 : specVersion.hashCode())
				+ 11 * (sourceAgencyId == null ? 1 : sourceAgencyId.hashCode())
				+ 13 * (inRangeMethod == null ? 1 : inRangeMethod.hashCode())
				+ 17 * (outRangeLowMethod == null ? 1 : outRangeLowMethod.hashCode())
				+ 19 * (outRangeHighMethod == null ? 1 : outRangeHighMethod.hashCode())
				+ 23 * (active ? 3 : 7)
				+ 29 * (autoUpdate ? 3 : 7)
				+ 31 * (autoActivate ? 3 : 7)
				+ 37 * (autoMigrateExtensions ? 3 : 7)
				+ 41 * (depRoundingSpec == null ? 1 : depRoundingSpec.hashCode());
		if (indRoundingSpecs == null) {
			hashCode += 43;
		}
		else {
			hashCode += 47 * indRoundingSpecs.length;
			for (int i = 0; i < indRoundingSpecs.length; ++i) {
				hashCode += 53 * (indRoundingSpecs[i] == null ? i+1 : indRoundingSpecs[i].hashCode());
			}
		}
		return hashCode;
	}
	/**
	 * Copies the data from this object into the specified RatingSpecContainer
	 * @param other The RatingSpecContainer object to receive the copy
	 */
	public void clone(RatingSpecContainer other) {
		super.clone(other);
		other.specId = specId;
		other.officeId = officeId;
		other.locationId = locationId;
		other.specVersion = specVersion;
		other.sourceAgencyId = sourceAgencyId;
		other.inRangeMethod = inRangeMethod;
		other.outRangeLowMethod = outRangeLowMethod;
		other.outRangeHighMethod = outRangeHighMethod;
		other.active = active;
		other.autoUpdate = autoUpdate;
		other.autoActivate = autoActivate;
		other.autoMigrateExtensions = autoMigrateExtensions;
		other.indRoundingSpecs = Arrays.copyOf(indRoundingSpecs, indRoundingSpecs.length);
		other.depRoundingSpec = depRoundingSpec;
		other.specDescription = specDescription;
	}
	/**
	 * Returns a new RatingSpecContainer object cloned from this one
	 */
	public RatingSpecContainer clone() {
		RatingSpecContainer other = new RatingSpecContainer();
		clone(other);
		return other;
	}
	/**
	 * Populates the RatingSpecContainer from the first &lt;rating-spec&gt; element in an XML string or null if no such element is found.
	 * @param xmlStr The XML string
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingSpecXmlFactory#ratingSpecContainer(String) instead
	 */
	@Deprecated
	public void populateFromXml(String xmlStr) throws RatingObjectDoesNotExistException {
		RatingContainerXmlCompatUtil service = RatingContainerXmlCompatUtil.getInstance();
		RatingSpecContainer ratingSpecContainer = service.createRatingSpecContainer(xmlStr);
		ratingSpecContainer.clone(this);
	}
	/**
	 * Generates an XML string (template and spec) from this object
	 * @param indent The amount to indent each level (initial leve = 0)
	 * @return the generated XML
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingSpecXmlFactory#toXml(RatingSpecContainer, CharSequence, int, boolean) instead
	 */
	@Deprecated
	public String toXml(CharSequence indent) {
		return toXml(indent, 0);
	}

	/**
	 * Generates an XML string (template and spec) from this object
	 * @param indent The amount to indent each level
	 * @param level The initial level of indentation
	 * @return the generated XML
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingSpecXmlFactory#toXml(RatingSpecContainer, CharSequence, int, boolean) instead
	 */
	@Deprecated
	public String toXml(CharSequence indent, int level, boolean includeTemplate) {
		RatingContainerXmlCompatUtil service = RatingContainerXmlCompatUtil.getInstance();
		return service.toXml(this, indent, level, includeTemplate);
	}

	/**
	 *
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingSpecXmlFactory#toXml(RatingSpecContainer, CharSequence, int, boolean) instead
	 */
	@Deprecated
	public String toXml(CharSequence indent, int level) {
		return toXml(indent, level, true);
	}

	/**
	 * Generates a specification XML string from this object
	 * @param indent The amount to indent each level (initial level = 0)
	 * @return the generated XML
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingSpecXmlFactory#toXml(RatingSpecContainer, CharSequence, int, boolean) instead
	 */
	@Deprecated
	public String toSpecXml(CharSequence indent) {
		return toSpecXml(indent, 0);
	}

	/**
	 * Generates a specification XML string from this object
	 * @param indent The amount to indent each level
	 * @param level The initial level of indentation
	 * @return the generated XML
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingSpecXmlFactory#toXml(RatingSpecContainer, CharSequence, int, boolean) instead
	 */
	@Deprecated
	public String toSpecXml(CharSequence indent, int level) {
		RatingContainerXmlCompatUtil service = RatingContainerXmlCompatUtil.getInstance();
		return service.toSpecXml(this, indent, level);
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		try {
			return String.format("%s/%s", specOfficeId, specId);
		}
		catch (Throwable t) {
			return ((Object)this).toString();
		}
	}

}
