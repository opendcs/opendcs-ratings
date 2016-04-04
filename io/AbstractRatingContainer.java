package hec.data.cwmsRating.io;

import static hec.lang.Const.UNDEFINED_TIME;

import java.io.IOException;
import java.io.StringReader;

import hec.data.IVerticalDatum;
import hec.data.RatingException;
import hec.data.RatingObjectDoesNotExistException;
import hec.data.VerticalDatumException;
import hec.data.cwmsRating.AbstractRating;
import hec.data.cwmsRating.RatingConst;
import hec.heclib.util.HecTime;
import hec.io.VerticalDatumContainer;
import hec.util.TextUtil;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

/**
 * Container for AbstractRating data
 *
 * @author Mike Perryman
 */
public class AbstractRatingContainer implements IVerticalDatum, Comparable<AbstractRatingContainer> {
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
	public VerticalDatumContainer vdc = null;
	/**
	 * Fills another AbstractRatingContainer object with information from this one
	 * @param other The AbstractRatingContainer object to fill
	 */
	public void clone(AbstractRatingContainer other) {
		other.officeId = officeId;
		other.ratingSpecId = ratingSpecId;
		other.unitsId = unitsId;
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
	 * @see hec.data.IVerticalDatum#getNativeVerticalDatum()
	 */
	@Override
	public String getNativeVerticalDatum() throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		return vdc.getNativeVerticalDatum();
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#getCurrentVerticalDatum()
	 */
	@Override
	public String getCurrentVerticalDatum() throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		return vdc.getCurrentVerticalDatum();
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#isCurrentVerticalDatumEstimated()
	 */
	@Override
	public boolean isCurrentVerticalDatumEstimated() throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		return vdc.isCurrentVerticalDatumEstimated();
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#toNativeVerticalDatum()
	 */
	@Override
	public boolean toNativeVerticalDatum() throws VerticalDatumException {
		throw new VerticalDatumException("Method is not implemented for " + this.getClass().getName());
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#toNGVD29()
	 */
	@Override
	public boolean toNGVD29() throws VerticalDatumException {
		throw new VerticalDatumException("Method is not implemented for " + this.getClass().getName());
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#toNAVD88()
	 */
	@Override
	public boolean toNAVD88() throws VerticalDatumException {
		throw new VerticalDatumException("Method is not implemented for " + this.getClass().getName());
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#toVerticalDatum(java.lang.String)
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
	 * @see hec.data.IVerticalDatum#forceVerticalDatum(java.lang.String)
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
	 * @see hec.data.IVerticalDatum#getCurrentOffset()
	 */
	@Override
	public double getCurrentOffset() throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		return vdc.getCurrentOffset();
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#getCurrentOffset(java.lang.String)
	 */
	@Override
	public double getCurrentOffset(String unit) throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		return vdc.getCurrentOffset(unit);
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#getNGVD29Offset()
	 */
	@Override
	public double getNGVD29Offset() throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		return vdc.getNGVD29Offset();
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#getNGVD29Offset(java.lang.String)
	 */
	@Override
	public double getNGVD29Offset(String unit) throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		return vdc.getNGVD29Offset(unit);
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#getNAVD88Offset()
	 */
	@Override
	public double getNAVD88Offset() throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		return vdc.getNAVD88Offset();
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#getNAVD88Offset(java.lang.String)
	 */
	@Override
	public double getNAVD88Offset(String unit) throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		return vdc.getNAVD88Offset(unit);
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#isNGVD29OffsetEstimated()
	 */
	@Override
	public boolean isNGVD29OffsetEstimated() throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		return vdc.isNGVD29OffsetEstimated();
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#isNAVD88OffsetEstimated()
	 */
	@Override
	public boolean isNAVD88OffsetEstimated() throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		return vdc.isNAVD88OffsetEstimated();
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#getVerticalDatumInfo()
	 */
	@Override
	public String getVerticalDatumInfo() throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		return vdc.getVerticalDatumInfo();
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#setVerticalDatumInfo(java.lang.String)
	 */
	@Override
	public void setVerticalDatumInfo(String xmlStr) throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		vdc.setVerticalDatumInfo(xmlStr);
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
	 */
	public static AbstractRatingContainer fromXml(String xmlStr) throws RatingException {
		AbstractRatingContainer arc = null;
		Document doc;
		try {
			doc = new SAXBuilder().build(new StringReader(xmlStr));
			//----------------------------//
			// first try the root element //
			//----------------------------//
			Element elem = doc.getRootElement();
			if (elem.getName().equals("rating")) {
				if(elem.getChild("formula") != null) {
					arc = ExpressionRatingContainer.fromXml(elem);
				}
				else {
					arc = TableRatingContainer.fromXml(elem);
				}
			}
			else if (elem.getName().equals("usgs-stream-rating")) {
				arc = UsgsStreamTableRatingContainer.fromXml(elem);
			}
			else {
				//------------------------------------------//
				// next try immediate descendants from root //
				//------------------------------------------//
				for (Object obj : elem.getChildren()) {
					elem = (Element)obj;
					if (elem.getName().equals("rating")) {
						if(elem.getChild("formula") != null) {
							arc = ExpressionRatingContainer.fromXml(elem);
							break;
						}
						else {
							arc = TableRatingContainer.fromXml(elem);
							break;
						}
					}
					else if (elem.getName().equals("usgs-stream-rating")) {
						arc = UsgsStreamTableRatingContainer.fromXml(elem);
						break;
					}
				}
				if (arc == null) {
					throw new RatingObjectDoesNotExistException("No <rating>  or <usgs-stream-rating> element in XML.");
				}
			}
		}
		catch (JDOMException | IOException e) {
		}
		return arc;
	}
	/**
	 * Common code called from subclasses
	 * @throws VerticalDatumException 
	 */
	protected static void fromXml(Element ratingElement, AbstractRatingContainer arc) throws VerticalDatumException {
		HecTime hectime = new HecTime();
		String data = null;
		arc.officeId = ratingElement.getAttributeValue("office-id");
		arc.ratingSpecId = ratingElement.getChildTextTrim("rating-spec-id");
		arc.unitsId = ratingElement.getChildTextTrim("units-id");
		Element verticalDatumElement = ratingElement.getChild("vertical-datum-info");
		if (verticalDatumElement != null) {
			arc.vdc = new VerticalDatumContainer(new XMLOutputter().outputString(verticalDatumElement));
		}
		data = ratingElement.getChildTextTrim("effective-date");
		if (data != null) {
			hectime.set(data);
			arc.effectiveDateMillis = hectime.getTimeInMillis();
		}
		data = ratingElement.getChildTextTrim("create-date");
		if (data != null) {
			hectime.set(data);
			arc.createDateMillis = hectime.getTimeInMillis();
		}
		arc.active = Boolean.parseBoolean(ratingElement.getChildTextTrim("active"));
		arc.description = ratingElement.getChildTextTrim("description");
	}
	/**
	 * Generates an XML string from this object. The subclass overrides should normally be called instead of this.
	 * @param indent The amount to indent each level
	 * @return the generated XML
	 */
	public String toXml(CharSequence indent) {
		return toXml(indent, 0);
	}
	/**
	 * Generates an XML string from this object. The subclass overrides should normally be called instead of this.
	 * @param indent The amount to indent each level
	 * @param level The initial level of indentation
	 * @return the generated XML
	 */
	public String toXml(CharSequence indent, int level) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < level; ++i) sb.append(indent);
		String prefix = sb.toString();
		sb.delete(0, sb.length());
		if (level == 0) {
			sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
			sb.append("<ratings xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://www.hec.usace.army.mil/xmlSchema/cwms/Ratings.xsd\">\n");
			prefix += indent;
		}
		sb.append(toXml(prefix, indent, "rating")).append(prefix).append("</rating>\n");
		if (level == 0) {
			sb.append("</ratings>\n");
		}
		return sb.toString();
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
	/**
	 * Common code called from subclasses
	 */
	protected String toXml(CharSequence prefix, CharSequence indent, String elementName) {
		HecTime hectime = new HecTime();
		StringBuilder sb = new StringBuilder();
		sb.append(prefix).append("<").append(elementName).append(" office-id=\"").append(officeId == null ? "" : TextUtil.xmlEntityEncode(officeId)).append("\">\n");
		sb.append(prefix).append(indent).append("<rating-spec-id>").append(ratingSpecId == null ? "" : TextUtil.xmlEntityEncode(ratingSpecId)).append("</rating-spec-id>\n");
		if (vdc != null) {
			int level = indent.length() == 0 ? 0 : prefix.length() / indent.length();
			sb.append(vdc.toXml(indent, level+1));
		}
		if (!(this instanceof VirtualRatingContainer)) {
			sb.append(prefix).append(indent).append("<units-id>").append(unitsId == null ? "" : TextUtil.xmlEntityEncode(unitsId)).append("</units-id>\n");
		}
		if (effectiveDateMillis == UNDEFINED_TIME) {
			sb.append(prefix).append(indent).append("<effective-date/>\n");
		}
		else {
			hectime.setTimeInMillis(effectiveDateMillis);
			sb.append(prefix).append(indent).append("<effective-date>").append(hectime.getXMLDateTime(0)).append("</effective-date>\n");
		}
		if (transitionStartDateMillis == UNDEFINED_TIME) {
			sb.append(prefix).append(indent).append("<transition-start-date/>\n");
		}
		else {
			hectime.setTimeInMillis(transitionStartDateMillis);
			sb.append(prefix).append(indent).append("<transition-start-date>").append(hectime.getXMLDateTime(0)).append("</transition-start-date>\n");
		}
		if (createDateMillis == UNDEFINED_TIME) {
			sb.append(prefix).append(indent).append("<create-date/>\n");
		}
		else {
			hectime.setTimeInMillis(createDateMillis);
			sb.append(prefix).append(indent).append("<create-date>").append(hectime.getXMLDateTime(0)).append("</create-date>\n");
		}
		sb.append(prefix).append(indent).append("<active>").append(active).append("</active>\n");
		if (description == null || description.length() == 0) {
			sb.append(prefix).append(indent).append("<description/>\n");
		}
		else{
			sb.append(prefix).append(indent).append("<description>").append(TextUtil.xmlEntityEncode(description)).append("</description>\n");
		}
		return sb.toString();
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
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		boolean result = false;
		if (obj instanceof AbstractRatingContainer) {
			AbstractRatingContainer other = (AbstractRatingContainer)obj;
			result = toString().equals(other.toString()) && effectiveDateMillis == other.effectiveDateMillis;
			if (result) {
				result = toXml("").trim().equals(other.toXml("").trim());
			}
		}
		return result;
	}
	
	
}
