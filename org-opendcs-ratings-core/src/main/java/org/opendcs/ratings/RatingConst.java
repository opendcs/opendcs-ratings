/*
 * Copyright (c) 2021. Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved. HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 *
 */

package org.opendcs.ratings;

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

	
	/**
	 * Constants specifying rating behaviors in various contexts.
	 *
	 * @author Mike Perryman
	 */
	public enum RatingMethod {
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
