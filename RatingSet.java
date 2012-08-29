package hec.data.cwmsRating;

import static hec.data.cwmsRating.RatingConst.SEPARATOR1;
import static hec.data.cwmsRating.RatingConst.SEPARATOR2;
import static hec.data.cwmsRating.RatingConst.SEPARATOR3;
import static hec.data.cwmsRating.RatingConst.builder;
import static hec.data.cwmsRating.RatingConst.formulaXpath;
import static hec.data.cwmsRating.RatingConst.initXmlParsing;
import static hec.data.cwmsRating.RatingConst.officeIdXpath;
import static hec.data.cwmsRating.RatingConst.ratingNodesXpath;
import static hec.data.cwmsRating.RatingConst.ratingPointGroupNodesXpath;
import static hec.data.cwmsRating.RatingConst.ratingSpecIdXpath;
import static hec.data.cwmsRating.RatingConst.specNodeXpathStr;
import static hec.data.cwmsRating.RatingConst.templateNodeXpathStr;
import static hec.data.cwmsRating.RatingConst.unitsIdXpath;
import static hec.data.cwmsRating.RatingConst.xpath;
import static hec.util.TextUtil.join;
import static hec.util.TextUtil.replaceAll;
import static hec.util.TextUtil.split;
import static javax.xml.xpath.XPathConstants.NODE;
import static javax.xml.xpath.XPathConstants.NODESET;
import static javax.xml.xpath.XPathConstants.STRING;
import hec.data.IRating;
import hec.data.Parameter;
import hec.data.RatingException;
import hec.data.Units;
import hec.data.cwmsRating.RatingConst.RatingMethod;
import hec.data.cwmsRating.io.AbstractRatingContainer;
import hec.data.cwmsRating.io.IndependentValuesContainer;
import hec.data.cwmsRating.io.RatingSetContainer;
import hec.data.cwmsRating.io.TableRatingContainer;
import hec.heclib.util.HecTime;
import hec.hecmath.HecMathException;
import hec.hecmath.TextMath;
import hec.hecmath.TimeSeriesMath;
import hec.io.Conversion;
import hec.io.TextContainer;
import hec.io.TimeSeriesContainer;
import hec.lang.Const;
import hec.lang.Observable;
import hec.util.TextUtil;

import java.io.StringReader;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observer;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
/**
 * Implements CWMS-style ratings (time series of ratings)
 *  
 * @author Mike Perryman
 */
public class RatingSet implements IRating, Observer {

	protected static final Logger logger = Logger.getLogger(RatingSet.class.getPackage().getName());

	/**
	 * Flag specifying whether new RatingSet objects will by default allow "risky" behavior such as using mismatched units, unknown parameters, etc.
	 */
	protected static boolean alwaysAllowUnsafe   = true;
	/**
	 * Flag specifying whether new RatingSet objects will by default output messages about "risky" behavior such as using mismatched units, unknown parameters, etc.
	 */
	protected static boolean alwaysWarnUnsafe    = true;
	/**
	 * Object that provides the Observable-by-composition functionality
	 */
	protected Observable observationTarget = null;
	/**
	 * The CWMS-style rating specification (including rating template)
	 */
	protected RatingSpec ratingSpec = null;
	/**
	 * The time series of all ratings in this set, whether active or not
	 */
	protected TreeMap<Long, AbstractRating> ratings = new TreeMap<Long, AbstractRating>();
	/**
	 * The time series of active ratings in this set.  A rating may be inactive by being explicitly marked as such
	 * or by having a creation date later than the current value of the "ratingTime" field. 
	 */
	protected TreeMap<Long, AbstractRating> activeRatings = new TreeMap<Long, AbstractRating>();
	/**
	 * A time to associate with all values that don't specify their own times.  This time, along with the rating
	 * effective dates, is used to determine which ratings to use to rate values.
	 */
	protected long defaultValueTime = Const.UNDEFINED_TIME;
	/**
	 * A time used to allow the rating of values with information that was known at a specific time. No ratings
	 * with a creation date after this time will be used to rate values.
	 */
	protected long ratingTime = Long.MAX_VALUE;
	/**
	 * Flag specifying whether this rating set allows "risky" behavior such as using mismatched units, unknown parameters, etc.
	 */
	protected boolean allowUnsafe = true;
	/**
	 * Flag specifying whether this rating set outputs messges about "risky" behavior such as using mismatched units, unknown parameters, etc.
	 */
	protected boolean warnUnsafe = true;

