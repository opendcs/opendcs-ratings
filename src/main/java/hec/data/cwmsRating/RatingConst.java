/*
 * Copyright (c) 2021. Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved. HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 *
 */

package hec.data.cwmsRating;

import java.io.IOException;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import hec.data.RatingException;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Various constants for CWMS Ratings 
 * @author Mike Perryman
 */
public class RatingConst {
	
	/**
	 * Used to separate top-level portions of rating templates and rating specifications
	 */
	public static final String SEPARATOR1 = ".";
	/**
	 * Used to separate independent parameters from dependent parameter in rating templates
	 */
	public static final String SEPARATOR2 = ";";
	/**
	 * Used to separate individual independent parameters in rating templates
	 */
	public static final String SEPARATOR3 = ",";
	/**
	 * Used in generating rating spec for shifts
	 */
	public static final String USGS_SHIFTS_SUBPARAM = "shift";
	/**
	 * Used in generating rating spec for shifts
	 */
	public static final String USGS_SHIFTS_TEMPLATE_VERSION = "Linear";
	/**
	 * Used in generating rating spec for shifts
	 */
	public static final String USGS_SHIFTS_SPEC_VERSION = "Production";
	/**
	 * Used in generating rating spec for offsets
	 */
	public static final String USGS_OFFSETS_SUBPARAM = "offset";
	/**
	 * Used in generating rating spec for offsets
	 */
	public static final String USGS_OFFSETS_TEMPLATE_VERSION = "Step";
	/**
	 * Used in generating rating spec for offsets
	 */
	public static final String USGS_OFFSETS_SPEC_VERSION = "Production";
	
	static final String specNodeXpathStr = "/ratings/rating-spec[@office-id='%s' and normalize-space(rating-spec-id)='%s']";
	static final String templateNodeXpathStr = "/ratings/rating-template[@office-id='%s' and normalize-space(parameters-id)='%s' and normalize-space(version)='%s']";
	static boolean xmlParsingInitialized                    = false;
	private static DocumentBuilder builder					= null;
	static XPath xpath                                      = null;
	static XPathExpression officeIdXpath                    = null;
	static XPathExpression parametersIdXpath                = null;
	static XPathExpression versionXpath                     = null;
	static XPathExpression indParamsNodeXpath               = null;
	static XPathExpression indParamNodesXpath               = null;
	static XPathExpression indParamPosXpath                 = null;
	static XPathExpression parameterXpath                   = null;
	static XPathExpression inRangeMethodXpath               = null;
	static XPathExpression outRangeLowMethodXpath           = null;
	static XPathExpression outRangeHighMethodXpath          = null;
	static XPathExpression depParamXpath                    = null;
	static XPathExpression descriptionXpath                 = null;
	static XPathExpression ratingNodesXpath                 = null;
	static XPathExpression ratingSpecIdXpath                = null;
	static XPathExpression templateIdXpath                  = null;
	static XPathExpression locationIdXpath                  = null;
	static XPathExpression sourceAgencyXpath                = null;
	static XPathExpression autoUpdateXpath                  = null;
	static XPathExpression autoActivateXpath                = null;
	static XPathExpression autoMigrateExtXpath              = null;
	static XPathExpression indRoundingNodeXpath             = null;
	static XPathExpression indRoundingNodesXpath            = null;
	static XPathExpression depRoundingXpath                 = null;
	static XPathExpression unitsIdXpath                     = null;
	static XPathExpression effectiveDateXpath               = null;
	static XPathExpression createDateXpath                  = null;
	static XPathExpression activeXpath                      = null;
	static XPathExpression formulaXpath                     = null;
	static XPathExpression ratingPointGroupNodesXpath       = null;
	static XPathExpression extensionPointGroupNodesXpath    = null;
	static XPathExpression otherIndParamNodesXpath          = null;
	static XPathExpression otherIndValXPath                 = null;
	static XPathExpression pointNodesXpath                  = null;
	static XPathExpression indValXpath                      = null;
	static XPathExpression depValXpath                      = null;
	static XPathExpression noteXpath                        = null;
	static XPathExpression shiftNodesXpath                  = null;
	static XPathExpression offsetNodeXpath                  = null;

