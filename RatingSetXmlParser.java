package hec.data.cwmsRating;

import static hec.data.cwmsRating.RatingConst.SEPARATOR1;
import static hec.data.cwmsRating.RatingConst.SEPARATOR2;
import static hec.data.cwmsRating.RatingConst.USGS_SHIFTS_SUBPARAM;
import static hec.data.cwmsRating.RatingConst.USGS_SHIFTS_TEMPLATE_VERSION;
import static hec.data.cwmsRating.RatingConst.USGS_SHIFTS_SPEC_VERSION;
import static hec.data.cwmsRating.RatingConst.USGS_OFFSETS_SUBPARAM;
import static hec.data.cwmsRating.RatingConst.USGS_OFFSETS_TEMPLATE_VERSION;
import static hec.data.cwmsRating.RatingConst.USGS_OFFSETS_SPEC_VERSION;
import hec.data.RatingException;
import hec.data.RatingObjectDoesNotExistException;
import hec.data.VerticalDatumException;
import hec.data.cwmsRating.RatingConst.RatingMethod;
import hec.data.cwmsRating.io.AbstractRatingContainer;
import hec.data.cwmsRating.io.ExpressionRatingContainer;
import hec.data.cwmsRating.io.RatingSetContainer;
import hec.data.cwmsRating.io.RatingSpecContainer;
import hec.data.cwmsRating.io.RatingTemplateContainer;
import hec.data.cwmsRating.io.RatingValueContainer;
import hec.data.cwmsRating.io.TableRatingContainer;
import hec.data.cwmsRating.io.TransitionalRatingContainer;
import hec.data.cwmsRating.io.UsgsStreamTableRatingContainer;
import hec.data.cwmsRating.io.VirtualRatingContainer;
import hec.heclib.util.HecTime;
import hec.io.VerticalDatumContainer;
import hec.lang.Const;
import hec.util.TextUtil;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Parser;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.ParserAdapter;
import org.xml.sax.helpers.XMLFilterImpl;
import org.xml.sax.helpers.XMLReaderFactory;

@SuppressWarnings("deprecation")
/**
 * Class to parse RatingSet objects from XML.  This implementation uses SAX for performance.
 * The previous parser used XPath, which proved to be unusable on large rating sets due to
 * extremely poor performance
 *
 * @author Mike Perryman
 */
public class RatingSetXmlParser extends XMLFilterImpl {

	//-----------------------------//
	// strings expected in the XML //
	//-----------------------------//
	private static final String ACTIVE_STR = "active";
	private static final String AUTO_ACTIVATE_STR = "auto-activate";
	private static final String AUTO_MIGRATE_EXTENSION_STR = "auto-migrate-extension";
	private static final String AUTO_UPDATE_STR	= "auto-update";
	private static final String CASE_STR = "case";
	private static final String CONNECTIONS_STR = "connections";
	private static final String CREATE_DATE_STR = "create-date";
	private static final String DEFAULT_STR = "default";
	private static final String DEP_PARAMETER_STR = "dep-parameter";
	private static final String DEP_ROUNDING_SPEC_STR = "dep-rounding-spec";
	private static final String DEP_STR = "dep";
	private static final String DESCRIPTION_STR = "description";
	private static final String EFFECTIVE_DATE_STR = "effective-date";
	private static final String EXTENSION_POINTS_STR = "extension-points";
	private static final String FORMULA_STR = "formula";
	private static final String HEIGHT_OFFSETS_STR = "height-offsets";
	private static final String HEIGHT_SHIFTS_STR = "height-shifts";
	private static final String IND_PARAMETER_SPECS_STR = "ind-parameter-specs";
	private static final String IND_PARAMETER_SPEC_STR = "ind-parameter-spec";
	private static final String IND_ROUNDING_SPECS_STR = "ind-rounding-specs";
	private static final String IND_ROUNDING_SPEC_STR = "ind-rounding-spec";
	private static final String IND_STR = "ind";
	private static final String IN_RANGE_METHOD_STR = "in-range-method";
	private static final String LOCATION_ID_STR = "location-id";
	private static final String NOTE_STR = "note";
	private static final String OFFICE_ID_STR = "office-id";
	private static final String OTHER_IND_STR = "other-ind";
	private static final String OUT_RANGE_HIGH_METHOD_STR = "out-range-high-method";
	private static final String OUT_RANGE_LOW_METHOD_STR = "out-range-low-method";
	private static final String PARAMETERS_ID_STR = "parameters-id";
	private static final String PARAMETER_STR = "parameter";
	private static final String POINT_STR = "point";
	private static final String POSITION_STR = "position";
	private static final String RATINGS_STR = "ratings";
	private static final String RATING_EXPRESSION_STR = "rating-expression";
	private static final String RATING_POINTS_STR = "rating-points";
	private static final String RATING_SPEC_ID_STR = "rating-spec-id";
	private static final String RATING_SPEC_STR = "rating-spec";
	private static final String RATING_STR = "rating";
	private static final String RATING_TEMPLATE_STR = "rating-template";
	private static final String SELECT_STR = "select";
	private static final String SIMPLE_RATING_STR = "simple-rating";
	private static final String SOURCE_AGENCY_STR = "source-agency";
	private static final String SOURCE_RATINGS_STR = "source-ratings";
	private static final String SOURCE_RATING_STR = "source-rating";
	private static final String TEMPLATE_ID_STR	= "template-id";
	private static final String THEN_STR = "then";
	private static final String TRANSITIONAL_RATING_STR = "transitional-rating";
	private static final String TRANSITION_START_DATE_STR = "transition-start-date";
	private static final String UNITS_ID_STR = "units-id";
	private static final String USGS_STREAM_RATING_STR = "usgs-stream-rating";
	private static final String VALUE_STR = "value";
	private static final String VERSION_STR = "version";
	private static final String VERTICAL_DATUM_INFO_STR = "vertical-datum-info";
	private static final String VIRTUAL_RATING_STR = "virtual-rating";
	private static final String WHEN_STR = "when";

	//--------------------//
	// instance variables //
	//--------------------//
	private String[] parts = new String[6]; // deepest element is 5
	private int partsLen = -1;
	private StringBuilder chars = new StringBuilder();
	private RatingSetContainer rsc = null;
	private RatingTemplateContainer rtc = null;
	private Map<String, RatingTemplateContainer> rtcsById = null;
	private RatingSpecContainer rspc = null;
	private Map<String, RatingSpecContainer> rspcsById = null;
	private AbstractRatingContainer arc = null;
	private SortedSet<AbstractRatingContainer> arcs = null;
	private Map<String, SortedSet<AbstractRatingContainer>> arcsById = null;
	private ExpressionRatingContainer erc = null;
	private TableRatingContainer trc = null;
	private UsgsStreamTableRatingContainer urc = null;
	private List<ShiftInfo> shiftInfo = null;
	private VirtualRatingContainer vrc = null;
	private TransitionalRatingContainer trrc = null;
	private Map<Integer, String> sourceRatingIdsByPos = null;
	private Map<Integer, String> conditions = null;
	private Map<Integer, String> evaluations = null;
	private String defaultEvaluation = null;
	private StringBuilder verticalDatumInfo = null;
	private List<VerticalDatumContainer> vdcs = null;
	private boolean inVerticalDatumInfo = false;
	private int pos = -1;
	private HecTime hectime = new HecTime();
	private List<RatingPoints> ratingPoints = null;
	private List<RatingPoints> extensionPoints = null;
	private List<RatingPoints> offsetPoints = null;
	private List<RatingPoints> shiftPoints = null;
	private int ratingPointSetCount = 0;
	private int extensionPointSetCount = 0;
	private int offsetPointSetCount = 0;
	private int shiftPointSetCount = 0;
	private boolean requireRatingPoints = true;
	
	//----------------//
	// helper classes //
	//----------------//
	class PointValue {
		public double ind = Const.UNDEFINED_DOUBLE;
		public double dep = Const.UNDEFINED_DOUBLE;
		public String note = null;
	}
	
	class RatingPoints {
		private int otherIndCount = 0;
		private int pointCount = 0;
		private double[] otherInds = null;
		private List<PointValue> points = null;
		public RatingPoints() {}
		public void addOtherInd(double value) {
			if (otherInds == null) otherInds = new double[10];
			otherInds[otherIndCount++] = value;
		}
		public void addIndValue(double value) {
			if (points == null) points = new ArrayList<PointValue>();
			points.add(new PointValue());
			points.get(pointCount++).ind = value;			
		}
		public void addDepValue(double value) {
			points.get(pointCount-1).dep = value;
		}
		public void addNote(String note) {
			points.get(pointCount-1).note = note;
		}
		public int getOtherIndCount() {
			return otherIndCount;
		}
		public double getOtherInd(int i) {
			return otherInds[i];
		}
		public int getPointCount() {
			return pointCount;
		}
		public double getIndValue(int i) {
			return points.get(i).ind;
		}
		public double getDepValue(int i) {
			return points.get(i).dep;
		}
		public String getNote(int i) {
			return points.get(i).note;
		}
	}
	
	class ShiftInfo {
		public long effectiveDate = Const.UNDEFINED_TIME;
		public long transitionStartDate = Const.UNDEFINED_TIME;
		public long createDate = Const.UNDEFINED_TIME;
		public boolean active = false;
	}
	
