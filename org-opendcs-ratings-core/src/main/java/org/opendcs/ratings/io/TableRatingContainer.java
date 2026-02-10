/*
 * Copyright (c) 2021. Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved. HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 *
 */

package org.opendcs.ratings.io;

import mil.army.usace.hec.metadata.VerticalDatumException;
import org.opendcs.ratings.AbstractRating;
import org.opendcs.ratings.RatingConst.RatingMethod;
import org.opendcs.ratings.RatingException;
import org.opendcs.ratings.TableRating;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

import static org.opendcs.ratings.XmlUtil.getChildElementText;

/**
 * Data container class for TableRating objects
 *
 * @author Mike Perryman
 */
public class TableRatingContainer extends AbstractRatingContainer {
	/**
	 * The rating values
	 */
	public RatingValueContainer[] values = null;
	/**
	 * The extension values
	 */
	public RatingValueContainer[] extensionValues = null;
	/**
	 * The rating method for handling independent parameter values that lie between values in the rating table
	 * @see org.opendcs.ratings.RatingConst.RatingMethod
	 */
	public String inRangeMethod = null;
	/**
	 * The rating method for handling independent parameter values that are less than the least values in the rating table
	 * @see org.opendcs.ratings.RatingConst.RatingMethod
	 */
	public String outRangeLowMethod = null;
	/**
	 * The rating method for handling independent parameter values that are greater than the greatest values in the rating table
	 * @see org.opendcs.ratings.RatingConst.RatingMethod
	 */
	public String outRangeHighMethod = null;
	/**
	 * Public empty constructor
	 */
	public TableRatingContainer() {}
	/**
	 * Public constructor from a DOM element
	 * @param element the DOM element
	 * @throws RatingException on error
	 * @deprecated Use =mil.army.usace.hec.cwms.rating.io.xml.RatingXmlFactory#tableRatingContainer(Element) instead
	 */
	@Deprecated
	public TableRatingContainer(Element element) throws RatingException {
		populateFromXml(element);
	}
	/**
	 * Public constructor from an XML snippet
	 * @param xmlText the XML snippet
	 * @throws RatingException on error
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingXmlFactory#tableRatingContainer(String) instead
	 */
	@Deprecated
	public TableRatingContainer(String xmlText) throws RatingException {
		populateFromXml(xmlText);
	}
	/**
	 * Populate this container from a DOM element
	 * @param ratingElement The DOM element
	 * @throws RatingException or error
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingXmlFactory#tableRatingContainer(Element) instead
	 */
	public void populateFromXml(Element ratingElement) throws RatingException {
		RatingContainerXmlCompatUtil service = RatingContainerXmlCompatUtil.getInstance();
		TableRatingContainer tableRatingContainer = service.createTableRatingContainer(ratingElement);
		tableRatingContainer.clone(this);
	}
	/**
	 * Populate this container from an XML snippet
	 * @param xmlText the XML snippet
	 * @throws RatingException on error
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingXmlFactory#tableRatingContainer(String) instead
	 */
	@Deprecated
	public void populateFromXml(String xmlText) throws RatingException {
		RatingContainerXmlCompatUtil service = RatingContainerXmlCompatUtil.getInstance();
		TableRatingContainer tableRatingContainer = service.createTableRatingContainer(xmlText);
		tableRatingContainer.clone(this);
	}
	/* (non-Javadoc)
	 * @see org.opendcs.ratings.io.AbstractRatingContainer#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		boolean result = obj == this;
		if (!result) {
			test:
				do {
					if (obj == null || obj.getClass() != getClass()) break;
					if (!super.equals(obj)) break;
					TableRatingContainer other = (TableRatingContainer)obj;
					if ((other.values == null) != (values == null)) break;
					if (values != null) {
						if (other.values.length != values.length) break;
						for (int i = 0; i < values.length; ++i) {
							if (!other.values[i].equals(values[i])) break test;
						}
					}
					if ((other.extensionValues == null) != (extensionValues == null)) break;
					if (extensionValues != null) {
						if (other.extensionValues.length != extensionValues.length) break;
						for (int i = 0; i < extensionValues.length; ++i) {
							if (!other.extensionValues[i].equals(extensionValues[i])) break test;
						}
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
					result = true;
				} while(false);
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see org.opendcs.ratings.io.AbstractRatingContainer#hashCode()
	 */
	@Override
	public int hashCode() {
		int hashCode = getClass().getName().hashCode() 
				+ super.hashCode()
				+ 3 * (inRangeMethod == null ? 1 : inRangeMethod.hashCode())
				+ 5 * (outRangeLowMethod == null ? 1 : outRangeLowMethod.hashCode())
				+ 7 * (outRangeHighMethod == null ? 1 : outRangeHighMethod.hashCode());
		if (values == null) {
			hashCode += 11;
		}
		else {
			hashCode += 13 * values.length;
			for (int i = 0; i < values.length; ++i) {
				hashCode += 17 * (values[i] == null ? i+1 : values[i].hashCode());
			}
		}
		if (extensionValues == null) {
			hashCode += 19;
		}
		else {
			hashCode += 23 * extensionValues.length;
			for (int i = 0; i < extensionValues.length; ++i) {
				hashCode += 29 * (extensionValues[i] == null ? i+1 : extensionValues[i].hashCode());
			}
		}
		return hashCode;
	}