	/**
	 * Initializes static variables for parsing CWMS-style ratings XML instances
	 * @throws ParserConfigurationException
	 * @throws XPathExpressionException
	 */
	static void initXmlParsing() throws ParserConfigurationException, XPathExpressionException {
		if (!xmlParsingInitialized) {
			builder                       = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			xpath                         = XPathFactory.newInstance().newXPath();
			officeIdXpath                 = xpath.compile("./@office-id");
			parametersIdXpath             = xpath.compile("./parameters-id");
			versionXpath                  = xpath.compile("./version");
			indParamsNodeXpath            = xpath.compile("./ind-parameter-specs");
			indParamNodesXpath            = xpath.compile("./ind-parameter-spec");
			indParamPosXpath              = xpath.compile("./@position");
			parameterXpath                = xpath.compile("./parameter");
			inRangeMethodXpath            = xpath.compile("./in-range-method");
			outRangeLowMethodXpath        = xpath.compile("./out-range-low-method");
			outRangeHighMethodXpath       = xpath.compile("./out-range-high-method");
			depParamXpath                 = xpath.compile("./dep-parameter");
			descriptionXpath              = xpath.compile("./description");
			ratingNodesXpath              = xpath.compile("/ratings/rating|/ratings/usgs-stream-rating");
			ratingSpecIdXpath             = xpath.compile("./rating-spec-id");
			templateIdXpath               = xpath.compile("./template-id");
			locationIdXpath               = xpath.compile("./location-id");
			sourceAgencyXpath             = xpath.compile("./source-agency");
			autoUpdateXpath               = xpath.compile("./auto-update");
			autoActivateXpath             = xpath.compile("./auto-activate");
			autoMigrateExtXpath           = xpath.compile("./auto-migrate-extension");
			indRoundingNodeXpath          = xpath.compile("./ind-rounding-specs");
			indRoundingNodesXpath         = xpath.compile("./ind-rounding-spec");
			depRoundingXpath              = xpath.compile("./dep-rounding-spec");
			unitsIdXpath                  = xpath.compile("./units-id");
			effectiveDateXpath            = xpath.compile("./effective-date");
			createDateXpath               = xpath.compile("./create-date");
			activeXpath                   = xpath.compile("./active");
			formulaXpath                  = xpath.compile("./formula");
			ratingPointGroupNodesXpath    = xpath.compile("./rating-points");
			extensionPointGroupNodesXpath = xpath.compile("./extension-points");
			otherIndParamNodesXpath       = xpath.compile("./other-ind");
			otherIndValXPath              = xpath.compile("./@value");
			pointNodesXpath               = xpath.compile("./point");
			indValXpath                   = xpath.compile("./ind");
			depValXpath                   = xpath.compile("./dep");
			noteXpath                     = xpath.compile("./note");
			shiftNodesXpath               = xpath.compile("./height-shifts");
			offsetNodeXpath               = xpath.compile("./height-offsets");
			xmlParsingInitialized         = true;
		}
	}

	static synchronized Document readXmlAsDocument(String xml) throws IOException, SAXException
	{
		return builder.parse(new InputSource(new StringReader(xml)));
	}
	
	/**
	 * Constants specifying rating behaviors in various contexts.
	 *
	 * @author Mike Perryman
	 */
	public static enum RatingMethod {
		/**
		 * Return null if between values or outside range.<p>XML text representation: "NULL"
		 */
		NULL        ("NULL",        "Return null if between values or outside range"),
		/**
		 * Raise an exception if between values or outside range.<p>XML text representation: "ERROR"
		 */
		ERROR       ("ERROR",       "Raise an exception if between values or outsie range"),
		/**
		 * Linear interpolation or extrapolation of independent and dependent values.<p>XML text representation: "LINEAR"
		 */
		LINEAR      ("LINEAR",      "Linear interpolation or extrapolation of independent and dependent values"),
		/**
		 * Logarithmic interpolation or extrapolation of independent and dependent values.<p>XML text representation: "LOGARITHMIC"
		 */
		LOGARITHMIC ("LOGARITHMIC", "Logarithmic interpolation or extrapolation of independent and dependent values"),
		/**
		 * Linear interpolation/extrapolation of independent values, Logarithmic of dependent values.<p>XML text representation: "LIN-LOG"
		 */
		LIN_LOG     ("LIN-LOG",     "Linear interpolation/extrapolation of independent values, Logarithmic of dependent values"),
		/**
		 * Logarithmic interpolation/extrapolation of independent values, Linear of dependent values.<p>XML text representation: "LOG-LIN"
		 */
		LOG_LIN     ("LOG-LIN",     "Logarithmic interpolation/extrapolation of independent values, Linear of dependent values"),
		/**
		 * Return the value that is lower in position.<p>XML text representation: "PREVIOUS"
		 */
		PREVIOUS    ("PREVIOUS",    "Return the value that is lower in position"),
		/**
		 * Return the value that is higher in position.<p>XML text representation: "NEXT"
		 */
		NEXT        ("NEXT",        "Return the value that is higher in position"),
		/**
		 * Return the value that is nearest in position.<p>XML text representation: "NEAREST"
		 */
		NEAREST     ("NEAREST",     "Return the value that is nearest in position"),
		/**
		 * Return the value that is lower in magnitude.<p>XML text representation: "LOWER"
		 */
		LOWER       ("LOWER",       "Return the value that is lower in magnitude"),
		/**
		 * Return the value that is higher in magnitude.<p>XML text representation: "HIGHER"
		 */
		HIGHER      ("HIGHER",      "Return the value that is higher in magnitude"),
		/**
		 * Return the value that is closest in magnitude.<p>XML text representation: "CLOSEST"
		 */
		CLOSEST     ("CLOSEST",     "Return the value that is closest in magnitude");

		/**
		 * Specifies the name used the XML representation
		 */
		private String xmlName;
		private String description;
		RatingMethod(String xmlName, String description) {
			this.xmlName = xmlName;
			this.description = description;
		}
		/**
		 * Returns the rating method corresponding to the specifed XML representation.
		 * 
		 * @param xmlName The XML representation.
		 * @return The corresponding rating method.
		 */
		public static RatingMethod fromString(String xmlName) throws RatingException {
			for (RatingMethod rm : RatingMethod.values()) {
				if (rm.toString().equalsIgnoreCase(xmlName)) return rm;
			}
			throw new RatingException("\""+xmlName+"\" is not a valid rating method.");
		}
		/**
		 * Return the description for a rating method.
		 * @return The rating method's description
		 */
		public String description() {return this.description;}
		/**
		 * Returns the XML representation of the rating method.
		 */
		@Override
		public String toString() {return this.xmlName;}
	}
}