	//--------------------------//
	// public parsing interface //
	//--------------------------//
	
	/**
	 * Parses a RatingSetContainer XML instance from a string.
	 * @param str The string containing the XML
	 * @return The resulting RatingSetContainer object
	 * @throws RatingException
	 */
	public static RatingSetContainer parseString(String str) throws RatingException {
		return parseString(str, true);
	}
	/**
	 * Parses a RatingSetContainer XML instance from a string.
	 * @param str The string containing the XML
	 * @param requireRatingPoints specifies whether rating points are required for successful parsing
	 * @return The resulting RatingSetContainer object
	 * @throws RatingException
	 */
	public static RatingSetContainer parseString(String str, boolean requireRatingPoints) throws RatingException {
		return parseReader(new StringReader(str), requireRatingPoints);
	}
	/**
	 * Parses a RatingSetContainer XML instance from a File object.
	 * @param file The File object containing the XML
	 * @return The resulting RatingSetContainer object
	 * @throws RatingException
	 */
	public static RatingSetContainer parseFile(File file) throws RatingException {
		return parseFile(file, true);
	}
	/**
	 * Parses a RatingSetContainer XML instance from a File object.
	 * @param file The File object containing the XML
	 * @param requireRatingPoints specifies whether rating points are required for successful parsing
	 * @return The resulting RatingSetContainer object
	 * @throws RatingException
	 */
	public static RatingSetContainer parseFile(File file, boolean requireRatingPoints) throws RatingException {
		try {
			return parseReader(new FileReader(file), requireRatingPoints);
		} 
		catch (Throwable t) {
			if (t instanceof RatingException) throw (RatingException)t;
			throw new RatingException(t);
		}
	}
	/**
	 * Parses a RatingSetContainer XML instance from a file.
	 * @param filename The name of the file containing the XML
	 * @return The resulting RatingSetContainer object
	 * @throws RatingException
	 */
	public static RatingSetContainer parseFile(String filename) throws RatingException {
		return parseFile(filename, true);
	}
	/**
	 * Parses a RatingSetContainer XML instance from a file.
	 * @param filename The name of the file containing the XML
	 * @param requireRatingPoints specifies whether rating points are required for successful parsing
	 * @return The resulting RatingSetContainer object
	 * @throws RatingException
	 */
	public static RatingSetContainer parseFile(String filename, boolean requireRatingPoints) throws RatingException {
		try {
			return parseReader(new FileReader(new File(filename)), requireRatingPoints);
		} 
		catch (Throwable t) {
			if (t instanceof RatingException) throw (RatingException)t;
			throw new RatingException(t);
		}
	}
	/**
	 * Parses a RatingSetContainer XML instance from a Reader object.
	 * @param r The Reader object to get the XML from
	 * @return The resulting RatingSetContainer object
	 * @throws RatingException
	 */
	public static RatingSetContainer parseReader(Reader r) throws RatingException {
		return parseReader(r, true);
	}
	/**
	 * Parses a RatingSetContainer XML instance from a Reader object.
	 * @param r The Reader object to get the XML from
	 * @param requireRatingPoints specifies whether rating points are required for successful parsing
	 * @return The resulting RatingSetContainer object
	 * @throws RatingException
	 */
	public static RatingSetContainer parseReader(Reader r, boolean requireRatingPoints) throws RatingException {
		return parseInputSource(new InputSource(r), requireRatingPoints);
	}
	/**
	 * Parses a RatingSetContainer XML instance from a InputSource object.
	 * @param is The InputSource object to get the XML from
	 * @return The resulting RatingSetContainer object
	 * @throws RatingException
	 */
	public static RatingSetContainer parseInputSource(InputSource is) throws RatingException {
		return parseInputSource(is, true);
	}
	/**
	 * Parses a RatingSetContainer XML instance from a InputSource object.
	 * @param is The InputSource object to get the XML from
	 * @param requireRatingPoints specifies whether rating points are required for successful parsing
	 * @return The resulting RatingSetContainer object
	 * @throws RatingException
	 */
	public static RatingSetContainer parseInputSource(InputSource is, boolean requireRatingPoints) throws RatingException {
		RatingSetXmlParser parser = null;
		try {
			parser = new RatingSetXmlParser(requireRatingPoints);
			parser.parse(is);
		}
		catch (Throwable t) {
			if (parser != null && parser.getRatingSetContainer() == null) {
				throw new RatingObjectDoesNotExistException(t);
			}
			else {
				if (t instanceof RatingException) throw (RatingException)t;
				throw new RatingException(t);
			}
		}
		return parser.getRatingSetContainer();
	}

	
	/*
	 * Private constructor 
	 * @throws SAXException
	 */
	private RatingSetXmlParser() throws SAXException {
		this(true);
	}
	/*
	 * Private constructor 
	 * @throws SAXException
	 */
	private RatingSetXmlParser(boolean requireRatingPoints) throws SAXException {
		super(new ParserAdapter((Parser) XMLReaderFactory.createXMLReader()));
		this.requireRatingPoints = requireRatingPoints;
	}
	/*
	 * @return the RatingSetContainer object
	 */
	private RatingSetContainer getRatingSetContainer() {
		return rsc;
	}
	/**
	 * @throws a new RuntimeException if XML doesn't match expected structure
	 */
	private void elementError() {
		if (!inVerticalDatumInfo) { // don't worry about structure in here
			StringBuilder message = new StringBuilder("Unexpected element: \"");
			for (int i = 0; i < partsLen; ++i) {
				message.append(i == 0 ? "" : "/").append(parts[i]);
			}
			message.append("\"");
			throw new RuntimeException(message.toString());
		}
	}
	/**
	 * Process data collected by characters() SAX method
	 */
	private void processCharacters() {
		if (chars.length() > 0) {
			String data = chars.toString().trim();
			chars.setLength(0);
			if (inVerticalDatumInfo) {
				verticalDatumInfo.append(data);
			}
			else {
				switch (partsLen) {
				case 3 :
					if (parts[1].equals(RATING_TEMPLATE_STR)) {
						if (parts[2].equals(PARAMETERS_ID_STR)) {
							rtc.parametersId = data;
							int count = TextUtil.split(TextUtil.split(data, RatingConst.SEPARATOR2)[0], RatingConst.SEPARATOR3).length;
							rtc.indParams = new String[count];
							rtc.inRangeMethods = new String[count];
							rtc.outRangeLowMethods = new String[count];
							rtc.outRangeHighMethods = new String[count];
						}
						else if (parts[2].equals(VERSION_STR)) {
							rtc.templateVersion = data;
							rtc.templateId = rtc.parametersId + RatingConst.SEPARATOR1 + rtc.templateVersion;
						}
						else if (parts[2].equals(DEP_PARAMETER_STR)) {
							rtc.depParam = data;
						}
						else if (parts[2].equals(DESCRIPTION_STR)) {
							rtc.templateDescription = data;
						}
					}
					else if (parts[1].equals(RATING_SPEC_STR)) {
						if (parts[2].equals(RATING_SPEC_ID_STR)) {
							rspc.specId = data;
						}
						else if (parts[2].equals(TEMPLATE_ID_STR)) {
							int count = TextUtil.split(TextUtil.split(data, RatingConst.SEPARATOR2)[0], RatingConst.SEPARATOR3).length;
							rspc.indRoundingSpecs = new String[count];
						}
						else if (parts[2].equals(LOCATION_ID_STR)) {
							rspc.locationId = data;
						}
						else if (parts[2].equals(VERSION_STR)) {
							rspc.specVersion = data;
						}
						else if (parts[2].equals(SOURCE_AGENCY_STR)) {
							rspc.sourceAgencyId = data;
						}
						else if (parts[2].equals(IN_RANGE_METHOD_STR)) {
							rspc.inRangeMethod = data;
						}
						else if (parts[2].equals(OUT_RANGE_LOW_METHOD_STR)) {
							rspc.outRangeLowMethod = data;
						}
						else if (parts[2].equals(OUT_RANGE_HIGH_METHOD_STR)) {
							rspc.outRangeHighMethod = data;
						}
						else if (parts[2].equals(ACTIVE_STR)) {
							rspc.active = Boolean.parseBoolean(data);
						}
						else if (parts[2].equals(AUTO_UPDATE_STR)) {
							rspc.autoUpdate = Boolean.parseBoolean(data);
						}
						else if (parts[2].equals(AUTO_ACTIVATE_STR)) {
							rspc.autoActivate = Boolean.parseBoolean(data);
						}
						else if (parts[2].equals(AUTO_MIGRATE_EXTENSION_STR)) {
							rspc.autoMigrateExtensions = Boolean.parseBoolean(data);
						}
						else if (parts[2].equals(DEP_ROUNDING_SPEC_STR)) {
							rspc.depRoundingSpec = data;
						}
						else if (parts[2].equals(DESCRIPTION_STR)) {
							rspc.specDescription = data;
						}
					}
					else if (parts[1].equals(RATING_STR            ) ||
							 parts[1].equals(SIMPLE_RATING_STR     ) ||
							 parts[1].equals(USGS_STREAM_RATING_STR)) {
						if (parts[2].equals(RATING_SPEC_ID_STR)) {
							arc.ratingSpecId = data;
						}
						else if (parts[2].equals(UNITS_ID_STR)) {
							arc.unitsId = data;
						}
						else if (parts[2].equals(EFFECTIVE_DATE_STR)) {
							hectime.set(data);
							arc.effectiveDateMillis = hectime.getTimeInMillis();
						}
						else if (parts[2].equals(TRANSITION_START_DATE_STR)) {
							if (data.length() > 0) {
								hectime.set(data);
								arc.transitionStartDateMillis = hectime.getTimeInMillis();
							}
						}
						else if (parts[2].equals(CREATE_DATE_STR)) {
							if (data.length() > 0) {
								hectime.set(data);
								arc.createDateMillis = hectime.getTimeInMillis();
							}
						}
						else if (parts[2].equals(ACTIVE_STR)) {
							arc.active = Boolean.parseBoolean(data);
						}
						else if (parts[2].equals(DESCRIPTION_STR)) {
							arc.description = data;
						}
						else if (parts[2].equals(FORMULA_STR)) {
							if (!(parts[1].equals(RATING_STR) || parts[1].equals(SIMPLE_RATING_STR))) {
								elementError(); 
							}
							erc = new ExpressionRatingContainer();
							arc.clone(erc);
							erc.expression = data;
						}
					}
					else if (parts[1].equals(VIRTUAL_RATING_STR)) {
						if (parts[2].equals(RATING_SPEC_ID_STR)) {
							vrc.ratingSpecId = data;
						}
						else if (parts[2].equals(EFFECTIVE_DATE_STR)) {
							hectime.set(data);
							vrc.effectiveDateMillis = hectime.getTimeInMillis();
						}
						else if (parts[2].equals(TRANSITION_START_DATE_STR)) {
							if (data.length() > 0) {
								hectime.set(data);
								vrc.transitionStartDateMillis = hectime.getTimeInMillis();
							}
						}
						else if (parts[2].equals(CREATE_DATE_STR)) {
							if (data.length() > 0) {
								hectime.set(data);
								vrc.createDateMillis = hectime.getTimeInMillis();
							}
						}
						else if (parts[2].equals(ACTIVE_STR)) {
							vrc.active = Boolean.parseBoolean(data);
						}
						else if (parts[2].equals(DESCRIPTION_STR)) {
							vrc.description = data;
						}
						else if (parts[2].equals(CONNECTIONS_STR)) {
							vrc.connections = data;
						}
					}
					else if (parts[1].equals(TRANSITIONAL_RATING_STR)) {
						if (parts[2].equals(RATING_SPEC_ID_STR)) {
							trrc.ratingSpecId = data;
						}
						else if (parts[2].equals(UNITS_ID_STR)) {
							trrc.unitsId = data;
						}
						else if (parts[2].equals(EFFECTIVE_DATE_STR)) {
							hectime.set(data);
							trrc.effectiveDateMillis = hectime.getTimeInMillis();
						}
						else if (parts[2].equals(TRANSITION_START_DATE_STR)) {
							if (data.length() > 0) {
								hectime.set(data);
								trrc.transitionStartDateMillis = hectime.getTimeInMillis();
							}
						}
						else if (parts[2].equals(CREATE_DATE_STR)) {
							if (data.length() > 0) {
								hectime.set(data);
								trrc.createDateMillis = hectime.getTimeInMillis();
							}
						}
						else if (parts[2].equals(ACTIVE_STR)) {
							trrc.active = Boolean.parseBoolean(data);
						}
						else if (parts[2].equals(DESCRIPTION_STR)) {
							trrc.description = data;
						}
					}
					break;
				case 4 :
					if (parts[1].equals(RATING_SPEC_STR)) {
						if (parts[2].equals(IND_ROUNDING_SPECS_STR) && parts[3].equals(IND_ROUNDING_SPEC_STR)) {
							rspc.indRoundingSpecs[pos] = data;
						}
					}
					else if (parts[1].equals(USGS_STREAM_RATING_STR)) {
						if (parts[2].equals(HEIGHT_SHIFTS_STR)) {
							if (parts[3].equals(EFFECTIVE_DATE_STR)) {
								hectime.set(data);
								shiftInfo.get(shiftPointSetCount-1).effectiveDate = hectime.getTimeInMillis();
							}
							else if (parts[3].equals(CREATE_DATE_STR)) {
								hectime.set(data);
								shiftInfo.get(shiftPointSetCount-1).createDate = hectime.getTimeInMillis();
							}
							else if (parts[3].equals(TRANSITION_START_DATE_STR)) {
								if (data.length() > 0) {
									hectime.set(data);
									shiftInfo.get(shiftPointSetCount-1).transitionStartDate = hectime.getTimeInMillis();
								}
							}
							else if (parts[3].equals(ACTIVE_STR)) {
								hectime.set(data);
								shiftInfo.get(shiftPointSetCount-1).active = Boolean.parseBoolean(data);
							}
						}
					}
					else if (parts[1].equals(TRANSITIONAL_RATING_STR)) {
						if (parts[2].equals(SOURCE_RATINGS_STR)) {
							if (parts[3].equals(RATING_SPEC_ID_STR)) {
									if (sourceRatingIdsByPos == null) {
										sourceRatingIdsByPos = new HashMap<Integer, String>();
									}
									if (sourceRatingIdsByPos.containsKey(pos)) {
										throw new RuntimeException(String.format("Transitional rating %s specifies source rating %d more than once.", trrc.ratingSpecId, pos+1));
									}
									sourceRatingIdsByPos.put(pos, trrc.officeId+"/"+data);
							}
						}
						else if (parts[2].equals(SELECT_STR)) {
							if (parts[3].equals(DEFAULT_STR)) {
								if (defaultEvaluation != null) {
									throw new RuntimeException(String.format("Select structure or transitional rating %s specifies multiple default evalations", trrc.ratingSpecId));
								}
								defaultEvaluation = data;
							}
						}
					}
					break;
				case 5 :
					if (parts[1].equals(RATING_TEMPLATE_STR)) {
						if (parts[2].equals(IND_PARAMETER_SPECS_STR) && parts[3].equals(IND_PARAMETER_SPEC_STR)) {
							if (parts[4].equals(PARAMETER_STR)) {
								rtc.indParams[pos] = data;
							}
							else if (parts[4].equals(IN_RANGE_METHOD_STR)) {
								rtc.inRangeMethods[pos] = data;
							}
							else if (parts[4].equals(OUT_RANGE_LOW_METHOD_STR)) {
								rtc.outRangeLowMethods[pos] = data;
							}
							else if (parts[4].equals(OUT_RANGE_HIGH_METHOD_STR)) {
								rtc.outRangeHighMethods[pos] = data;
							}
						}
					}
					else if (parts[1].equals(RATING_STR) ||
							 parts[1].equals(SIMPLE_RATING_STR) ||
							 parts[1].equals(USGS_STREAM_RATING_STR)) {
						if (parts[3].equals(POINT_STR)) {
							if (parts[2].equals(RATING_POINTS_STR)) {
								if (parts[4].equals(IND_STR)) {
									ratingPoints.get(ratingPointSetCount-1).addIndValue(Double.parseDouble(data));
								}
								else if (parts[4].equals(DEP_STR)) {
									ratingPoints.get(ratingPointSetCount-1).addDepValue(Double.parseDouble(data));
								}
								else if (parts[4].equals(NOTE_STR)) {
									ratingPoints.get(ratingPointSetCount-1).addNote(data);
								}
							}
							else if (parts[2].equals(EXTENSION_POINTS_STR)) {
								if (parts[4].equals(IND_STR)) {
									extensionPoints.get(extensionPointSetCount-1).addIndValue(Double.parseDouble(data));
								}
								else if (parts[4].equals(DEP_STR)) {
									extensionPoints.get(extensionPointSetCount-1).addDepValue(Double.parseDouble(data));
								}
								else if (parts[4].equals(NOTE_STR)) {
									extensionPoints.get(extensionPointSetCount-1).addNote(data);
								}
							}
							else if (parts[2].equals(HEIGHT_OFFSETS_STR)) {
								if (parts[4].equals(IND_STR)) {
									offsetPoints.add(new RatingPoints());
									offsetPoints.get(offsetPointSetCount-1).addIndValue(Double.parseDouble(data));
								}
								else if (parts[4].equals(DEP_STR)) {
									offsetPoints.get(offsetPointSetCount-1).addDepValue(Double.parseDouble(data));
								}
								else if (parts[4].equals(NOTE_STR)) {
									offsetPoints.get(offsetPointSetCount-1).addNote(data);
								}
							}
							else if (parts[2].equals(HEIGHT_SHIFTS_STR)) {
								if (parts[4].equals(IND_STR)) {
									shiftPoints.add(new RatingPoints());
									shiftPoints.get(shiftPointSetCount-1).addIndValue(Double.parseDouble(data));
								}
								else if (parts[4].equals(DEP_STR)) {
									shiftPoints.get(shiftPointSetCount-1).addDepValue(Double.parseDouble(data));
								}
								else if (parts[4].equals(NOTE_STR)) {
									shiftPoints.get(shiftPointSetCount-1).addNote(data);
								}
							}
						}
					}
					else if (parts[1].equals(VIRTUAL_RATING_STR)) {
						if (parts[2].equals(SOURCE_RATINGS_STR)) {
 							if (parts[3].equals(SOURCE_RATING_STR)) {
 								if (parts[4].equals(RATING_SPEC_ID_STR) || 
 									parts[4].equals(RATING_EXPRESSION_STR)) {
 										sourceRatingIdsByPos.put(pos, arc.officeId+"/"+data);
 								}
 							}
						}
					}
					else if (parts[1].equals(TRANSITIONAL_RATING_STR)) {
						if (parts[2].equals(SELECT_STR)) {
							if (parts[3].equals(CASE_STR)) {
								if (parts[4].equals(WHEN_STR)) {
									if (conditions == null) {
										conditions = new HashMap<Integer, String>();
									}
									if (conditions.containsKey(pos)) {
										throw new RuntimeException(String.format("Transitional rating %s selection case %d is specified more than once, or case %d contains multiple <WHEN> elements", trrc.ratingSpecId, pos+1, pos+1));
									}
									conditions.put(pos, data);
								}
								else if (parts[4].equals(THEN_STR)) {
									if (evaluations == null) {
										evaluations = new HashMap<Integer, String>();
									}
									if (evaluations.containsKey(pos)) {
										throw new RuntimeException(String.format("Transitional rating %s selection case %d is specified more than once, or case %d contains multiple <THEN> elements", trrc.ratingSpecId, pos+1, pos+1));
									}
									evaluations.put(pos, data);
								}
							}
						}
					}
					break;
				}
			}
		}
	}
	/*
	 * SAX method - do some initialization 
	 */
	@Override
	public void startDocument() {
		partsLen = 0;
	}
	/*
	 * SAX method - generate the RatingSet object from the data
	 */
	@Override
	public void endDocument() {
		
		if (rtcsById == null || rtcsById.size() == 0) {
			throw new RuntimeException("No rating templates in data");
		}
		if(rspcsById == null || rspcsById.size() == 0) {
			throw new RuntimeException("No rating specifications in data");
		}
		if(arcs == null || arcs.size() == 0) {
			throw new RuntimeException("No ratings in data");
		}
		int rtcCount = rtcsById.size();
		int rspcCount = rspcsById.size();
		int vrcCount = 0;
		int trcCount = 0;
		for (AbstractRatingContainer arc : arcs) {
			if (arc instanceof VirtualRatingContainer) {
				vrcCount++;
			}
			else if (arc instanceof TransitionalRatingContainer) {
				trcCount++;
			}
		}
		if (vdcs != null && vdcs.size() > 0) {
			for (int i = 1; i < vdcs.size(); ++i) {
				if (!vdcs.get(i).equals(vdcs.get(0))) {
					throw new RuntimeException("XML contains inconsistent vertical datum information");
				}
			}
				for (Iterator<AbstractRatingContainer> it = arcs.iterator(); it.hasNext();) {
					it.next().vdc = vdcs.get(0).clone();
			}
			vdcs = null;
		}
		if (vrcCount + trcCount > 0) {
			//------------------------------------//
			// determine the usage of each rating //
			//------------------------------------//
			final int NO_INFO = 0;
			final int HAS_SOURCE_RATINGS = 1;
			final int IS_SOURCE_RATING = 2;
			Map<String, Integer> usageById = new HashMap<String, Integer>();
			for(SortedSet<AbstractRatingContainer> rcs : arcsById.values()) {
				String specId = rcs.first().toString();
				usageById.put(specId, NO_INFO);
			}
			for(SortedSet<AbstractRatingContainer> rcs : arcsById.values()) {
				AbstractRatingContainer rc = rcs.first();
				if (rc instanceof VirtualRatingContainer) {
					String specId = rc.toString();
					usageById.put(specId, usageById.get(specId) | HAS_SOURCE_RATINGS);
					for (String srcSpecId : ((VirtualRatingContainer)rc).sourceRatingIds) {
						srcSpecId = TextUtil.split(srcSpecId, "{")[0].trim(); // cut off the units
						if (usageById.containsKey(srcSpecId)) {
							usageById.put(srcSpecId, usageById.get(srcSpecId) | IS_SOURCE_RATING);
						}
					}
				}
				else if (rc instanceof TransitionalRatingContainer) {
					String specId = rc.toString();
					usageById.put(specId, usageById.get(specId) | HAS_SOURCE_RATINGS);
					TransitionalRatingContainer trrc = (TransitionalRatingContainer)rc;
					if (trrc.sourceRatingIds != null) {
						for (String srcSpecId : trrc.sourceRatingIds) {
							srcSpecId = TextUtil.split(srcSpecId, "{")[0].trim(); // cut off the units
							if (usageById.containsKey(srcSpecId)) {
								usageById.put(srcSpecId, usageById.get(srcSpecId) | IS_SOURCE_RATING);
							}
						}
					}
				}
			}
			List<String> noInfoIds = new ArrayList<String>();
			List<String> topLevelIds = new ArrayList<String>();
			for(SortedSet<AbstractRatingContainer> rcs : arcsById.values()) {
				String specId = rcs.first().toString();
				switch (usageById.get(specId)) {
				case NO_INFO :
					noInfoIds.add(specId);
					break;
				case HAS_SOURCE_RATINGS :
					if (!topLevelIds.contains(specId)) {
						topLevelIds.add(specId);
					}
					break;
				case HAS_SOURCE_RATINGS | IS_SOURCE_RATING :
					// source rating that has source ratings
					break;
				}
			}
			//--------------------------------------------//
			// raise exceptions on invalid configurations //
			//--------------------------------------------//
			if (topLevelIds.size() > 1) {
				StringBuilder sb = new StringBuilder();
				sb.append("XML includes more than one top level rating:");
				for (int i = 0; i < topLevelIds.size(); ++i) {
					sb.append("\n\t").append((i+1)).append(": ").append(topLevelIds.get(i));
				}
				throw new RuntimeException(sb.toString());
			}
			else if (noInfoIds.size() > 0) {
				StringBuilder sb = new StringBuilder();
				sb.append("XML includes one or more unused ratings:");
				for (int i = 0; i < noInfoIds.size(); ++i) {
					sb.append("\n\t").append((i+1)).append(": ").append(noInfoIds.get(i));
				}
				throw new RuntimeException(sb.toString());
			}
			String topLevelSpecId = topLevelIds.get(0);
			//-----------------------------//
			// build the rating containers //
			//-----------------------------//
			arcs = arcsById.get(topLevelSpecId);
			for (AbstractRatingContainer arc : arcs) {
				if (arc instanceof VirtualRatingContainer) {
					vrc = (VirtualRatingContainer)arc;
					vrc.populateSourceRatings(arcsById, rspcsById, rtcsById);
				}
				else if (arc instanceof TransitionalRatingContainer) {
					trrc = (TransitionalRatingContainer)arc;
					trrc.populateSourceRatings(arcsById, rspcsById, rtcsById);
				}
			}
			//------------------------------------//
			// populate the rating spec container //
			//------------------------------------//
			rspc = rspcsById.get(topLevelSpecId).clone();
			String[] parts = TextUtil.split(topLevelSpecId, SEPARATOR1);
			String templateId = TextUtil.join(SEPARATOR1, parts[1], parts[2]);
			rtcsById.get(templateId).clone(rspc);
			//-----------------------------------//
			// populate the rating set container //
			//-----------------------------------//
			rsc.ratingSpecContainer = rspc;
			rsc.abstractRatingContainers = arcs.toArray(new AbstractRatingContainer[0]);
			
			//----------------------------------------------//
			// output info about unused specs and templates //
			//----------------------------------------------//
			List<AbstractRatingContainer> containersUsed = new ArrayList<AbstractRatingContainer>();
			containersUsed.addAll(arcs);
			for (int i = 0; i < containersUsed.size(); ++i) {
				AbstractRatingContainer _arc = containersUsed.get(i);
				if (_arc instanceof VirtualRatingContainer) {
					VirtualRatingContainer _vrc = (VirtualRatingContainer)_arc;
					for (int j = 0; j < _vrc.sourceRatings.length; ++j) {
						if (_vrc.sourceRatings[j].rsc != null) {
							for (int k = 0; k < _vrc.sourceRatings[j].rsc.abstractRatingContainers.length; ++k) {
								if (!containersUsed.contains(_vrc.sourceRatings[j].rsc.abstractRatingContainers[k])) {
									containersUsed.add(_vrc.sourceRatings[j].rsc.abstractRatingContainers[k]);
								}
							}
						}
					}
				}
				else if (_arc instanceof TransitionalRatingContainer) {
					TransitionalRatingContainer _trc = (TransitionalRatingContainer)_arc;
					for (int j = 0; j < _trc.sourceRatings.length; ++j) {
						if (_trc.sourceRatings[j].rsc != null) {
							for (int k = 0; k < _trc.sourceRatings[j].rsc.abstractRatingContainers.length; ++k) {
								if (!containersUsed.contains(_trc.sourceRatings[j].rsc.abstractRatingContainers[k])) {
									containersUsed.add(_trc.sourceRatings[j].rsc.abstractRatingContainers[k]);
								}
							}
						}
					}
				}
			}
			Set<String> usedIds = new TreeSet<String>();
			for (AbstractRatingContainer _arc : containersUsed) {
				String specId = String.format("%s/%s", _arc.officeId, _arc.ratingSpecId);
				parts = TextUtil.split(_arc.ratingSpecId, SEPARATOR1);
				templateId = TextUtil.join(SEPARATOR1, parts[1], parts[2]);
				usedIds.add(specId);
				usedIds.add(templateId);
			}
			StringBuilder sb1 = new StringBuilder();
			StringBuilder sb2 = new StringBuilder();
			for (Iterator<String> it = rspcsById.keySet().iterator(); it.hasNext();) {
				String specId = it.next();
				if (!usedIds.contains(specId)) {
					sb2.append("\n\tspecification: ").append(specId);
				}
				parts = TextUtil.split(specId, SEPARATOR1);
				templateId = TextUtil.join(SEPARATOR1, parts[1], parts[2]);
				if (!usedIds.contains(templateId)) {
					sb1.append("\n\ttemplate     : ").append(templateId);
				}
			}
			sb1.append(sb2);
			if (sb1.length() > 0) {
				AbstractRating.logger.info("XML conatins unused templates and/or specifications:" + sb1.toString());
			}
			AbstractRating.logger.fine("Top level rating = " + topLevelIds.get(0));
		}
		else {
			//------------------------------------------------------------------------------------------------//
			// simple ratings only, must have only one template, one spec, and they should agree with ratings //
			//------------------------------------------------------------------------------------------------//
			if (rtcCount > 1) {
				throw new RuntimeException("XML specifies more than one rating template.");
			}
			if (rspcCount > 1) {
				throw new RuntimeException("XML specifies more than one rating specification.");
			}
			//------------------------------------//
			// populate the rating spec container //
			//------------------------------------//
			rspc = rspcsById.values().iterator().next();
			rtc = rtcsById.values().iterator().next();
			String specId = rspc.toString();
			String templateId = rtc.toString();
			String[] parts = TextUtil.split(specId, SEPARATOR1);
			if (!(TextUtil.join(SEPARATOR1, parts[1], parts[2])).equals(templateId)) {
				throw new RuntimeException("Specification ("+specId+") does not agree with template ("+templateId+")");
			}
			rtc.clone(rspc);
			//-----------------------------------//
			// populate the rating set container //
			//-----------------------------------//
			rsc.ratingSpecContainer = rspc.clone();
			rsc.abstractRatingContainers = arcs.toArray(new AbstractRatingContainer[0]);
		}
		
		partsLen = -1;
		StringBuilder sb = new StringBuilder();
		sb.append("\nRating Templates:\n");
		for (RatingTemplateContainer rtc : rtcsById.values()) {
			sb.append("\t" + rtc.toString()+"\n");
		}
		sb.append("\nRating Specs:\n");
		for (RatingSpecContainer rspc : rspcsById.values()) {
			sb.append("\t" + rspc.toString()+"\n");
		}
		sb.append("\nRatings:\n");
		for (SortedSet<AbstractRatingContainer> rcs : arcsById.values()) {
			for (AbstractRatingContainer rc : rcs) {
				HecTime ht = new HecTime();
				ht.setTimeInMillis(rc.effectiveDateMillis);
				sb.append("\t").
				   append(rc.toString()).
				   append("@").
				   append(ht.getXMLDateTime(0)).
				   append(" ").
				   append(" (").
				   append(rc.getClass().getName()).
				   append(")\n");
				
			}
		}
		AbstractRating.logger.fine(sb.toString());
	}
	/*
	 * SAX method - validate structure and process attributes 
	 */
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attrs) {
		if (inVerticalDatumInfo) {
			verticalDatumInfo.append("<").append(localName);
			for (int i = 0; i < attrs.getLength(); ++i) {
				verticalDatumInfo.append(" ")
				                 .append(attrs.getLocalName(i))
				                 .append("=\"")
				                 .append(attrs.getValue(i))
                                 .append("\"");
                                 
			}
			verticalDatumInfo.append(">");
		}
		parts[partsLen++] = localName;
		switch (partsLen) {
		case 1 :
			if (localName.equals(RATINGS_STR)) {
				rsc = new RatingSetContainer();
			}
			else {
				throw new RuntimeException("Invalid document node name: " + parts[0]);
			}
			break;
		case 2 :
			if (localName.equals(RATING_TEMPLATE_STR)) {
				if (rspc != null) {
					throw new RuntimeException("Only one rating template may be processed");
				}
				rtc = new RatingTemplateContainer();
				rtc.officeId = attrs.getValue(OFFICE_ID_STR);
			}
			else if (localName.equals(RATING_SPEC_STR)) {
				rspc = new RatingSpecContainer();
				rspc.specOfficeId = rspc.officeId = attrs.getValue(OFFICE_ID_STR);
				if (rtc != null) {
					rtc.clone(rspc);
				}
			}
			else if (localName.equals(RATING_STR) ||
					 localName.equals(SIMPLE_RATING_STR) ||
					 localName.equals(USGS_STREAM_RATING_STR) ||
					 localName.equals(VIRTUAL_RATING_STR) ||
					 localName.equals(TRANSITIONAL_RATING_STR)) {
				arc = new AbstractRatingContainer();
				arc.officeId = attrs.getValue(OFFICE_ID_STR);
			}
			else elementError();
			break;
		case 3 :
			if (parts[1].equals(RATING_STR) ||
				parts[1].equals(SIMPLE_RATING_STR) ||
				parts[1].equals(USGS_STREAM_RATING_STR)) {
				if (localName.equals(RATING_POINTS_STR)) {
					if (parts[1].equals(RATING_STR) || parts[1].equals(SIMPLE_RATING_STR)) {
						trc = new TableRatingContainer();
						arc.clone(trc);
						if (ratingPoints == null) ratingPoints = new ArrayList<RatingPoints>();
						ratingPoints.add(new RatingPoints());
						++ratingPointSetCount;
					}
					else {
						if (ratingPointSetCount == 0) {
							urc = new UsgsStreamTableRatingContainer();
							arc.clone(urc);
							ratingPoints = new ArrayList<RatingPoints>();
							ratingPoints.add(new RatingPoints());
							++ratingPointSetCount;
						}
						else {
							throw new RuntimeException("Multiple independent paramters are not allowed on USGS-style stream ratings"); 
						}
					}
				}
				else if (localName.equals(FORMULA_STR)) {
					erc = new ExpressionRatingContainer();
					arc.clone(erc);
				}
				else if (localName.equals(EXTENSION_POINTS_STR)) {
					switch (ratingPointSetCount) {
					case 0 :
						throw new RuntimeException("Extension points cannot be specified without or before rating points");
					case 1 :
						if (extensionPointSetCount == 0) {
							extensionPoints = new ArrayList<RatingPoints>();
							extensionPoints.add(new RatingPoints());
							++extensionPointSetCount;
						}
						else {
							throw new RuntimeException("Only one set of extension points is allowed");
						}
						break;
					default :
						throw new RuntimeException("Extension points are allowed only on single independent parameter ratings");
					}
				}
				else if (localName.equals(HEIGHT_SHIFTS_STR)) {
					if (parts[1].equals(USGS_STREAM_RATING_STR)) {
						if (shiftPointSetCount == 0) {
							shiftPoints = new ArrayList<RatingPoints>();
							shiftInfo = new ArrayList<ShiftInfo>();
						}
						shiftPoints.add(new RatingPoints());
						shiftInfo.add(new ShiftInfo());
						++shiftPointSetCount;					
					}
					else elementError();
					}
				else if (localName.equals(HEIGHT_OFFSETS_STR)) {
					if (parts[1].equals(USGS_STREAM_RATING_STR)) {
						if (offsetPointSetCount == 0) {
							offsetPoints = new ArrayList<RatingPoints>();
							++offsetPointSetCount;
						}
						else {
							throw new RuntimeException("Only one set offsets is allowed");
						}
					}
					else elementError();
				}
				else if (localName.equals(RATING_SPEC_ID_STR)) ;
				else if (localName.equals(VERTICAL_DATUM_INFO_STR)) {
					inVerticalDatumInfo = true;
					if (verticalDatumInfo == null) {
						verticalDatumInfo = new StringBuilder();
					}
					else {
						verticalDatumInfo.delete(0, verticalDatumInfo.length());
					}
					verticalDatumInfo.append("<" + VERTICAL_DATUM_INFO_STR);
					for (int i = 0; i < attrs.getLength(); ++i) {
						verticalDatumInfo.append(" ")
						                 .append(attrs.getLocalName(i))
						                 .append("=\"")
						                 .append(attrs.getValue(i))
						                 .append("\"");
					}
					verticalDatumInfo.append(">");
				}
				else if (localName.equals(UNITS_ID_STR)) ;
				else if (localName.equals(EFFECTIVE_DATE_STR)) ;
				else if (localName.equals(TRANSITION_START_DATE_STR)) ;
				else if (localName.equals(CREATE_DATE_STR)) ;
				else if (localName.equals(ACTIVE_STR)) ;
				else if (localName.equals(DESCRIPTION_STR)) ;
				else elementError();
			}
			else if (parts[1].equals(VIRTUAL_RATING_STR)) {
				if (localName.equals(RATING_SPEC_ID_STR)) {
					vrc = new VirtualRatingContainer();
					arc.clone(vrc);
				}
				else if (localName.equals(EFFECTIVE_DATE_STR));
				else if (localName.equals(TRANSITION_START_DATE_STR)) ;
				else if (localName.equals(CREATE_DATE_STR));
				else if (localName.equals(ACTIVE_STR));
				else if (localName.equals(DESCRIPTION_STR));
				else if (localName.equals(CONNECTIONS_STR));
				else if (localName.equals(SOURCE_RATINGS_STR));
				else elementError();
			}
			else if (parts[1].equals(TRANSITIONAL_RATING_STR)) {
				if (localName.equals(RATING_SPEC_ID_STR)) {
					trrc = new TransitionalRatingContainer();
					arc.clone(trrc);
				}
				else if (localName.equals(UNITS_ID_STR)) ;
				else if (localName.equals(EFFECTIVE_DATE_STR));
				else if (localName.equals(TRANSITION_START_DATE_STR)) ;
				else if (localName.equals(CREATE_DATE_STR));
				else if (localName.equals(ACTIVE_STR));
				else if (localName.equals(DESCRIPTION_STR));
				else if (localName.equals(SELECT_STR));
				else if (localName.equals(SOURCE_RATINGS_STR));
				else elementError();
			}
			else if (parts[1].equals(RATING_TEMPLATE_STR)) {
				if      (localName.equals(PARAMETERS_ID_STR)) ;
				else if (localName.equals(VERSION_STR)) ;
				else if (localName.equals(IND_PARAMETER_SPECS_STR)) ;
				else if (localName.equals(DEP_PARAMETER_STR)) ;
				else if (localName.equals(DESCRIPTION_STR)) ;
				else elementError();
			}
			else if (parts[1].equals(RATING_SPEC_STR)) {
				if      (localName.equals(RATING_SPEC_ID_STR)) ;
				else if (localName.equals(TEMPLATE_ID_STR)) ;
				else if (localName.equals(LOCATION_ID_STR)) ;
				else if (localName.equals(VERSION_STR)) ;
				else if (localName.equals(SOURCE_AGENCY_STR)) ;
				else if (localName.equals(IN_RANGE_METHOD_STR)) ;
				else if (localName.equals(OUT_RANGE_LOW_METHOD_STR)) ;
				else if (localName.equals(OUT_RANGE_HIGH_METHOD_STR)) ;
				else if (localName.equals(ACTIVE_STR)) ;
				else if (localName.equals(AUTO_UPDATE_STR)) ;
				else if (localName.equals(AUTO_ACTIVATE_STR)) ;
				else if (localName.equals(AUTO_MIGRATE_EXTENSION_STR)) ;
				else if (localName.equals(IND_ROUNDING_SPECS_STR)) ;
				else if (localName.equals(DEP_ROUNDING_SPEC_STR)) ;
				else if (localName.equals(DESCRIPTION_STR)) ;
				else elementError();
				}
			else elementError();
			break;
		case 4 :
			if (parts[1].equals(RATING_STR) ||
				parts[1].equals(SIMPLE_RATING_STR) ||
				parts[1].equals(USGS_STREAM_RATING_STR)) {
				if (parts[2].equals(HEIGHT_OFFSETS_STR)) {
					if(localName.equals(POINT_STR)) ;
					else elementError();
				}
				else if (parts[2].equals(RATING_POINTS_STR) || parts[2].equals(EXTENSION_POINTS_STR)) {
					if (localName.equals(OTHER_IND_STR)) {
						if (parts[2].equals(RATING_POINTS_STR)) {
							ratingPoints.get(ratingPointSetCount-1).addOtherInd(Double.parseDouble(attrs.getValue(VALUE_STR)));
						}
						else if (parts[2].equals(EXTENSION_POINTS_STR)) {
							extensionPoints.get(extensionPointSetCount-1).addOtherInd(Double.parseDouble(attrs.getValue(VALUE_STR)));
						}
						else elementError();
						}
					}
				else if (parts[2].equals(HEIGHT_SHIFTS_STR)) {
					if      (localName.equals(EFFECTIVE_DATE_STR)) ;
					else if (localName.equals(TRANSITION_START_DATE_STR)) ;
					else if (localName.equals(CREATE_DATE_STR)) ;
					else if (localName.equals(ACTIVE_STR)) ;
					else if (localName.equals(DESCRIPTION_STR)) ;
					else if (localName.equals(POINT_STR)) ;
					else elementError();
				}
				else elementError();
			}
			else if (parts[1].equals(VIRTUAL_RATING_STR)) {
				if (!(parts[2].equals(SOURCE_RATINGS_STR) && localName.equals(SOURCE_RATING_STR))) {
						elementError();
					}
				pos = Integer.parseInt(attrs.getValue(POSITION_STR)); 
				if (pos < 1) {
					throw new RuntimeException("Virtual rating specifies invalid source rating postion: ("+pos+").");
				}
				if (sourceRatingIdsByPos == null) {
					sourceRatingIdsByPos = new HashMap<Integer, String>();
				}
				else if (sourceRatingIdsByPos.containsKey(pos)) {
					throw new RuntimeException("Virtual rating specifies source rating postion "+pos+" more than once.");
				}
			}
			else if (parts[1].equals(TRANSITIONAL_RATING_STR)) {
				if (parts[2].equals(SELECT_STR)) {
					if (localName.equals(CASE_STR)) {
						pos = Integer.parseInt(attrs.getValue(POSITION_STR)); 
				}
					else if (localName.equals(DEFAULT_STR)) ;
					else elementError();
				}
				else if (parts[2].equals(SOURCE_RATINGS_STR)) {
					if (localName.equals(RATING_SPEC_ID_STR)) {
						pos = Integer.parseInt(attrs.getValue(POSITION_STR)); 
					}
					else elementError();
				}
				else elementError();
			}
			else if (parts[1].equals(RATING_TEMPLATE_STR)) {
				if (parts[2].equals(IND_PARAMETER_SPECS_STR)) {
					if (localName.equals(IND_PARAMETER_SPEC_STR)) {
						pos = Integer.parseInt(attrs.getValue(POSITION_STR)) - 1; 
					}
					else elementError();
					}
				else elementError();
				}
			else if (parts[1].equals(RATING_SPEC_STR)) {
				if (parts[2].equals(IND_ROUNDING_SPECS_STR)) {
					if (localName.equals(IND_ROUNDING_SPEC_STR)) {
						pos = Integer.parseInt(attrs.getValue(POSITION_STR)) - 1; 
					}
					else elementError();
					}
				else elementError();
				}
			else elementError();
			break;
		case 5 :
			if (parts[1].equals(RATING_TEMPLATE_STR)) {
				if (parts[2].equals(IND_PARAMETER_SPECS_STR)) {
					if (parts[3].equals(IND_PARAMETER_SPEC_STR)) {
						if      (localName.equals(PARAMETER_STR)) ;
						else if (localName.equals(IN_RANGE_METHOD_STR)) ;
						else if (localName.equals(OUT_RANGE_LOW_METHOD_STR)) ;
						else if (localName.equals(OUT_RANGE_HIGH_METHOD_STR)) ;
						else elementError();
					}
					else elementError();
				}
				else elementError();
			}
			else if (parts[1].equals(RATING_STR) ||
					 parts[1].equals(SIMPLE_RATING_STR) ||
					 parts[1].equals(USGS_STREAM_RATING_STR)) {
				if (parts[2].equals(HEIGHT_OFFSETS_STR) || parts[2].equals(HEIGHT_SHIFTS_STR)) {
					if (parts[1].equals(RATING_STR) || parts[1].equals(SIMPLE_RATING_STR)) {
						elementError();
					}
					if (parts[3].equals(POINT_STR)) {
						if      (localName.equals(IND_STR)) ;
						else if (localName.equals(DEP_STR)) ;
						else if (localName.equals(NOTE_STR)) ;
						else elementError();
						}
					else elementError();
				}
				else if (parts[2].equals(RATING_POINTS_STR) || parts[2].equals(EXTENSION_POINTS_STR)) {
					if (parts[3].equals(POINT_STR)) {
						if      (localName.equals(IND_STR)) ;
						else if (localName.equals(DEP_STR)) ;
						else if (localName.equals(NOTE_STR)) ;
						else elementError();
					}
					else elementError();
				}
				else elementError();
			}
			else if (parts[1].equals(VIRTUAL_RATING_STR)) {
				if (parts[2].equals(SOURCE_RATINGS_STR)) {
					if (parts[3].equals(SOURCE_RATING_STR)) {
						if      (localName.equals(RATING_SPEC_ID_STR)) ;
						else if (localName.equals(RATING_EXPRESSION_STR)) ;
						else elementError();
						}
					else elementError();
					}
				else elementError();
					}
			else if (parts[1].equals(TRANSITIONAL_RATING_STR)) {
				if (parts[2].equals(SELECT_STR)) {
					if (parts[3].equals(CASE_STR)) {
						if      (localName.equals(WHEN_STR)) ;
						else if (localName.equals(THEN_STR)) ;
						else elementError();
				}
					else elementError();
				}
				else elementError();
			}
			else elementError();
			break;			
		default :
			elementError();
		}
	}
	/*
	 * SAX method - validate well-formedness and process data components
	 */
	@Override
	public void endElement(String uri, String localName, String qName) {
		processCharacters();
		if (inVerticalDatumInfo) {
			verticalDatumInfo.append("</").append(localName).append(">");
		}
		if (!localName.equals(parts[partsLen-1])) {
			throw new RuntimeException("Expected end element " + parts[partsLen-1] + ", got " + localName);
		}
		switch (partsLen) {
		case 2 :
			if (localName.equals(RATING_TEMPLATE_STR)) {
				try {
					RatingMethod[] inRangeMethods = new RatingMethod[rtc.indParams.length];
					RatingMethod[] outRangeLowMethods = new RatingMethod[rtc.indParams.length];
					RatingMethod[] outRangeHighMethods = new RatingMethod[rtc.indParams.length];
					for (int i = 0; i < rtc.indParams.length; ++i) {
						inRangeMethods[i] = RatingMethod.fromString(rtc.inRangeMethods[i]);
						outRangeLowMethods[i] = RatingMethod.fromString(rtc.outRangeLowMethods[i]);
						outRangeHighMethods[i] = RatingMethod.fromString(rtc.outRangeHighMethods[i]);
					}
					String id = rtc.toString();
					if (rtcsById == null) {
						rtcsById = new HashMap<String, RatingTemplateContainer>();
				}
					else if (rtcsById.containsKey(id)) {
						throw new RuntimeException("Rating template specified multiple times: "+id);
					}
					rtcsById.put(rtc.toString(), rtc);
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
				rtc = null;
			}
			else if (localName.equals(RATING_SPEC_STR)) {
				String id = rspc.toString();
				if (rspcsById == null) {
					rspcsById = new HashMap<String, RatingSpecContainer>();
				}
				else if (rspcsById.containsKey(id)) {
					throw new RuntimeException("Rating specification specified multiple times: "+id);
				}
				rspcsById.put(id, rspc);
				rspc = null;
			}
			else if (localName.equals(RATING_STR             ) || 
					 localName.equals(SIMPLE_RATING_STR      ) ||
					 localName.equals(USGS_STREAM_RATING_STR ) ||
					 localName.equals(VIRTUAL_RATING_STR     ) ||
					 localName.equals(TRANSITIONAL_RATING_STR)) {
				if (arcs == null) {
					arcs = new TreeSet<AbstractRatingContainer>();
					arcsById = new HashMap<String, SortedSet<AbstractRatingContainer>>();
				}
				if (localName.equals(RATING_STR) || localName.equals(SIMPLE_RATING_STR)) {
					if (erc != null) {
						arcs.add(erc.clone());
						arc = erc;
					erc = null;
					}
					else if (trc != null) {
						String[] parts = TextUtil.split(trc.ratingSpecId, SEPARATOR1);
						RatingTemplateContainer thisRtc = rtcsById.get(TextUtil.join(SEPARATOR1, parts[1], parts[2]));
						trc.inRangeMethod = thisRtc.inRangeMethods[0];
						trc.outRangeLowMethod = thisRtc.outRangeLowMethods[0];
						trc.outRangeHighMethod = thisRtc.outRangeHighMethods[0];
						int width = 0;
						int depth = 0;
						for (int i = 0; i < ratingPointSetCount; ++i) {
							RatingPoints pointSet = ratingPoints.get(i);
							if (i == 0) {
								width = pointSet.getOtherIndCount();
							}
							else if (pointSet.getOtherIndCount() != width) {
								throw new RuntimeException("Inconsistent number of independent parameters");
							}
							depth += pointSet.getPointCount();							
						}
						width += 2;
						double[][] points = new double[depth][];
						String[] notes = new String[depth];
						for (int i = 0; i < depth; ++i) {
							points[i] = new double[width];
							notes[i] = null;
						}
						for (int i = 0, d = 0; i < ratingPointSetCount; ++i) {
							RatingPoints pointSet = ratingPoints.get(i);
							for (int j = 0; j < pointSet.getPointCount(); ++j, ++d) {
								for (int w = 0; w < width-2; ++w) {
									points[d][w] = pointSet.getOtherInd(w);
								}
								points[d][width-2] = pointSet.getIndValue(j);
								points[d][width-1] = pointSet.getDepValue(j);
								notes[d] = pointSet.getNote(j);
							}
						}
						if (points == null || points.length == 0) {
							if (requireRatingPoints) {
								throw new RuntimeException("No rating values!");
							}
							trc.values = null;
						}
						else {
							trc.values = RatingValueContainer.makeContainers(
									points, 
									notes, 
											thisRtc.inRangeMethods,
											thisRtc.outRangeLowMethods,
											thisRtc.outRangeHighMethods);
						}
						if (extensionPointSetCount == 1) {
							if (width > 2) {
								throw new RuntimeException("Cannot have extension points with more than one independent parameter");
							}
							int pointCount = extensionPoints.get(0).getPointCount();
							trc.extensionValues = new RatingValueContainer[pointCount];
							for (int i = 0; i < pointCount; ++i) {
								trc.extensionValues[i] = new RatingValueContainer();
								trc.extensionValues[i].indValue = extensionPoints.get(0).getIndValue(i);
								trc.extensionValues[i].depValue = extensionPoints.get(0).getDepValue(i);
								trc.extensionValues[i].note = extensionPoints.get(0).getNote(i);
							}
						}
						ratingPoints = null;
						ratingPointSetCount = 0;
						extensionPoints = null;
						extensionPointSetCount = 0;
						arcs.add(trc.clone());
						arc = trc;
						trc = null;
					}
				}
				else if (localName.equals(USGS_STREAM_RATING_STR)) {
					String[] parts = TextUtil.split(urc.ratingSpecId, SEPARATOR1);
					String location = parts[0];
					String indParam = TextUtil.split(parts[1], SEPARATOR2)[0];
					String heightUnit = TextUtil.split(TextUtil.split(urc.unitsId, RatingConst.SEPARATOR2)[0], RatingConst.SEPARATOR3)[0];
					RatingTemplateContainer rtc = rtcsById.get(TextUtil.join(SEPARATOR1, parts[1], parts[2]));
					urc.inRangeMethod = rtc.inRangeMethods[0];
					urc.outRangeLowMethod = rtc.outRangeLowMethods[0];
					urc.outRangeHighMethod = rtc.outRangeHighMethods[0];
					int pointCount = ratingPoints.get(0).getPointCount();
					if (pointCount > 0) {
						urc.values = new RatingValueContainer[pointCount];
						for (int i = 0; i < pointCount; ++i) {
							urc.values[i] = new RatingValueContainer();
							urc.values[i].indValue = ratingPoints.get(0).getIndValue(i);
							urc.values[i].depValue = ratingPoints.get(0).getDepValue(i);
							urc.values[i].note = ratingPoints.get(0).getNote(i);
						}
						if (extensionPointSetCount == 1) {
							pointCount = extensionPoints.get(0).getPointCount();
							urc.extensionValues = new RatingValueContainer[pointCount];
							for (int i = 0; i < pointCount; ++i) {
								urc.extensionValues[i] = new RatingValueContainer();
								urc.extensionValues[i].indValue = extensionPoints.get(0).getIndValue(i);
								urc.extensionValues[i].depValue = extensionPoints.get(0).getDepValue(i);
								urc.extensionValues[i].note = extensionPoints.get(0).getNote(i);
							}
						}
						if (offsetPointSetCount > 0) {
							urc.offsets = new TableRatingContainer();
							RatingPoints pointSet = offsetPoints.get(0);
							int offsetCount = pointSet.getPointCount();
							urc.offsets.values = new RatingValueContainer[offsetCount];
							for (int i = 0; i < offsetCount; ++i) {
								urc.offsets.values[i] = new RatingValueContainer();
								urc.offsets.values[i].indValue = pointSet.getIndValue(i);
								urc.offsets.values[i].depValue = pointSet.getDepValue(i);
								urc.offsets.values[i].note = pointSet.getNote(i);
							}
							urc.offsets.unitsId = String.format("%s%s%s", heightUnit, SEPARATOR2, heightUnit);
							urc.offsets.ratingSpecId = TextUtil.join(SEPARATOR1, 
									location,
									String.format("%s%s%s-%s", indParam, SEPARATOR2, indParam, USGS_OFFSETS_SUBPARAM),
									USGS_OFFSETS_TEMPLATE_VERSION,
									USGS_OFFSETS_SPEC_VERSION);
							urc.offsets.inRangeMethod = "PREVIOUS";
							urc.offsets.outRangeLowMethod = "NEXT";
							urc.offsets.outRangeHighMethod = "PREVIOUS";
						}
						if (shiftPointSetCount > 0) {
							urc.shifts = new RatingSetContainer();
							urc.shifts.ratingSpecContainer = new RatingSpecContainer();
							urc.shifts.ratingSpecContainer.inRangeMethod = "LINEAR";
							urc.shifts.ratingSpecContainer.outRangeLowMethod = "NEAREST";
							urc.shifts.ratingSpecContainer.outRangeHighMethod = "NEAREST";
							urc.shifts.ratingSpecContainer.indParams = new String[1];
							urc.shifts.ratingSpecContainer.indParams[0] = indParam;
							urc.shifts.ratingSpecContainer.depParam = String.format("%s-%s", indParam, USGS_SHIFTS_SUBPARAM);
							urc.shifts.ratingSpecContainer.locationId = location;
							urc.shifts.ratingSpecContainer.parametersId = String.format("%s%s%s", urc.shifts.ratingSpecContainer.indParams[0], SEPARATOR2, urc.shifts.ratingSpecContainer.depParam);
							urc.shifts.ratingSpecContainer.templateVersion = USGS_SHIFTS_TEMPLATE_VERSION;
							urc.shifts.ratingSpecContainer.templateId = String.format("%s.%s", urc.shifts.ratingSpecContainer.parametersId, urc.shifts.ratingSpecContainer.templateVersion);
							urc.shifts.ratingSpecContainer.specVersion = USGS_SHIFTS_SPEC_VERSION;
							urc.shifts.ratingSpecContainer.specId = TextUtil.join(SEPARATOR1, 
									urc.shifts.ratingSpecContainer.locationId, 
									urc.shifts.ratingSpecContainer.templateId, 
									urc.shifts.ratingSpecContainer.specVersion);
							urc.shifts.ratingSpecContainer.inRangeMethods = new String[1];
							urc.shifts.ratingSpecContainer.inRangeMethods[0] = "LINEAR";
							urc.shifts.ratingSpecContainer.outRangeLowMethods = new String[1];
							urc.shifts.ratingSpecContainer.outRangeLowMethods[0] = "NEAREST";
							urc.shifts.ratingSpecContainer.outRangeHighMethods = new String[1];
							urc.shifts.ratingSpecContainer.outRangeHighMethods[0] = "NEAREST";
							urc.shifts.ratingSpecContainer.indRoundingSpecs = new String[1];
							urc.shifts.ratingSpecContainer.indRoundingSpecs[0] = "4444444449";
							urc.shifts.ratingSpecContainer.depRoundingSpec = "4444444449";
							int emptyCount = 0;
							for (int i = 0; i < shiftPointSetCount; ++i) {
								if (shiftPoints.get(i).getPointCount() == 0) ++emptyCount;
							}
							urc.shifts.abstractRatingContainers = new TableRatingContainer[shiftPointSetCount-emptyCount];
							for (int i = 0, j = -1; i < shiftPointSetCount; ++i) {
								RatingPoints pointSet = shiftPoints.get(i);
								int shiftCount = pointSet.getPointCount();
								if (shiftCount > 0) {
									urc.shifts.abstractRatingContainers[++j] = new TableRatingContainer();
									((TableRatingContainer)urc.shifts.abstractRatingContainers[j]).ratingSpecId = TextUtil.join(SEPARATOR1, 
											urc.shifts.ratingSpecContainer.locationId, 
											urc.shifts.ratingSpecContainer.templateId, 
											urc.shifts.ratingSpecContainer.specVersion);
									((TableRatingContainer)urc.shifts.abstractRatingContainers[j]).unitsId = String.format("%s%s%s", heightUnit, SEPARATOR2, heightUnit);
									((TableRatingContainer)urc.shifts.abstractRatingContainers[j]).effectiveDateMillis = shiftInfo.get(i).effectiveDate;
									((TableRatingContainer)urc.shifts.abstractRatingContainers[j]).transitionStartDateMillis = shiftInfo.get(i).transitionStartDate;
									((TableRatingContainer)urc.shifts.abstractRatingContainers[j]).createDateMillis = shiftInfo.get(i).createDate == Const.UNDEFINED_TIME ? System.currentTimeMillis() : shiftInfo.get(i).createDate;
									((TableRatingContainer)urc.shifts.abstractRatingContainers[j]).active = shiftInfo.get(i).active;
									((TableRatingContainer)urc.shifts.abstractRatingContainers[j]).values = new RatingValueContainer[shiftCount];
									((TableRatingContainer)urc.shifts.abstractRatingContainers[j]).inRangeMethod = "LINEAR";
									((TableRatingContainer)urc.shifts.abstractRatingContainers[j]).outRangeLowMethod = "NEAREST";
									((TableRatingContainer)urc.shifts.abstractRatingContainers[j]).outRangeHighMethod = "NEAREST";
									for (int k = 0; k < shiftCount; ++k) {
										((TableRatingContainer)urc.shifts.abstractRatingContainers[j]).values[k] = new RatingValueContainer();
										((TableRatingContainer)urc.shifts.abstractRatingContainers[j]).values[k].indValue = pointSet.getIndValue(k);
										((TableRatingContainer)urc.shifts.abstractRatingContainers[j]).values[k].depValue = pointSet.getDepValue(k);
										((TableRatingContainer)urc.shifts.abstractRatingContainers[j]).values[k].note = pointSet.getNote(k);
									}
								}
							}
						}
					}
					offsetPoints = null;
					offsetPointSetCount = 0;
					shiftPoints = null;
					shiftPointSetCount = 0;
					ratingPoints = null;
					ratingPointSetCount = 0;
					extensionPoints = null;
					extensionPointSetCount = 0;
					shiftInfo = null;
					arcs.add(urc.clone());
					arc = urc;
					urc = null;
				}
				else if (localName.equals(VIRTUAL_RATING_STR)) {
					SortedSet<Integer> keys = new TreeSet<Integer>(sourceRatingIdsByPos.keySet());
					vrc.sourceRatingIds = new String[keys.size()];
					Iterator<Integer> it = keys.iterator();
					for (int i = 0; it.hasNext(); ++i) {
						if (it.next() != (i+1)) {
							throw new RuntimeException("No position "+(i+1)+" in source ratings.");
						}
						vrc.sourceRatingIds[i] = sourceRatingIdsByPos.get((i+1));
					}
					sourceRatingIdsByPos.clear();
					arcs.add(vrc.clone());
					arc = vrc;
					vrc = null;
				}				
				else if (localName.equals(TRANSITIONAL_RATING_STR)) {
					SortedSet<Integer> keys = new TreeSet<Integer>(conditions.keySet());
					trrc.conditions = new String[keys.size()];
					Iterator<Integer> it = keys.iterator();
					for (int i = 0; it.hasNext(); ++i) {
						if (it.next() != (i+1)) {
							throw new RuntimeException("No position "+(i+1)+" in conditions.");
						}
						trrc.conditions[i] = conditions.get((i+1));
					}
					conditions.clear();
					keys.clear();
					keys.addAll(evaluations.keySet());
					if (keys.size() != trrc.conditions.length) {
						throw new RuntimeException(String.format("Transitional rating %s has inconsitent numbers of conditions and evaluations", trrc.ratingSpecId));
					}
					trrc.evaluations = new String[keys.size()+1];
					it = keys.iterator();
					for (int i = 0; it.hasNext(); ++i) {
						if (it.next() != (i+1)) {
							throw new RuntimeException("No position "+(i+1)+" in evaluations.");
						}
						trrc.evaluations[i] = evaluations.get((i+1));
					}
					evaluations.clear();
					if (defaultEvaluation == null) {
						throw new RuntimeException(String.format("Transitional rating %s doesn't specify a default evaluation", trrc.ratingSpecId));
					}
					trrc.evaluations[trrc.evaluations.length-1] = defaultEvaluation;
					defaultEvaluation = null;
					keys.clear();
					if (sourceRatingIdsByPos != null) {
						keys.addAll(sourceRatingIdsByPos.keySet());
						trrc.sourceRatingIds = new String[keys.size()];
						it = keys.iterator();
						for (int i = 0; it.hasNext(); ++i) {
							if (it.next() != (i+1)) {
								throw new RuntimeException("No position "+(i+1)+" in source ratings.");
							}
							trrc.sourceRatingIds[i] = sourceRatingIdsByPos.get((i+1));
						}
						sourceRatingIdsByPos.clear();
					}
					arcs.add(trrc.clone());
					arc = trrc;
					trrc = null;
				}				
				String specId = arc.toString();
				SortedSet<AbstractRatingContainer> arcSet = arcsById.get(specId);
				if (arcSet == null) {
					arcSet = new TreeSet<AbstractRatingContainer>();
					arcsById.put(arc.toString(), arcSet);
				}
				if (!arcSet.add(arc.clone())) {
					StringBuilder msg = new StringBuilder("XML Specifies rating ");
					HecTime ht = new HecTime();
					ht.setTimeInMillis(arc.effectiveDateMillis);
					msg.append(arc.toString()).append("@").append(ht.getXMLDateTime(0)).append(" more than once.");
					AbstractRating.logger.warning(msg.toString());
				}
			}
		case 3 :
			if (localName.equals(VERTICAL_DATUM_INFO_STR)) {
				inVerticalDatumInfo = false;
				if (vdcs == null) {
					vdcs = new ArrayList<VerticalDatumContainer>();
				}
				try {
					vdcs.add(new VerticalDatumContainer(this.verticalDatumInfo.toString()));
				}
				catch (VerticalDatumException e) {
					AbstractRating.getLogger().warning(e.getMessage());
		}
			}
		}
		parts[--partsLen] = null;
	}
	/*
	 * SAX method - collect CDATA from elements
	 */
	@Override
	public void characters(char[] ch, int start, int len) {
		chars.append(ch, start, len);
	}
	
//	public static void main(String[] args) throws Exception {
//		String filename = "t:/rating.xml";
//		RatingSet rs = null;
//		long t1 = System.currentTimeMillis();
//		rs = RatingSetXmlParser.parseFile(filename);
//		long t2 = System.currentTimeMillis();
//		System.out.println(rs.toXmlString("  "));
//		System.out.println(t2-t1);
//	}                              
	
}