	/**
	 * Fills another TableRatingContainer object with information from this one
	 * @param other The TableRatingContainer object to fill
	 */
	@Override
	public void clone(AbstractRatingContainer other) {
		if (!(other instanceof TableRatingContainer)) {
			throw new IllegalArgumentException("Clone-to object must be a TableRatingContainer.");
		}
		super.clone(other);
		TableRatingContainer trc = (TableRatingContainer)other;
		trc.inRangeMethod = inRangeMethod;
		trc.outRangeLowMethod = outRangeLowMethod;
		trc.outRangeHighMethod = outRangeHighMethod;
		if (values == null) {
			trc.values = null;
		}
		else {
			trc.values = new RatingValueContainer[values.length];
			for (int i = 0; i < values.length; ++i) {
				trc.values[i] = new RatingValueContainer();
				values[i].clone(trc.values[i]);
			}
		}
		if (extensionValues == null) {
			trc.extensionValues = null;
		}
		else {
			trc.extensionValues = new RatingValueContainer[extensionValues.length];
			for (int i = 0; i < extensionValues.length; ++i) {
				trc.extensionValues[i] = new RatingValueContainer();
				extensionValues[i].clone(trc.extensionValues[i]);
			}
		}
	}

	@Override
	public AbstractRatingContainer getInstance() {
		return new TableRatingContainer();
	}
	
	/* (non-Javadoc)
	 * @see org.opendcs.ratings.io.AbstractRatingContainer#newRating()
	 */
	@Override
	public AbstractRating newRating() throws RatingException {
		return new TableRating(this);
	}

	/**
	 *
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingXmlFactory#toXml(TableRatingContainer, CharSequence, int) instead
	 */
	@Deprecated
	@Override
	public String toXml(CharSequence indent) {
		return toXml(indent, 0);
	}

