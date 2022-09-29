/*
 * Copyright (c) 2021. Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved. HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 *
 */

package hec.data.cwmsRating.io;

import hec.data.RatingObjectDoesNotExistException;
import java.util.Arrays;

/**
 * Container class for RatingTemplate objects
 *
 * @author Mike Perryman
 */
public class RatingTemplateContainer {
	/**
	 * The office associated with the template
	 */
	public String officeId = null;
	/**
	 * The text identifier of the rating template
	 */
	public String templateId = null;
	/**
	 * The parameters portion of the template identifier
	 */
	public String parametersId = null;
	/**
	 * The independent parameters of the rating template
	 */
	public String[] indParams = null;
	/**
	 * The dependent parameter of the rating template
	 */
	public String depParam = null;
	/**
	 * The version portion of the template identifier
	 */
	public String templateVersion = null;
	/**
	 * The rating method for handling independent parameter values that lie between values in the rating tables, one for each independent parameter
	 * @see hec.data.cwmsRating.RatingConst.RatingMethod
	 */
	public String[] inRangeMethods = null;
	/**
	 * The rating method for handling independent parameter values that are less than the least values in the rating tables, one for each independent parameter
	 * @see hec.data.cwmsRating.RatingConst.RatingMethod
	 */
	public String[] outRangeLowMethods = null;
	/**
	 * The rating method for handling independent parameter values that are greater than the greatest values in the rating tables, one for each independent parameter
	 * @see hec.data.cwmsRating.RatingConst.RatingMethod
	 */
	public String[] outRangeHighMethods = null;
	/**
	 * The description of the rating template
	 */
	public String templateDescription = null;
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
					RatingTemplateContainer other = (RatingTemplateContainer)obj;
					if ((other.officeId == null) != (officeId == null)) break;
					if (officeId != null) {
						if (!other.officeId.equals(officeId)) break;
					}
					if ((other.templateId == null) != (templateId == null)) break;
					if (templateId != null) {
						if (!other.templateId.equals(templateId)) break;
					}
					if ((other.parametersId == null) != (parametersId == null)) break;
					if (parametersId != null) {
						if (!other.parametersId.equals(parametersId)) break;
					}
					if ((other.indParams == null) != (indParams == null)) break;
					if (indParams != null) {
						if (other.indParams.length != indParams.length) break;
						for (int i = 0; i < indParams.length; ++i) {
							if ((other.indParams[i] == null) != (indParams[i] == null)) break test;
							if (indParams[i] != null) {
								if (!other.indParams[i].equals(indParams[i])) break test;;
							}
						}
					}
					if ((other.depParam == null) != (depParam == null)) break;
					if (depParam != null) {
						if (!other.depParam.equals(depParam)) break;
					}
					if ((other.templateVersion == null) != (templateVersion == null)) break;
					if (templateVersion != null) {
						if (!other.templateVersion.equals(templateVersion)) break;
					}
					if ((other.inRangeMethods == null) != (inRangeMethods == null)) break;
					if (inRangeMethods != null) {
						if (other.inRangeMethods.length != inRangeMethods.length) break;
						for (int i = 0; i < inRangeMethods.length; ++i) {
							if ((other.inRangeMethods[i] == null) != (inRangeMethods[i] == null)) break test;
							if (inRangeMethods[i] != null) {
								if (!other.inRangeMethods[i].equalsIgnoreCase(inRangeMethods[i])) break test;
							}
						}
					}
					if ((other.outRangeLowMethods == null) != (outRangeLowMethods == null)) break;
					if (outRangeLowMethods != null) {
						if (other.outRangeLowMethods.length != outRangeLowMethods.length) break;
						for (int i = 0; i < outRangeLowMethods.length; ++i) {
							if ((other.outRangeLowMethods[i] == null) != (outRangeLowMethods[i] == null)) break test;
							if (outRangeLowMethods[i] != null) {
								if (!other.outRangeLowMethods[i].equalsIgnoreCase(outRangeLowMethods[i])) break test;
							}
						}
					}
					if ((other.outRangeHighMethods == null) != (outRangeHighMethods == null)) break;
					if (outRangeHighMethods != null) {
						if (other.outRangeHighMethods.length != outRangeHighMethods.length) break;
						for (int i = 0; i < outRangeHighMethods.length; ++i) {
							if ((other.outRangeHighMethods[i] == null) != (outRangeHighMethods[i] == null)) break test;
							if (outRangeHighMethods[i] != null) {
								if (!other.outRangeHighMethods[i].equalsIgnoreCase(outRangeHighMethods[i])) break test;
							}
						}
					}
					result = true;
				} while(false);
		}
		return result;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		int hashCode = getClass().getName().hashCode()
				+  3 * (officeId == null ? 1 : officeId.hashCode())
				+  5 * (templateId == null ? 1 : templateId.hashCode())
				+  7 * (parametersId == null ? 1 : parametersId.hashCode())
				+ 11 * (depParam == null ? 1 : depParam.hashCode())
				+ 13 * (templateVersion == null ? 1 : templateVersion.hashCode());
		if (indParams == null) {
			hashCode += 19;
		}
		else {
			hashCode += 23 * indParams.length;
			for (int i = 0; i < indParams.length; ++i) {
				hashCode += 27 * (indParams[i] == null ? i+1 : indParams[i].hashCode());
			}
		}
		if (inRangeMethods == null) {
			hashCode += 31;
		}
		else {
			hashCode += 37 * inRangeMethods.length;
			for (int i = 0; i < inRangeMethods.length; ++i) {
				hashCode += 41 * (inRangeMethods[i] == null ? i+1 : inRangeMethods[i].hashCode());
			}
		}
		if (outRangeLowMethods == null) {
			hashCode += 43;
		}
		else {
			hashCode = 47 * outRangeLowMethods.length;
			for (int i = 0; i < outRangeLowMethods.length; ++i) {
				hashCode += 53 * (outRangeLowMethods[i] == null ? i+1 : outRangeLowMethods[i].hashCode());
			}
		}
		if (outRangeHighMethods == null) {
			hashCode += 59;
		}
		else {
			hashCode += 61 * outRangeHighMethods.length;
			for (int i = 0; i < outRangeHighMethods.length; ++i) {
				hashCode += 67 * (outRangeHighMethods[i] == null ? i+1 : outRangeHighMethods[i].hashCode());
			}
		}
		return hashCode;
	}
	/**
	 * Copies the data from this object into the specified RatingTemplateContainer
	 * @param other The RatingTemplateContainer object to receive the copy
	 */
	public void clone(RatingTemplateContainer other) {
		other.officeId = officeId;
		other.templateId = templateId;
		other.parametersId = parametersId;
		other.indParams = indParams == null ? null : Arrays.copyOf(indParams, indParams.length);
		other.depParam = depParam;
		other.templateVersion = templateVersion;
		other.inRangeMethods = inRangeMethods == null ? null : Arrays.copyOf(inRangeMethods, inRangeMethods.length);
		other.outRangeLowMethods = outRangeLowMethods == null ? null : Arrays.copyOf(outRangeLowMethods, outRangeLowMethods.length);
		other.outRangeHighMethods = outRangeHighMethods == null ? null : Arrays.copyOf(outRangeHighMethods, outRangeHighMethods.length);
		other.templateDescription = templateDescription;
	}
	/**
	 * Public empty constructor
	 */
	public RatingTemplateContainer() {}
	/**
	 * Public constructor from an XML snippet
	 * @param xmlStr
	 * @throws RatingObjectDoesNotExistException
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingSpecXmlFactory#toXml(RatingTemplateContainer, CharSequence, int) instead
	 */
	@Deprecated
	public RatingTemplateContainer(String xmlStr) throws RatingObjectDoesNotExistException {
		populateFromXml(xmlStr);
	}

	/**
	 * Populates a RatingTemplateContainer from the first &lt;rating-template&gt; element in an XML string or null if no such element is found.
	 * @param xmlStr The XML string
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingSpecXmlFactory#ratingTemplateContainer(String) instead
	 */
	@Deprecated
	public void populateFromXml(String xmlStr) throws RatingObjectDoesNotExistException {
		RatingContainerXmlCompatUtil service = RatingContainerXmlCompatUtil.getInstance();
		RatingTemplateContainer ratingTemplateContainer = service.createRatingTemplateContainer(xmlStr);
		ratingTemplateContainer.clone(this);
	}
	/**
	 * Generates a template XML string from this object
	 * @param indent The amount to indent each level (initial level = 0)
	 * @return
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingSpecXmlFactory#toXml(RatingTemplateContainer, CharSequence, int) instead
	 */
	@Deprecated
	public String toTemplateXml(CharSequence indent) {
		return toTemplateXml(indent, 0);
	}
	/**
	 * Generates a template XML string from this object
	 * @param indent The amount to indent each level
	 * @param level The initial level of indentation
	 * @return
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingSpecXmlFactory#toXml(RatingTemplateContainer, CharSequence, int) instead
	 */
	@Deprecated
	public String toTemplateXml(CharSequence indent, int level) {
		RatingContainerXmlCompatUtil service = RatingContainerXmlCompatUtil.getInstance();
		return service.toXml(this, indent, level);
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		try {
			return templateId;
		}
		catch(Throwable t) {
			return super.toString();
		}
	}
}