	/**
	 * Returns whether new RatingSet objects will by default allow "risky" behavior such as using mismatched units, unknown parameters, etc.
	 * @return A flag specifying whether new RatingSet objects will by default allow "risky" behavior
	 */
	public static boolean getAlwaysAllowUnsafe() {
		return alwaysAllowUnsafe;
	}
	/**
	 * Sets whether new RatingSet objects will by default allow "risky" behavior such as using mismatched units, unknown parameters, etc.
	 * @param alwaysAllowUnsafe A flag specifying whether new RatingSet objects will by default allow "risky" behavior
	 */
	public static void setAlwaysAllowUnsafe(Boolean alwaysAllowUnsafe) {
		RatingSet.alwaysAllowUnsafe = alwaysAllowUnsafe;
	}
	/**
	 * Returns whether new RatingSet objects will by default output messages about "risky" behavior such as using mismatched units, unknown parameters, etc.
	 * @return A flag specifying whether new RatingSet objects will by default output messages about "risky" behavior
	 */
	public static boolean getAlwaysWarnUnsafe() {
		return alwaysWarnUnsafe;
	}
	/**
	 * Sets whether new RatingSet objects will by default output messages about "risky" behavior such as using mismatched units, unknown parameters, etc.
	 * @param alwaysWarnUnsafe A flag specifying whether new RatingSet objects will by default output messages about "risky" behavior
	 */
	public static void setAlwaysWarnUnsafe(Boolean alwaysWarnUnsafe) {
		RatingSet.alwaysWarnUnsafe = alwaysWarnUnsafe;
	}
	/**
	 * Generates a new RatingSet object from an XML instance.
	 * @param xmlText The XML instance to construct the RatingSet object from. The document (root) node is expected to be
	 *        &lt;ratings&gt;, which is expected to have one or more &lt;rating&gt; or &lt;usgs-stream-rating&gt; child nodes, all of the same
	 *        rating specification.  Appropriate <rating-template> and &lt;rating-spec&gt; nodes are required for the rating set;
	 *        any other template and specification nodes are ignored.
	 * @return A new RatingSet object
	 * @throws RatingException
	 */
	public static RatingSet fromXml(String xmlText) throws RatingException {
		try {
			initXmlParsing();
			Document doc = builder.parse(new InputSource(new StringReader(xmlText)));
			NodeList ratingNodes = (NodeList)ratingNodesXpath.evaluate(doc, NODESET);
			if (ratingNodes.getLength() == 0) {
				throw new RatingException("No rating nodes.");
			}
			String officeId = null;
			String ratingSpecId = null;
			String unitsId = null;
			String parametersId = null;
			String templateVersion = null;
			Node templateNode = null;
			Node specNode = null;
			RatingSpec ratingSpec = null;
			List<AbstractRating> ratings = new Vector<AbstractRating>();
			for (int i = 0; i < ratingNodes.getLength(); ++i) {
				Node ratingNode = ratingNodes.item(i);
				if (i == 0) {
			         officeId = ((String)officeIdXpath.evaluate(ratingNode, STRING)).trim();
	                 ratingSpecId = ((String)ratingSpecIdXpath.evaluate(ratingNode, STRING)).trim();
	                 unitsId = ((String)unitsIdXpath.evaluate(ratingNode, STRING)).trim();
	                 String[] parts = Pattern.compile(SEPARATOR1, Pattern.LITERAL).split(ratingSpecId);
//	                 location = parts[0];
	                 parametersId = parts[1];
	                 templateVersion = parts[2];
//	                 specVersion = parts[3];
	                 templateNode = (Node)xpath.compile(String.format(templateNodeXpathStr, officeId, parametersId, templateVersion)).evaluate(doc, NODE);
	                 specNode = (Node)xpath.compile(String.format(specNodeXpathStr, officeId, ratingSpecId)).evaluate(doc, NODE);
	                 ratingSpec = RatingSpec.fromXml(templateNode, specNode);
				}
				else {
			         if (!officeId.equals(((String)officeIdXpath.evaluate(ratingNode, STRING)).trim())) {
			        	 throw new RatingException("Individual ratings have different offices.");
			         }
	                 if (!ratingSpecId.equals(((String)ratingSpecIdXpath.evaluate(ratingNode, STRING)).trim())) {
			        	 throw new RatingException("Individual ratings have different rating specifications.");
	                 }
	                 if (!unitsId.equals(((String)unitsIdXpath.evaluate(ratingNode, STRING)).trim())) {
			        	 throw new RatingException("Individual ratings have different units.");
	                 }
				}
				if (ratingNode.getNodeName().equals("usgs-stream-rating")) {
					ratings.add(UsgsStreamTableRating.fromXml(templateNode, ratingNode));
				}
				else {
					Node node = (Node)ratingPointGroupNodesXpath.evaluate(ratingNode, NODE);
					if (node != null)  {
						ratings.add(TableRating.fromXml(templateNode, ratingNode));
					}
					else {
						node = (Node)formulaXpath.evaluate(ratingNode, NODE);
						if (node != null) {
							ratings.add(ExpressionRating.fromXml(templateNode, ratingNode));
						}
					}
					if (node == null) {
						throw new RatingException("Rating element does not include either rating-points or formula sub-elements");
					}
				}
			}
			return new RatingSet(ratingSpec, ratings);
		}
		catch (Throwable t) {
			if (t instanceof RatingException) throw (RatingException)t;
			throw new RatingException(t);
		}
	}
	/**
	 * Generates a new RatingSet object from a compressed XML instance.
	 * @param compressedXml The compressed XML instance to construct the RatingSet object from. The document (root) node is expected to be
	 *        &lt;ratings&gt;, which is expected to have one or more &lt;rating&gt; or &lt;usgs-stream-rating&gt; child nodes, all of the same
	 *        rating specification.  Appropriate <rating-template> and &lt;rating-spec&gt; nodes are required for the rating set;
	 *        any other template and specification nodes are ignored.
	 * @return A new RatingSet object
	 * @throws RatingException
	 */
	public static RatingSet fromCompressedXml(String compressedXml) throws RatingException {
		try {
			return fromXml(TextUtil.uncompress(compressedXml, "base64"));
		} 
		catch (Throwable t) {
			if (t instanceof RatingException) throw (RatingException)t;
			throw new RatingException(t);
		}
	}
	/**
	 * Generates a new RatingSet object from a CWMS database connection
	 * @param conn The connection to a CWMS database
	 * @param officeId The identifier of the office owning the rating. If null, the office associated with the connect user is used. 
	 * @param ratingSpecId The rating specification identifier
	 * @param startTime The earliest effective date to retrieve, in milliseconds.  If null, no earliest limit is set.
	 * @param endTime The latest effective date to retrieve, in milliseconds.  If null, no latest limit is set.
	 * @return The new RatingSet object
	 * @throws RatingException
	 */
	public static RatingSet fromDatabase(
			Connection conn, 
			String officeId, 
			String ratingSpecId, 
			Long startTime, 
			Long endTime)
			throws RatingException {
		try {
			String sql = 
					"declare " +
					   "l_millis_start         integer := :1;" +
					   "l_millis_end           integer := :2;" +
					   "l_effective_date_start date;" +
					   "l_effective_date_end   date;" +
					"begin " +
					   "if l_millis_start is not null then " +
					      "l_effective_date_start := cast(cwms_util.to_timestamp(l_millis_start) as date);" +
					   "end if;" +
					   "if l_millis_end is not null then "   +
					      "l_effective_date_end := cast(cwms_util.to_timestamp(l_millis_end) as date);" +
					   "end if;" +
					   "cwms_rating.retrieve_ratings_xml2("  +
					      "p_ratings              => :3,"    +
					      "p_spec_id_mask         => :4,"    +
					      "p_effective_date_start => l_effective_date_start," +
					      "p_effective_date_end   => l_effective_date_end,"   +
					      "p_time_zone            => 'UTC'," +
					      "p_office_id_mask       => :5);"   +
					"end;";
			CallableStatement stmt = conn.prepareCall(sql);
			stmt.registerOutParameter(3, Types.CLOB);
			stmt.setString(4, ratingSpecId);
			if (startTime == null) {
				stmt.setNull(1, Types.INTEGER);
			}
			else {
				stmt.setLong(1, startTime);
			}
			if (endTime == null) {
				stmt.setNull(2, Types.INTEGER);
			}
			else {
				stmt.setLong(2, endTime);
			}
			if (officeId == null) {
				stmt.setNull(5, Types.VARCHAR);
			}
			else {
				stmt.setString(5, officeId);
			}
			stmt.execute();
			Clob clob = stmt.getClob(3);
			stmt.close();
			if (clob.length() > Integer.MAX_VALUE) {
				throw new RatingException("CLOB too long.");
			}
			String xmlText = clob.getSubString(1, (int)clob.length());
			return fromXml(xmlText);
		}
		catch (Throwable t) {
			if (t instanceof RatingException) throw (RatingException)t;
			throw new RatingException(t);
		}
	}
	/**
	 * Generates a new RatingSet object from a CWMS database connection
	 * @param conn The connection to a CWMS database
	 * @param ratingSpecId The rating specification identifier
	 * @param startTime The earliest effective date to retrieve, in milliseconds.  If null, no earliest limit is set.
	 * @param endTime The latest effective date to retrieve, in milliseconds.  If null, no latest limit is set.
	 * @return The new RatingSet object
	 * @throws RatingException
	 */
	public static RatingSet fromDatabase(
			Connection conn, 
			String ratingSpecId, 
			Long startTime, 
			Long endTime)
			throws RatingException {
		
		return fromDatabase(conn, null, ratingSpecId, startTime, endTime);
	}
	/**
	 * Generates a new RatingSet object from a CWMS database connection
	 * @param conn The connection to a CWMS database
	 * @param officeId The identifier of the office owning the rating. If null, the office associated with the connect user is used. 
	 * @param ratingSpecId The rating specification identifier
	 * @return The new RatingSet object
	 * @throws RatingException
	 */
	public static RatingSet fromDatabase(
			Connection conn, 
			String officeId, 
			String ratingSpecId)
			throws RatingException {
		
		return fromDatabase(conn, officeId, ratingSpecId, null, null);
	}
	/**
	 * Generates a new RatingSet object from a CWMS database connection
	 * @param conn The connection to a CWMS database
	 * @param ratingSpecId The rating specification identifier
	 * @return The new RatingSet object
	 * @throws RatingException
	 */
	public static RatingSet fromDatabase(
			Connection conn, 
			String ratingSpecId) 
			throws RatingException {
				
		return fromDatabase(conn, null, ratingSpecId, null, null);
				
	}
	/**
	 * Public Constructor - sets rating specification only
	 * @param ratingSpec The rating specification
	 * @throws RatingException 
	 */
	public RatingSet(RatingSpec ratingSpec) throws RatingException {
		this.ratingSpec = ratingSpec;
		allowUnsafe = alwaysAllowUnsafe;
		warnUnsafe = alwaysWarnUnsafe;
		observationTarget = new Observable();
		validate();
	}
	/**
	 * Public Constructor - sets rating specification and single rating
	 * @param ratingSpec The rating specification
	 * @param rating The rating
	 * @throws RatingException
	 */
	public RatingSet(RatingSpec ratingSpec, AbstractRating rating) throws RatingException {
		setRatingSpec(ratingSpec);
		addRating(rating);
		allowUnsafe = alwaysAllowUnsafe;
		warnUnsafe = alwaysWarnUnsafe;
		observationTarget = new Observable();
		validate();
	}
	/**
	 * Public Constructor - sets rating specification and a time series of ratings
	 * @param ratingSpec The rating specification
	 * @param rating The time series of ratings
	 * @throws RatingException
	 */
	public RatingSet(RatingSpec ratingSpec, AbstractRating[] ratings) throws RatingException {
		setRatingSpec(ratingSpec);
		addRatings(ratings);
		allowUnsafe = alwaysAllowUnsafe;
		warnUnsafe = alwaysWarnUnsafe;
		observationTarget = new Observable();
		validate();
	}
	/**
	 * Public Constructor - sets rating specification and a time series of ratings
	 * @param ratingSpec The rating specification
	 * @param rating The time series of ratings
	 * @throws RatingException
	 */
	public RatingSet(RatingSpec ratingSpec, Iterable<AbstractRating> ratings) throws RatingException {
		setRatingSpec(ratingSpec);
		addRatings(ratings);
		allowUnsafe = alwaysAllowUnsafe;
		warnUnsafe = alwaysWarnUnsafe;
		observationTarget = new Observable();
		validate();
	}
	/**
	 * Public Constructor from RatingSetContainer
	 * @param rsc The RatingSetContainer object to initialize from
	 * @throws RatingException
	 */
	public RatingSet(RatingSetContainer rsc) throws RatingException {
		setData(rsc);
		allowUnsafe = alwaysAllowUnsafe;
		warnUnsafe = alwaysWarnUnsafe;
		observationTarget = new Observable();
		validate();
	}
	/**
	 * Public Constructor from TextContainer (as read from DSS)
	 * @param tc The TextContainer object to initialize from
	 * @throws RatingException
	 */
	public RatingSet(TextContainer tc) throws RatingException {
		setData(tc);
		allowUnsafe = alwaysAllowUnsafe;
		warnUnsafe = alwaysWarnUnsafe;
		observationTarget = new Observable();
		validate();
	}
	/**
	 * Public Constructor from TextMath (as read from DSS)
	 * @param tm The TextMath object to initialize from
	 * @throws RatingException
	 */
	public RatingSet(TextMath tm) throws RatingException {
		try {
			setData((TextContainer)tm.getData());
		} 
		catch (Throwable t) {
			if (t instanceof RatingException) throw (RatingException)t;
			throw new RatingException(t);
		}
		allowUnsafe = alwaysAllowUnsafe;
		warnUnsafe = alwaysWarnUnsafe;
		observationTarget = new Observable();
		validate();
	}
	/**
	 * Adds a single rating to the existing ratings.
	 * @param rating The rating to add
	 * @throws RatingException
	 */
	public void addRating(AbstractRating rating) throws RatingException {
		Long effectiveDate = rating.getEffectiveDate();
		if(ratings.containsKey(effectiveDate)) {
			throw new RatingException("Rating with same effective date already exists; cannot add rating");
		}
		if (ratingSpec != null && rating.getIndParamCount() != ratingSpec.getIndParamCount()) {
			throw new RatingException("Number of independent parameters does not match rating specification");
		}
		if (ratings.size() > 0) {
			if (!rating.getRatingSpecId().equals(ratings.firstEntry().getValue().getRatingSpecId())) {
				throw new RatingException("Cannot add rating with different rating specification.");
			}
			if (!rating.getRatingUnitsId().equals(ratings.firstEntry().getValue().getRatingUnitsId())) {
				throw new RatingException("Cannot add rating with different units.");
			}
		}
		ratings.put(effectiveDate, rating);
		if (rating.isActive()) {
			activeRatings.put(effectiveDate, rating);
		}
		rating.deleteObserver(this);
		rating.addObserver(this);
		validate();
		if (observationTarget != null) {
			observationTarget.setChanged();
			observationTarget.notifyObservers();
		}
	}
	/**
	 * Adds multiple ratings to the existing ratings.
	 * @param ratings The ratings to add
	 * @throws RatingException
	 */
	public void addRatings(AbstractRating[] ratings) throws RatingException {
		addRatings(Arrays.asList(ratings));
	}
	/**
	 * Adds multiple ratings to the existing ratings.
	 * @param ratings The ratings to add
	 * @throws RatingException
	 */
	public void addRatings(Iterable<AbstractRating> ratings) throws RatingException {
		String ratingSpecId = null;
		String unitsId = null;
		for (AbstractRating rating : ratings) {
			if(this.ratings.containsKey(rating.getEffectiveDate())) {
				throw new RatingException("Rating with same effective date already exists; cannot add rating");
			}
			if (ratingSpec != null && rating.getIndParamCount() != ratingSpec.getIndParamCount()) {
				throw new RatingException("Number of independent parameters does not match rating specification");
			}
			if (ratingSpecId == null) {
				ratingSpecId = rating.getRatingSpecId();
			}
			else if (!rating.getRatingSpecId().equals(ratingSpecId)) {
				throw new RatingException("Ratings have inconsistent rating specifications.");
			}
			if (unitsId == null) {
				unitsId = rating.getRatingUnitsId();
			}
			else if (!rating.getRatingUnitsId().equals(unitsId)) {
				throw new RatingException("Ratings have inconsistent units.");
			}
		}
		if (this.ratings.size() > 0) {
			if (!this.ratings.firstEntry().getValue().getRatingSpecId().equals(ratingSpecId)) {
				throw new RatingException("Cannot add ratings with different rating specification.");
			}
			if (!this.ratings.firstEntry().getValue().getRatingUnitsId().equals(unitsId)) {
				throw new RatingException("Cannot add ratings with different units.");
			}
		}
		for (AbstractRating rating : ratings) {
			this.ratings.put(rating.getEffectiveDate(), rating);
			if (rating.isActive() && rating.createDate <= ratingTime) {
				activeRatings.put(rating.getEffectiveDate(), rating);
			}
			rating.deleteObserver(this);
			rating.addObserver(this);
			validate();
		}
		if (observationTarget != null) {
			observationTarget.setChanged();
			observationTarget.notifyObservers();
		}
	}
	/**
	 * Removes a single rating from the existing ratings.
	 * @param effectiveDate The effective date of the rating to remove, in Java milliseconds
	 * @throws RatingException
	 */
	public void removeRating(long effectiveDate) throws RatingException {
		ICwmsRating cwmsRating = ratings.get(effectiveDate);
		if (cwmsRating == null) {
			throw new RatingException("Rating with specified effective date does not exist; cannot remove rating");
		}
		cwmsRating.deleteObserver(this);
		ratings.remove(effectiveDate);
		if (activeRatings.containsKey(effectiveDate)) {
			activeRatings.remove(effectiveDate);
		}
		if (observationTarget != null) {
			observationTarget.setChanged();
			observationTarget.notifyObservers();
		}
	}
	/**
	 * Removes all existing ratings.
	 */
	public void removeAllRatings() {
		for (ICwmsRating cwmsRating : ratings.values()) {
			cwmsRating.deleteObserver(this);
		}
		ratings.clear();
		activeRatings.clear();
		if (observationTarget != null) {
			observationTarget.setChanged();
			observationTarget.notifyObservers();
		}
	}
	/**
	 * Replaces a single rating in the existing ratings
	 * @param rating The rating to replace an existing one
	 * @throws RatingException
	 */
	public void replaceRating(AbstractRating rating) throws RatingException {
		Long effectiveDate = rating.getEffectiveDate();
		if (!ratings.containsKey(effectiveDate)) {
			throw new RatingException("Rating with same effective date does not exist; cannot replace rating");
		}
		if (ratingSpec != null && rating.getIndParamCount() != ratingSpec.getIndParamCount()) {
			throw new RatingException("Number of independent parameters does not match rating specification");
		}
		if (!rating.getRatingSpecId().equals(ratings.firstEntry().getValue().getRatingSpecId())) {
			throw new RatingException("Cannot replace rating with different rating specification.");
		}
		if (!rating.getRatingUnitsId().equals(ratings.firstEntry().getValue().getRatingUnitsId())) {
			throw new RatingException("Cannot replace rating with different units.");
		}
		ratings.put(effectiveDate, rating).deleteObserver(this);
		if (rating.isActive()) {
			activeRatings.put(effectiveDate, rating);
		}
		rating.deleteObserver(this);
		rating.addObserver(this);
		validate();
		if (observationTarget != null) {
			observationTarget.setChanged();
			observationTarget.notifyObservers();
		}
	}
	/**
	 * Replaces multiple ratings in the existing ratings.
	 * @param ratings The ratings to replace existing ones
	 * @throws RatingException
	 */
	public void replaceRatings(AbstractRating[] ratings) throws RatingException {
		replaceRatings(Arrays.asList(ratings));
	}
	/**
	 * Replaces multiple ratings in the existing ratings.
	 * @param ratings The ratings to replace existing ones
	 * @throws RatingException
	 */
	public void replaceRatings(Iterable<AbstractRating> ratings) throws RatingException {
		String ratingSpecId = null;
		String unitsId = null;
		for (AbstractRating rating : ratings) {
			if (!this.ratings.containsKey(rating.getEffectiveDate())) {
				throw new RatingException("Rating with same effective date does not exist; cannot replace rating");
			}
			if (ratingSpec != null && rating.getIndParamCount() != ratingSpec.getIndParamCount()) {
				throw new RatingException("Number of independent parameters does not match rating specification");
			}
			if (ratingSpecId == null) {
				ratingSpecId = rating.getRatingSpecId();
			}
			else if (!rating.getRatingSpecId().equals(ratingSpecId)) {
				throw new RatingException("Ratings have inconsistent rating specifications.");
			}
			if (unitsId == null) {
				unitsId = rating.getRatingUnitsId();
			}
			else if (!rating.getRatingUnitsId().equals(unitsId)) {
				throw new RatingException("Ratings have inconsistent units.");
			}
		}
		if (!this.ratings.firstEntry().getValue().getRatingSpecId().equals(ratingSpecId)) {
			throw new RatingException("Cannot replace ratings with different rating specification.");
		}
		if (!this.ratings.firstEntry().getValue().getRatingUnitsId().equals(unitsId)) {
			throw new RatingException("Cannot replace ratings with different units.");
		}
		for (AbstractRating rating : ratings) {
			this.ratings.put(rating.getEffectiveDate(), rating).deleteObserver(this);
			if (rating.isActive() && rating.createDate <= ratingTime) {
				activeRatings.put(rating.getEffectiveDate(), rating);
			}
			rating.deleteObserver(this);
			rating.addObserver(this);
		}
		validate();
		if (observationTarget != null) {
			observationTarget.setChanged();
			observationTarget.notifyObservers();
		}
	}
	/**
	 * Retrieves a rated value for a specified single input value and time. The rating set must
	 * be for a single independent parameter
	 * 
	 * @param value The value to rate
	 * @param valueTime The time associated with the value, in Java milliseconds
	 * @return the rated value
	 * @throws RatingException
	 */
	public double rate(double value, long valueTime) throws RatingException {
		double[] values = {value};
		return rate(values, valueTime)[0];
	}
	/**
	 * Retrieves rated values for specified multiple input values at a single time. The rating set must
	 * be for a single independent parameter
	 * 
	 * @param values The values to rate
	 * @param valueTime The time associated with the values, in Java milliseconds
	 * @return the rated value
	 * @throws RatingException
	 */
	public double[] rate(double[] values, long valueTime) throws RatingException {
		long[] valueTimes = new long[values.length];
		Arrays.fill(valueTimes, valueTime);
		return rateOne(values, valueTimes);
	}
	/**
	 * Retrieves rated values for specified multiple input values and times. The rating set must
	 * be for a single independent parameter
	 * 
	 * @param values The values to rate
	 * @param valueTime The time associated with the values, in Java milliseconds
	 * @return the rated value
	 * @throws RatingException
	 */
	public double[] rateOne(double[] values, long[] valueTimes) throws RatingException {
		double[][] valueSets = new double[values.length][1];
		for (int i = 0; i < values.length; ++i) valueSets[i][0] = values[i];
		return rate(valueSets, valueTimes);
	}
	/**
	 * Retrieves a single rated value for specified input value set at a single time. The rating set must
	 * be for as many independent parameters as the length of the value set.
	 * 
	 * @param valueSet The value set to rate
	 * @param valueTime The time associated with the values, in Java milliseconds
	 * @return the rated value
	 * @throws RatingException
	 */
	public double rateOne(double[] valueSet, long valueTime) throws RatingException {
		double[][] valueSets = {valueSet};
		long[] valueTimes = {valueTime};
		return rate(valueSets, valueTimes)[0];
	}
	/**
	 * Retrieves rated values for specified multiple input value Sets and times. The rating set must
	 * be for as many independent parameter as each value set
	 * 
	 * @param valueSets The value sets to rate
	 * @param valueTimes The times associated with the values, in Java milliseconds
	 * @return the rated value
	 * @throws RatingException
	 */
	public double[] rate(double[][] valueSets, long[] valueTimes) throws RatingException {
		if (activeRatings.size() == 0) {
			throw new RatingException("No active ratings.");
		}
		if (valueSets.length != valueTimes.length) {
			throw new RatingException("Values and times have different lengths");
		}
		for (int i = 1; i < valueSets.length; ++i) {
			if (valueSets[i].length != valueSets[0].length) {
				throw new RatingException("Value sets are not all of same length.");
			}
			if (valueSets[i].length != ratings.firstEntry().getValue().getIndParamCount()) {
				throw new RatingException("Value sets have different parameter counts than ratings.");
			}
		}
		double[] Y = new double[valueSets.length];
		Map.Entry<Long, AbstractRating> lowerRating = null;
		Map.Entry<Long, AbstractRating> upperRating = null;
		IRating lastUsedRating = null;
		RatingMethod method = null;
		for (int i = 0; i < valueSets.length; ++i) {
			if (i > 0 && valueTimes[i] == valueTimes[i-1]) {
				if (lastUsedRating == null) {
					Y[i] = Y[i-1];
				}
				else {
					Y[i] = lastUsedRating.rateOne(valueTimes[i], valueSets[i]);
				}
				continue;
			}
			else {
				lowerRating = activeRatings.floorEntry(valueTimes[i]);
				upperRating = activeRatings.ceilingEntry(valueTimes[i]);
				//-------------------------//
				// handle out of range low //
				//-------------------------//
				if (lowerRating == null) {
					switch(ratingSpec.getOutRangeLowMethod()) {
					case ERROR:
						throw new RatingException("Effective date is before earliest rating");
					case NULL:
						Y[i] = Const.UNDEFINED_DOUBLE;
						lastUsedRating = null;
						continue;
					case NEXT:
					case NEAREST:
					case HIGHER:
					case CLOSEST:
						lastUsedRating = activeRatings.firstEntry().getValue(); 
						Y[i] = lastUsedRating.rateOne(valueTimes[i], valueSets[i]);
						continue;
					}
					method = ratingSpec.getOutRangeLowMethod();
					if (activeRatings.size() == 1) {
						throw new RatingException(String.format("Cannot use rating method %s with only one active rating.", method));
					}
					lowerRating = activeRatings.firstEntry();
					upperRating = activeRatings.higherEntry(lowerRating.getKey());
				}
				//--------------------------//
				// handle out of range high //
				//--------------------------//
				if (upperRating == null) {
					switch(ratingSpec.getOutRangeHighMethod()) {
					case ERROR:
						throw new RatingException("Effective date is after latest rating");
					case NULL:
						Y[i] = Const.UNDEFINED_DOUBLE;
						lastUsedRating = null;
						continue;
					case PREVIOUS:
					case NEAREST:
					case LOWER:
					case CLOSEST:
						lastUsedRating = activeRatings.lastEntry().getValue();
						Y[i] = lastUsedRating.rateOne(valueTimes[i], valueSets[i]);
						continue;
					}
					method = ratingSpec.getOutRangeHighMethod();
					if (activeRatings.size() == 1) {
						throw new RatingException(String.format("Cannot use rating method %s with only one active rating.", method));
					}
					upperRating = activeRatings.lastEntry();
					lowerRating = activeRatings.lowerEntry(upperRating.getKey());
				}
				//-----------------------------------//
				// handle in-range and extrapolation //
				//-----------------------------------//
				if (lowerRating.getKey() == valueTimes[i]) {
					Y[i] = lowerRating.getValue().rateOne(valueTimes[i], valueSets[i]);
					continue;
				}
				if (upperRating.getKey() == valueTimes[i]) {
					lastUsedRating = upperRating.getValue();
					Y[i] = lastUsedRating.rateOne(valueTimes[i], valueSets[i]);
					continue;
				}
				switch (ratingSpec.getInRangeMethod()) {
				case ERROR:
					throw new RatingException("Effective date is between existing rating");
				case NULL:
					Y[i] = Const.UNDEFINED_DOUBLE;
					lastUsedRating = null;
					continue;
				case PREVIOUS:
				case LOWER:
					lastUsedRating = lowerRating.getValue();
					Y[i] = lastUsedRating.rateOne(valueTimes[i], valueSets[i]);
					continue;
				case NEXT:
				case HIGHER:
					lastUsedRating = upperRating.getValue();
					Y[i] = lastUsedRating.rateOne(valueTimes[i], valueSets[i]);
					continue;
				case CLOSEST:
					if (valueTimes[i] - lowerRating.getKey() < upperRating.getKey() - valueTimes[i]) {
						lastUsedRating = lowerRating.getValue();
					}
					else {
						lastUsedRating = upperRating.getValue();
					}
					Y[i] = lastUsedRating.rateOne(valueTimes[i], valueSets[i]);
					continue;
				}
				//------------------------------------//
				// handle interpolation/extrapolation //
				//------------------------------------//
				method = ratingSpec.getInRangeMethod();
			}
			lastUsedRating = null;
			double x  = (double)valueTimes[i];
			double x1 = (double)lowerRating.getKey();
			double x2 = (double)upperRating.getKey();
			double Y1 = lowerRating.getValue().rateOne(valueTimes[i], valueSets[i]);
			double Y2 = upperRating.getValue().rateOne(valueTimes[i], valueSets[i]);
			double y1 = Y1;
			double y2 = Y2;
			if (lowerRating instanceof UsgsStreamTableRating) {
				x1 = ((UsgsStreamTableRating)lowerRating).getLatestEffectiveDate(valueTimes[i]);
			}
			double y = y1 + ((x - x1) / (x2 - x1)) * (y2 - y1);
			Y[i] = y;
		}
		return Y;
	}
	/**
	 * Rates the values in a TimeSeriesContainer and returns the results in a new TimeSeriesContainer. 
	 * The rating must be for a single independent parameter. 
	 * @param tsc The TimeSeriesContainer to rate
	 * @return A TimeSeriesContainer of the rated values. The rated unit is the native unit of dependent parameter of the rating.
	 * @throws RatingException
	 */
	public TimeSeriesContainer rate(TimeSeriesContainer tsc) throws RatingException {
		return rate(tsc, null);
	}
	/**
	 * Rates the values in a TimeSeriesContainer and returns the results in a new TimeSeriesContainer with the specified unit. 
	 * The rating must be for a single independent parameter. 
	 * @param tsc The TimeSeriesContainer to rate
	 * @param ratedUnitStr The unit to return the rated values in.
	 * @return A TimeSeriesContainer of the rated values. The rated unit is the specified unit.
	 * @throws RatingException
	 */
	public TimeSeriesContainer rate(TimeSeriesContainer tsc, String ratedUnitStr) throws RatingException {
		if (ratingSpec.getIndParamCount() != 1) {
			throw new RatingException(String.format("Cannot rate a TimeSeriesContainer with a rating that has %d independent parameters", ratingSpec.getIndParamCount()));
		}
		if (ratings.size() == 0) throw new RatingException("No ratings.");
		String unitsId = ratings.firstEntry().getValue().getRatingUnitsId();
		String[] units = split(unitsId.replace(SEPARATOR2, SEPARATOR3), SEPARATOR3, "L");
		String[] params = ratingSpec.getIndParameters();
		if (ratedUnitStr == null || ratedUnitStr.length() == 0) ratedUnitStr = units[units.length-1];
		try {
			//-------------------------------------//
			// validate the time zone if specified //
			//-------------------------------------//
			TimeZone tz = null;
			if (tsc.timeZoneID != null) {
				tz = TimeZone.getTimeZone(tsc.timeZoneID);
				if (!tz.getID().equals(tsc.timeZoneID)) {
					String msg = String.format("TimeSeriesContainer has invalid time zone \"%s\".", tsc.timeZoneID);
					if (!allowUnsafe) throw new RatingException(msg);
					if (warnUnsafe) logger.warning(msg + "  Value times will be treated as UTC.");
					tz = null;
				}
			}
			//-----------------------------------------//
			// validate the parameter and unit to rate //
			//-----------------------------------------//
			Parameter tscParam = null;
			Units tscUnit = null;
			Units ratedUnit = null;
			boolean convertTscUnit = false;
			boolean convertRatedUnit = false;
			try {
				tscParam = new Parameter(tsc.parameter);
			}
			catch (Throwable t) {
				if (!allowUnsafe) throw new RatingException(t);
				if (warnUnsafe) logger.warning(t.getMessage());
			}
			if (tscParam != null) {
				if (!tscParam.getParameter().equals(params[0])) {
					String msg = String.format("Parameter \"%s\" does not match rating parameter \"%s\".", tscParam.getParameter(), params[0]);
					if (!allowUnsafe) throw new RatingException(msg);
					if (warnUnsafe) logger.warning(msg);
				}
			}
			try {
				tscUnit = new Units(tsc.units);
			}
			catch (Throwable t) {
				if (!allowUnsafe) throw new RatingException(t);
				if (warnUnsafe) logger.warning(t.getMessage());
			}
			if (tscParam != null) {
				if (!Units.canConvertBetweenUnits(tsc.units, tscParam.getUnitsString())) {
					
					String msg = String.format("Unit \"%s\" is not valid for parameter \"%s\".", tsc.units, tscParam.getParameter());
					if (!allowUnsafe) throw new RatingException(msg);
					if (warnUnsafe) logger.warning(msg);
				}
			} 
			if (tscUnit != null) {
				if (!tsc.units.equals(units[0])) {
					if(Units.canConvertBetweenUnits(tsc.units, units[0])) {
						convertTscUnit = true;
					}
					else {
						String msg = String.format("Cannot convert from \"%s\" to \"%s\".", tsc.units, units[0]);
						if (!allowUnsafe) throw new RatingException(msg);
						if (warnUnsafe) logger.warning(msg + "  Rating will be performed on unconverted values.");
					}
				}
			}
			//--------------------------//
			// validate the result unit //
			//--------------------------//
			try {
				ratedUnit = new Units(ratedUnitStr);
			}
			catch (Throwable t) {
				if (!allowUnsafe) throw new RatingException(t);
				if (warnUnsafe) logger.warning(t.getMessage());
			}
			if (ratedUnit != null) {
				if (!ratedUnitStr.equals(units[units.length-1])) {
					if (Units.canConvertBetweenUnits(ratedUnitStr, units[units.length-1])) {
						convertRatedUnit = true;
					}
					else {
						String msg = String.format("Cannot convert from \"%s\" to \"%s\".", units[units.length-1], ratedUnit);
						if (!allowUnsafe) throw new RatingException(msg);
						if (warnUnsafe) logger.warning(msg + "  Rated values will be unconverted.");
					}
				}
			}
			//-------------------------//
			// finally - do the rating //
			//-------------------------//
			double[] indVals = null;
			long[] millis = new long[tsc.times.length];
			if (tz == null) {
				for (int i = 0; i < millis.length; ++i) {
					millis[i] = Conversion.toMillis(tsc.times[i]);
				}
			}
			else {
				Calendar cal = Calendar.getInstance();
				cal.setTimeZone(tz);
				SimpleDateFormat sdf = new SimpleDateFormat("ddMMMyyyy, HH:mm");
				HecTime t = new HecTime();
				for (int i = 0; i < millis.length; ++i) {
					t.set(tsc.times[i]);
					cal.setTime(sdf.parse((t.dateAndTime(4))));
					millis[i] = cal.getTimeInMillis();
				}
			}
			if (convertTscUnit) {
				indVals = Arrays.copyOf(tsc.values, tsc.values.length);
				Units.convertUnits(indVals, tsc.units, units[0]);
			}
			else {
				indVals = tsc.values;
			}
			double[] depVals = rateOne(indVals, millis);
			//-----------------------------------------//
			// construct the rated TimeSeriesContainer //
			//-----------------------------------------//
			if (convertRatedUnit) Units.convertUnits(depVals, units[units.length-1], ratedUnitStr);
			TimeSeriesContainer ratedTsc = new TimeSeriesContainer();
			tsc.clone(ratedTsc);
			ratedTsc.values = depVals;
			String paramStr = getRatingSpec().getDepParameter();
			if (tsc.subParameter == null) {
				ratedTsc.fullName = replaceAll(tsc.fullName, tsc.parameter, paramStr, "IL");
			}
			else {
				ratedTsc.fullName = replaceAll(tsc.fullName, String.format("%s-%s", tsc.parameter, tsc.subParameter), paramStr, "IL");
			}
			String[] parts = split(paramStr, "-", "L", 2);
			ratedTsc.parameter = parts[0];
			ratedTsc.subParameter = parts.length > 1 ? parts[1] : null;
			ratedTsc.units = ratedUnitStr;
			return ratedTsc;
		}
		catch (Throwable t) {
			if (t instanceof RatingException) throw (RatingException) t;
			throw new RatingException(t);
		}
	}
	/**
	 * Rates the values in a set of TimeSeriesContainers and returns the results in a new TimeSeriesContainer. 
	 * The rating must be for as many independent parameters as the number of TimeSeriesContainers.
	 * If all the TimeSeriesContainers have the same interval the rated TimeSeriesContainer will have the same interval, otherwise
	 * the rated TimeSeriesContainer will have an interval of 0 (irregular).  The rated TimeSeriesContainer will have values
	 * only at times that are common to all the input TimeSeriesContainers. 
	 * @param tscs The TimeSeriesContainers to rate, in order of the independent parameters of the rating.
	 * @return A TimeSeriesContainer of the rated values. The rated unit is the native unit of the dependent parameter of the rating.
	 * @throws RatingException
	 */
	public TimeSeriesContainer rate(TimeSeriesContainer[] tscs) throws RatingException {
		return rate(tscs, null);
	}
	/**
	 * Rates the values in a set of TimeSeriesContainers and returns the results in a new TimeSeriesContainer with the specified unit.
	 * The rating must be for as many independent parameters as the number of TimeSeriesContainers. 
	 * If all the TimeSeriesContainers have the same interval the rated TimeSeriesContainer will have the same interval, otherwise
	 * the rated TimeSeriesContainer will have an interval of 0 (irregular).  The rated TimeSeriesContainer will have values
	 * only at times that are common to all the input TimeSeriesContainers. 
	 * @param tscs The TimeSeriesContainers to rate, in order of the independent parameters of the rating.
	 * @param ratedUnitStr The unit to return the rated values in.
	 * @return A TimeSeriesContainer of the rated values. The rated unit is the specified unit.
	 * @throws RatingException
	 */
	public TimeSeriesContainer rate(TimeSeriesContainer[] tscs, String ratedUnitStr) throws RatingException {
		if (ratingSpec.getIndParamCount() != tscs.length) {
			throw new RatingException(String.format("Cannot rate a set of %d TimeSeriesContainers with a rating that has %d independent parameters", tscs.length, ratingSpec.getIndParamCount()));
		}
		if (ratings.size() == 0) throw new RatingException("No ratings.");
		String[] units = ratings.firstEntry().getValue().getRatingUnits();
		String[] params = ratingSpec.getIndParameters();
		if (ratedUnitStr == null || ratedUnitStr.length() == 0) ratedUnitStr = units[units.length-1];
		int ratedInterval = tscs[0].interval;
		int indParamCount = tscs.length;
		try {
			//------------------------//
			// validate the intervals //
			//------------------------//
			for (int i = 1; i < tscs.length; ++i) {
				if (tscs[i].interval != tscs[0].interval) {
					String msg = "TimeSeriesContainers have inconsistent intervals.";
					if (!allowUnsafe) throw new RatingException(msg);
					if (warnUnsafe) logger.warning(msg + "  Rated values will be irregular interval.");
					ratedInterval = 0;
					break;
				}
			}
			//--------------------------------------//
			// validate the time zones if specified //
			//--------------------------------------//
			String tzid = tscs[0].timeZoneID;
			for (int i = 1; i < tscs.length; ++i) {
				if (!TextUtil.equals(tscs[i].timeZoneID, tzid)) {
					String msg = "TimeSeriesContainers have inconsistent time zones.";
					if (!allowUnsafe) throw new RatingException(msg);
					if (warnUnsafe) logger.warning(msg + "  Value times will be treated as UTC.");
					tzid = null;
					break;
				}
			}
			TimeZone tz = null;
			if (tzid != null) {
				tz = TimeZone.getTimeZone(tzid);
				if (!tz.getID().equals(tzid)) {
					String msg = String.format("TimeSeriesContainers have invalid time zone \"%s\".", tzid);
					if (!allowUnsafe) throw new RatingException(msg);
					if (warnUnsafe) logger.warning(msg + "  Value times will be treated as UTC.");
					tz = null;
				}
			}
			//-------------------------------------------//
			// validate the parameters and units to rate //
			//-------------------------------------------//
			Parameter[] tscParam = new Parameter[indParamCount];
			Units[] tscUnit = new Units[indParamCount];
			Units ratedUnit = null;
			boolean[] convertTscUnit = new boolean[indParamCount];
			boolean convertRatedUnit = false;
			for (int i = 0; i < indParamCount; ++i) {
				tscParam[i] = null;
				try {
					tscParam[i] = new Parameter(tscs[i].parameter);
				}
				catch (Throwable t) {
					if (!allowUnsafe) throw new RatingException(t);
					if (warnUnsafe) logger.warning(t.getMessage());
				}
				if (tscParam[i] != null) {
					if (!tscParam[i].getParameter().equals(params[i])) {
						String msg = String.format("Parameter \"%s\" does not match rating parameter \"%s\".", tscParam[i].getParameter(), params[i]);
						if (!allowUnsafe) throw new RatingException(msg);
						if (warnUnsafe) logger.warning(msg);
					}
				}
				try {
					tscUnit[i] = new Units(tscs[i].units);
				}
				catch (Throwable t) {
					if (!allowUnsafe) throw new RatingException(t);
					if (warnUnsafe) logger.warning(t.getMessage());
				}
				if (tscParam[i] != null) {
					if (!Units.canConvertBetweenUnits(tscs[i].units, tscParam[i].getUnitsString())) {
						
						String msg = String.format("Unit \"%s\" is not valid for parameter \"%s\".", tscs[i].units, tscParam[i].getParameter());
						if (!allowUnsafe) throw new RatingException(msg);
						if (warnUnsafe) logger.warning(msg);
					}
				} 
				if (tscUnit != null) {
					if (!tscs[i].units.equals(units[i])) {
						if(Units.canConvertBetweenUnits(tscs[i].units, units[i])) {
							convertTscUnit[i] = true;
						}
						else {
							String msg = String.format("Cannot convert from \"%s\" to \"%s\".", tscs[i].units, units[i]);
							if (!allowUnsafe) throw new RatingException(msg);
							if (warnUnsafe) logger.warning(msg + "  Rating will be performed on unconverted values.");
						}
					}
				}
			}
			//--------------------------//
			// validate the result unit //
			//--------------------------//
			try {
				ratedUnit = new Units(ratedUnitStr);
			}
			catch (Throwable t) {
				if (!allowUnsafe) throw new RatingException(t);
				if (warnUnsafe) logger.warning(t.getMessage());
			}
			if (ratedUnit != null) {
				if (!ratedUnitStr.equals(units[units.length-1])) {
					if (Units.canConvertBetweenUnits(ratedUnitStr, units[units.length-1])) {
						convertRatedUnit = true;
					}
					else {
						String msg = String.format("Cannot convert from \"%s\" to \"%s\".", units[units.length-1], ratedUnit);
						if (!allowUnsafe) throw new RatingException(msg);
						if (warnUnsafe) logger.warning(msg + "  Rated values will be unconverted.");
					}
				}
			}
			//-------------------------//
			// finally - do the rating //
			//-------------------------//
			IndependentValuesContainer ivc = RatingConst.tscsToIvc(tscs, units, tz, allowUnsafe, warnUnsafe);
			double[] depVals = rate(ivc.indVals, ivc.valTimes);
			//-----------------------------------------//
			// construct the rated TimeSeriesContainer //
			//-----------------------------------------//
			if (convertRatedUnit) Units.convertUnits(depVals, units[units.length-1], ratedUnitStr);
			TimeSeriesContainer ratedTsc = new TimeSeriesContainer();
			tscs[0].clone(ratedTsc);
			ratedTsc.interval = ratedInterval;
			if (ivc.valTimes.length == tscs[0].times.length) {
				ratedTsc.times = Arrays.copyOf(tscs[0].times, tscs[0].times.length);
			}
			else {
				ratedTsc.times = new int[ivc.valTimes.length];
				if (tz == null) {
					for (int i = 0; i < ivc.valTimes.length; ++i) {
						ratedTsc.times[i] = Conversion.toMinutes(ivc.valTimes[i]);
					}
				}
				else {
					Calendar cal = Calendar.getInstance();
					cal.setTimeZone(tz);
					SimpleDateFormat sdf = new SimpleDateFormat("ddMMMyyyy, HH:mm");
					HecTime t = new HecTime();
					for (int i = 0; i < ivc.valTimes.length; ++i) {
						cal.setTimeInMillis(ivc.valTimes[i]);
						t.set(sdf.format(cal.getTime()));
						ratedTsc.times[i] = t.value();
					}
				}
			}
			ratedTsc.values = depVals;
			ratedTsc.numberValues = ratedTsc.times.length;
			String paramStr = params[params.length-1];
			if (tscs[0].subParameter == null) {
				ratedTsc.fullName = replaceAll(tscs[0].fullName, tscs[0].parameter, paramStr, "IL");
			}
			else {
				ratedTsc.fullName = replaceAll(tscs[0].fullName, String.format("%s-%s", tscs[0].parameter, tscs[0].subParameter), paramStr, "IL");
			}
			String[] parts = split(paramStr, "-", "L", 2);
			ratedTsc.parameter = parts[0];
			ratedTsc.subParameter = parts.length > 1 ? parts[1] : null;
			ratedTsc.units = ratedUnitStr;
			return ratedTsc;
		}
		catch (Throwable t) {
			if (t instanceof RatingException) throw (RatingException) t;
			throw new RatingException(t);
		}
	}
	/* (non-Javadoc)
	 * @see hec.data.IRating#rate(hec.hecmath.TimeSeriesMath)
	 */
	@Override
	public TimeSeriesMath rate(TimeSeriesMath tsm) throws RatingException {
		return rate(tsm, null);
	}
	/**
	 * Rates the values in a TimeSeriesMath and returns the results in a new TimeSeriesMath with the specified unit. 
	 * The rating must be for a single independent parameter. 
	 * @param tsm The TimeSeriesMath to rate
	 * @param ratedUnitStr The unit to return the rated values in.
	 * @return A TimeSeriesMath of the rated values. The rated unit is the specified unit.
	 * @throws RatingException
	 */
	public TimeSeriesMath rate(TimeSeriesMath tsm, String ratedUnitStr) throws RatingException {
		try {
			return new TimeSeriesMath(rate((TimeSeriesContainer)tsm.getData(), ratedUnitStr));
		}
		catch (Throwable t) {
			if (t instanceof RatingException) throw (RatingException) t;
			throw new RatingException(t);
		}
	}
	/* (non-Javadoc)
	 * @see hec.data.IRating#rate(hec.hecmath.TimeSeriesMath[])
	 */
	@Override
	public TimeSeriesMath rate(TimeSeriesMath[] tsms) throws RatingException {
		return rate(tsms, null);
	}
	/**
	 * Rates the values in a set of TimeSeriesMaths and returns the results in a new TimeSeriesMath with the specified unit.
	 * The rating must be for as many independent parameters as the number of TimeSeriesMaths. 
	 * If all the TimeSeriesMaths have the same interval the rated TimeSeriesMath will have the same interval, otherwise
	 * the rated TimeSeriesMath will have an interval of 0 (irregular).  The rated TimeSeriesMath will have values
	 * only at times that are common to all the input TimeSeriesMaths. 
	 * @param tsms The TimeSeriesMaths to rate, in order of the independent parameters of the rating.
	 * @param ratedUnitStr The unit to return the rated values in.
	 * @return A TimeSeriesMath of the rated values. The rated unit is the specified unit.
	 * @throws RatingException
	 */
	public TimeSeriesMath rate(TimeSeriesMath[] tsms, String ratedUnitStr) throws RatingException {
		TimeSeriesContainer[] tscs = new TimeSeriesContainer[tsms.length];
		try {
			for (int i = 0; i < tsms.length; ++i) {
				tscs[i] = (TimeSeriesContainer)tsms[i].getData();
			}
			return new TimeSeriesMath(rate(tscs, ratedUnitStr));
		}
		catch (Throwable t) {
			if (t instanceof RatingException) throw (RatingException) t;
			throw new RatingException(t);
		}
	}
	/**
	 * Retrieves the rating specification.
	 * @return The rating specification
	 */
	public RatingSpec getRatingSpec() {
		return ratingSpec;
	}
	/**
	 * Sets the rating specification.
	 * @param ratingSpec The rating specification
	 * @throws RatingException
	 */
	public void setRatingSpec(RatingSpec ratingSpec) throws RatingException {
		if (ratings != null && ratings.size() > 0) {
			if (ratingSpec.getIndParamCount() != ratings.firstEntry().getValue().getIndParamCount()) {
				throw new RatingException("Number of independent parameters does not match existing ratings");
			}
		}
		if (ratingSpec != null) {
			switch (ratingSpec.getInRangeMethod()) {
			case LOGARITHMIC :
			case LOG_LIN:
			case LIN_LOG:
				throw new RatingException("Invalid in-range rating method for rating times: "+ratingSpec.getInRangeMethod());
			}
			switch (ratingSpec.getOutRangeLowMethod()) {
			case LOGARITHMIC :
			case LOG_LIN:
			case LIN_LOG:
			case PREVIOUS:
				throw new RatingException("Invalid out-of-range low rating method for rating times: "+ratingSpec.getOutRangeLowMethod());
			}
			switch (ratingSpec.getOutRangeHighMethod()) {
			case LOGARITHMIC :
			case LOG_LIN:
			case LIN_LOG:
			case NEXT:
				throw new RatingException("Invalid out-of-range high rating method for rating times: "+ratingSpec.getOutRangeHighMethod());
			}
		}
		this.ratingSpec = ratingSpec;
	}
	/**
	 * Retrieves the times series of ratings.
	 * @return The times series of ratings.
	 */
	public AbstractRating[] getRatings() {
		return ratings.values().toArray(new AbstractRating[ratings.size()]);
	}
	/**
	 * Sets the times series of ratings, replacing any existing ratings.
	 * @param ratings The time series of ratings
	 * @throws RatingException 
	 */
	public void setRatings(AbstractRating[] ratings) throws RatingException {
		removeAllRatings();
		addRatings(ratings);
	}
	/**
	 * Retrieves the number of ratings in this set.
	 * @return The number of ratings in this set
	 */
	public int getRatingCount() {
		return ratings == null ? 0 : ratings.size();
	}
	/**
	 * Retrieves the default value time. This is used for rating values that have no inherent times.
	 * @return The default value time
	 */
	public long getDefaultValuetime() {
		return defaultValueTime;
	}
	/**
	 * Resets the default value time. This is used for rating values that have no inherent times.
	 */
	public void resetDefaultValuetime() {
		this.defaultValueTime = Const.UNDEFINED_TIME;
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#getRatingTime()
	 */
	@Override
	public long getRatingTime() {
		return ratingTime;
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#setRatingTime(long)
	 */
	@Override
	public void setRatingTime(long ratingTime) {
		this.ratingTime = ratingTime;
		if (ratings != null) {
			for (AbstractRating rating : ratings.values()) {
				rating.setRatingTime(ratingTime);
			}
		}
		refreshRatings();
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#resetRatingtime()
	 */
	@Override
	public void resetRatingTime() {
		ratingTime = Long.MAX_VALUE;
		if (ratings != null) {
			for (AbstractRating rating : ratings.values()) {
				rating.resetRatingTime();
			}
		}
		refreshRatings();
	}
	/**
	 * Retrieves whether this object allows "risky" operations such as working with mismatched units, unknown parameters, etc.
	 * @return A flag specifying whether this object allows "risky" operations such as working with mismatched units, unknown parameters, etc.
	 */
	public boolean doesAllowUnsafe() {
		return allowUnsafe;
	}
	/**
	 * Sets whether this object allows "risky" operations such as working with mismatched units, unknown parameters, etc.
	 * @param allowUnsafe A flag specifying whether this object allows "risky" operations such as working with mismatched units, unknown parameters, etc.
	 */
	public void setAllowUnsafe(boolean allowUnsafe) {
		this.allowUnsafe = allowUnsafe;
	}
	/**
	 * Retrieves whether this object outputs warning messages about "risky" operations such as working with mismatched units, unknown parameters, etc.
	 * @return A flag specifying whether this object outputs warning messages about "risky" operations such as working with mismatched units, unknown parameters, etc.
	 */
	public boolean doesWarnUnsafe() {
		return warnUnsafe;
	}
	/**
	 * Sets whether this object outputs warning messages about "risky" operations such as working with mismatched units, unknown parameters, etc.
	 * @param warnUnsafe  A flag specifying whether this object outputs warning messages about "risky" operations such as working with mismatched units, unknown parameters, etc.
	 */
	public void setWarnUnsafe(boolean warnUnsafe) {
		this.warnUnsafe = warnUnsafe;
	}
	/**
	 * Retrieves the standard HEC-DSS pathname for this rating set
	 * @return The standard HEC-DSS pathname for this rating set
	 */
	public String getDssPathname() {
		return ratingSpec == null ? null : ratingSpec.getDssPathname();
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#getName()
	 */
	@Override
	public String getName() {
		String name = null;
		if (ratingSpec == null) {
			if (ratings.size() > 0) {
				name = ratings.firstEntry().getValue().getName();
			}
		}
		else {
			name = ratingSpec.getRatingSpecId();
		}
		return name;
	}
	/* (non-Javadoc)
	 * @see hec.data.IRating#setName(java.lang.String)
	 */
	@Override
	public void setName(String name) throws RatingException {
		for (AbstractRating rating : ratings.values()) {
			rating.setName(name);
		}
		if (ratingSpec != null) {
			String[] parts = split(name, SEPARATOR1, "L");
			ratingSpec.setLocationId(parts[0]);
			ratingSpec.setParametersId(parts[1]);
			ratingSpec.setTemplateVersion(parts[2]);
			ratingSpec.setVersion(parts[3]);
		}
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#getRatingParameters()
	 */
	@Override
	public String[] getRatingParameters() {
		return getRatingSpec().getParameters();
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#getRatingUnits()
	 */
	@Override
	public String[] getRatingUnits() {
		String[] units = null;
		if (ratings.size() > 0) {
			units = ratings.firstEntry().getValue().getRatingUnits();
		}
		return units;
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#getDataUnits()
	 */
	@Override
	public String[] getDataUnits() {
		String[] units = null;
		if (ratings.size() > 0) {
			units = ratings.firstEntry().getValue().getDataUnits();
		}
		return units;
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#setDataUnits(java.lang.String[])
	 */
	@Override
	public void setDataUnits(String[] units) throws RatingException {
		for (ICwmsRating rating : ratings.values()) {
			rating.setDataUnits(units);
		}
	}
	/* (non-Javadoc)
	 * @see hec.data.IRating#getRatingExtents()
	 */
	@Override
	public double[][] getRatingExtents() throws RatingException {
		// TODO Auto-generated method stub
		return getRatingExtents(getRatingTime());
	}
	/* (non-Javadoc)
	 * @see hec.data.IRating#getRatingExtents(long)
	 */
	@Override
	public double[][] getRatingExtents(long ratingTime) throws RatingException {
		if (activeRatings.size() == 0) {
			throw new RatingException("No active ratings.");
		}
		return activeRatings.floorEntry(ratingTime).getValue().getRatingExtents();
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#getDefaultValueTime()
	 */
	@Override
	public long getDefaultValueTime() {
		return defaultValueTime;
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#setDefaultValueTime(long)
	 */
	@Override
	public void setDefaultValueTime(long defaultValueTime) {
		this.defaultValueTime = defaultValueTime;
		for (ICwmsRating rating : ratings.values()) {
			rating.setDefaultValueTime(defaultValueTime);
		}
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#rate(double)
	 */
	@Override
	public double rate(double indVal) throws RatingException {
		return rate(indVal, defaultValueTime);
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#rate(double[])
	 */
	@Override
	public double rateOne(double... indVals) throws RatingException {
		return rateOne(indVals, defaultValueTime);
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#rateOne(double[])
	 */
	@Override
	public double[] rate(double[] indVals) throws RatingException {
		return rate(indVals, defaultValueTime);
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#rate(double[][])
	 */
	@Override
	public double[] rate(double[][] indVals) throws RatingException {
		long[] valTimes = new long[indVals.length];
		Arrays.fill(valTimes, defaultValueTime);
		return rate(indVals, valTimes);
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#rate(long, double)
	 */
	@Override
	public double rate(long valTime, double indVal) throws RatingException {
		return rate(indVal, valTime);
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#rate(long, double[])
	 */
	@Override
	public double rateOne(long valTime, double... indVals) throws RatingException {
		return rateOne(indVals, valTime);
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#rateOne(long, double[])
	 */
	@Override
	public double[] rate(long valTime, double[] indVals)
			throws RatingException {
		return rate(indVals, valTime);
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#rateOne(long[], double[])
	 */
	@Override
	public double[] rate(long[] valTimes, double[] indVals)
			throws RatingException {
		return rateOne(indVals, valTimes);
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#rate(long, double[][])
	 */
	@Override
	public double[] rate(long valTime, double[][] indVals)
			throws RatingException {
		long[] valTimes = new long[indVals.length];
		Arrays.fill(valTimes, valTime);
		return rate(indVals, valTimes);
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#rate(long[], double[][])
	 */
	@Override
	public double[] rate(long[] valTimes, double[][] indVals)
			throws RatingException {
		return rate(indVals, valTimes);
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#reverseRate(double)
	 */
	@Override
	public double reverseRate(double depVal) throws RatingException {
		long[] valTimes = {defaultValueTime};
		double[] depVals = {depVal};
		return reverseRate(valTimes, depVals)[0];
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#reverseRate(double[])
	 */
	@Override
	public double[] reverseRate(double[] depVals) throws RatingException {
		long[] valTimes = {defaultValueTime};
		return reverseRate(valTimes, depVals);
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#reverseRate(long, double)
	 */
	@Override
	public double reverseRate(long valTime, double depVal)
			throws RatingException {
		long[] valTimes = {valTime};
		double[] depVals = {depVal};
		return reverseRate(valTimes, depVals)[0];
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#reverseRate(long, double[])
	 */
	@Override
	public double[] reverseRate(long valTime, double[] depVals)
			throws RatingException {
		long[] valTimes = {valTime};
		return reverseRate(valTimes, depVals);
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#reverseRate(long[], double[])
	 */
	@Override
	public double[] reverseRate(long[] valTimes, double[] depVals) throws RatingException {
		if (activeRatings.size() == 0) {
			throw new RatingException("No active ratings.");
		}
		double[] Y = new double[depVals.length];
		Map.Entry<Long, AbstractRating> lowerRating = null;
		Map.Entry<Long, AbstractRating> upperRating = null;
		IRating lastUsedRating = null;
		RatingMethod method = null;
		for (int i = 0; i < depVals.length; ++i) {
			if (i > 0 && valTimes[i] == valTimes[i-1]) {
				if (lastUsedRating == null) {
					Y[i] = Y[i-1];
				}
				else {
					Y[i] = lastUsedRating.reverseRate(valTimes[i], depVals[i]);
				}
				continue;
			}
			else {
				lowerRating = activeRatings.floorEntry(valTimes[i]);
				upperRating = activeRatings.ceilingEntry(valTimes[i]);
				//-------------------------//
				// handle out of range low //
				//-------------------------//
				if (lowerRating == null) {
					switch(ratingSpec.getOutRangeLowMethod()) {
					case ERROR:
						throw new RatingException("Effective date is before earliest rating");
					case NULL:
						Y[i] = Const.UNDEFINED_DOUBLE;
						lastUsedRating = null;
						continue;
					case NEXT:
					case NEAREST:
					case HIGHER:
					case CLOSEST:
						lastUsedRating = activeRatings.firstEntry().getValue(); 
						Y[i] = lastUsedRating.reverseRate(valTimes[i], depVals[i]);
						continue;
					}
					method = ratingSpec.getOutRangeLowMethod();
					if (activeRatings.size() == 1) {
						throw new RatingException(String.format("Cannot use rating method %s with only one active rating.", method));
					}
					lowerRating = activeRatings.firstEntry();
					upperRating = activeRatings.higherEntry(lowerRating.getKey());
				}
				//--------------------------//
				// handle out of range high //
				//--------------------------//
				if (upperRating == null) {
					switch(ratingSpec.getOutRangeHighMethod()) {
					case ERROR:
						throw new RatingException("Effective date is after latest rating");
					case NULL:
						Y[i] = Const.UNDEFINED_DOUBLE;
						lastUsedRating = null;
						continue;
					case PREVIOUS:
					case NEAREST:
					case LOWER:
					case CLOSEST:
						lastUsedRating = activeRatings.lastEntry().getValue();
						Y[i] = lastUsedRating.reverseRate(valTimes[i], depVals[i]);
						continue;
					}
					method = ratingSpec.getOutRangeHighMethod();
					if (activeRatings.size() == 1) {
						throw new RatingException(String.format("Cannot use rating method %s with only one active rating.", method));
					}
					upperRating = activeRatings.lastEntry();
					lowerRating = activeRatings.lowerEntry(upperRating.getKey());
				}
				//-----------------------------------//
				// handle in-range and extrapolation //
				//-----------------------------------//
				if (lowerRating.getKey() == valTimes[i]) {
					Y[i] = lowerRating.getValue().reverseRate(valTimes[i], depVals[i]);
					continue;
				}
				if (upperRating.getKey() == valTimes[i]) {
					lastUsedRating = upperRating.getValue();
					Y[i] = lastUsedRating.reverseRate(valTimes[i], depVals[i]);
					continue;
				}
				switch (ratingSpec.getInRangeMethod()) {
				case ERROR:
					throw new RatingException("Effective date is between existing rating");
				case NULL:
					Y[i] = Const.UNDEFINED_DOUBLE;
					lastUsedRating = null;
					continue;
				case PREVIOUS:
				case LOWER:
					lastUsedRating = lowerRating.getValue();
					Y[i] = lastUsedRating.reverseRate(valTimes[i], depVals[i]);
					continue;
				case NEXT:
				case HIGHER:
					lastUsedRating = upperRating.getValue();
					Y[i] = lastUsedRating.reverseRate(valTimes[i], depVals[i]);
					continue;
				case CLOSEST:
					if (valTimes[i] - lowerRating.getKey() < upperRating.getKey() - valTimes[i]) {
						lastUsedRating = lowerRating.getValue();
					}
					else {
						lastUsedRating = upperRating.getValue();
					}
					Y[i] = lastUsedRating.reverseRate(valTimes[i], depVals[i]);
					continue;
				}
				//------------------------------------//
				// handle interpolation/extrapolation //
				//------------------------------------//
				method = ratingSpec.getInRangeMethod();
			}
			lastUsedRating = null;
			boolean ind_log = method == RatingMethod.LOGARITHMIC || method == RatingMethod.LIN_LOG;
			boolean dep_log = method == RatingMethod.LOGARITHMIC || method == RatingMethod.LOG_LIN;
			double x  = (double)valTimes[i];
			double x1 = (double)lowerRating.getKey();
			double x2 = (double)upperRating.getKey();
			double Y1 = lowerRating.getValue().reverseRate(valTimes[i], depVals[i]);
			double Y2 = upperRating.getValue().reverseRate(valTimes[i], depVals[i]);
			double y1 = Y1;
			double y2 = Y2;
			if (ind_log) {
				x  = Math.log10(x);
				x1 = Math.log10(x1);
				x2 = Math.log10(x2);
				if (Double.isNaN(x) || Double.isInfinite(x)   
						|| Double.isNaN(x1) || Double.isInfinite(x1) 
						|| Double.isNaN(x2) || Double.isInfinite(x2))  {
					//-------------------------------------------------//
					// fall back from LOGARITHMIC or LOG_LIN to LINEAR //
					//-------------------------------------------------//
					x  = (double)valTimes[i];
					x1 = (double)lowerRating.getKey();
					x2 = (double)upperRating.getKey();
					dep_log = false;
				}
			}
			if (dep_log) {
				y1 = Math.log10(y1);
				y2 = Math.log10(y2);
				if (Double.isNaN(y1) || Double.isInfinite(y1) || Double.isNaN(y2) || Double.isInfinite(y2))  {
					//-------------------------------------------------//
					// fall back from LOGARITHMIC or LIN_LOG to LINEAR //
					//-------------------------------------------------//
					x  = (double)valTimes[i];
					x1 = (double)lowerRating.getKey();
					x2 = (double)upperRating.getKey();
					y1 = Y1;
					y2 = Y2;
					dep_log = false;
				}
			}
			double y = y1 + ((x - x1) / (x2 - x1)) * (y2 - y1);
			if (dep_log) y = Math.pow(10, y);
			Y[i] = y;
		}
		return Y;
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#reverseRate(hec.io.TimeSeriesContainer)
	 */
	@Override
	public TimeSeriesContainer reverseRate(TimeSeriesContainer tsc)
			throws RatingException {
		TimeSeriesContainer[] tscs = {tsc};
		String[] units = {tsc.units};
		TimeZone tz = null;
		if (tsc.timeZoneID != null) {
			tz = TimeZone.getTimeZone(tsc.timeZoneID);
			if (!tz.getID().equals(tsc.timeZoneID)) {
				String msg = String.format("TimeSeriesContainers have invalid time zone \"%s\".", tsc.timeZoneID);
				if (!allowUnsafe) throw new RatingException(msg);
				if (warnUnsafe) logger.warning(msg + "  Value times will be treated as UTC.");
				tz = null;
			}
		}
		IndependentValuesContainer ivc = RatingConst.tscsToIvc(tscs, units, tz, allowUnsafe, warnUnsafe);
		TimeSeriesContainer ratedTsc = new TimeSeriesContainer();
		tsc.clone(ratedTsc);
		double[] depVals = new double[ivc.indVals.length];
		for (int i = 0; i < depVals.length; ++i) depVals[i] = ivc.indVals[i][0];
		ratedTsc.values = reverseRate(ivc.valTimes, depVals);
		String[] params = getRatingParameters();
		String paramStr = params[0];
		if (tsc.subParameter == null) {
			ratedTsc.fullName = replaceAll(tsc.fullName, tsc.parameter, paramStr, "IL");
		}
		else {
			ratedTsc.fullName = replaceAll(tsc.fullName, String.format("%s-%s", tsc.parameter, tsc.subParameter), paramStr, "IL");
		}
		String[] parts = split(paramStr, "-", "L", 2);
		ratedTsc.parameter = parts[0];
		ratedTsc.subParameter = parts.length > 1 ? parts[1] : null;
		String[] dataUnits = getDataUnits();
		ratedTsc.units = dataUnits == null ? getRatingUnits()[0] : dataUnits[0];
		return ratedTsc;
	}
	/* (non-Javadoc)
	 * @see hec.data.IRating#reverseRate(hec.hecmath.TimeSeriesMath)
	 */
	@Override
	public TimeSeriesMath reverseRate(TimeSeriesMath tsm) throws RatingException {
		try {
			return new TimeSeriesMath(reverseRate((TimeSeriesContainer)tsm.getData()));
		}
		catch (Throwable t) {
			if (t instanceof RatingException) throw (RatingException) t;
			throw new RatingException(t);
		}
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#getIndParamCount()
	 */
	@Override
	public int getIndParamCount() throws RatingException {
		return ratingSpec == null ? ratingSpec.getIndParamCount() : ratings.firstEntry().getValue().getIndParamCount();
	}
	/**
	 * Outputs the rating set as an XML instance
	 * @param indent the text use for indentation
	 * @return the XML text
	 * @throws RatingException
	 */
	public String toXmlString(CharSequence indent) throws RatingException {
		StringBuilder sb = new StringBuilder();
		if(ratingSpec != null) {
			sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
			  .append("<ratings xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://www.hec.usace.army.mil/xmlSchema/cwms/Ratings.xsd\">\n")
			  .append(ratingSpec.toXmlString(indent, 1));
		}
		for (ICwmsRating cwmsRating : ratings.values()) {
			sb.append(cwmsRating.toXmlString(indent, 1));
		}
		if(ratingSpec != null) {
			sb.append("</ratings>\n");
		}
		return sb.toString();
	}
	/**
	 * Outputs the rating set in a compress XML instance suitable for storing in DSS
	 * @return The compressed XML text
	 * @throws RatingException
	 */
	public String toCompressedXmlString() throws RatingException {
 		try {
			return TextUtil.compress(toXmlString("  "), "base64");
		}
		catch (Throwable t) {
			if (t instanceof RatingException) throw (RatingException)t;
			throw new RatingException(t);
		}
	}
	/**
	 * Stores the rating set to a CWMS database
	 * @param conn The connection to the CWMS database
	 * @param overwriteExisting Flag specifying whether to overwrite any existing rating data
	 * @throws RatingException
	 */
	public void storeToDatabase(Connection conn, boolean overwriteExisting) throws RatingException {
		CallableStatement[] stmts = new CallableStatement[3];
		try {
			stmts[0] = conn.prepareCall("begin :1 := cwms_msg.get_msg_id; end;");
			stmts[0].registerOutParameter(1, Types.VARCHAR);
			stmts[0].execute();
			String lowerMessageBound = stmts[0].getString(1);
			
			stmts[1] = conn.prepareCall("begin cwms_rating.store_ratings_xml(:1, :2); end;");
			stmts[1].setString(1, toXmlString(""));
			stmts[1].setString(2, overwriteExisting ? "T" : "F");
			stmts[1].execute();
			
			stmts[0].execute();
			String upperMessageBound = stmts[0].getString(1);

			stmts[2] = conn.prepareCall(
				"select msg_text " +
				"  from cwms_v_log_message " +
				" where msg_id between :1 and :2 " +
				"   and msg_level = 'Basic' " +
				"   and properties like 'procedure = cwms\\_rating.store\\_%' escape '\\' " +
				"   and msg_text like 'ORA-%' " +
				" order by msg_id"); 
			stmts[2].setString(1, lowerMessageBound);
			stmts[2].setString(2, upperMessageBound);
			ResultSet rs = stmts[2].executeQuery();
			Vector<String> errors = new Vector<String>();
			while (rs.next()) {
				errors.add(rs.getString(1));
			}
			rs.close();
			
			for (int i = 0; i < stmts.length; ++i) {
				stmts[i].close();
				stmts[i] = null;
			}
			
			if (errors.size() > 0) {
				throw new RatingException(join("\n", errors.toArray(new String[errors.size()])));
			}
		}
		catch (Throwable t) {
			for (int i = 0; i < stmts.length; ++i) {
				try {if (stmts[i] != null) stmts[i].close();} catch(Throwable t1){}
			}
			if (t instanceof RatingException) throw (RatingException)t;
			throw new RatingException(t);
		}
	}
	/* (non-Javadoc)
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */
	@Override
	public void update(java.util.Observable arg0, Object arg1) {
		refreshRatings();
	}
	/**
	 * Adds an Observer to this RatingSet. The Observer will be notified of any changes to this RatingSet
	 * @param o The Observer object to add
	 * @see java.util.Observer
	 */
	public void addObserver(Observer o) {
		observationTarget.addObserver(o);
	}
	/**
	 * Deletes an Observer from this RatingSet. The Observer will no longer be notified of any changes to this RatingSet
	 * @param o The Observer object to delete
	 * @see java.util.Observer
	 */
	public void deleteObserver(Observer o) {
		observationTarget.deleteObserver(o);
	}
	/**
	 * Retrieves a RatingSetContainer containing the data of this object. 
	 * @return The RatingSetContainer
	 */
	public RatingSetContainer getData() {
		RatingSetContainer rsc = new RatingSetContainer();
		if (ratingSpec != null) {
			rsc.ratingSpecContainer = ratingSpec.getData();
		}
		if (ratings.size() > 0) {
			rsc.abstractRatingContainers = new AbstractRatingContainer[ratings.size()];
			Iterator<AbstractRating> it = ratings.values().iterator();
			for (int i = 0; it.hasNext(); ++i) {
				rsc.abstractRatingContainers[i] = it.next().getData(); 
			}
		}
		return rsc;
	}
	/**
	 * Sets the data from this object from a RatingSetContainer
	 * @param rsc The RatingSetContainer with the data
	 * @throws RatingException
	 */
	public void setData(RatingSetContainer rsc) throws RatingException {
		try {
			removeAllRatings();
			if (rsc.ratingSpecContainer == null) {
				ratingSpec = null;
			}
			else {
				setRatingSpec(new RatingSpec(rsc.ratingSpecContainer));
			}
			if (rsc.abstractRatingContainers != null) {
				for (int i = 0; i < rsc.abstractRatingContainers.length; ++i) {
					if (rsc.abstractRatingContainers[i] instanceof TableRatingContainer) {
						this.addRating(new TableRating((TableRatingContainer)rsc.abstractRatingContainers[i]));
					}
					else {
						throw new RatingException("Unexpected object type: " + rsc.abstractRatingContainers[i].getClass().getName());
					}
				}
			}
			if (observationTarget != null) {
				observationTarget.setChanged();
				observationTarget.notifyObservers();
			}
		}
		catch (Throwable t) {
			if (t instanceof RatingException) throw (RatingException)t;
			throw new RatingException(t);
		}
	}
	/**
	 * Retrieves a TextContainer containing the data of this object, suitable for storing to DSS. 
	 * @return The TextContainer
	 * @throws RatingException 
	 */
	public TextContainer getDssData() throws RatingException {
		try {
			TextContainer tc = new TextContainer();
			tc.fullName = getDssPathname();
			tc.text = String.format("%s\n%s", this.getClass().getName(), toCompressedXmlString());
			return tc;
		}		
		catch (Throwable t) {
			if (t instanceof RatingException) throw (RatingException)t;
			throw new RatingException(t);
		}
	}
	/**
	 * Sets the data from this object from a TextContainer (as read from DSS)
	 * @param tc The TextContainer with the data
	 * @throws RatingException
	 */
	public void setData(TextContainer tc) throws RatingException {
		String[] lines = tc.text.split("\\n");
		String className = getClass().getName();
		for (int i = 0; i < lines.length-1; ++i) {
			if (lines[i].equals(className)) {
				int extra = lines[i+1].length() % 4;
				int last = lines[i+1].length() - extra;
				RatingSet other = RatingSet.fromCompressedXml(lines[i+1].substring(0, last));
				removeAllRatings();
				setData(other.getData());
				return;
			}
		}
		throw new RatingException("Invalid text for RatingSet");
	}

	private void refreshRatings() {
		AbstractRating[] _ratings = ratings.values().toArray(new AbstractRating[ratings.size()]);
		ratings.clear();
		activeRatings.clear();
		for (int i = 0; i < _ratings.length; ++i) {
			long effectiveDate = _ratings[i].getEffectiveDate();
			ratings.put(effectiveDate, _ratings[i]);
			if (i < _ratings.length - 1 && _ratings[i] instanceof UsgsStreamTableRating) {
				UsgsStreamTableRating ustr = (UsgsStreamTableRating)_ratings[i];
				ustr.setRatingTime(Math.min(ustr.getRatingTime(), _ratings[i+1].getCreateDate()));
			}
			if (_ratings[i].isActive() && _ratings[i].getCreateDate() < ratingTime) {
				activeRatings.put(effectiveDate, _ratings[i]);
			}
			_ratings[i].deleteObserver(this);
			_ratings[i].addObserver(this);
		}
		if (observationTarget != null) {
			observationTarget.setChanged();
			observationTarget.notifyObservers();
		}
	}
	/**
	 * Validates the rating set
	 * @throws RatingException if the rating set is not valid
	 */
	private void validate() throws RatingException {
		if (ratings.size() == 0) return;
		String unitsId = ratings.firstEntry().getValue().getRatingUnitsId();
		String[] units = unitsId == null ? null : split(unitsId.replace(SEPARATOR2, SEPARATOR3), SEPARATOR3, "L");
		String[] params = null;
		boolean[] validParams = null;
		boolean[] validUnits = null;
		try {
			if (ratingSpec != null) {
				params = ratingSpec.getIndParameters();
				validParams = new boolean[ratingSpec.getIndParamCount()+1];
				validUnits = new boolean[ratingSpec.getIndParamCount()+1];
				Parameter ratingParam = null;
				Units ratingUnit = null;
				for (int i = 0; i < params.length; ++i) {
					ratingParam = null;
					validParams[i] = false;
					if (ratingSpec != null) {
						try {
							ratingParam = new Parameter(params[i]);
							validParams[i] = true;
						}
						catch (Throwable t) {
							if (!allowUnsafe) throw new RatingException(t);
							if (warnUnsafe) logger.warning(t.getMessage());
						}
					}
				}
				for (int i = 0; i < units.length; ++i) {
					ratingUnit = null;
					validUnits[i] = false;
					try {
						ratingUnit = new Units(units[i], true);
						validUnits[i] = true;
					}
					catch (Throwable t) {
						if (!allowUnsafe) throw new RatingException(t);
						if (warnUnsafe) logger.warning(t.getMessage());
					}
				}
				for (int i = 0; i < params.length; ++i) {
					if (validParams[i] && validUnits[i]) {
						ratingParam = new Parameter(params[i]);
						ratingUnit = new Units(units[i], true);
						if (!Units.canConvertBetweenUnits(ratingParam.getUnitsString(), ratingUnit.toString())) {
							String msg = String.format("Unit \"%s\" is not consistent with parameter \"%s\".", units[i], params[i]);
							if (!allowUnsafe) throw new RatingException(msg);
							if (warnUnsafe) logger.warning(msg + "  Rating will be performed on unconverted values.");
						}
					}
				}
			}
		}
		catch (Throwable t) {
			if (t instanceof RatingException) throw (RatingException) t;
			throw new RatingException(t);
		}
	}
		
//	public static void main(String[] args) throws Exception {
//		Connection conn = wcds.dbi.client.JdbcConnection.getConnection("jdbc:oracle:thin:@192.168.65.129:1521/CWMS22DEV", "cwms_20", "cwms22dev");
//		RatingSet rs = RatingSet.fromDatabase(conn, "SWT", "ARCA.Count-Conduit_Gates,Opening-Conduit_Gates,Elev;Flow-Conduit_Gates.Standard.Production");
//		double[][] extents = rs.getRatingExtents();
//		String[] params = rs.getRatingParameters();
//		String[] units = rs.getDataUnits();
//		for (int i = 0; i < params.length; ++i) {
//			System.out.println(String.format("%s\tmin = %f\tmax = %f\t%s", params[i], extents[0][i], extents[1][i], units[i]));
//		}
//		System.out.println("==================================");
//		rs.setDataUnits(new String[] {"unit", "m", "m", "cms"});
//		for (int i = 0; i < params.length; ++i) {
//			System.out.println(String.format("%s\tmin = %f\tmax = %f\t%s", params[i], extents[0][i], extents[1][i], units[i]));
//		}
//		System.out.println("==================================");
//		hec.heclib.dss.HecDss dssFile = hec.heclib.dss.HecDss.open("u:/junk/service_level.dss");
//		hec.hecmath.PairedDataMath r = (hec.hecmath.PairedDataMath)dssFile.read("/MISSOURI/MAINSTEM/DATE-VOLUME/SERVICE_LEVEL///");
//		hec.io.PairedDataContainer pdc = (hec.io.PairedDataContainer)r.getData();
//		extents = r.getRatingExtents();
//		System.out.println(String.format("%s\tmin = %f\tmax = %f\t%s", "Service Level", extents[0][0], extents[1][0], "n/a"));
//		System.out.println(String.format("%s\tmin = %f\tmax = %f\t%s", pdc.xparameter,  extents[0][1], extents[1][1], pdc.xunits));
//		System.out.println(String.format("%s\tmin = %f\tmax = %f\t%s", pdc.yparameter,  extents[0][2], extents[1][2], pdc.yunits));
//	}
	
//	public static void main(String[] args) throws Exception {
//		
//		Connection conn = wcds.dbi.client.JdbcConnection.getConnection("jdbc:oracle:thin:@192.168.65.128:1521/CWMS22DEV", "cwms_20", "cwms22dev");
//		RatingSet rs = RatingSet.fromDatabase(conn, "SWT", "TULA.Stage;Flow.Logarithmic.Production");
//		RatingSetContainer rsc = rs.getData();
//		RatingSet rs2 = new RatingSet(rsc);		
//		rs.getRatingSpec().setVersion("TESTER");
//		for(AbstractRating rating : rs.getRatings()) {
//			rating.setRatingSpecId(rs.getRatingSpec().getRatingSpecId());
//		}
//		rs.storeToDatabase(conn, true);
//		conn.commit();
//		double minStage = -Double.MAX_VALUE;
//		double maxStage = Double.MAX_VALUE;
//		AbstractRating[] ratings = rs.getRatings();
//		long[] valueTimes = new long[ratings.length * 2 + 1];
//		valueTimes[0] = ratings[0].getEffectiveDate();
//		UsgsRounder indRounder = rs.getRatingSpec().getIndRoundingSpecs()[0];
//		UsgsRounder depRounder = rs.getRatingSpec().getDepRoundingSpec();
//
//		String sql = 
//				"begin " +
//				   ":1 := cwms_rating.rate_f("   +
//				      "p_rating_spec => :2,"     +
//				      "p_value       => :3,"     +
//				      "p_units       => cwms_util.split_text(replace(:4, ';', ','), ',')," +
//				      "p_value_times => cast(cwms_util.to_timestamp(:5) as date),"   +
//				      "p_time_zone   => 'UTC',"  +
//				      "p_office_id   => :6);"    +
//				"end;";
//		CallableStatement rateStmt = conn.prepareCall(sql);
//		rateStmt.registerOutParameter(1, Types.NUMERIC);
//		rateStmt.setString(2, rs.getRatingSpec().getRatingSpecId());
//		rateStmt.setString(4, rs.getRatings()[0].getRatingUnitsId());
//		rateStmt.setString(6, rs.getRatingSpec().getOfficeId());
//		
//		CallableStatement revRateStmt = conn.prepareCall(sql.replaceAll("rate_f", "reverse_rate_f"));
//		revRateStmt.registerOutParameter(1, Types.NUMERIC);
//		revRateStmt.setString(2, rs.getRatingSpec().getRatingSpecId());
//		revRateStmt.setString(4, rs.getRatings()[0].getRatingUnitsId());
//		revRateStmt.setString(6, rs.getRatingSpec().getOfficeId());
//		
//		for (int i = 0; i < ratings.length; ++i) {
//			TableRating tr = (TableRating)ratings[i];
//			TableRatingContainer trc = (TableRatingContainer)tr.getData();
//			int j = 2 * i + 1;
//			int k = 2 * (i + 1);
//			valueTimes[k] = trc.effectiveDateMillis;
//			valueTimes[j] = (valueTimes[i] + valueTimes[k]) / 2;
//			double low = trc.values[0].indValue;
//			double high = trc.values[trc.values.length-1].indValue;
//			if (low > minStage) minStage = low;
//			if (high < maxStage) maxStage = high;
//		}
//		for (long valueTime : valueTimes) System.out.println(valueTime);
//		double flow;
//		double stage2;
//		for (double stage = minStage; stage <= maxStage; stage += 2.5) {
//			System.out.print(String.format("%12s", indRounder.format(stage)));
//			for (int i = 0; i < valueTimes.length; ++i) {
//				flow = rs.rate(valueTimes[i], stage);
//				stage2 = rs.reverseRate(valueTimes[i], flow);
//				System.out.print(String.format("%12s", depRounder.format(flow)));
//				System.out.print(String.format("%12s", indRounder.format(stage2)));
//			}
//			System.out.println("");
//			System.out.print(String.format("%12s", indRounder.format(stage)));
//			for (int i = 0; i < valueTimes.length; ++i) {
//				rateStmt.setDouble(3, stage);
//				rateStmt.setLong(5, valueTimes[i]);
//				rateStmt.execute();
//				flow = rateStmt.getDouble(1);
//				revRateStmt.setDouble(3, flow);
//				revRateStmt.setLong(5, valueTimes[i]);
//				revRateStmt.execute();
//				stage2 = revRateStmt.getDouble(1);
//				System.out.print(String.format("%12s", depRounder.format(flow)));
//				System.out.print(String.format("%12s", indRounder.format(stage2)));
//			}
//			System.out.println("\n");
//			
//		}
//
//		rs = RatingSet.fromDatabase(conn, "SWT", "KEYS.Elev;Stor.Linear.Production");
//		rs.setDefaultValueTime(System.currentTimeMillis());
//		indRounder = rs.getRatingSpec().getIndRoundingSpecs()[0];
//		depRounder = rs.getRatingSpec().getDepRoundingSpec();
//		
//		rateStmt.setString(2, rs.getRatingSpec().getRatingSpecId());
//		rateStmt.setString(4, rs.getRatings()[0].getRatingUnitsId());
//		rateStmt.setString(6, rs.getRatingSpec().getOfficeId());
//		
//		revRateStmt.setString(2, rs.getRatingSpec().getRatingSpecId());
//		revRateStmt.setString(4, rs.getRatings()[0].getRatingUnitsId());
//		revRateStmt.setString(6, rs.getRatingSpec().getOfficeId());
//		
//		for (double elev = 658.; elev < 780.; elev += 5.) {
//			double stor = rs.rate(elev);
//			double elev2 = rs.reverseRate(stor);
//			System.out.print(String.format("%12s", indRounder.format(elev)));
//			System.out.print(String.format("%12s", depRounder.format(stor)));
//			System.out.print(String.format("%12s", indRounder.format(elev2)));
//			System.out.println("");
//			rateStmt.setDouble(3, elev);
//			rateStmt.setLong(5, System.currentTimeMillis());
//			rateStmt.execute();
//			stor = rateStmt.getDouble(1);
//			revRateStmt.setDouble(3, stor);
//			revRateStmt.setLong(5, System.currentTimeMillis());
//			revRateStmt.execute();
//			elev2 = revRateStmt.getDouble(1);
//			System.out.print(String.format("%12s", indRounder.format(elev)));
//			System.out.print(String.format("%12s", depRounder.format(stor)));
//			System.out.print(String.format("%12s", indRounder.format(elev2)));
//			System.out.println("\n");
//		}
//
//		rs = RatingSet.fromDatabase(conn, "SWT", "ARCA.Count-Conduit_Gates,Opening-Conduit_Gates,Elev;Flow-Conduit_Gates.Standard.Production");
//		rs.setDefaultValueTime(System.currentTimeMillis());
//		UsgsRounder[] indRounders = rs.getRatingSpec().getIndRoundingSpecs();
//		indRounders[2] = new UsgsRounder("5555555555");
//		depRounder = rs.getRatingSpec().getDepRoundingSpec();
//		
//		sql = 
//			"begin " +
//			   ":1 := cwms_rating.rate_one_f("   +
//			      "p_rating_spec => :2,"     +
//			      "p_values      => double_tab_t(:3,:4,:5),"     +
//			      "p_units       => cwms_util.split_text(replace(:6, ';', ','), ',')," +
//			      "p_value_time  => cast(cwms_util.to_timestamp(:7) as date),"   +
//			      "p_time_zone   => 'UTC',"  +
//			      "p_office_id   => :8);"    +
//			"end;";
//		rateStmt = conn.prepareCall(sql);
//		rateStmt.registerOutParameter(1, Types.NUMERIC);
//		rateStmt.setString(2, rs.getRatingSpec().getRatingSpecId());
//		rateStmt.setString(6, rs.getRatings()[0].getRatingUnitsId());
//		rateStmt.setLong(7, System.currentTimeMillis());
//		rateStmt.setString(8, rs.getRatingSpec().getOfficeId());
//		
//		double[] valueSet = new double[3];
//		for (double count = 1.; count <= 2.; count += 1.) {
//			valueSet[0] = count;
//			rateStmt.setDouble(3, count);
//			for (double opening = .5; opening <= 7.; opening += .5) {
//				valueSet[1] = opening;
//				rateStmt.setDouble(4, opening);
//				for (double elev = 956.; elev <= 1030.; elev += 2.5) {
//					valueSet[2] = elev;
//					rateStmt.setDouble(5, elev);
//					flow = rs.rateOne(valueSet);
//					System.out.println(String.format(
//							"%12s%12s%12s%12s", 
//							indRounders[0].format(count), 
//							indRounders[1].format(opening), 
//							indRounders[2].format(elev), 
//							depRounder.format(flow)));
//					rateStmt.execute();
//					flow = rateStmt.getDouble(1);
//					System.out.println(String.format(
//							"%12s%12s%12s%12s\n", 
//							indRounders[0].format(count), 
//							indRounders[1].format(opening), 
//							indRounders[2].format(elev), 
//							depRounder.format(flow)));
//				}
//			}
//		}
//		conn.close();
//	}
}
