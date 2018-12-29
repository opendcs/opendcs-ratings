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
	 */
	public RatingTemplateContainer(String xmlStr) throws RatingObjectDoesNotExistException {
		populateFromXml(xmlStr);
	}
	/**
	 * Populates a RatingTemplateContainer from the first &lt;rating-template&gt; element in an XML string or null if no such element is found.
	 * @param xmlStr The XML string
	 * @return The RatingTemplateContainer object
	 */
	public void populateFromXml(String xmlStr) throws RatingObjectDoesNotExistException {
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
				officeId = root.getAttributeValue("office-id");
				elem = root.getChild("version");
				if (elem != null) templateVersion = elem.getTextTrim();
				elem = root.getChild("ind-parameter-specs");
				if (elem != null) {
					elems = elem.getChildren("ind-parameter-spec");
					indParams = new String[elems.size()];
					inRangeMethods = new String[elems.size()];
					outRangeLowMethods = new String[elems.size()];
					outRangeHighMethods = new String[elems.size()];
					for (Object obj : elems) {
						elem = (Element)obj;
						try {
							int i = Integer.parseInt(elem.getAttributeValue("position")) - 1;
							if (i >= 0 && i < elems.size()) {
								indParams[i] = elem.getChildTextTrim("parameter");
								inRangeMethods[i] = elem.getChildTextTrim("in-range-method");
								outRangeLowMethods[i] = elem.getChildTextTrim("out-range-low-method");
								outRangeHighMethods[i] = elem.getChildTextTrim("out-range-high-method");
							}
						}
						catch (Throwable t) {}
					}
				}
				elem = root.getChild("dep-parameter");
				if (elem != null) depParam = elem.getTextTrim();
				elem = root.getChild("description");
				if (elem != null) templateDescription = elem.getTextTrim();
			}
			if (indParams != null && indParams.length > 0 && depParam != null) {
				parametersId = String.format("%s%s%s", TextUtil.join(SEPARATOR3, indParams), SEPARATOR2, depParam);
			}
			if (parametersId != null && templateVersion != null) {
				templateId = String.format("%s.%s", parametersId, templateVersion);
			}
		}
		catch (JDOMException | IOException e) {
			AbstractRating.getLogger().severe(e.getMessage());
		}
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
		sb.append(prefix).append(indent).append("<parameters-id>").append(parametersId == null ? "" : TextUtil.xmlEntityEncode(parametersId)).append("</parameters-id>\n");
		sb.append(prefix).append(indent).append("<version>").append(templateVersion == null ? "" : TextUtil.xmlEntityEncode(templateVersion)).append("</version>\n");
		sb.append(prefix).append(indent).append("<ind-parameter-specs>\n");
		if (indParams != null) {
			for (int i = 0; i < indParams.length; ++i) {
				String indParam = i < indParams.length && indParams[i] != null ? indParams[i] : "";
				String inRangeMethod = inRangeMethods != null && i < inRangeMethods.length && inRangeMethods[i] != null ? inRangeMethods[i] : "";
				String outRangeLowMethod = outRangeLowMethods != null && i < outRangeLowMethods.length && outRangeLowMethods[i] != null ? outRangeLowMethods[i] : "";
				String outRangeHighMethod = outRangeHighMethods != null && i < outRangeHighMethods.length && outRangeHighMethods[i] != null ? outRangeHighMethods[i] : "";
				sb.append(prefix).append(indent).append(indent).append("<ind-parameter-spec position=\"").append(i+1).append("\">\n");
				sb.append(prefix).append(indent).append(indent).append(indent).append("<parameter>").append(TextUtil.xmlEntityEncode(indParam)).append("</parameter>\n");
				sb.append(prefix).append(indent).append(indent).append(indent).append("<in-range-method>").append(inRangeMethod).append("</in-range-method>\n");
				sb.append(prefix).append(indent).append(indent).append(indent).append("<out-range-low-method>").append(outRangeLowMethod).append("</out-range-low-method>\n");
				sb.append(prefix).append(indent).append(indent).append(indent).append("<out-range-high-method>").append(outRangeHighMethod).append("</out-range-high-method>\n");
				sb.append(prefix).append(indent).append(indent).append("</ind-parameter-spec>\n");
			}
		}
		sb.append(prefix).append(indent).append("</ind-parameter-specs>\n");
		sb.append(prefix).append(indent).append("<dep-parameter>").append(depParam == null ? "" : TextUtil.xmlEntityEncode(depParam)).append("</dep-parameter>\n");
		sb.append(prefix).append(indent).append("<description>").append(templateDescription == null ? "" : TextUtil.xmlEntityEncode(templateDescription)).append("</description>\n");
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
