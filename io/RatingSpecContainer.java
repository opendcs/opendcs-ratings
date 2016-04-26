package hec.data.cwmsRating.io;

import static hec.data.cwmsRating.RatingConst.SEPARATOR2;
import static hec.data.cwmsRating.RatingConst.SEPARATOR3;
import hec.data.RatingObjectDoesNotExistException;
import hec.data.cwmsRating.AbstractRating;
import hec.util.TextUtil;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;

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
	 * @see hec.data.cwmsRating.RatingConst.RatingMethod
	 */
	public String inRangeMethod = null;
	/**
	 * The rating method for handling dates that are earlier than the earliest effective date of the ratings
	 * @see hec.data.cwmsRating.RatingConst.RatingMethod
	 */
	public String outRangeLowMethod = null;
	/**
	 * The rating method for handling dates that are later than the latest effective date of the ratings
	 * @see hec.data.cwmsRating.RatingConst.RatingMethod
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
	 * @see hecjavadev.hec.data.UsgsRounder.java
	 */
	public String[] indRoundingSpecs = null;
	/**
	 * Usgs-style rounding specification for the independent parameter.
	 * @see hecjavadev.hec.data.UsgsRounder.java
	 */
	public String depRoundingSpec = null;
	/**
	 * The description of this rating specification
	 */
	public String specDescription = null;

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		boolean result = false;
		test:
		do {
			if (!super.equals(obj)) break;
			if (!(obj instanceof RatingSpecContainer)) break;
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
				if (!other.inRangeMethod.equals(inRangeMethod)) break;
			}
			if ((other.outRangeLowMethod == null) != (outRangeLowMethod == null)) break;
			if (outRangeLowMethod != null) {
				if (!other.outRangeLowMethod.equals(outRangeLowMethod)) break;
			}
			if ((other.outRangeHighMethod == null) != (outRangeHighMethod == null)) break;
			if (outRangeHighMethod != null) {
				if (!other.outRangeHighMethod.equals(outRangeHighMethod)) break;
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
		return result;
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
	 * Constructs a RatingTemplateContainer from the first &lt;rating-template&gt; element in an XML string or null if no such element is found.
	 * @param xmlStr The XML string
	 * @return The RatingTemplateContainer object
	 */
	public static RatingSpecContainer fromXml(String xmlStr) throws RatingObjectDoesNotExistException {
		RatingSpecContainer rsc = new RatingSpecContainer();
		final String elementName = "rating-spec";
		RatingObjectDoesNotExistException noTemplateException = null;
		try {
			RatingTemplateContainer rtc = RatingTemplateContainer.fromXml(xmlStr);
			rtc.clone(rsc);
		}
		catch (RatingObjectDoesNotExistException e) {
			noTemplateException = e;
		}
		try {
			Document doc = new SAXBuilder().build(new StringReader(xmlStr));
			Element root = doc.getRootElement();
			if (!root.getName().equals(elementName)) {
				@SuppressWarnings("rawtypes")
				Iterator it = root.getDescendants(new ElementFilter(elementName));
				if (it.hasNext()) {
					root = (Element)it.next();
				}
			}
			if (!root.getName().equals(elementName)) {
				throw new RatingObjectDoesNotExistException(String.format("No <%s> element in XML.", elementName));
			}
			else {
				Element elem = null;
				@SuppressWarnings("rawtypes")
				List elems = null;
				rsc.specOfficeId = root.getAttributeValue("office-id");
				elem = root.getChild("rating-spec-id");
				if (elem != null) rsc.specId = elem.getTextTrim();
				elem = root.getChild("template-id");
				if (elem != null) rsc.templateId = elem.getTextTrim();
				elem = root.getChild("location-id");
				if (elem != null) rsc.locationId = elem.getTextTrim();
				elem = root.getChild("version");
				if (elem != null) rsc.specVersion = elem.getTextTrim();
				elem = root.getChild("source-agency");
				if (elem != null) rsc.sourceAgencyId = elem.getTextTrim();
				elem = root.getChild("in-range-method");
				if (elem != null) rsc.inRangeMethod = elem.getTextTrim();
				elem = root.getChild("out-range-low-method");
				if (elem != null) rsc.outRangeLowMethod = elem.getTextTrim();
				elem = root.getChild("out-range-high-method");
				if (elem != null) rsc.outRangeHighMethod = elem.getTextTrim();
				elem = root.getChild("active");
				if (elem != null) rsc.active = Boolean.parseBoolean(elem.getTextTrim());
				elem = root.getChild("auto-update");
				if (elem != null) rsc.autoUpdate = Boolean.parseBoolean(elem.getTextTrim());
				elem = root.getChild("auto-activate");
				if (elem != null) rsc.autoActivate = Boolean.parseBoolean(elem.getTextTrim());
				elem = root.getChild("auto-migrate-extension");
				if (elem != null) rsc.autoMigrateExtensions = Boolean.parseBoolean(elem.getTextTrim());
				elem = root.getChild("ind-rounding-specs");
				if (elem != null) {
					elems = elem.getChildren("ind-rounding-spec");
					rsc.indRoundingSpecs = new String[elems.size()];
					for (Object obj : elems) {
						elem = (Element)obj;
						try {
							int i = Integer.parseInt(elem.getAttributeValue("position")) - 1;
							if (i >= 0 && i < elems.size()) {
								rsc.indRoundingSpecs[i] = elem.getTextTrim();
							}
						}
						catch (Throwable t) {}
					}
				}
				elem = root.getChild("dep-rounding-spec");
				if (elem != null) rsc.depRoundingSpec = elem.getTextTrim();
				elem = root.getChild("description");
				if (elem != null) rsc.specDescription = elem.getTextTrim();
			}
			if (noTemplateException != null) {
				AbstractRating.getLogger().finer(noTemplateException.getMessage());
			}
		}
		catch (JDOMException | IOException e) {
			AbstractRating.getLogger().severe(e.getMessage());
		}
		return rsc;
	}
	/**
	 * Generates an XML string (template and spec) from this object
	 * @param indent The amount to indent each level (initial leve = 0)
	 * @return the generated XML
	 */
	public String toXml(CharSequence indent) {
		return toXml(indent, 0);
	}
	/**
	 * Generates an XML string (template and spec) from this object
	 * @param indent The amount to indent each level
	 * @param level The initial level of indentation
	 * @return the generated XML
	 */
	public String toXml(CharSequence indent, int level, boolean includeTemplate) {
		StringBuilder sb = new StringBuilder();
		int newLevel = level;
		if (level == 0) {
			newLevel = 1;
			sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
			sb.append("<ratings xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://www.hec.usace.army.mil/xmlSchema/cwms/Ratings.xsd\">\n");
		}
		if (includeTemplate) sb.append(toTemplateXml(indent, newLevel));
		sb.append(toSpecXml(indent, newLevel));
		if (level == 0) {
			sb.append("</ratings>\n");
		}
		return sb.toString();
	}
	public String toXml(CharSequence indent, int level) {
		return toXml(indent, level, true);
	}
	/**
	 * Generates a specification XML string from this object
	 * @param indent The amount to indent each level (initial level = 0)
	 * @return the generated XML
	 */
	public String toSpecXml(CharSequence indent) {
		return toSpecXml(indent, 0);
	}
	/**
	 * Generates a specification XML string from this object
	 * @param indent The amount to indent each level
	 * @param level The initial level of indentation
	 * @return the generated XML
	 */
	public String toSpecXml(CharSequence indent, int level) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < level; ++i) sb.append(indent);
		String prefix = sb.toString();
		sb.delete(0, sb.length());
		
		if (level == 0) {
			sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
			sb.append("<ratings xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://www.hec.usace.army.mil/xmlSchema/cwms/Ratings.xsd\">\n");
			prefix += indent;
		}
		sb.append(prefix).append("<rating-spec office-id=\"").append(specOfficeId == null ? officeId == null ? "" : officeId : specOfficeId).append("\">\n");
		sb.append(prefix).append(indent).append("<rating-spec-id>").append(specId == null ? "" : TextUtil.xmlEntityEncode(specId)).append("</rating-spec-id>\n");
		sb.append(prefix).append(indent).append("<template-id>").append(templateId == null ? "" : TextUtil.xmlEntityEncode(templateId)).append("</template-id>\n");
		sb.append(prefix).append(indent).append("<location-id>").append(locationId == null ? "" : TextUtil.xmlEntityEncode(locationId)).append("</location-id>\n");
		sb.append(prefix).append(indent).append("<version>").append(specVersion == null ? "" : TextUtil.xmlEntityEncode(specVersion)).append("</version>\n");
		sb.append(prefix).append(indent).append("<source-agency>").append(sourceAgencyId == null ? "" : TextUtil.xmlEntityEncode(sourceAgencyId)).append("</source-agency>\n");
		sb.append(prefix).append(indent).append("<in-range-method>").append(inRangeMethod == null ? "" : inRangeMethod).append("</in-range-method>\n");
		sb.append(prefix).append(indent).append("<out-range-low-method>").append(outRangeLowMethod == null ? "" : outRangeLowMethod).append("</out-range-low-method>\n");
		sb.append(prefix).append(indent).append("<out-range-high-method>").append(outRangeHighMethod == null ? "" : outRangeHighMethod).append("</out-range-high-method>\n");
		sb.append(prefix).append(indent).append("<active>").append(active).append("</active>\n");
		sb.append(prefix).append(indent).append("<auto-update>").append(autoUpdate).append("</auto-update>\n");
		sb.append(prefix).append(indent).append("<auto-activate>").append(autoActivate).append("</auto-activate>\n");
		sb.append(prefix).append(indent).append("<auto-migrate-extension>").append(autoMigrateExtensions).append("</auto-migrate-extension>\n");
		sb.append(prefix).append(indent).append("<ind-rounding-specs>\n");
		if (indRoundingSpecs != null) {
			for (int i = 0; i < indRoundingSpecs.length; ++i) {
				sb.append(prefix).append(indent).append(indent).append("<ind-rounding-spec position=\"").append(i+1).append("\">").append(indRoundingSpecs[i] == null ? "" : indRoundingSpecs[i]).append("</ind-rounding-spec>\n");
			}
		}
		sb.append(prefix).append(indent).append("</ind-rounding-specs>\n");
		sb.append(prefix).append(indent).append("<dep-rounding-spec>").append(depRoundingSpec == null ? "" : depRoundingSpec).append("</dep-rounding-spec>\n");
		sb.append(prefix).append(indent).append("<description>").append(specDescription == null ? "" : TextUtil.xmlEntityEncode(specDescription)).append("</description>\n");
		sb.append(prefix).append("</rating-spec>\n");
		if (level == 0) {
			sb.append("</ratings>\n");
		}
		return sb.toString();
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
