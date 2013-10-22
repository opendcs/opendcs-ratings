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
	 * Constructs a RatingTemplateContainer from the first &lt;rating-template&gt; element in an XML string or null if no such element is found.
	 * @param xmlStr The XML string
	 * @return The RatingTemplateContainer object
	 */
	public static RatingTemplateContainer fromXml(String xmlStr) throws RatingObjectDoesNotExistException {
		RatingTemplateContainer rtc = new RatingTemplateContainer();
		final String elementName = "rating-template";
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
				rtc.officeId = root.getAttributeValue("office-id");
				elem = root.getChild("version");
				if (elem != null) rtc.templateVersion = elem.getTextTrim();
				elem = root.getChild("ind-parameter-specs");
				if (elem != null) {
					elems = elem.getChildren("ind-parameter-spec");
					rtc.indParams = new String[elems.size()];
					rtc.inRangeMethods = new String[elems.size()];
					rtc.outRangeLowMethods = new String[elems.size()];
					rtc.outRangeHighMethods = new String[elems.size()];
					for (Object obj : elems) {
						elem = (Element)obj;
						try {
							int i = Integer.parseInt(elem.getAttributeValue("position")) - 1;
							if (i >= 0 && i < elems.size()) {
								rtc.indParams[i] = elem.getChildTextTrim("parameter");
								rtc.inRangeMethods[i] = elem.getChildTextTrim("in-range-method");
								rtc.outRangeLowMethods[i] = elem.getChildTextTrim("out-range-low-method");
								rtc.outRangeHighMethods[i] = elem.getChildTextTrim("out-range-high-method");
							}
						}
						catch (Throwable t) {}
					}
				}
				elem = root.getChild("dep-parameter");
				if (elem != null) rtc.depParam = elem.getTextTrim();
				elem = root.getChild("description");
				if (elem != null) rtc.templateDescription = elem.getTextTrim();
			}
			if (rtc.indParams != null && rtc.indParams.length > 0 && rtc.depParam != null) {
				rtc.parametersId = String.format("%s%s%s", TextUtil.join(SEPARATOR3, rtc.indParams), SEPARATOR2, rtc.depParam);
			}
			if (rtc.parametersId != null && rtc.templateVersion != null) {
				rtc.templateId = String.format("%s.%s", rtc.parametersId, rtc.templateVersion);
			}
		}
		catch (JDOMException | IOException e) {
			AbstractRating.getLogger().severe(e.getMessage());
		}
		return rtc;
	}
	/**
	 * Generates a template XML string from this object
	 * @param indent The amount to indent each level (initial level = 0)
	 * @return
	 */
	public String toTemplateXml(CharSequence indent) {
		return toTemplateXml(indent, 0);
	}
	/**
	 * Generates a template XML string from this object
	 * @param indent The amount to indent each level
	 * @param level The initial level of indentation
	 * @return
	 */
	public String toTemplateXml(CharSequence indent, int level) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < level; ++i) sb.append(indent);
		String prefix = sb.toString();
		sb.delete(0, sb.length());
		
		if (level == 0) {
			sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
			sb.append("<ratings xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://www.hec.usace.army.mil/xmlSchema/cwms/Ratings.xsd\">\n");
			prefix += indent;
		}
		sb.append(prefix).append("<rating-template office-id=\"").append(officeId == null ? "" : officeId).append("\">\n");
		sb.append(prefix).append(indent).append("<parameters-id>").append(parametersId == null ? "" : parametersId).append("</parameters-id>\n");
		sb.append(prefix).append(indent).append("<version>").append(templateVersion == null ? "" : templateVersion).append("</version>\n");
		sb.append(prefix).append(indent).append("<ind-parameter-specs>\n");
		if (indParams != null) {
			for (int i = 0; i < indParams.length; ++i) {
				String indParam = i < indParams.length && indParams[i] != null ? indParams[i] : "";
				String inRangeMethod = inRangeMethods != null && i < inRangeMethods.length && inRangeMethods[i] != null ? inRangeMethods[i] : "";
				String outRangeLowMethod = outRangeLowMethods != null && i < outRangeLowMethods.length && outRangeLowMethods[i] != null ? outRangeLowMethods[i] : "";
				String outRangeHighMethod = outRangeHighMethods != null && i < outRangeHighMethods.length && outRangeHighMethods[i] != null ? outRangeHighMethods[i] : "";
				sb.append(prefix).append(indent).append(indent).append("<ind-parameter-spec position=\"").append(i+1).append("\">\n");
				sb.append(prefix).append(indent).append(indent).append(indent).append("<parameter>").append(indParam).append("</parameter>\n");
				sb.append(prefix).append(indent).append(indent).append(indent).append("<in-range-method>").append(inRangeMethod).append("</in-range-method>\n");
				sb.append(prefix).append(indent).append(indent).append(indent).append("<out-range-low-method>").append(outRangeLowMethod).append("</out-range-low-method>\n");
				sb.append(prefix).append(indent).append(indent).append(indent).append("<out-range-high-method>").append(outRangeHighMethod).append("</out-range-high-method>\n");
				sb.append(prefix).append(indent).append(indent).append("</ind-parameter-spec>\n");
			}
		}
		sb.append(prefix).append(indent).append("</ind-parameter-specs>\n");
		sb.append(prefix).append(indent).append("<dep-parameter>").append(depParam == null ? "" : depParam).append("</dep-parameter>\n");
		sb.append(prefix).append(indent).append("<description>").append(templateDescription == null ? "" : templateDescription).append("</description>\n");
		sb.append(prefix).append("</rating-template>\n");
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
			return templateId;
		}
		catch(Throwable t) {
			return super.toString();
		}
	}
}