	/**
	 *
	 * @deprecated Use =mil.army.usace.hec.cwms.rating.io.xml.RatingXmlFactory#toXml(TableRatingContainer, CharSequence, int) instead
	 */
	@Deprecated
	@Override
	public String toXml(CharSequence indent, int level) {
		RatingContainerXmlCompatUtil service = RatingContainerXmlCompatUtil.getInstance();
		return service.toXml(this, indent, level);
	}
	/**
	 * @return an XML string containing the lookup behaviors inherited from the rating template
	 */
	public String getLookupMethods() {
		return String.format("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<lookup-behaviors>\n\t<in-range>%s</in-range>\n\t<out-range-low>%s</out-range-low>\n\t<out-range-high>%s</out-range-high>\n</lookup-behaviors>",
				inRangeMethod,
				outRangeLowMethod,
				outRangeHighMethod);
	}
	/**
	 * Sets the lookup behaviors from an XML string
	 * @param xml the XML string in the format as generated by getLookupBehaviors()
	 */
	public void setLookupMethods(String xml) {
		try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(xml)); 
			Document doc = db.parse(is);
			Element elem = doc.getDocumentElement();
			if (elem.getTagName().equals("lookup-behaviors")) {
				inRangeMethod = RatingMethod.fromString(getChildElementText(elem, "in-range")).name();
				outRangeLowMethod = RatingMethod.fromString(getChildElementText(elem, "out-range-low")).name();
				outRangeHighMethod = RatingMethod.fromString(getChildElementText(elem, "out-range-high")).name();
			}
		}
		catch (Exception e) {
		}
	}
	/* (non-Javadoc)
	 * @see org.opendcs.ratings.io.AbstractRatingContainer#toNativeVerticalDatum()
	 */
	@Override
	public boolean toNativeVerticalDatum() throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		boolean change = false;
		double offset = vdc.getCurrentOffset();
		if (offset != 0.) {
			change = true;
			String[][] paramsAndUnits = getParamsAndUnits();
			String[] params = paramsAndUnits[0];
			String[] units  = paramsAndUnits[1];
			for (int i = 0; i < params.length; ++i) {
				if (params[i].startsWith("Elev")) {
					int paramNum = i == params.length-1 ? -1 : i;
					offset = vdc.getCurrentOffset(units[i]);
					try {
						addOffset(paramNum, -offset);
					}
					catch (RatingException e) {
						throw new VerticalDatumException(e);
					}
				}
			}
			vdc.toNativeVerticalDatum();
		}
		return change;
	}

	/* (non-Javadoc)
	 * @see org.opendcs.ratings.io.AbstractRatingContainer#toNGVD29()
	 */
	@Override
	public boolean toNGVD29() throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		boolean change = false;
		if (!vdc.getCurrentVerticalDatum().equals("NGVD29")) {
			change = true;
			String[][] paramsAndUnits = getParamsAndUnits();
			String[] params = paramsAndUnits[0];
			String[] units  = paramsAndUnits[1];
			for (int i = 0; i < params.length; ++i) {
				if (params[i].startsWith("Elev")) {
					int paramNum = i == params.length-1 ? -1 : i;
					double offset1 = vdc.getNGVD29Offset(units[i]);
					double offset2 = vdc.getCurrentOffset(units[i]);
					try {
						addOffset(paramNum, offset1 - offset2);
					}
					catch (RatingException e) {
						throw new VerticalDatumException(e);
					}
				}
			}
			vdc.toNGVD29();
		}
		return change;
	}

	/* (non-Javadoc)
	 * @see org.opendcs.ratings.io.AbstractRatingContainer#toNAVD88()
	 */
	@Override
	public boolean toNAVD88() throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		boolean change = false;
		if (!vdc.getCurrentVerticalDatum().equals("NAVD88")) {
			change = true;
			String[][] paramsAndUnits = getParamsAndUnits();
			String[] params = paramsAndUnits[0];
			String[] units  = paramsAndUnits[1];
			for (int i = 0; i < params.length; ++i) {
				if (params[i].startsWith("Elev")) {
					int paramNum = i == params.length-1 ? -1 : i;
					double offset1 = vdc.getNAVD88Offset(units[i]);
					double offset2 = vdc.getCurrentOffset(units[i]);
					try {
						addOffset(paramNum, offset1 - offset2);
					}
					catch (RatingException e) {
						throw new VerticalDatumException(e);
					}
				}
			}
			vdc.toNAVD88();
		}
		return change;
	}


	/* (non-Javadoc)
	 * @see org.opendcs.ratings.io.AbstractRatingContainer#addOffset(int, double)
	 */
	@Override
	public void addOffset(int paramNum, double offset) throws RatingException {
		if (values != null) {
            for (RatingValueContainer value : values) {
                if (paramNum == 0) {
                    value.indValue += offset;
                } else if (paramNum == -1) {
                    if (value.depTable == null) {
                        value.depValue += offset;
                    } else {
                        value.depTable.addOffset(paramNum, offset);
                    }
                } else {
                    if (value.depTable == null) {
                        throw new RatingException("Invalid parameter number");
                    }
                    value.depTable.addOffset(paramNum - 1, offset);
                }
            }
		}
		if (extensionValues != null) {
            for (RatingValueContainer extensionValue : extensionValues) {
                if (paramNum == 0) {
                    extensionValue.indValue += offset;
                } else if (paramNum == -1) {
                    if (extensionValue.depTable == null) {
                        extensionValue.depValue += offset;
                    } else {
                        extensionValue.depTable.addOffset(paramNum, offset);
                    }
                } else {
                    if (extensionValue.depTable == null) {
                        throw new RatingException("Invalid parameter number");
                    }
                    extensionValue.depTable.addOffset(paramNum - 1, offset);
                }
            }
		}
	}
}
