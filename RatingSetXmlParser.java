package hec.data.cwmsRating;

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
import hec.data.cwmsRating.io.UsgsStreamTableRatingContainer;
import hec.heclib.util.HecTime;
import hec.io.VerticalDatumContainer;
import hec.lang.Const;
import hec.util.TextUtil;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

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
	private static final String CREATE_DATE_STR = "create-date";
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
	private static final String RATING_POINTS_STR = "rating-points";
	private static final String RATING_SPEC_ID_STR = "rating-spec-id";
	private static final String RATING_SPEC_STR = "rating-spec";
	private static final String RATING_STR = "rating";
	private static final String RATING_TEMPLATE_STR = "rating-template";
	private static final String SOURCE_AGENCY_STR = "source-agency";
	private static final String TEMPLATE_ID_STR	= "template-id";
	private static final String UNITS_ID_STR = "units-id";
	private static final String USGS_STREAM_RATING_STR = "usgs-stream-rating";
	private static final String VALUE_STR = "value";
	private static final String VERTICAL_DATUM_INFO_STR = "vertical-datum-info";
	private static final String VERSION_STR = "version";

	//--------------------//
	// instance variables //
	//--------------------//
	private String[] parts = new String[6]; // deepest element is 5
	private int partsLen = -1;
	private StringBuilder chars = new StringBuilder();
	private RatingSetContainer rsc = null;
	private RatingTemplateContainer rtc = null;
	private RatingSpecContainer rspc = null;
	private AbstractRatingContainer arc = null;
	private List<AbstractRatingContainer> arcs = null;
	private ExpressionRatingContainer erc = null;
	private TableRatingContainer trc = null;
	private UsgsStreamTableRatingContainer urc = null;
	private List<ShiftInfo> shiftInfo = null;
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
		return parseReader(new StringReader(str));
	}
	/**
	 * Parses a RatingSetContainer XML instance from a File object.
	 * @param file The File object containing the XML
	 * @return The resulting RatingSetContainer object
	 * @throws RatingException
	 */
	public static RatingSetContainer parseFile(File file) throws RatingException {
		try {
			return parseReader(new FileReader(file));
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
		try {
			return parseReader(new FileReader(new File(filename)));
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
		return parseInputSource(new InputSource(r));
	}
	/**
	 * Parses a RatingSetContainer XML instance from a InputSource object.
	 * @param is The InputSource object to get the XML from
	 * @return The resulting RatingSetContainer object
	 * @throws RatingException
	 */
	public static RatingSetContainer parseInputSource(InputSource is) throws RatingException {
		RatingSetXmlParser parser = null;
		try {
			parser = new RatingSetXmlParser();
			parser.parse(is);
		}
		catch (Throwable t) {
			if (parser.getRatingSetContainer() == null) {
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
		super(new ParserAdapter((Parser) XMLReaderFactory.createXMLReader()));
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
					else if (parts[1].equals(RATING_STR) || parts[1].equals(USGS_STREAM_RATING_STR)) {
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
						else if (parts[2].equals(CREATE_DATE_STR)) {
							hectime.set(data);
							arc.createDateMillis = hectime.getTimeInMillis();
						}
						else if (parts[2].equals(ACTIVE_STR)) {
							arc.active = Boolean.parseBoolean(data);
						}
						else if (parts[2].equals(DESCRIPTION_STR)) {
							arc.description = data;
						}
						else if (parts[2].equals(FORMULA_STR)) {
							if (!parts[1].equals(RATING_STR)) {
								elementError(); 
							}
							erc = new ExpressionRatingContainer();
							arc.clone(erc);
							erc.expression = data;
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
							else if (parts[3].equals(ACTIVE_STR)) {
								hectime.set(data);
								shiftInfo.get(shiftPointSetCount-1).active = Boolean.parseBoolean(data);
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
					else if (parts[1].equals(RATING_STR) || parts[1].equals(USGS_STREAM_RATING_STR)) {
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
		if (vdcs != null) {
			for (int i = 1; i < vdcs.size(); ++i) {
				if (!vdcs.get(i).equals(vdcs.get(0))) {
					throw new RuntimeException("XML contains inconsistent vertical datum information");
				}
			}
			for (int i = 0; i < arcs.size(); ++i) {
				arcs.get(i).vdc = vdcs.get(0).clone();
			}
			vdcs = null;
		}
		if (rspc != null && arcs != null) {
			rsc.ratingSpecContainer = rspc;
			rsc.abstractRatingContainers = new AbstractRatingContainer[arcs.size()];
			for (int i = 0; i < arcs.size(); ++i) {
				rsc.abstractRatingContainers[i] = arcs.get(i);
			}
		}
		partsLen = -1;
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
				                 .append(attrs.getValue(i));
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
				rspc.specOfficeId = attrs.getValue(OFFICE_ID_STR);
				if (rtc != null) {
					rtc.clone(rspc);
				}
			}
			else if (localName.equals(RATING_STR) || localName.equals(USGS_STREAM_RATING_STR)) {
				arc = new AbstractRatingContainer();
				arc.officeId = attrs.getValue(OFFICE_ID_STR);
			}
			else {
				elementError();
			}
			break;
		case 3 :
			if (parts[1].equals(RATING_STR) || parts[1].equals(USGS_STREAM_RATING_STR)) {
				if (localName.equals(RATING_POINTS_STR)) {
					if (parts[1].equals(RATING_STR)) {
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
					else {
						elementError(); 
					}
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
					else {
						elementError(); 
					}
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
				else if (localName.equals(CREATE_DATE_STR)) ;
				else if (localName.equals(ACTIVE_STR)) ;
				else if (localName.equals(DESCRIPTION_STR)) ;
				else {
					elementError(); 
				}
			}
			else if (parts[1].equals(RATING_TEMPLATE_STR)) {
				if      (localName.equals(PARAMETERS_ID_STR)) ;
				else if (localName.equals(VERSION_STR)) ;
				else if (localName.equals(IND_PARAMETER_SPECS_STR)) ;
				else if (localName.equals(DEP_PARAMETER_STR)) ;
				else if (localName.equals(DESCRIPTION_STR)) ;
				else {
					elementError(); 
				}
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
				else {
					elementError(); 
				}
			}
			else {
				elementError(); 
			}
			break;
		case 4 :
			if (parts[1].equals(RATING_STR) || parts[1].equals(USGS_STREAM_RATING_STR)) {
				if (parts[2].equals(HEIGHT_OFFSETS_STR)) {
					if(localName.equals(POINT_STR)) ;
					else {
						elementError();
					}
				}
				else if (parts[2].equals(RATING_POINTS_STR) || parts[2].equals(EXTENSION_POINTS_STR)) {
					if (localName.equals(OTHER_IND_STR)) {
						if (parts[2].equals(RATING_POINTS_STR)) {
							ratingPoints.get(ratingPointSetCount-1).addOtherInd(Double.parseDouble(attrs.getValue(VALUE_STR)));
						}
						else if (parts[2].equals(EXTENSION_POINTS_STR)) {
							extensionPoints.get(extensionPointSetCount-1).addOtherInd(Double.parseDouble(attrs.getValue(VALUE_STR)));
						}
						else {
							elementError();
						}
					}
				}
				else if (parts[2].equals(HEIGHT_SHIFTS_STR)) {
					if      (localName.equals(EFFECTIVE_DATE_STR)) ;
					else if (localName.equals(CREATE_DATE_STR)) ;
					else if (localName.equals(ACTIVE_STR)) ;
					else if (localName.equals(DESCRIPTION_STR)) ;
					else if (localName.equals(POINT_STR)) ;
					else {
						elementError();
					}
				}
				else {
					elementError();
				}
			}
			else if (parts[1].equals(RATING_TEMPLATE_STR)) {
				if (parts[2].equals(IND_PARAMETER_SPECS_STR)) {
					if (localName.equals(IND_PARAMETER_SPEC_STR)) {
						pos = Integer.parseInt(attrs.getValue(POSITION_STR)) - 1; 
					}
					else {
						elementError();
					}
				}
				else {
					elementError();
				}
			}
			else if (parts[1].equals(RATING_SPEC_STR)) {
				if (parts[2].equals(IND_ROUNDING_SPECS_STR)) {
					if (localName.equals(IND_ROUNDING_SPEC_STR)) {
						pos = Integer.parseInt(attrs.getValue(POSITION_STR)) - 1; 
					}
					else {
						elementError();
					}
				}
				else {
					elementError();
				}
			}
			else {
				elementError();
			}
			break;
		case 5 :
			if (parts[1].equals(RATING_TEMPLATE_STR)) {
				if (parts[2].equals(IND_PARAMETER_SPECS_STR)) {
					if (parts[3].equals(IND_PARAMETER_SPEC_STR)) {
						if      (localName.equals(PARAMETER_STR)) ;
						else if (localName.equals(IN_RANGE_METHOD_STR)) ;
						else if (localName.equals(OUT_RANGE_LOW_METHOD_STR)) ;
						else if (localName.equals(OUT_RANGE_HIGH_METHOD_STR)) ;
						else {
							elementError();
						}
					}
					else elementError();
				}
				else elementError();
			}
			else if (parts[1].equals(RATING_STR) || parts[1].equals(USGS_STREAM_RATING_STR)) {
				if (parts[2].equals(HEIGHT_OFFSETS_STR) || parts[2].equals(HEIGHT_SHIFTS_STR)) {
					if (parts[1].equals(RATING_STR)) {
						elementError();
					}
					if (parts[3].equals(POINT_STR)) {
						if      (localName.equals(IND_STR)) ;
						else if (localName.equals(DEP_STR)) ;
						else if (localName.equals(NOTE_STR)) ;
						else {
							elementError();
						}
					}
					else {
						elementError();
					}
				}
				else if (parts[2].equals(RATING_POINTS_STR) || parts[2].equals(EXTENSION_POINTS_STR)) {
					if (parts[3].equals(POINT_STR)) {
						if      (localName.equals(IND_STR)) ;
						else if (localName.equals(DEP_STR)) ;
						else if (localName.equals(NOTE_STR)) ;
						else {
							elementError();
						}
					}
					else {
						elementError();
					}
				}
				else {
					elementError();
				}
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
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
			else if (localName.equals(RATING_STR)) {
				if (arcs == null) arcs = new ArrayList<AbstractRatingContainer>();
				if (erc != null) {
					ExpressionRatingContainer erc2 = new ExpressionRatingContainer();
					erc.clone(erc2);
					arcs.add(erc2);
					erc = null;
				}
				else if (trc != null) {
					trc.inRangeMethod = rtc.inRangeMethods[0];
					trc.outRangeLowMethod = rtc.outRangeLowMethods[0];
					trc.outRangeHighMethod = rtc.outRangeHighMethods[0];
					if (ratingPointSetCount == 1) {
						int pointCount = ratingPoints.get(0).getPointCount();
						trc.values = new RatingValueContainer[pointCount];
						for (int i = 0; i < pointCount; ++i) {
							trc.values[i] = new RatingValueContainer();
							trc.values[i].indValue = ratingPoints.get(0).getIndValue(i);
							trc.values[i].depValue = ratingPoints.get(0).getDepValue(i);
							trc.values[i].note = ratingPoints.get(0).getNote(i);
						}
						if (extensionPointSetCount == 1) {
							pointCount = extensionPoints.get(0).getPointCount();
							trc.extensionValues = new RatingValueContainer[pointCount];
							for (int i = 0; i < pointCount; ++i) {
								trc.extensionValues[i] = new RatingValueContainer();
								trc.extensionValues[i].indValue = extensionPoints.get(0).getIndValue(i);
								trc.extensionValues[i].depValue = extensionPoints.get(0).getDepValue(i);
								trc.extensionValues[i].note = extensionPoints.get(0).getNote(i);
							}
						}
					}
					else {
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
						trc.values = RatingValueContainer.makeContainers(
							points, 
							notes, 
							rtc.inRangeMethods,
							rtc.outRangeLowMethods,
							rtc.outRangeHighMethods);
					}
					ratingPoints = null;
					ratingPointSetCount = 0;
					extensionPoints = null;
					extensionPointSetCount = 0;
					TableRatingContainer trc2 = new TableRatingContainer();
					trc.clone(trc2);
					arcs.add(trc2);
					trc = null;
				}
			}
			else if (localName.equals(USGS_STREAM_RATING_STR)) {
				if (arcs == null) arcs = new ArrayList<AbstractRatingContainer>();
				urc.inRangeMethod = rtc.inRangeMethods[0];
				urc.outRangeLowMethod = rtc.outRangeLowMethods[0];
				urc.outRangeHighMethod = rtc.outRangeHighMethods[0];
				int pointCount = ratingPoints.get(0).getPointCount();
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
					urc.shifts.ratingSpecContainer.indParams[0] = "Stage";
					urc.shifts.ratingSpecContainer.depParam = "Stage-shifted";
					urc.shifts.ratingSpecContainer.parametersId = String.format("%s;%s", urc.shifts.ratingSpecContainer.indParams[0], urc.shifts.ratingSpecContainer.depParam);
					urc.shifts.ratingSpecContainer.templateVersion = "Standard";
					urc.shifts.ratingSpecContainer.templateId = String.format("%s.%s", urc.shifts.ratingSpecContainer.parametersId, urc.shifts.ratingSpecContainer.templateVersion);
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
							String shiftUnit = TextUtil.split(TextUtil.split(urc.unitsId, RatingConst.SEPARATOR2)[0], RatingConst.SEPARATOR3)[0];
							urc.shifts.abstractRatingContainers[++j] = new TableRatingContainer();
							((TableRatingContainer)urc.shifts.abstractRatingContainers[j]).ratingSpecId = String.format("%s.%s.%s", rspc.locationId, urc.shifts.ratingSpecContainer.templateId, "Production");
							((TableRatingContainer)urc.shifts.abstractRatingContainers[j]).unitsId = String.format("%s;%s", shiftUnit, shiftUnit);
							((TableRatingContainer)urc.shifts.abstractRatingContainers[j]).effectiveDateMillis = shiftInfo.get(i).effectiveDate;
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
				offsetPoints = null;
				offsetPointSetCount = 0;
				shiftPoints = null;
				shiftPointSetCount = 0;
				ratingPoints = null;
				ratingPointSetCount = 0;
				shiftInfo = null;
				UsgsStreamTableRatingContainer urc2 = new UsgsStreamTableRatingContainer();
				urc.clone(urc2);
				arcs.add(urc2);
				urc = null;
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
