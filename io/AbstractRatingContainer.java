package hec.data.cwmsRating.io;

import static hec.lang.Const.UNDEFINED_TIME;

import java.io.IOException;
import java.io.StringReader;

import hec.data.RatingException;
import hec.data.RatingObjectDoesNotExistException;
import hec.heclib.util.HecTime;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

/**
 * Container for AbstractRating data
 *
 * @author Mike Perryman
 */
public class AbstractRatingContainer {
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
	 * The date/time that the rating was loaded into the database
	 */
	public long createDateMillis = UNDEFINED_TIME;
	/**
	 * Text description of the rating
	 */
	public String description = null;
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
		other.createDateMillis = createDateMillis;
		other.description = description;
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
	 * Intended to be overriden to allow sub-classes to return empty instances for cloning.
	 * @return
	 */
	public AbstractRatingContainer getInstance()
	{
		return new AbstractRatingContainer();
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
	 */
	protected static void fromXml(Element ratingElement, AbstractRatingContainer arc) {
		HecTime hectime = new HecTime();
		String data = null;
		arc.officeId = ratingElement.getAttributeValue("office-id");
		arc.ratingSpecId = ratingElement.getChildTextTrim("rating-spec-id");
		arc.unitsId = ratingElement.getChildTextTrim("units-id");
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
	 * Common code called from subclasses
	 */
	protected String toXml(CharSequence prefix, CharSequence indent, String elementName) {
		HecTime hectime = new HecTime();
		StringBuilder sb = new StringBuilder();
		sb.append(prefix).append("<").append(elementName).append(" office-id=\"").append(officeId).append("\">\n");
		sb.append(prefix).append(indent).append("<rating-spec-id>").append(ratingSpecId == null ? "" : ratingSpecId).append("</rating-spec-id>\n");
		sb.append(prefix).append(indent).append("<units-id>").append(unitsId == null ? "" : unitsId).append("</units-id>\n");
		if (effectiveDateMillis == UNDEFINED_TIME) {
			sb.append(prefix).append(indent).append("<effective-date/>\n");
		}
		else {
			hectime.setTimeInMillis(effectiveDateMillis);
			sb.append(prefix).append(indent).append("<effective-date>").append(hectime.getXMLDateTime(0)).append("</effective-date>\n");
		}
		if (createDateMillis == UNDEFINED_TIME) {
			sb.append(prefix).append(indent).append("<create-date/>\n");
		}
		else {
			hectime.setTimeInMillis(createDateMillis);
			sb.append(prefix).append(indent).append("<create-date>").append(hectime.getXMLDateTime(0)).append("</create-date>\n");
		}
		sb.append(prefix).append(indent).append("<active>").append(active).append("</active>\n");
		sb.append(prefix).append(indent).append("<description>").append(description == null ? "" : description).append("</description>\n");
		return sb.toString();
	}
}
