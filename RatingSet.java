package hec.data.cwmsRating;

import static hec.data.cwmsRating.RatingConst.SEPARATOR1;
import static hec.data.cwmsRating.RatingConst.SEPARATOR2;
import static hec.data.cwmsRating.RatingConst.SEPARATOR3;
import static hec.lang.Const.UNDEFINED_TIME;
import static hec.util.TextUtil.join;
import static hec.util.TextUtil.replaceAll;
import static hec.util.TextUtil.split;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observer;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import hec.data.DataSetException;
import hec.data.IRating;
import hec.data.IRatingSet;
import hec.data.IVerticalDatum;
import hec.data.Parameter;
import hec.data.RatingException;
import hec.data.RatingObjectDoesNotExistException;
import hec.data.RoundingException;
import hec.data.Units;
import hec.data.VerticalDatumException;
import hec.data.cwmsRating.RatingConst.RatingMethod;
import hec.data.cwmsRating.io.AbstractRatingContainer;
import hec.data.cwmsRating.io.IndependentValuesContainer;
import hec.data.cwmsRating.io.RatingSetContainer;
import hec.data.cwmsRating.io.RatingSpecContainer;
import hec.data.cwmsRating.io.ReferenceRatingContainer;
import hec.data.cwmsRating.io.TableRatingContainer;
import hec.data.rating.IRatingSpecification;
import hec.data.rating.IRatingTemplate;
import hec.heclib.util.HecTime;
import hec.hecmath.TextMath;
import hec.hecmath.TimeSeriesMath;
import hec.io.Conversion;
import hec.io.TextContainer;
import hec.io.TimeSeriesContainer;
import hec.lang.Const;
import hec.lang.Observable;
import hec.util.TextUtil;
/**
 * Implements CWMS-style ratings (time series of ratings)
 *  
 * @author Mike Perryman
 */
public class RatingSet implements IRating, IRatingSet, Observer, IVerticalDatum {

	protected static final Logger logger = Logger.getLogger(RatingSet.class.getPackage().getName());
	
	/**
	 * Rating object for rating by reference
	 */
	private ReferenceRating dbrating = null;
	/**
	 * Flag specifying whether to load actual ratings only when needed.
	 */
	private boolean isLazy = false;
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
	 * The specified units of the data to rate
	 */
	protected String[] dataUnits = null;
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
	 * Connection for lazy and reference ratings.
	 */
	protected Connection conn = null;
	/**
	 * Connection info for lazy and reference ratings.
	 */
	protected DbInfo dbInfo = null;
	/**
	 * Class for use in LAZY and REFERENCE ratings to be able to release and re-retrieve connections from the connection pool
	 */
	protected class DbInfo {
		private String url = null;
		private String userName = null;
		private String officeId = null;
		
		public DbInfo(String url, String userName, String officeId) throws RatingException {
			if (url      == null) throw new RatingException("DbInfo.url cannot be null");
			if (userName == null) throw new RatingException("DbInfo.userName cannot be null");
			if (officeId == null) throw new RatingException("DbInfo.officeId cannot be null");
			this.userName = userName;
			this.url = url;
			this.officeId = officeId;
		}
		
		public String getUserName() { return userName; }
		public String getUrl()      { return url;      }
		public String getOfficeId() { return officeId; }
		@Override
		public int hashCode() { 
			return getClass().getName().hashCode() 
					+ 3 * url.toLowerCase().hashCode() 
					+ 5 * userName.toLowerCase().hashCode() 
					+ 7 * officeId.toLowerCase().hashCode();
		}
		@Override
		public boolean equals(Object obj) {
			return obj == this 
					|| (obj instanceof DbInfo
							&& ((DbInfo)obj).url.equalsIgnoreCase(url)
							&& ((DbInfo)obj).userName.equalsIgnoreCase(userName)
							&& ((DbInfo)obj).officeId.equalsIgnoreCase(officeId));
		}
	}
	
	protected class ConnectionInfo {
		private Connection conn = null;
		private boolean wasRetrieved = false;
		
		public ConnectionInfo(Connection conn, boolean wasRetrieved) {
			this.conn = conn;
			this.wasRetrieved = wasRetrieved;
		}
		
		public Connection getConnection() { return conn; }
		public boolean wasRetrieved() { return wasRetrieved; }
		@Override
		public int hashCode() {
			return getClass().getName().hashCode() + conn.hashCode() + (wasRetrieved ? 3 : 5);
		}
		@Override
		public boolean equals(Object obj) {
			boolean result = obj == this;
			if (!result) {
				if (obj instanceof ConnectionInfo) {
					ConnectionInfo other = (ConnectionInfo) obj;
					if (other.wasRetrieved != wasRetrieved) return false;
					if (other.conn == conn) return true;
					DatabaseMetaData md1;
					DatabaseMetaData md2;
					try {
						md1 = conn.getMetaData();
						md2 = other.conn.getMetaData();
						if (!md1.getURL().equalsIgnoreCase(md2.getURL())) return false;
						if (!md1.getUserName().equalsIgnoreCase(md2.getUserName())) return false;
						return true;
					} catch (SQLException e) {}
				}
			}
			return result;
		}
	}
	/**
	 * Enumeration for specifying the method used to load a RatingSet object from a CWMS database 
	 * <table border>
	 *   <tr>
	 *     <th>Value</th>
	 *     <th>Interpretation</th>
	 *   </tr>
	 *   <tr>
	 *     <td>EAGER</td>
	 *     <td>Ratings for all effective times are loaded initially</td>
	 *     <td>LAZY</td>
	 *     <td>No ratings are loaded initially - each rating is only loaded when it is needed</td>
	 *     <td>REFERENCE</td>
	 *     <td>No ratings are loaded ever - values are passed to database to be rated</td>
	 *   </tr>
	 * </table>
	 */
	public enum DatabaseLoadMethod {
		EAGER,
		LAZY,
		REFERENCE
	}
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
				
		return new RatingSet(null, conn, null, ratingSpecId, null, null, false);
				
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
		
		return new RatingSet(null, conn, null, ratingSpecId, startTime, endTime, false);
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
		
		return new RatingSet(null, conn, officeId, ratingSpecId, null, null, false);
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
	public static RatingSet fromDatabaseEffective(
			Connection conn, 
			String ratingSpecId, 
			Long startTime, 
			Long endTime)
			throws RatingException {
		
		return new RatingSet(null, conn, null, ratingSpecId, startTime, endTime, true);
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
		
		return new RatingSet(null, conn, officeId, ratingSpecId, startTime, endTime, false);
	}
	/**
	 * Generates a new RatingSet object from a CWMS database connection
	 * @param conn The connection to a CWMS database
	 * @param officeId The identifier of the office owning the rating. If null, the office associated with the connect user is used. 
	 * @param ratingSpecId The rating specification identifier
	 * @param startTime The time of the earliest data to rate, in milliseconds.  If null, no earliest limit is set.
	 * @param endTime The time of the latest data to rate, in milliseconds.  If null, no latest limit is set.
	 * @return The new RatingSet object
	 * @throws RatingException
	 */
	public static RatingSet fromDatabaseEffective(
			Connection conn, 
			String officeId, 
			String ratingSpecId, 
			Long startTime, 
			Long endTime)
			throws RatingException {
		
		return new RatingSet(null, conn, officeId, ratingSpecId, startTime, endTime, true);
	}
	/**
	 * Generates a new RatingSet object from a CWMS database connection
	 * @param officeId The identifier of the office owning the rating. If null, the office associated with the connect user is used. 
	 * @param ratingSpecId The rating specification identifier
	 * @param startTime The earliest time to retrieve, as interpreted by inEffectTimes, in milliseconds.  If null, no earliest limit is set.
	 * @param endTime The latest time to retrieve, as interpreted by inEffectTimes, in milliseconds.  If null, no latest limit is set.
	 * @param dataTimes Determines how startTime and endTime are interpreted.
	 *        <table border>
	 *          <tr>
	 *            <th>Value</th>
	 *            <th>Interpretation</th>
	 *          </tr>
	 *          <tr>
	 *            <td>false</td>
	 *            <td>The time window specifies the extent of when the ratings became effective</td>
	 *            <td>true</td>
	 *            <td>Time time window specifies the time extent of data rate</td>
	 *          </tr>
	 *        </table>
	 * @return The new RatingSet object
	 * @throws RatingException
	 */
	public static RatingSet fromDatabase(
			Connection conn, 
			String officeId, 
			String ratingSpecId, 
			Long startTime, 
			Long endTime,
			boolean dataTimes)
			throws RatingException {
		return new RatingSet(null, conn, officeId, ratingSpecId, startTime, endTime, dataTimes);
	}
	/**
	 * Generates a new RatingSet object from a CWMS database connection
	 * @param loadMethod The method used to load the object from the database. If null, the value of the property
	 *        "hec.data.cwmsRating.RatingSet.databaseLoadMethod" is used. If both the argument and property value
	 *        are null (or if an invalid value is specified) the Lazy method will be used.
	 *        <table border>
	 *          <tr>
	 *            <th>Value (case insensitive)</th>
	 *            <th>Interpretation</th>
	 *          </tr>
	 *          <tr>
	 *            <td>Eager</td>
	 *            <td>Ratings for all effective times are loaded initially</td>
	 *            <td>Lazy</td>
	 *            <td>No ratings are loaded initially - each rating is only loaded when it is needed</td>
	 *            <td>Reference</td>
	 *            <td>No ratings are loaded ever - values are passed to database to be rated</td>
	 *          </tr>
	 *        </table>
	 * @param conn The connection to a CWMS database
	 * @param ratingSpecId The rating specification identifier
	 * @return The new RatingSet object
	 * @throws RatingException
	 */
	public static RatingSet fromDatabase(
			DatabaseLoadMethod loadMethod,
			Connection conn, 
			String ratingSpecId) 
			throws RatingException {
				
		return new RatingSet(loadMethod, conn, null, ratingSpecId, null, null, false);
				
	}
	/**
	 * Generates a new RatingSet object from a CWMS database connection
	 * @param loadMethod The method used to load the object from the database. If null, the value of the property
	 *        "hec.data.cwmsRating.RatingSet.databaseLoadMethod" is used. If both the argument and property value
	 *        are null (or if an invalid value is specified) the Lazy method will be used.
	 *        <table border>
	 *          <tr>
	 *            <th>Value (case insensitive)</th>
	 *            <th>Interpretation</th>
	 *          </tr>
	 *          <tr>
	 *            <td>Eager</td>
	 *            <td>Ratings for all effective times are loaded initially</td>
	 *            <td>Lazy</td>
	 *            <td>No ratings are loaded initially - each rating is only loaded when it is needed</td>
	 *            <td>Reference</td>
	 *            <td>No ratings are loaded ever - values are passed to database to be rated</td>
	 *          </tr>
	 *        </table>
	 * @param conn The connection to a CWMS database
	 * @param ratingSpecId The rating specification identifier
	 * @param startTime The earliest effective date to retrieve, in milliseconds.  If null, no earliest limit is set.
	 * @param endTime The latest effective date to retrieve, in milliseconds.  If null, no latest limit is set.
	 * @return The new RatingSet object
	 * @throws RatingException
	 */
	public static RatingSet fromDatabase(
			DatabaseLoadMethod loadMethod,
			Connection conn, 
			String ratingSpecId, 
			Long startTime, 
			Long endTime)
			throws RatingException {
		
		return new RatingSet(loadMethod, conn, null, ratingSpecId, startTime, endTime, false);
	}
	/**
	 * Generates a new RatingSet object from a CWMS database connection
	 * @param loadMethod The method used to load the object from the database. If null, the value of the property
	 *        "hec.data.cwmsRating.RatingSet.databaseLoadMethod" is used. If both the argument and property value
	 *        are null (or if an invalid value is specified) the Lazy method will be used.
	 *        <table border>
	 *          <tr>
	 *            <th>Value (case insensitive)</th>
	 *            <th>Interpretation</th>
	 *          </tr>
	 *          <tr>
	 *            <td>Eager</td>
	 *            <td>Ratings for all effective times are loaded initially</td>
	 *            <td>Lazy</td>
	 *            <td>No ratings are loaded initially - each rating is only loaded when it is needed</td>
	 *            <td>Reference</td>
	 *            <td>No ratings are loaded ever - values are passed to database to be rated</td>
	 *          </tr>
	 *        </table>
	 * @param conn The connection to a CWMS database
	 * @param officeId The identifier of the office owning the rating. If null, the office associated with the connect user is used. 
	 * @param ratingSpecId The rating specification identifier
	 * @return The new RatingSet object
	 * @throws RatingException
	 */
	public static RatingSet fromDatabase(
			DatabaseLoadMethod loadMethod,
			Connection conn, 
			String officeId, 
			String ratingSpecId)
			throws RatingException {
		
		return new RatingSet(loadMethod, conn, officeId, ratingSpecId, null, null, false);
	}
	/**
	 * Generates a new RatingSet object from a CWMS database connection
	 * @param loadMethod The method used to load the object from the database. If null, the value of the property
	 *        "hec.data.cwmsRating.RatingSet.databaseLoadMethod" is used. If both the argument and property value
	 *        are null (or if an invalid value is specified) the Lazy method will be used.
	 *        <table border>
	 *          <tr>
	 *            <th>Value (case insensitive)</th>
	 *            <th>Interpretation</th>
	 *          </tr>
	 *          <tr>
	 *            <td>Eager</td>
	 *            <td>Ratings for all effective times are loaded initially</td>
	 *            <td>Lazy</td>
	 *            <td>No ratings are loaded initially - each rating is only loaded when it is needed</td>
	 *            <td>Reference</td>
	 *            <td>No ratings are loaded ever - values are passed to database to be rated</td>
	 *          </tr>
	 *        </table>
	 * @param conn The connection to a CWMS database
	 * @param ratingSpecId The rating specification identifier
	 * @param startTime The earliest effective date to retrieve, in milliseconds.  If null, no earliest limit is set.
	 * @param endTime The latest effective date to retrieve, in milliseconds.  If null, no latest limit is set.
	 * @return The new RatingSet object
	 * @throws RatingException
	 */
	public static RatingSet fromDatabaseEffective(
			DatabaseLoadMethod loadMethod,
			Connection conn, 
			String ratingSpecId, 
			Long startTime, 
			Long endTime)
			throws RatingException {
		
		return new RatingSet(loadMethod, null, null, ratingSpecId, startTime, endTime, true);
	}
	/**
	 * Generates a new RatingSet object from a CWMS database connection
	 * @param loadMethod The method used to load the object from the database. If null, the value of the property
	 *        "hec.data.cwmsRating.RatingSet.databaseLoadMethod" is used. If both the argument and property value
	 *        are null (or if an invalid value is specified) the Lazy method will be used.
	 *        <table border>
	 *          <tr>
	 *            <th>Value (case insensitive)</th>
	 *            <th>Interpretation</th>
	 *          </tr>
	 *          <tr>
	 *            <td>Eager</td>
	 *            <td>Ratings for all effective times are loaded initially</td>
	 *            <td>Lazy</td>
	 *            <td>No ratings are loaded initially - each rating is only loaded when it is needed</td>
	 *            <td>Reference</td>
	 *            <td>No ratings are loaded ever - values are passed to database to be rated</td>
	 *          </tr>
	 *        </table>
	 * @param conn The connection to a CWMS database
	 * @param officeId The identifier of the office owning the rating. If null, the office associated with the connect user is used. 
	 * @param ratingSpecId The rating specification identifier
	 * @param startTime The earliest effective date to retrieve, in milliseconds.  If null, no earliest limit is set.
	 * @param endTime The latest effective date to retrieve, in milliseconds.  If null, no latest limit is set.
	 * @return The new RatingSet object
	 * @throws RatingException
	 */
	public static RatingSet fromDatabase(
			DatabaseLoadMethod loadMethod,
			Connection conn, 
			String officeId, 
			String ratingSpecId, 
			Long startTime, 
			Long endTime)
			throws RatingException {
		
		return new RatingSet(loadMethod, conn, officeId, ratingSpecId, startTime, endTime, false);
	}
	/**
	 * Generates a new RatingSet object from a CWMS database connection
	 * @param loadMethod The method used to load the object from the database. If null, the value of the property
	 *        "hec.data.cwmsRating.RatingSet.databaseLoadMethod" is used. If both the argument and property value
	 *        are null (or if an invalid value is specified) the Lazy method will be used.
	 *        <table border>
	 *          <tr>
	 *            <th>Value (case insensitive)</th>
	 *            <th>Interpretation</th>
	 *          </tr>
	 *          <tr>
	 *            <td>Eager</td>
	 *            <td>Ratings for all effective times are loaded initially</td>
	 *            <td>Lazy</td>
	 *            <td>No ratings are loaded initially - each rating is only loaded when it is needed</td>
	 *            <td>Reference</td>
	 *            <td>No ratings are loaded ever - values are passed to database to be rated</td>
	 *          </tr>
	 *        </table>
	 * @param conn The connection to a CWMS database
	 * @param officeId The identifier of the office owning the rating. If null, the office associated with the connect user is used. 
	 * @param ratingSpecId The rating specification identifier
	 * @param startTime The time of the earliest data to rate, in milliseconds.  If null, no earliest limit is set.
	 * @param endTime The time of the latest data to rate, in milliseconds.  If null, no latest limit is set.
	 * @return The new RatingSet object
	 * @throws RatingException
	 */
	public static RatingSet fromDatabaseEffective(
			DatabaseLoadMethod loadMethod,
			Connection conn, 
			String officeId, 
			String ratingSpecId, 
			Long startTime, 
			Long endTime)
			throws RatingException {
		return new RatingSet(loadMethod, conn, officeId, ratingSpecId, startTime, endTime, true);
	}
	/**
	 * Generates a new RatingSet object from a CWMS database connection
	 * @param loadMethod The method used to load the object from the database. If null, the value of the property
	 *        "hec.data.cwmsRating.RatingSet.databaseLoadMethod" is used. If both the argument and property value
	 *        are null (or if an invalid value is specified) the Lazy method will be used.
	 *        <table border>
	 *          <tr>
	 *            <th>Value (case insensitive)</th>
	 *            <th>Interpretation</th>
	 *          </tr>
	 *          <tr>
	 *            <td>Eager</td>
	 *            <td>Ratings for all effective times are loaded initially</td>
	 *            <td>Lazy</td>
	 *            <td>No ratings are loaded initially - each rating is only loaded when it is needed</td>
	 *            <td>Reference</td>
	 *            <td>No ratings are loaded ever - values are passed to database to be rated</td>
	 *          </tr>
	 *        </table>
	 * @param conn The connection to a CWMS database
	 * @param officeId The identifier of the office owning the rating. If null, the office associated with the connect user is used. 
	 * @param ratingSpecId The rating specification identifier
	 * @param startTime The earliest time to retrieve, as interpreted by inEffectTimes, in milliseconds.  If null, no earliest limit is set.
	 * @param endTime The latest time to retrieve, as interpreted by inEffectTimes, in milliseconds.  If null, no latest limit is set.
	 * @param dataTimes Determines how startTime and endTime are interpreted.
	 *        <table border>
	 *          <tr>
	 *            <th>Value</th>
	 *            <th>Interpretation</th>
	 *          </tr>
	 *          <tr>
	 *            <td>false</td>
	 *            <td>The time window specifies the extent of when the ratings became effective</td>
	 *            <td>true</td>
	 *            <td>Time time window specifies the time extent of data rate</td>
	 *          </tr>
	 *        </table>
	 * @return The new RatingSet object
	 * @throws RatingException
	 */
	public static RatingSet fromDatabase(
			DatabaseLoadMethod loadMethod,
			Connection conn, 
			String officeId, 
			String ratingSpecId, 
			Long startTime, 
			Long endTime,
			boolean dataTimes)
			throws RatingException {
		
		return new RatingSet(loadMethod, conn, officeId, ratingSpecId, startTime, endTime, dataTimes);
	}
	/**
	 * Generates a new RatingSet object from an XML instance.
	 * @param xmlText The XML instance to construct the RatingSet object from. The document (root) node is expected to be
	 *        &lt;ratings&gt;, which is expected to have one or more &lt;rating&gt; or &lt;usgs-stream-rating&gt; child nodes, all of the same
	 *        rating specification.  Appropriate &lt;rating-template&gt; and &lt;rating-spec&gt; nodes are required for the rating set;
	 *        any other template and specification nodes are ignored. The XML instance may be compressed (gzip+base64).
	 * @return A new RatingSet object
	 * @throws RatingException
	 */
	public static RatingSet fromXml(String xmlText) throws RatingException {
		try {
			return new RatingSet(xmlText, false);
		}
		catch (RatingException e1) {
			try {
				return new RatingSet(xmlText, true);
			}
			catch (RatingException e2) {
				throw new RatingException("Text is not a valid compressed or uncompressed CWMS Ratings XML instance.");
			}
		}
	}
	/**
	 * Returns the XML required to generate a new RatingSet object based on specified criteria
	 * @param loadMethod The method used to load the object from the database. If null, the value of the property
	 *        "hec.data.cwmsRating.RatingSet.databaseLoadMethod" is used. If both the argument and property value
	 *        are null (or if an invalid value is specified) the Lazy method will be used.
	 *        <table border>
	 *          <tr>
	 *            <th>Value (case insensitive)</th>
	 *            <th>Interpretation</th>
	 *          </tr>
	 *          <tr>
	 *            <td>Eager</td>
	 *            <td>Ratings for all effective times are loaded initially</td>
	 *            <td>Lazy</td>
	 *            <td>No ratings are loaded initially - each rating is only loaded when it is needed</td>
	 *            <td>Reference</td>
	 *            <td>No ratings are loaded ever - values are passed to database to be rated</td>
	 *          </tr>
	 *        </table>
	 * @param conn The connection to a CWMS database
	 * @param officeId The identifier of the office owning the rating. If null, the office associated with the connect user is used. 
	 * @param ratingSpecId The rating specification identifier
	 * @param startTime The earliest time to retrieve, as interpreted by inEffectTimes, in milliseconds.  If null, no earliest limit is set.
	 * @param endTime The latest time to retrieve, as interpreted by inEffectTimes, in milliseconds.  If null, no latest limit is set.
	 * @param dataTimes Determines how startTime and endTime are interpreted.
	 *        <table border>
	 *          <tr>
	 *            <th>Value</th>
	 *            <th>Interpretation</th>
	 *          </tr>
	 *          <tr>
	 *            <td>false</td>
	 *            <td>The time window specifies the extent of when the ratings became effective</td>
	 *            <td>true</td>
	 *            <td>Time time window specifies the time extent of data rate</td>
	 *          </tr>
	 *        </table>
	 * @return The XML instance
	 * @throws RatingException
	 */
	public static String getXmlFromDatabase(
			DatabaseLoadMethod loadMethod,
			Connection conn, 
			String officeId, 
			String ratingSpecId, 
			Long startTime, 
			Long endTime,
			boolean dataTimes)
			throws RatingException {
		
		CallableStatement cstmt = null;
		String specifiedLoadMethod = null;
		String databaseLoadMethod = null;
		String xmlText = null;
		synchronized (conn) {
			try {
				specifiedLoadMethod = (loadMethod == null ? System.getProperty("hec.data.cwmsRating.RatingSet.databaseLoadMethod", "lazy") : loadMethod.name()).toLowerCase();
				databaseLoadMethod = specifiedLoadMethod.toLowerCase();
				String sql = null;
				switch (databaseLoadMethod) {
				case "eager" :
				case "lazy" : 
				case "reference" :
					break;
				default :
						logger.log(
								Level.WARNING,
								"Invalid value for property hec.data.cwmsRating.RatingSet.databaseLoadMethod: "
								+ specifiedLoadMethod
								+ "\nMust be one of \"Eager\", \"Lazy\", or \"Reference\".\nUsing \"Lazy\"");
					databaseLoadMethod = "lazy";
				}
				Clob clob = null;
				switch (databaseLoadMethod) {
				case "eager" :
					//------------------------------------//
					// Load all rating data from database //
					//------------------------------------//
					if (dataTimes) 
						sql =
							"declare " +
							   "l_millis_start integer := :1;" +
							   "l_millis_end   integer := :2;" +
							   "l_date_start   date;" +
							   "l_date_end     date;" +
							"begin " +
							   "if l_millis_start is not null then " +
							      "l_date_start := cast(cwms_util.to_timestamp(l_millis_start) as date);" +
							   "end if;" +
							   "if l_millis_end is not null then "   +
							      "l_date_end := cast(cwms_util.to_timestamp(l_millis_end) as date);" +
							   "end if;" +
							   "cwms_rating.retrieve_eff_ratings_xml3("  +
							      "p_ratings        => :3,"    +
							      "p_spec_id_mask   => :4,"    +
							      "p_start_date     => l_date_start," +
							      "p_end_date       => l_date_end,"   +
							      "p_time_zone      => 'UTC'," +
							      "p_office_id_mask => :5);"   +
							"end;";
					else
						sql = 
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
							   "cwms_rating.retrieve_ratings_xml3("  +
							      "p_ratings              => :3,"    +
							      "p_spec_id_mask         => :4,"    +
							      "p_effective_date_start => l_effective_date_start," +
							      "p_effective_date_end   => l_effective_date_end,"   +
							      "p_time_zone            => 'UTC'," +
							      "p_office_id_mask       => :5);"   +
							"end;";
					cstmt = conn.prepareCall(sql);
					try {
						cstmt.registerOutParameter(3, Types.CLOB);
						cstmt.setString(4, ratingSpecId);
						if (startTime == null) {
							cstmt.setNull(1, Types.INTEGER);
						}
						else {
							cstmt.setLong(1, startTime);
						}
						if (endTime == null) {
							cstmt.setNull(2, Types.INTEGER);
						}
						else {
							cstmt.setLong(2, endTime);
						}
						if (officeId == null) {
							cstmt.setNull(5, Types.VARCHAR);
						}
						else {
							cstmt.setString(5, officeId);
						}
						cstmt.execute();
						clob = cstmt.getClob(3);
						try {
							if (clob.length() > Integer.MAX_VALUE) {
								throw new RatingException("CLOB too long.");
							}
							xmlText = clob.getSubString(1, (int)clob.length());
						}
						finally {
							freeClob(clob);
						}
					}
					finally {
						cstmt.close();
					}
				break;
				case "lazy" :
					//-----------------------------------------------//
					// load only spec and rating times from database //
					//-----------------------------------------------//
					sql =
					"declare " +
					   "l_millis_start integer := :1;" +
					   "l_millis_end   integer := :2;" +
					   "l_date_start   date;" +
					   "l_date_end     date;" +
					"begin " +
					   "if l_millis_start is not null then " +
					      "l_date_start := cast(cwms_util.to_timestamp(l_millis_start) as date);" +
					   "end if;" +
					   "if l_millis_end is not null then "   +
					      "l_date_end := cast(cwms_util.to_timestamp(l_millis_end) as date);" +
					   "end if;" +
					   ":3 := cwms_rating.retrieve_ratings_xml_data(" +
					      "p_effective_tw         => :4," + 
					      "p_spec_id_mask         => :5," +
					      "p_start_date           => l_date_start," +
					      "p_end_date             => l_date_end," + 
					      "p_time_zone            => 'UTC'," +
					      "p_include_points       => 'F'," + 
					      "p_office_id_mask       => :6);" +
					"end;";
					cstmt = conn.prepareCall(sql);
					try {
						cstmt.registerOutParameter(3, Types.CLOB);
						cstmt.setString(4, dataTimes ? "T" : "F");
						cstmt.setString(5, ratingSpecId);
						if (startTime == null) {
							cstmt.setNull(1, Types.INTEGER);
						}
						else {
							cstmt.setLong(1, startTime);
						}
						if (endTime == null) {
							cstmt.setNull(2, Types.INTEGER);
						}
						else {
							cstmt.setLong(2, endTime);
						}
						if (officeId == null) {
							cstmt.setNull(6, Types.VARCHAR);
						}
						else {
							cstmt.setString(6, officeId);
						}
						logger.log(Level.INFO, "Retrieving clob from database");
						cstmt.execute();
						clob = cstmt.getClob(3);
						logger.log(Level.INFO, "Clob retrieved from database");
						try {
							logger.log(Level.INFO, "Clob length = " + clob.length());
							if (clob.length() > Integer.MAX_VALUE) {
								throw new RatingException("CLOB too long.");
							}
							xmlText = clob.getSubString(1, (int)clob.length());
							logger.log(Level.INFO, "XML length = " + xmlText);
						}
						finally {
							logger.log(Level.INFO, "Freeing clob");
							freeClob(clob);
						}
					}
					finally {
						cstmt.close();
					}
				break;
				case "reference" :
					//----------------------------------------//
					// Load only /template+spec from database //
					//----------------------------------------//
					String specXmlText = RatingSpec.getXmlfromDatabase(conn, officeId, ratingSpecId);
					String[] parts = TextUtil.split(ratingSpecId, SEPARATOR1);
					String ratingTemplateId = TextUtil.join(SEPARATOR1, parts[1], parts[2]);
					String templateXmlText = RatingTemplate.getXmlfromDatabase(conn, officeId, ratingTemplateId);
					StringBuilder sb = new StringBuilder();
					sb.append(templateXmlText.substring(0, templateXmlText.indexOf("</ratings>")));
					sb.append("  ");
					sb.append(specXmlText.substring(specXmlText.indexOf("<rating-spec")));
					xmlText = sb.toString();
					break;
				}
			}
			catch (Throwable t) {
				if (t instanceof RatingException) throw (RatingException)t;
				throw new RatingException(t);
			}
			logger.log(Level.FINE,"Retrieved XML:\n"+xmlText);
			return xmlText;
		}
	}
	/**
	 * Stores the rating set to a CWMS database
	 * @param conn The connection to the CWMS database
	 * @param overwriteExisting Flag specifying whether to overwrite any existing rating data
	 * @throws RatingException
	 */
	static void storeToDatabase(Connection conn, String xml, boolean overwriteExisting) throws RatingException {
		synchronized (conn) {
			storeToDatabase(conn, xml, overwriteExisting, true);
		}
	}
	/**
	 * Stores the rating set to a CWMS database
	 * @param conn The connection to the CWMS database
	 * @param overwriteExisting Flag specifying whether to overwrite any existing rating data
	 * @param replaceBase Flag specifying whether to replace the base curves of USGS-style stream ratings (false = only store shifts)
	 * @throws RatingException
	 */
	static void storeToDatabase(Connection conn, String xml, boolean overwriteExisting, boolean replaceBase) 
			throws RatingException {
		synchronized (conn) {
			try {
				try {
					CallableStatement stmt = conn.prepareCall("begin cwms_rating.store_ratings_xml(:1, :2, :3, :4); end;");
					stmt.registerOutParameter(1, Types.CLOB);
					stmt.setString(2, xml);
					stmt.setString(3, overwriteExisting ? "F" : "T"); // db api parameter is p_fail_if_exists = opposite of overwrite
					stmt.setString(4, replaceBase ? "T" : "F");
					stmt.execute();
					Clob clob = stmt.getClob(1);
					if (clob != null) {
						try {
							if (clob.length() > Integer.MAX_VALUE) {
								throw new RatingException("CLOB too long.");
							}
							String errors = clob.getSubString(1, (int)clob.length());
							if (errors.length() > 0) {
								throw new RatingException(errors);
							}
						}
						finally {
							freeClob(clob);
						}
					}
				}
				catch (SQLException e) {
					if (e.getMessage().indexOf("PLS-00306") < 0) {
						throw e;
					}
					// try older database API
					storeToDatabaseOld(conn, xml, overwriteExisting, replaceBase);
				}
			}
			catch (Throwable t) {
				if (t instanceof RatingException) throw (RatingException)t;
				throw new RatingException(t);
			}
		}
	}
	/**
	 * Stores the rating set to a CWMS database
	 * @param conn The connection to the CWMS database
	 * @param overwriteExisting Flag specifying whether to overwrite any existing rating data
	 * @param replaceBase Flag specifying whether to replace the base curves of USGS-style stream ratings (false = only store shifts)
	 * @throws RatingException
	 * @deprecated Should not be needed after database schema 18.1.1 is fully distributed
	 */
	static void storeToDatabaseOld(Connection conn, String xml, boolean overwriteExisting, boolean replaceBase) 
			throws RatingException {
		CallableStatement[] stmts = new CallableStatement[3];
		synchronized (conn) {
			try {
				stmts[0] = conn.prepareCall("begin :1 := cwms_msg.get_msg_id; end;");
				stmts[0].registerOutParameter(1, Types.VARCHAR);
				stmts[0].execute();
				String lowerMessageBound = stmts[0].getString(1);
				
				// first try newer schema with 3 parameters on CWMS_RATING.STORE_RATINGS_XML()
				stmts[1] = conn.prepareCall("begin cwms_rating.store_ratings_xml(:1, :2, :3); end;");
				stmts[1].setString(1, xml);
				stmts[1].setString(2, overwriteExisting ? "F" : "T"); // db api parameter is p_fail_if_exists = opposite of overwrite
				stmts[1].setString(3, replaceBase ? "T" : "F");
				try {
					stmts[1].execute();
				}
				catch (SQLException e) {
					if (e.getMessage().indexOf("PLS-00306") < 0) {
						throw e;
					}
					// allow for older schema with only 2 parameters on CWMS_RATING.STORE_RATINGS_XML()
					stmts[1] = conn.prepareCall("begin cwms_rating.store_ratings_xml(:1, :2); end;");
					stmts[1].setString(1, xml);
					stmts[1].setString(2, overwriteExisting ? "F" : "T"); // db api parameter is p_fail_if_exists = opposite of overwrite
					stmts[1].execute();
				}
				
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
	}
	/**
	 * Public constructor from a CWMS database connection
	 * @param conn The connection to a CWMS database
	 * @param ratingSpecId The rating specification identifier
	 * @return The new RatingSet object
	 * @throws RatingException
	 */
	public RatingSet(
			Connection conn, 
			String ratingSpecId) 
			throws RatingException {
				
		this(null, conn, null, ratingSpecId, null, null);
				
	}
	/**
	 * Public constructor from a CWMS database connection
	 * @param conn The connection to a CWMS database
	 * @param ratingSpecId The rating specification identifier
	 * @param startTime The earliest effective date to retrieve, in milliseconds.  If null, no earliest limit is set.
	 * @param endTime The latest effective date to retrieve, in milliseconds.  If null, no latest limit is set.
	 * @return The new RatingSet object
	 * @throws RatingException
	 */
	public RatingSet(
			Connection conn, 
			String ratingSpecId, 
			Long startTime, 
			Long endTime)
			throws RatingException {
		
		this(null, conn, null, ratingSpecId, startTime, endTime);
	}
	/**
	 * Public constructor from a CWMS database connection
	 * @param conn The connection to a CWMS database
	 * @param officeId The identifier of the office owning the rating. If null, the office associated with the connect user is used. 
	 * @param ratingSpecId The rating specification identifier
	 * @return The new RatingSet object
	 * @throws RatingException
	 */
	public RatingSet(
			Connection conn, 
			String officeId, 
			String ratingSpecId)
			throws RatingException {
		
		this(null, conn, officeId, ratingSpecId, null, null);
	}
	/**
	 * Public constructor from a CWMS database connection
	 * @param conn The connection to a CWMS database
	 * @param officeId The identifier of the office owning the rating. If null, the office associated with the connect user is used. 
	 * @param ratingSpecId The rating specification identifier
	 * @param startTime The earliest effective date to retrieve, in milliseconds.  If null, no earliest limit is set.
	 * @param endTime The latest effective date to retrieve, in milliseconds.  If null, no latest limit is set.
	 * @return The new RatingSet object
	 * @throws RatingException
	 */
	public RatingSet(
			Connection conn, 
			String officeId, 
			String ratingSpecId, 
			Long startTime, 
			Long endTime)
			throws RatingException {
		this(null, conn, officeId, ratingSpecId, startTime, endTime, false);
	}
	/**
	 * Generates a new RatingSet object from a CWMS database connection
	 * @param officeId The identifier of the office owning the rating. If null, the office associated with the connect user is used. 
	 * @param ratingSpecId The rating specification identifier
	 * @param startTime The earliest time to retrieve, as interpreted by inEffectTimes, in milliseconds.  If null, no earliest limit is set.
	 * @param endTime The latest time to retrieve, as interpreted by inEffectTimes, in milliseconds.  If null, no latest limit is set.
	 * @param dataTimes Determines how startTime and endTime are interpreted.
	 *        <table border>
	 *          <tr>
	 *            <th>Value</th>
	 *            <th>Interpretation</th>
	 *          </tr>
	 *          <tr>
	 *            <td>false</td>
	 *            <td>The time window specifies the extent of when the ratings became effective</td>
	 *            <td>true</td>
	 *            <td>Time time window specifies the time extent of data rate</td>
	 *          </tr>
	 *        </table>
	 * @return The new RatingSet object
	 * @throws RatingException
	 */
	public RatingSet(
			Connection conn, 
			String officeId, 
			String ratingSpecId, 
			Long startTime, 
			Long endTime,
			boolean dataTimes)
			throws RatingException {
		this(null, conn, officeId, ratingSpecId, startTime, endTime, dataTimes);
	}
	/**
	 * Public constructor from a CWMS database connection
	 * @param loadMethod The method used to load the object from the database. If null, the value of the property
	 *        "hec.data.cwmsRating.RatingSet.databaseLoadMethod" is used. If both the argument and property value
	 *        are null (or if an invalid value is specified) the Lazy method will be used.
	 *        <table border>
	 *          <tr>
	 *            <th>Value (case insensitive)</th>
	 *            <th>Interpretation</th>
	 *          </tr>
	 *          <tr>
	 *            <td>Eager</td>
	 *            <td>Ratings for all effective times are loaded initially</td>
	 *            <td>Lazy</td>
	 *            <td>No ratings are loaded initially - each rating is only loaded when it is needed</td>
	 *            <td>Reference</td>
	 *            <td>No ratings are loaded ever - values are passed to database to be rated</td>
	 *          </tr>
	 *        </table>
	 * @param conn The connection to a CWMS database
	 * @param ratingSpecId The rating specification identifier
	 * @return The new RatingSet object
	 * @throws RatingException
	 */
	public RatingSet(
			DatabaseLoadMethod loadMethod,
			Connection conn, 
			String ratingSpecId) 
			throws RatingException {
				
		this(loadMethod, conn, null, ratingSpecId, null, null);
				
	}
	/**
	 * Public constructor from a CWMS database connection
	 * @param loadMethod The method used to load the object from the database. If null, the value of the property
	 *        "hec.data.cwmsRating.RatingSet.databaseLoadMethod" is used. If both the argument and property value
	 *        are null (or if an invalid value is specified) the Lazy method will be used.
	 *        <table border>
	 *          <tr>
	 *            <th>Value (case insensitive)</th>
	 *            <th>Interpretation</th>
	 *          </tr>
	 *          <tr>
	 *            <td>Eager</td>
	 *            <td>Ratings for all effective times are loaded initially</td>
	 *            <td>Lazy</td>
	 *            <td>No ratings are loaded initially - each rating is only loaded when it is needed</td>
	 *            <td>Reference</td>
	 *            <td>No ratings are loaded ever - values are passed to database to be rated</td>
	 *          </tr>
	 *        </table>
	 * @param conn The connection to a CWMS database
	 * @param ratingSpecId The rating specification identifier
	 * @param startTime The earliest effective date to retrieve, in milliseconds.  If null, no earliest limit is set.
	 * @param endTime The latest effective date to retrieve, in milliseconds.  If null, no latest limit is set.
	 * @return The new RatingSet object
	 * @throws RatingException
	 */
	public RatingSet(
			DatabaseLoadMethod loadMethod,
			Connection conn, 
			String ratingSpecId, 
			Long startTime, 
			Long endTime)
			throws RatingException {
		
		this(loadMethod, conn, null, ratingSpecId, startTime, endTime);
	}
	/**
	 * Public constructor from a CWMS database connection
	 * @param loadMethod The method used to load the object from the database. If null, the value of the property
	 *        "hec.data.cwmsRating.RatingSet.databaseLoadMethod" is used. If both the argument and property value
	 *        are null (or if an invalid value is specified) the Lazy method will be used.
	 *        <table border>
	 *          <tr>
	 *            <th>Value (case insensitive)</th>
	 *            <th>Interpretation</th>
	 *          </tr>
	 *          <tr>
	 *            <td>Eager</td>
	 *            <td>Ratings for all effective times are loaded initially</td>
	 *            <td>Lazy</td>
	 *            <td>No ratings are loaded initially - each rating is only loaded when it is needed</td>
	 *            <td>Reference</td>
	 *            <td>No ratings are loaded ever - values are passed to database to be rated</td>
	 *          </tr>
	 *        </table>
	 * @param conn The connection to a CWMS database
	 * @param officeId The identifier of the office owning the rating. If null, the office associated with the connect user is used. 
	 * @param ratingSpecId The rating specification identifier
	 * @return The new RatingSet object
	 * @throws RatingException
	 */
	public RatingSet(
			DatabaseLoadMethod loadMethod,
			Connection conn, 
			String officeId, 
			String ratingSpecId)
			throws RatingException {
		
		this(loadMethod, conn, officeId, ratingSpecId, null, null);
	}
	/**
	 * Public constructor from a CWMS database connection
	 * @param loadMethod The method used to load the object from the database. If null, the value of the property
	 *        "hec.data.cwmsRating.RatingSet.databaseLoadMethod" is used. If both the argument and property value
	 *        are null (or if an invalid value is specified) the Lazy method will be used.
	 *        <table border>
	 *          <tr>
	 *            <th>Value (case insensitive)</th>
	 *            <th>Interpretation</th>
	 *          </tr>
	 *          <tr>
	 *            <td>Eager</td>
	 *            <td>Ratings for all effective times are loaded initially</td>
	 *            <td>Lazy</td>
	 *            <td>No ratings are loaded initially - each rating is only loaded when it is needed</td>
	 *            <td>Reference</td>
	 *            <td>No ratings are loaded ever - values are passed to database to be rated</td>
	 *          </tr>
	 *        </table>
	 * @param conn The connection to a CWMS database
	 * @param officeId The identifier of the office owning the rating. If null, the office associated with the connect user is used. 
	 * @param ratingSpecId The rating specification identifier
	 * @param startTime The earliest effective date to retrieve, in milliseconds.  If null, no earliest limit is set.
	 * @param endTime The latest effective date to retrieve, in milliseconds.  If null, no latest limit is set.
	 * @return The new RatingSet object
	 * @throws RatingException
	 */
	public RatingSet(
			DatabaseLoadMethod loadMethod,
			Connection conn, 
			String officeId, 
			String ratingSpecId, 
			Long startTime, 
			Long endTime)
			throws RatingException {
		this(loadMethod, conn, officeId, ratingSpecId, startTime, endTime, false);
	}
	/**
	 * @return a the current database connection plus a flag specifying whether it was retrieved using the DbInfo
	 * @throws RatingException
	 */
	protected ConnectionInfo getConnectionInfo() throws RatingException {
		synchronized(this) {
			if (conn != null) {
				return new ConnectionInfo(conn, false);
			}
			else {
				if (dbInfo == null) {
					String msg = String.format(
							"Rating set %s - %s is not currently connected to a database.\n"
							+ "Call setConnection(Connection) first or use a method with a Connection parameter.", 
							getRatingSpec().getRatingSpecId(),
							System.identityHashCode(this));
					throw new RatingException(msg);
				}
				else {
					try {
						conn = (Connection)Class.forName(
								"wcds.dbi.client.JdbcConnection"
								).getMethod(
										"retrieveConnection", 
										String.class, 
										String.class, 
										String.class
										).invoke(
												null, 
												dbInfo.getUrl(),
												dbInfo.getUserName(),
												dbInfo.getOfficeId()
												);
					} catch (Exception e) {
						throw new RatingException(e);
					} 
					if (conn == null) {
						String msg = String.format(
								"Rating set %s could not retrieve connection with current database information.\n"
								+ "Call setConnection(Connection) first or use a method with a Connection parameter.", 
								getRatingSpec().getRatingSpecId());
						throw new RatingException(msg);
					}
					setDatabaseConnection(conn);
					return new ConnectionInfo(conn, true);
				}
			}			
		}
	}
	/**
	 * Releases a database connection that was retrieved using the DbInfo
	 * @param ci The database connection information
	 * @throws RatingException
	 */
	protected void releaseConnection(ConnectionInfo ci) throws RatingException {
		if (ci.wasRetrieved()) {
			clearDatabaseConnection();
			try {
				Class.forName(
						"wcds.dbi.client.JdbcConnection"
						).getMethod(
								"closeConnection", 
								Connection.class).invoke(
										null, 
										ci.getConnection()
										);
			} catch (Exception e) {
				throw new RatingException(e);
			}
		}
	}
	/**
	 * @param rating
	 * @return the latest creation or effective date for this rating or its component parts
	 */
	protected long getReferenceTime(AbstractRating rating) {
		synchronized(this) {
			long referenceTime = Math.max(rating.createDate, rating.effectiveDate);
			if (rating instanceof UsgsStreamTableRating) {
				UsgsStreamTableRating sr = (UsgsStreamTableRating)rating;
				referenceTime = Math.max(referenceTime, getReferenceTime(sr.offsets));
				if (sr.shifts != null) {
					referenceTime = Math.max(referenceTime, sr.shifts.getReferenceTime());
				}
			}
			else if (rating instanceof VirtualRating) {
				VirtualRating vr = (VirtualRating)rating;
				if (vr.sourceRatings != null) {
					for (SourceRating sr : vr.sourceRatings) {
						if (sr.ratings != null) {
							referenceTime = Math.max(referenceTime, sr.ratings.getReferenceTime());
						}
					}
				}
			}
			else if (rating instanceof TransitionalRating) {
				TransitionalRating tr = (TransitionalRating)rating;
				if (tr.sourceRatings != null) {
					for (SourceRating sr : tr.sourceRatings) {
						if (sr.ratings != null) {
							referenceTime = Math.max(referenceTime, sr.ratings.getReferenceTime());
						}
					}
				}
			}
			return referenceTime;
		}
	}
	/**
	 * @return the latest creation or effective date for all the included ratings
	 */
	protected long getReferenceTime() {
		synchronized(this) {
			long referenceTime = UNDEFINED_TIME;
			for (AbstractRating rating : ratings.values()) {
				long t = getReferenceTime(rating);
				if (t > referenceTime) {
					referenceTime = t;
				}
			}
			return referenceTime;
		}
	}
	/**
	 * Collects rating specs used by rating and components
	 * @param rating the rating to inspect
	 * @param componentRatingSpecs the set of rating specs to collect into
	 */
	protected void getComponentRatingSpecs(AbstractRating rating, HashSet<String> componentRatingSpecs) {
		synchronized(this) {
			componentRatingSpecs.add(rating.getRatingSpecId());
			if (rating instanceof UsgsStreamTableRating) {
				UsgsStreamTableRating sr = (UsgsStreamTableRating)rating;
				if (sr.offsets != null) {
					getComponentRatingSpecs(sr.offsets, componentRatingSpecs);
				}
				if (sr.shifts != null) {
					componentRatingSpecs.addAll(sr.shifts.getComponentRatingSpecs());
				}
			}
			else if (rating instanceof VirtualRating) {
				VirtualRating vr = (VirtualRating)rating;
				if (vr.sourceRatings != null) {
					for (SourceRating sr : vr.sourceRatings) {
						if (sr.ratings != null) {
							componentRatingSpecs.addAll(sr.ratings.getComponentRatingSpecs());
						}
					}
				}
			}
			else if (rating instanceof TransitionalRating) {
				TransitionalRating tr = (TransitionalRating)rating;
				if (tr.sourceRatings != null) {
					for (SourceRating sr : tr.sourceRatings) {
						if (sr.ratings != null) {
							componentRatingSpecs.addAll(sr.ratings.getComponentRatingSpecs());
						}
					}
				}
			}
		}
	}
	/**
	 * @return all rating specs used in this rating set
	 */
	protected HashSet<String> getComponentRatingSpecs() {
		synchronized(this) {
			HashSet<String> componentRatingSpecs = new HashSet<String>();
			for (AbstractRating rating : ratings.values()) {
				getComponentRatingSpecs(rating, componentRatingSpecs);
			}
			return componentRatingSpecs;
		}
	}
	/**
	 * @return whether this rating set has been updated in the database
	 * @param conn The database connection to use
	 * @throws Exception
	 */
	public boolean isUpdated(Connection conn) throws Exception {
		setDatabaseConnection(conn);
		try {
			return isUpdated();
		}
		finally {
			clearDatabaseConnection();
		}
	}
	/**
	 * @return whether this rating set has been updated in the database
	 * @throws Exception
	 */
	private boolean isUpdated() throws Exception {
		synchronized(this) {
			ConnectionInfo ci = getConnectionInfo();
			Connection _conn = ci.getConnection();
			synchronized(_conn) {
				PreparedStatement stmt = null;
				ResultSet rs = null;
				try {
					stmt = _conn.prepareStatement("select cwms_util.to_millis(greatest(max(effective_date), max(create_date))) from cwms_v_rating where rating_id=:1");
					long referenceTime = getReferenceTime();
					for (String ratingSpec : getComponentRatingSpecs()) {
						stmt.setString(1, ratingSpec);
						rs = stmt.executeQuery();
						rs.next();
						if (rs.getLong(1) > referenceTime) {
							return true;
						}
					}
					return false;
				}
				finally {
					try {
						if (rs != null) {
							try {rs.close();}
							finally {}
						}
						if (stmt != null) {
							try {stmt.close();}
							finally {}
						}
					}
					finally {}
					releaseConnection(ci);
				}
			}
		}
	}
	/**
	 * Attempts to free the database resources held by a Clob object
	 * @param clob the object to free
	 */
	static void freeClob(Clob clob) {
		try {
			clob.free();
		}
		catch(Throwable t) {
			if (logger.isLoggable(Level.WARNING)) {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				t.printStackTrace(pw);
				logger.log(Level.WARNING, sw.toString());
			}
		}
	}
	/**
	 * Public Constructor - sets rating specification only
	 * @param officeId The office that owns the rating specification
	 * @param ratingSpecId The rating specification identifier
	 * @param sourceAgencyId The identifier of the agency that maintains ratings using this specification
	 * @param inRangeMethod The prescribed behavior for when the time of the value to rate falls within the range of rating effective dates
	 * @param outRangeLowMethod The prescribed behavior for when the time of the value to rate falls before the earliest rating effective date
	 * @param outRangeHighMethod The prescribed behavior for when the time of the value to rate falls after the latest rating effective date
	 * @param active Specifies whether to utilize any ratings using this specification
	 * @param autoUpdate Specifies whether ratings using this specification should be automatically loaded when new ratings are available
	 * @param autoActivate Specifies whether ratings using this specification should be automatically activated when new ratings are available
	 * @param autoMigrateExtensions Specifies whether existing should be automatically applied to ratings using this specification when new ratings are loaded  
	 * @param indRoundingSpecs The USGS-style rounding specifications for each independent parameter
	 * @param depRoundingSpec The USGS-style rounding specifications for the dependent parameter
	 * @param description The description of this rating specification
	 * @throws RatingException
	 * @throws RoundingException
	 */
	public RatingSet(
			String officeId,
			String ratingSpecId,
			String sourceAgencyId,
			String inRangeMethod,
			String outRangeLowMethod,
			String outRangeHighMethod,
			boolean active,
			boolean autoUpdate,
			boolean autoActivate,
			boolean autoMigrateExtensions,
			String[] indRoundingSpecs,
			String depRoundingSpec,
			String description) throws RatingException, RoundingException {
		this(new RatingSpec(
				officeId,
				ratingSpecId,
				sourceAgencyId,
				inRangeMethod,
				outRangeLowMethod,
				outRangeHighMethod,
				active,
				autoUpdate,
				autoActivate,
				autoMigrateExtensions,
				indRoundingSpecs,
				depRoundingSpec,
				description));
	}
	/**
	 * Public Constructor from RatingSpecContainer
	 * @param rsc The RatingSpecContainer object to initialize from
	 * @throws RatingException
	 */
	public RatingSet(RatingSpecContainer rsc) throws RatingException {
		this(new RatingSpec(rsc));
	}
	/**
	 * Public constructor a CWMS database connection
	 * @param loadMethod The method used to load the object from the database. If null, the value of the property
	 *        "hec.data.cwmsRating.RatingSet.databaseLoadMethod" is used. If both the argument and property value
	 *        are null (or if an invalid value is specified) the Lazy method will be used.
	 *        <table border>
	 *          <tr>
	 *            <th>Value (case insensitive)</th>
	 *            <th>Interpretation</th>
	 *          </tr>
	 *          <tr>
	 *            <td>Eager</td>
	 *            <td>Ratings for all effective times are loaded initially</td>
	 *            <td>Lazy</td>
	 *            <td>No ratings are loaded initially - each rating is only loaded when it is needed</td>
	 *            <td>Reference</td>
	 *            <td>No ratings are loaded ever - values are passed to database to be rated</td>
	 *          </tr>
	 *        </table>
	 * @param conn The connection to a CWMS database
	 * @param officeId The identifier of the office owning the rating. If null, the office associated with the connect user is used. 
	 * @param ratingSpecId The rating specification identifier
	 * @param startTime The earliest time to retrieve, as interpreted by inEffectTimes, in milliseconds.  If null, no earliest limit is set.
	 * @param endTime The latest time to retrieve, as interpreted by inEffectTimes, in milliseconds.  If null, no latest limit is set.
	 * @param dataTimes Determines how startTime and endTime are interpreted.
	 *        <table border>
	 *          <tr>
	 *            <th>Value</th>
	 *            <th>Interpretation</th>
	 *          </tr>
	 *          <tr>
	 *            <td>false</td>
	 *            <td>The time window specifies the extent of when the ratings became effective</td>
	 *            <td>true</td>
	 *            <td>Time time window specifies the time extent of data rate</td>
	 *          </tr>
	 *        </table>
	 * @return The new RatingSet object
	 * @throws RatingException
	 */
	public RatingSet(
			DatabaseLoadMethod loadMethod,
			Connection conn, 
			String officeId, 
			String ratingSpecId, 
			Long startTime, 
			Long endTime,
			boolean dataTimes)
			throws RatingException {
		
		setData(
			loadMethod,
			conn, 
			officeId, 
			ratingSpecId, 
			startTime, 
			endTime,
			dataTimes);
		
		observationTarget = new Observable();
		validate();
		
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
	 * Public Constructor from an uncompressed XML instance
	 * @param xmlStr The XML instance to initialize from
	 * @throws RatingException
	 */
	public RatingSet(String xmlStr) throws RatingException {
		this(xmlStr, false);
	}
	/**
	 * Public Constructor from an XML instance
	 * @param xmlStr The XML instance to initialize from
	 * @param isCompressed Flag specifying whether the string is a compressed XML string
	 * @throws RatingException
	 */
	public RatingSet(String xmlStr, boolean isCompressed) throws RatingException {
		try {
			setData(new RatingSetContainer(isCompressed ? TextUtil.uncompress(xmlStr, "base64") : xmlStr).clone()); // clone might return a ReferenceRatingContainer 
		} catch (Exception e) {
			if (e instanceof RatingException) throw (RatingException)e;
			throw new RatingException(e);
		}
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
	 * Retrieves a map of all included rating specifications, keyed by their rating spec ids
	 * @param specMap a non-null map to add to
	 * @param ratings the ratings to evaluate
	 * @throws RatingException if the same spec id maps to two non-equal rating specs 
	 */
	protected static void getAllRatingSpecs(HashMap<String, Object[]> specMap, Iterable<AbstractRating> ratings, String path) 
			throws RatingException {
		if (specMap == null) {
			throw new RatingException("Cannot use a null specMap parameter");
		}
		for (AbstractRating r : ratings) {
			String thisPath = path + "/" + r.getRatingSpecId(); 
			if (r instanceof VirtualRating) {
				VirtualRating vr = (VirtualRating)r;
				for (SourceRating sr : vr.getSourceRatings()) {
					if (sr.ratings != null) {
						getAllRatingSpecs(specMap, Arrays.asList(sr.ratings.getRatings()), thisPath);
					}
				}
			}
			else if (r instanceof TransitionalRating) {
				TransitionalRating tr = (TransitionalRating)r;
				for (SourceRating sr : tr.getSourceRatings()) {
					if (sr.ratings != null) {
						getAllRatingSpecs(specMap, Arrays.asList(sr.ratings.getRatings()), thisPath);
					}
				}
			}
			RatingSpec spec = r.ratingSpec;
			if (spec != null) {
				String specId = spec.getRatingSpecId();
				if (specMap.containsKey(specId)) {
					if (!spec.equals(specMap.get(specId)[1])) {
						throw new RatingException("Ratings contain multiple definitions of rating spec \""+specId+"\"");
					}
				}
				else {
					specMap.put(specId, new Object[] {thisPath, spec});
				}
			}
		}
	}
	/**
	 * Adds a single rating to the existing ratings.
	 * @param rating The rating to add
	 * @throws RatingException
	 */
	@Override
	public void addRating(AbstractRating rating) throws RatingException {
		addRatings(Arrays.asList(new AbstractRating[] {rating}));
	}
	/**
	 * Adds multiple ratings to the existing ratings.
	 * @param ratings The ratings to add
	 * @throws RatingException
	 */
	@Override
	public void addRatings(AbstractRating[] ratings) throws RatingException {
		addRatings(Arrays.asList(ratings));
	}
	/**
	 * Adds multiple ratings to the existing ratings.
	 * @param ratings The ratings to add
	 * @throws RatingException
	 */
	public void addRatings(Iterable<AbstractRating> ratings) throws RatingException {
		synchronized(this) {
			String ratingSpecId = null;
			String unitsId = null;
			if (dbrating != null) {
				throw new RatingException("Cannot add to a reference rating");
			}
			for (AbstractRating rating : ratings) {
				if (rating.getEffectiveDate() == Const.UNDEFINED_TIME) {
					throw new RatingException("Cannot add rating with undefined effective date.");
				}
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
				else if (!AbstractRating.compatibleUnits(unitsId, rating.getRatingUnitsId())) {
					throw new RatingException(String.format(
							"Rating units of \"%s\" aren't compatible with rating units of \"%s\"",
							rating.getRatingUnitsId(),
							unitsId));
				}
			}
			HashMap<String, Object[]> newSpecs = new HashMap<String, Object[]>();
			RatingSet.getAllRatingSpecs(newSpecs, ratings, "");
			if (this.ratings.size() > 0) {
				if (!this.ratings.firstEntry().getValue().getRatingSpecId().equals(ratingSpecId)) {
					throw new RatingException("Cannot add ratings with different rating specification IDs");
				}
				HashMap<String, Object[]> oldSpecs = new HashMap<String, Object[]>();
				RatingSet.getAllRatingSpecs(oldSpecs, this.ratings.values(), "");
				for (String specId : oldSpecs.keySet()) {
					if (newSpecs.containsKey(specId) && !newSpecs.get(specId)[1].equals(oldSpecs.get(specId)[1])) {
						StringBuilder sb = new StringBuilder("Cannot add ratings with different rating template or specification definitions.\n");
						sb.append("Existing :")
						  .append((String)oldSpecs.get(specId)[0])
						  .append("\n")
						  .append("Incoming :")
						  .append((String)newSpecs.get(specId)[0])
						  .append("\n");
						String oldXml = ((RatingSpec)oldSpecs.get(specId)[1]).toTemplateXml("  ", 3);
						String newXml = ((RatingSpec)newSpecs.get(specId)[1]).toTemplateXml("  ", 3);
						if (!newXml.equals(oldXml)) {
							sb.append("Definitions for template \"")
							  .append(((RatingSpec)oldSpecs.get(specId)[1]).getTemplateId())
							  .append("\" differ.\nExisting Definition :\n")
							  .append(oldXml)
							  .append("New Definition :\n")
							  .append(newXml);
						}
						oldXml = ((RatingSpec)oldSpecs.get(specId)[1]).toSpecXml("  ", 3);
						newXml = ((RatingSpec)newSpecs.get(specId)[1]).toSpecXml("  ", 3);
						if (!newXml.equals(oldXml)) {
							sb.append("Definitions for specification \"")
							  .append(((RatingSpec)oldSpecs.get(specId)[1]).getRatingSpecId())
							  .append("\"differ.\nExisting Definition :\n")
							  .append(oldXml)
							  .append("New Definition :\n")
							  .append(newXml);
						}
						throw new RatingException(sb.toString());
					}
				}
				if (!AbstractRating.compatibleUnits(unitsId, this.ratings.firstEntry().getValue().getRatingUnitsId())) {
					throw new RatingException(String.format(
							"Rating units of \"%s\" aren't compatible with rating units of \"%s\"",
							unitsId,
							this.ratings.firstEntry().getValue().getRatingUnitsId()));
				}
			}
			for (AbstractRating rating : ratings) {
				if (rating instanceof UsgsStreamTableRating) {
					UsgsStreamTableRating streamRating = (UsgsStreamTableRating)rating;
					if (streamRating.shifts != null) {
						streamRating.shifts.ratingSpec.inRangeMethod = this.ratingSpec.inRangeMethod;
					}
				}
				if (rating instanceof VirtualRating) {
					VirtualRating vr = (VirtualRating)rating;
					if (vr.isNormalized()) {
						this.ratings.put(rating.getEffectiveDate(), vr);
					}
					else {
						this.ratings.put(rating.getEffectiveDate(), vr.normalizedCopy());
					}
				}
				else {
					this.ratings.put(rating.getEffectiveDate(), rating);
				}
				this.ratings.get(rating.getEffectiveDate()).ratingSpec = ratingSpec;
				if (rating.isActive() && rating.createDate <= ratingTime) {
					activeRatings.put(rating.getEffectiveDate(), rating);
					activeRatings.get(rating.getEffectiveDate()).ratingSpec = ratingSpec;
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
	}
	/**
	 * Removes a single rating from the existing ratings.
	 * @param effectiveDate The effective date of the rating to remove, in Java milliseconds
	 * @throws RatingException
	 */
	public void removeRating(long effectiveDate) throws RatingException {
		synchronized(this) {
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
	}
	/**
	 * Removes all existing ratings.
	 */
	public void removeAllRatings() {
		synchronized(this) {
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
	}
	/**
	 * Replaces a single rating in the existing ratings
	 * @param rating The rating to replace an existing one
	 * @throws RatingException
	 */
	public void replaceRating(AbstractRating rating) throws RatingException {
		synchronized(this) {
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
	}
	/**
	 * Replaces multiple ratings in the existing ratings.
	 * @param ratings The ratings to replace existing ones
	 * @throws RatingException
	 */
	public void replaceRatings(AbstractRating[] ratings) throws RatingException {
		synchronized(this) {
			replaceRatings(Arrays.asList(ratings));
		}
	}
	/**
	 * Replaces multiple ratings in the existing ratings.
	 * @param ratings The ratings to replace existing ones
	 * @throws RatingException
	 */
	public void replaceRatings(Iterable<AbstractRating> ratings) throws RatingException {
		synchronized(this) {
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
	}
	/**
	 * Loads all rating values from table ratings that haven't already been loaded.
	 * @throws RatingException
	 */
	public void getConcreteRatings(Connection conn) throws RatingException {
		setDatabaseConnection(conn);
		for (Map.Entry<Long, AbstractRating> entry : activeRatings.entrySet()) {
			getConcreteRating(entry);
		}
		clearDatabaseConnection();
	}
	/**
	 * Loads all rating values from table ratings that haven't already been loaded.
	 * @deprecated
	 * @throws RatingException
	 */
	public void getConcreteRatings() throws RatingException {
		for (Map.Entry<Long, AbstractRating> entry : activeRatings.entrySet()) {
			getConcreteRating(entry);
		}
	}
	protected Map.Entry<Long, AbstractRating> getConcreteRating(Entry<Long, AbstractRating> ratingEntry) throws RatingException {
		synchronized(this) {
			ConnectionInfo ci = null;
			Map.Entry<Long, AbstractRating> newEntry = ratingEntry;
			try {
				if (ratingEntry != null) {
					Long key = ratingEntry.getKey();
					AbstractRating rating = ratingEntry.getValue();
					if (rating instanceof TableRating && ((TableRating)rating).values == null) {
						//----------------------------------------//
						// rating not yet retrieved from database //
						//----------------------------------------//
						if (ci == null) {
							ci = getConnectionInfo();
						}
						conn = ci.getConnection();
						if (logger.isLoggable(Level.INFO)) {
							SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
							sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
							logger.info(String.format(
									"Retrieving rating from %s: %s @ %s UTC", 
									conn.getMetaData().getURL(),
									getName(),
									sdf.format(key)));
						}
						synchronized(conn) {
							AbstractRating newRating = null;
							String sql = "begin cwms_rating.retrieve_ratings_xml(:1, :2, cwms_util.to_timestamp(:3), cwms_util.to_timestamp(:4),'UTC', :5); end;";
							CallableStatement stmt = conn.prepareCall(sql);
							stmt.registerOutParameter(1, Types.CLOB);
							stmt.setString(2, rating.ratingSpecId);
							stmt.setLong(3, rating.effectiveDate);
							stmt.setLong(4, rating.effectiveDate);
							stmt.setString(5, rating.officeId);
							stmt.execute();
							Clob clob = stmt.getClob(1);
							stmt.close();
							String xmlText = null;
							try {
								if (clob.length() > Integer.MAX_VALUE) {
									throw new RatingException("CLOB too long.");
								}
								xmlText = clob.getSubString(1, (int)clob.length());
							}
							catch (Exception e) {
								throw e;
							}
							finally {
								freeClob(clob);
							}
							logger.log(Level.FINE,"Retrieve XML:\n"+xmlText);
							if (xmlText.indexOf("<simple-rating ") > 0) {
								if (xmlText.indexOf("<formula>") > 0) {
									newRating = ExpressionRating.fromXml(xmlText);
								}
								else {
									newRating = TableRating.fromXml(xmlText);
									((TableRating)newRating).setBehaviors(ratingSpec);
								}
							}
							else if (xmlText.indexOf("<usgs-stream-rating ") > 0) {
								newRating = UsgsStreamTableRating.fromXml(xmlText);
								((UsgsStreamTableRating)newRating).setBehaviors(ratingSpec);
							}
							else if (xmlText.indexOf("<virtual-rating ") > 0) {
								newRating = VirtualRating.fromXml(xmlText);
							}
							else if (xmlText.indexOf("<transitional-rating ") > 0) {
								newRating = TransitionalRating.fromXml(xmlText);
							}
							else throw new RatingException("Unexpected rating type: \n" + xmlText);
							newRating.ratingSpec = ratingSpec;
							if (newRating.active) {
								activeRatings.put(key, newRating);
								newEntry = activeRatings.floorEntry(key);
								if (!newEntry.getKey().equals(key)) {
									throw new RatingException("Could not retrieve concrete rating from database.");
								}
							}
							else {
								ratings.get(key).setActive(false);
							}
							refreshRatings();
						}
						if (observationTarget != null) {
							observationTarget.setChanged();
							observationTarget.notifyObservers();
						}
					}
				}
			}
			catch (Exception e) {
				if (e instanceof RatingException) throw (RatingException)e;
				throw new RatingException(e);
			}
			finally {
				if (ci != null && ci.wasRetrieved()) {
					releaseConnection(ci);
					conn = null;
				}
			}
			return newEntry;
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
	 * @param valueTimes The times associated with the values, in Java milliseconds
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
		double[] Y = new double[valueSets.length];
		synchronized(this) {
			if (dbrating == null) {
				//-----------------//
				// concrete rating //
				//-----------------//
				int activeRatingCount = activeRatings.size();
				if (activeRatingCount == 0) {
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
				if (getDataUnits() == null) {
					Map.Entry<Long, AbstractRating> entry1 = ratings.firstEntry();
					Map.Entry<Long, AbstractRating> entry2 = ratings.higherEntry(entry1.getKey());
					while (entry2 != null) {
						if (!(entry1.getValue().getRatingUnitsId().equalsIgnoreCase(entry2.getValue().getRatingUnitsId()))) {
							throw new RatingException("Data units must be specified when rating set has multiple rating units.");
						}
						entry1 = entry2;
						entry2 = ratings.higherEntry(entry1.getKey());
					}
				}
				
				Map.Entry<Long, AbstractRating> lowerRating = null;
				Map.Entry<Long, AbstractRating> upperRating = null;
				IRating lastUsedRating = null;
				RatingMethod method = null;
				for (int i = 0; i < valueSets.length; ++i) {
					if (i > 0 && valueTimes[i] == valueTimes[i-1] && lastUsedRating != null) {
						Y[i] = lastUsedRating.rateOne(valueTimes[i], valueSets[i]);
						continue;
					}
					else {
						lowerRating = activeRatings.floorEntry(valueTimes[i]);
						upperRating = activeRatings.ceilingEntry(valueTimes[i]);
						//-------------------------//
						// handle out of range low //
						//-------------------------//
						if (lowerRating == null) {
							method = ratingSpec.getOutRangeLowMethod();
							switch(method) {
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
								if (isLazy) {
									getConcreteRating(activeRatings.firstEntry());
								}
								lastUsedRating = activeRatings.firstEntry().getValue(); 
								Y[i] = lastUsedRating.rateOne(valueTimes[i], valueSets[i]);
								continue;
							default:
								break;
							}
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
							method = ratingSpec.getOutRangeHighMethod();
							switch(method) {
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
								if (isLazy) {
									getConcreteRating(activeRatings.lastEntry());
								}
								lastUsedRating = activeRatings.lastEntry().getValue();
								Y[i] = lastUsedRating.rateOne(valueTimes[i], valueSets[i]);
								continue;
							default:
								break;
							}
							if (activeRatings.size() == 1) {
								switch (method) {
								case LINEAR :
									//-----------------------------------------------------------------//
									// allow LINEAR out of range high method with single active rating //
									//-----------------------------------------------------------------//
									if (isLazy) {
										getConcreteRating(activeRatings.lastEntry());
									}
									lastUsedRating = activeRatings.lastEntry().getValue();
									Y[i] = lastUsedRating.rateOne(valueTimes[i], valueSets[i]);
									continue;
								default :
									throw new RatingException(String.format("Cannot use rating method %s with only one active rating.", method));
								}
							}
							upperRating = activeRatings.lastEntry();
							lowerRating = activeRatings.lowerEntry(upperRating.getKey());
						}
						//-----------------------------------//
						// handle in-range and extrapolation //
						//-----------------------------------//
						if (lowerRating.getKey() == valueTimes[i]) {
							if (isLazy) {
								lowerRating = getConcreteRating(lowerRating);
							}
							Y[i] = lowerRating.getValue().rateOne(valueTimes[i], valueSets[i]);
							continue;
						}
						if (upperRating.getKey() == valueTimes[i]) {
							if (isLazy) {
								upperRating = getConcreteRating(upperRating);
							}
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
							if (isLazy) {
								lowerRating = getConcreteRating(lowerRating);
							}
							lastUsedRating = lowerRating.getValue();
							Y[i] = lastUsedRating.rateOne(valueTimes[i], valueSets[i]);
							continue;
						case NEXT:
						case HIGHER:
							if (isLazy) {
								upperRating = getConcreteRating(upperRating);
							}
							lastUsedRating = upperRating.getValue();
							Y[i] = lastUsedRating.rateOne(valueTimes[i], valueSets[i]);
							continue;
						case CLOSEST:
							if (valueTimes[i] - lowerRating.getKey() < upperRating.getKey() - valueTimes[i]) {
								if (isLazy) {
									lowerRating = getConcreteRating(lowerRating);
								}
								lastUsedRating = lowerRating.getValue();
							}
							else {
								if (isLazy) {
									upperRating = getConcreteRating(upperRating);
								}
								lastUsedRating = upperRating.getValue();
							}
							Y[i] = lastUsedRating.rateOne(valueTimes[i], valueSets[i]);
							continue;
						default:
							break;
						}
						//------------------------------------//
						// handle interpolation/extrapolation //
						//------------------------------------//
						method = ratingSpec.getInRangeMethod();
					}
					lastUsedRating = null;
					if (isLazy) {
						lowerRating = getConcreteRating(lowerRating);
						upperRating = getConcreteRating(upperRating);
					}
					long transitionStartMillis = upperRating.getValue().getTransitionStartDate();
					long t  = valueTimes[i];
					long t1 = lowerRating.getKey();
					long t2 = upperRating.getKey();
					double Y1 = lowerRating.getValue().rateOne(valueTimes[i], valueSets[i]);
					double Y2 = upperRating.getValue().rateOne(valueTimes[i], valueSets[i]);
					if (Y1 == Const.UNDEFINED_DOUBLE || Y2 == Const.UNDEFINED_DOUBLE) {
						Y[i] = Const.UNDEFINED_DOUBLE;
					}
					else {
						double y1 = Y1;
						double y2 = Y2;
						if (lowerRating.getValue() instanceof UsgsStreamTableRating) {
							t1 = ((UsgsStreamTableRating)lowerRating.getValue()).getLatestEffectiveDate(t2);
						}
						if (transitionStartMillis > t1 && transitionStartMillis < t2) {
							t1 = transitionStartMillis;
						}
						Y[i] = y1;
						if (t > t1) {
							Y[i] += (((double)t - t1) / (t2 - t1)) * (y2 - y1);
						}
					}
				}
			}
			else {
				//------------------//
				// reference rating //
				//------------------//
				Y = dbrating.rate(valueTimes, valueSets);
			}
			return Y;
		}
	}
	/**
	 * Rates the values in a TimeSeriesContainer and returns the results in a new TimeSeriesContainer. 
	 * The rating must be for a single independent parameter. 
	 * @param tsc The TimeSeriesContainer to rate
	 * @return A TimeSeriesContainer of the rated values. The rated unit is the native unit of dependent parameter of the rating.
	 * @throws RatingException
	 */
	@Override
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
		synchronized(this) {
			if (ratingSpec.getIndParamCount() != 1) {
				throw new RatingException(String.format("Cannot rate a TimeSeriesContainer with a rating that has %d independent parameters", ratingSpec.getIndParamCount()));
			}
			if (dbrating == null && ratings.size() == 0) throw new RatingException("No ratings.");
			String[] units = null;
			if (dbrating == null) {
				units = ratings.firstEntry().getValue().getRatingUnits();
			}
			else {
				units = dbrating.getRatingUnits();
			}
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
	@Override
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
		synchronized(this) {
			if (ratingSpec.getIndParamCount() != tscs.length) {
				throw new RatingException(String.format("Cannot rate a set of %d TimeSeriesContainers with a rating that has %d independent parameters", tscs.length, ratingSpec.getIndParamCount()));
			}
			String[] units = null;
			String[] params = ratingSpec.getIndParameters();
			if (dbrating == null) {
				if (ratings.size() == 0) throw new RatingException("No ratings.");
				units = ratings.firstEntry().getValue().getRatingUnits();
			}
			else {
				units = dbrating.getRatingUnits();
			}
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
						String[] parts = TextUtil.split(params[i], "-", 2);
						if (!tscParam[i].getBaseParameter().equals(parts[0]) || 
								(parts.length == 2 && !tscParam[i].getSubParameter().equals(parts[1]))) {
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
				IndependentValuesContainer ivc = RatingUtil.tscsToIvc(tscs, units, tz, allowUnsafe, warnUnsafe);
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
				String paramStr = ratingSpec.getDepParameter();
				if (ratedTsc.fullName.startsWith("/")) {
					paramStr = paramStr.toUpperCase();
				}
				if (tscs[0].subParameter == null || tscs[0].subParameter.length() == 0) {
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
	 * Retrieves the rating specification including all meta data.
	 * @return The rating specification
	 */
	public RatingSpec getRatingSpec() {
		synchronized(this) {
			return ratingSpec;
		}
	}
		
		
	/**
	 * Returns the unique identifying parts for the rating specification.
	 * 
	 * @return
	 * @throws DataSetException
	 */
	public IRatingSpecification getRatingSpecification() throws DataSetException
	{
		synchronized(this) {
			IRatingSpecification ratingSpecification = getRatingSpec().getRatingSpecification();
			return ratingSpecification;
		}
	}
	
	/**
	 * Returns the unique identifying parts for the rating template.
	 * @return
	 * @throws DataSetException
	 */
	public IRatingTemplate getRatingTemplate() throws DataSetException
	{
		synchronized(this) {
			IRatingTemplate ratingTemplate = getRatingSpec().getRatingTemplate();
			return ratingTemplate;
		}
	}
	
	/**
	 * Sets the rating specification.
	 * @param ratingSpec The rating specification
	 * @throws RatingException
	 */
	public void setRatingSpec(RatingSpec ratingSpec) throws RatingException {
		synchronized(this) {
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
				default:
					break;
				}
				switch (ratingSpec.getOutRangeLowMethod()) {
				case LOGARITHMIC :
				case LOG_LIN:
				case LIN_LOG:
				case PREVIOUS:
					throw new RatingException("Invalid out-of-range low rating method for rating times: "+ratingSpec.getOutRangeLowMethod());
				default:
					break;
				}
				switch (ratingSpec.getOutRangeHighMethod()) {
				case LOGARITHMIC :
				case LOG_LIN:
				case LIN_LOG:
				case NEXT:
					throw new RatingException("Invalid out-of-range high rating method for rating times: "+ratingSpec.getOutRangeHighMethod());
				default:
					break;
				}
			}
			this.ratingSpec = ratingSpec;
			for (Long effectiveDate : ratings.keySet()) {
				ratings.get(effectiveDate).ratingSpec = this.ratingSpec;
			}
			for (Long effectiveDate : activeRatings.keySet()) {
				activeRatings.get(effectiveDate).ratingSpec = this.ratingSpec;
			}
		}
	}
	/**
	 * Retrieves the times series of ratings.
	 * @return The times series of ratings.
	 */
	@Override
	public AbstractRating[] getRatings() {
		synchronized(this) {
			return ratings.values().toArray(new AbstractRating[ratings.size()]);
		}
	}
	
	@Override
	public TreeMap<Long, AbstractRating> getRatingsMap()
	{
		synchronized(this) {
			return ratings;
		}
	}
	
	@Override
	public AbstractRating getRating(Long effectiveDate)
	{
		synchronized(this) {
			AbstractRating retval = null;
			if(ratings != null)
			{
				retval = ratings.get(effectiveDate);
			}
			return retval;
		}
	}
	
	public AbstractRating getFloorRating(Long effectiveDate)
	{
		synchronized(this) {
			AbstractRating retval = null;
			if(ratings != null && effectiveDate != null)
			{
				Entry<Long, AbstractRating> floorEntry = ratings.floorEntry(effectiveDate);
				if (floorEntry != null)
				{
					retval = floorEntry.getValue();
				}
			}
			return retval;
		}
	}
	
//	public AbstractRating getTemporalInterpolatedRating(Long effectiveDate)
//	{
//		
//	}
	
	/**
	 * Sets the times series of ratings, replacing any existing ratings.
	 * @param ratings The time series of ratings
	 * @throws RatingException 
	 */
	public void setRatings(AbstractRating[] ratings) throws RatingException {
		synchronized(this) {
			removeAllRatings();
			addRatings(ratings);
		}
	}
	/**
	 * Retrieves the number of ratings in this set.
	 * @return The number of ratings in this set
	 */
	public int getRatingCount() {
		synchronized(this) {
			return ratings == null ? 0 : ratings.size();
		}
	}
	/**
	 * Retrieves the number of active ratings in this set.
	 * @return The number of active ratings in this set
	 */
	public int getActiveRatingCount() {
		synchronized(this) {
			int count =  0;
			if (ratings != null) {
				count = ratings.size();
				for (Iterator<AbstractRating> it = ratings.values().iterator(); it.hasNext();) {
					if (!it.next().active) --count;			
				}
			}
			return count;
		}
	}
	/**
	 * Retrieves the default value time. This is used for rating values that have no inherent times.
	 * @return The default value time
	 */
	public long getDefaultValuetime() {
		synchronized(this) {
			return defaultValueTime;
		}
	}
	/**
	 * Resets the default value time. This is used for rating values that have no inherent times.
	 */
	@Override
	public void resetDefaultValuetime() {
		synchronized(this) {
			this.defaultValueTime = Const.UNDEFINED_TIME;
		}
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#getRatingTime()
	 */
	@Override
	public long getRatingTime() {
		synchronized(this) {
			return ratingTime;
		}
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#setRatingTime(long)
	 */
	@Override
	public void setRatingTime(long ratingTime) {
		synchronized(this) {
			this.ratingTime = ratingTime;
			if (ratings != null) {
				for (AbstractRating rating : ratings.values()) {
					rating.setRatingTime(ratingTime);
				}
			}
			refreshRatings();
		}
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#resetRatingtime()
	 */
	@Override
	public void resetRatingTime() {
		synchronized(this) {
			ratingTime = Long.MAX_VALUE;
			if (ratings != null) {
				Long[] effectiveDates = ratings.keySet().toArray(new Long[0]);
				for (Long effectiveDate : effectiveDates) {
					ratings.get(effectiveDate).resetRatingTime();
				}
			}
			refreshRatings();
		}
	}
	/**
	 * Retrieves whether this object allows "risky" operations such as working with mismatched units, unknown parameters, etc.
	 * @return A flag specifying whether this object allows "risky" operations such as working with mismatched units, unknown parameters, etc.
	 */
	public boolean doesAllowUnsafe() {
		synchronized(this) {
			return allowUnsafe;
		}
	}
	/**
	 * Sets whether this object allows "risky" operations such as working with mismatched units, unknown parameters, etc.
	 * @param allowUnsafe A flag specifying whether this object allows "risky" operations such as working with mismatched units, unknown parameters, etc.
	 */
	public void setAllowUnsafe(boolean allowUnsafe) {
		synchronized(this) {
			this.allowUnsafe = allowUnsafe;
		}
	}
	/**
	 * Retrieves whether this object outputs warning messages about "risky" operations such as working with mismatched units, unknown parameters, etc.
	 * @return A flag specifying whether this object outputs warning messages about "risky" operations such as working with mismatched units, unknown parameters, etc.
	 */
	public boolean doesWarnUnsafe() {
		synchronized(this) {
			return warnUnsafe;
		}
	}
	/**
	 * Sets whether this object outputs warning messages about "risky" operations such as working with mismatched units, unknown parameters, etc.
	 * @param warnUnsafe  A flag specifying whether this object outputs warning messages about "risky" operations such as working with mismatched units, unknown parameters, etc.
	 */
	public void setWarnUnsafe(boolean warnUnsafe) {
		synchronized(this) {
			this.warnUnsafe = warnUnsafe;
		}
	}
	/**
	 * Retrieves the standard HEC-DSS pathname for this rating set
	 * @return The standard HEC-DSS pathname for this rating set
	 */
	public String getDssPathname() {
		synchronized(this) {
			return ratingSpec == null ? null : ratingSpec.getDssPathname();
		}
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#getName()
	 */
	@Override
	public String getName() {
		synchronized(this) {
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
	}
	/* (non-Javadoc)
	 * @see hec.data.IRating#setName(java.lang.String)
	 */
	@Override
	public void setName(String name) throws RatingException {
		synchronized(this) {
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
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#getRatingParameters()
	 */
	@Override
	public String[] getRatingParameters() {
		synchronized(this) {
			return getRatingSpec().getParameters();
		}
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#getRatingUnits()
	 */
	@Override
	public String[] getRatingUnits() {
		synchronized(this) {
			String[] units = null;
			if (dbrating == null) {
				if (ratings.size() > 0) {
					units = ratings.firstEntry().getValue().getRatingUnits();
				}
			}
			else {
				units = dbrating.getRatingUnits();
			}
			return units;
		}
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#getDataUnits()
	 */
	@Override
	public String[] getDataUnits() {
		synchronized(this) {
			String[] units = null;
			if (dataUnits != null) {
				units = Arrays.copyOf(dataUnits, dataUnits.length);
			}
			else {
				if (dbrating == null) {
					if (ratings.size() > 0) {
						units = ratings.firstEntry().getValue().getDataUnits();
					}
				}
				else {
					units = dbrating.getDataUnits();
				}
			}
			return units;
		}
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#setDataUnits(java.lang.String[])
	 */
	@Override
	public void setDataUnits(String[] units) throws RatingException {
		synchronized(this) {
			if (dbrating == null) {
				for (ICwmsRating rating : ratings.values()) {
					rating.setDataUnits(units);
				}
			}
			else {
				dbrating.setDataUnits(units);
			}
			dataUnits = units == null ? null : Arrays.copyOf(units, units.length);
		}
	}
	/* (non-Javadoc)
	 * @see hec.data.IRating#getRatingExtents()
	 */
	@Override
	public double[][] getRatingExtents() throws RatingException {
		if (dbrating != null) {
			return dbrating.getRatingExtents();
		}
		return getRatingExtents(getRatingTime());
	}
	/**
	 * Retrieves the rating extents for a specified time
	 * @param ratingTime The time for which to retrieve the rating extents 
	 * @param conn The database connection to use if the rating was lazily loaded
	 * @return The rating extents
	 * @throws RatingException
	 * @see hec.data.IRating#getRatingExtents(long)
	 */
	public double[][] getRatingExtents(long ratingTime, Connection conn) throws RatingException {
		synchronized(this) {
			setDatabaseConnection(conn);
			try { 
				return getRatingExtents(ratingTime); 
			}
			finally { 
				clearDatabaseConnection(); 
			}
		}
	}
	/* (non-Javadoc)
	 * @see hec.data.IRating#getRatingExtents(long)
	 */
	@Override
	public double[][] getRatingExtents(long ratingTime) throws RatingException {
		synchronized(this) {
			if (dbrating == null) {
				if (activeRatings.size() == 0) {
					throw new RatingException("No active ratings.");
				}
				Entry<Long, AbstractRating> rating = activeRatings.floorEntry(ratingTime);
		        
		        if (rating == null)
		        {
		            rating = activeRatings.ceilingEntry(ratingTime);
		        }
		        if (isLazy) {
		        	rating = getConcreteRating(rating);
		        }
		        return rating.getValue().getRatingExtents();
			}
			else {
				return dbrating.getRatingExtents(ratingTime);
			}
		}
	}
	/* (non-Javadoc)
	 * @see hec.data.IRating#getEffectiveDates()
	 */
	@Override
	public long[] getEffectiveDates() {
		synchronized(this) {
			long[] effectiveDates = null;
			if (dbrating == null) {
				effectiveDates = new long[activeRatings.size()];
				Iterator<AbstractRating> it = activeRatings.values().iterator(); 
				for (int i = 0; i < effectiveDates.length; ++i) {
					effectiveDates[i] = it.next().effectiveDate;
				}
			}
			else {
				return dbrating.getEffectiveDates();
			}
			return effectiveDates;
		}
	}
	/* (non-Javadoc)
	 * @see hec.data.IRating#getCreateDates()
	 */
	@Override
	public long[] getCreateDates() {
		synchronized(this) {
			long[] createDates = null;
			if (dbrating == null) {
				createDates = new long[activeRatings.size()];
				Iterator<AbstractRating> it = activeRatings.values().iterator(); 
				for (int i = 0; i < createDates.length; ++i) {
					createDates[i] = it.next().createDate;
				}
			}
			else {
				createDates = dbrating.getCreateDates();
			}
			return createDates;
		}
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#getDefaultValueTime()
	 */
	@Override
	public long getDefaultValueTime() {
		synchronized(this) {
			return defaultValueTime;
		}
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#setDefaultValueTime(long)
	 */
	@Override
	public void setDefaultValueTime(long defaultValueTime) {
		synchronized(this) {
			this.defaultValueTime = defaultValueTime;
			for (ICwmsRating rating : ratings.values()) {
				if (rating != null) {
					rating.setDefaultValueTime(defaultValueTime);
				}
			}
		}
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#rate(double)
	 */
	@Override
	public double rate(double indVal) throws RatingException {
		if (defaultValueTime == Const.UNDEFINED_TIME) {
			throw new RatingException("Default value time is not set");
		}
		return rate(indVal, defaultValueTime);
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#rate(double[])
	 */
	@Override
	public double rateOne(double... indVals) throws RatingException {
		if (defaultValueTime == Const.UNDEFINED_TIME) {
			throw new RatingException("Default value time is not set");
		}
		return rateOne(indVals, defaultValueTime);
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#rate(double[])
	 */
	@Override
	public double rateOne2(double[] indVals) throws RatingException {
		if (defaultValueTime == Const.UNDEFINED_TIME) {
			throw new RatingException("Default value time is not set");
		}
		return rateOne(indVals, defaultValueTime);
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#rateOne(double[])
	 */
	@Override
	public double[] rate(double[] indVals) throws RatingException {
		if (defaultValueTime == Const.UNDEFINED_TIME) {
			throw new RatingException("Default value time is not set");
		}
		return rate(indVals, defaultValueTime);
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#rate(double[][])
	 */
	@Override
	public double[] rate(double[][] indVals) throws RatingException {
		if (defaultValueTime == Const.UNDEFINED_TIME) {
			throw new RatingException("Default value time is not set");
		}
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
	 * @see hec.data.cwmsRating.IRating#rate(long, double[])
	 */
	@Override
	public double rateOne2(long valTime, double... indVals) throws RatingException {
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
		if (defaultValueTime == Const.UNDEFINED_TIME) {
			throw new RatingException("Default value time is not set");
		}
		long[] valTimes = {defaultValueTime};
		double[] depVals = {depVal};
		return reverseRate(valTimes, depVals)[0];
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#reverseRate(double[])
	 */
	@Override
	public double[] reverseRate(double[] depVals) throws RatingException {
		if (defaultValueTime == Const.UNDEFINED_TIME) {
			throw new RatingException("Default value time is not set");
		}
		return reverseRate(defaultValueTime, depVals);
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
		long[] valTimes = new long[depVals.length];
		Arrays.fill(valTimes, valTime);
		return reverseRate(valTimes, depVals);
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#reverseRate(long[], double[])
	 */
	@Override
	public double[] reverseRate(long[] valTimes, double[] depVals) throws RatingException {
		synchronized(this) {
			double[] Y = new double[depVals.length];
			if (dbrating == null) {
				if (activeRatings.size() == 0) {
					throw new RatingException("No active ratings.");
				}
				if (getDataUnits() == null) {
					Map.Entry<Long, AbstractRating> entry1 = ratings.firstEntry();
					Map.Entry<Long, AbstractRating> entry2 = ratings.higherEntry(entry1.getKey());
					while (entry2 != null) {
						if (!(entry1.getValue().getRatingUnitsId().equalsIgnoreCase(entry2.getValue().getRatingUnitsId()))) {
							throw new RatingException("Data units must be specified when rating set has multiple rating units.");
						}
						entry1 = entry2;
						entry2 = ratings.higherEntry(entry1.getKey());
					}
				}
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
							method = ratingSpec.getOutRangeLowMethod();
							switch(method) {
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
								if (isLazy) {
									getConcreteRating(activeRatings.firstEntry());
								}
								lastUsedRating = activeRatings.firstEntry().getValue(); 
								Y[i] = lastUsedRating.reverseRate(valTimes[i], depVals[i]);
								continue;
							default:
								break;
							}
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
							method = ratingSpec.getOutRangeHighMethod();
							switch(method) {
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
								if (isLazy) {
									getConcreteRating(activeRatings.lastEntry());
								}
								lastUsedRating = activeRatings.lastEntry().getValue();
								Y[i] = lastUsedRating.reverseRate(valTimes[i], depVals[i]);
								continue;
							default:
								break;
							}
							if (activeRatings.size() == 1) {
								switch (method) {
								case LINEAR :
									//-----------------------------------------------------------------//
									// allow LINEAR out of range high method with single active rating //
									//-----------------------------------------------------------------//
									if (isLazy) {
										getConcreteRating(activeRatings.lastEntry());
									}
									lastUsedRating = activeRatings.lastEntry().getValue();
									Y[i] = lastUsedRating.reverseRate(valTimes[i], depVals[i]);
									continue;
								default :
									throw new RatingException(String.format("Cannot use rating method %s with only one active rating.", method));
								}
							}
							upperRating = activeRatings.lastEntry();
							lowerRating = activeRatings.lowerEntry(upperRating.getKey());
						}
						//-----------------------------------//
						// handle in-range and extrapolation //
						//-----------------------------------//
						if (lowerRating.getKey() == valTimes[i]) {
							if (isLazy) {
								lowerRating = getConcreteRating(lowerRating);
							}
							Y[i] = lowerRating.getValue().reverseRate(valTimes[i], depVals[i]);
							continue;
						}
						if (upperRating.getKey() == valTimes[i]) {
							if (isLazy) {
								upperRating = getConcreteRating(upperRating);
							}
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
							if (isLazy) {
								lowerRating = getConcreteRating(lowerRating);
							}
							lastUsedRating = lowerRating.getValue();
							Y[i] = lastUsedRating.reverseRate(valTimes[i], depVals[i]);
							continue;
						case NEXT:
						case HIGHER:
							if (isLazy) {
								upperRating = getConcreteRating(upperRating);
							}
							lastUsedRating = upperRating.getValue();
							Y[i] = lastUsedRating.reverseRate(valTimes[i], depVals[i]);
							continue;
						case CLOSEST:
							if (valTimes[i] - lowerRating.getKey() < upperRating.getKey() - valTimes[i]) {
								if (isLazy) {
									lowerRating = getConcreteRating(lowerRating);
								}
								lastUsedRating = lowerRating.getValue();
							}
							else {
								if (isLazy) {
									upperRating = getConcreteRating(upperRating);
								}
								lastUsedRating = upperRating.getValue();
							}
							Y[i] = lastUsedRating.reverseRate(valTimes[i], depVals[i]);
							continue;
						default:
							break;
						}
						//------------------------------------//
						// handle interpolation/extrapolation //
						//------------------------------------//
						method = ratingSpec.getInRangeMethod();
					}
					lastUsedRating = null;
					if (isLazy) {
						lowerRating = getConcreteRating(lowerRating);
						upperRating = getConcreteRating(upperRating);
					}
					boolean ind_log = method == RatingMethod.LOGARITHMIC || method == RatingMethod.LIN_LOG;
					boolean dep_log = method == RatingMethod.LOGARITHMIC || method == RatingMethod.LOG_LIN;
					double x  = valTimes[i];
					double x1 = lowerRating.getKey();
					double x2 = upperRating.getKey();
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
							x  = valTimes[i];
							x1 = lowerRating.getKey();
							x2 = upperRating.getKey();
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
							x  = valTimes[i];
							x1 = lowerRating.getKey();
							x2 = upperRating.getKey();
							y1 = Y1;
							y2 = Y2;
							dep_log = false;
						}
					}
					double y = y1 + ((x - x1) / (x2 - x1)) * (y2 - y1);
					if (dep_log) y = Math.pow(10, y);
					Y[i] = y;
				}
			}
			else {
				Y = dbrating.reverseRate(valTimes, depVals);
			}
			return Y;
		}
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#reverseRate(hec.io.TimeSeriesContainer)
	 */
	@Override
	public TimeSeriesContainer reverseRate(TimeSeriesContainer tsc)
			throws RatingException {
		synchronized(this) {
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
			IndependentValuesContainer ivc = RatingUtil.tscsToIvc(tscs, units, tz, allowUnsafe, warnUnsafe);
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
		synchronized(this) {
			return ratingSpec != null ? ratingSpec.getIndParamCount() : ratings.firstEntry().getValue().getIndParamCount();
		}
	}
	/**
	 * Retrieves the data ratingUnits. These are the ratingUnits expected for independent parameters and the unit produced 
	 * for the dependent parameter.  If the underlying rating uses different ratingUnits, the rating must perform unit 
	 * conversions.
	 * @param conn The database connection to use for lazy ratings and reference ratings
	 * @return The ratingUnits identifier, one unit for each parameter
	 */
	public String[] getDataUnits(Connection conn) {
		Connection oldConn = this.conn;
		setDatabaseConnection(conn);
		try {
			return getDataUnits();
		}
		finally {
			if (oldConn != null) {
				setDatabaseConnection(oldConn);
			}
			else {
				clearDatabaseConnection();
			}
		}
	}

	/**
	 * Sets the data ratingUnits. These are  the ratingUnits expected for independent parameters and the unit produced 
	 * for the dependent parameter.  If the underlying rating uses different ratingUnits, the rating must perform unit 
	 * conversions.
	 * @param conn The database connection to use for lazy ratings and reference ratings
	 * @param ratingUnits The ratingUnits, one unit for each parameter
	 */
	public void setDataUnits(Connection conn, String[] units) throws RatingException {
		Connection oldConn = this.conn;
		setDatabaseConnection(conn);
		try {
			setDataUnits(units);
		}
		finally {
			if (oldConn != null) {
				setDatabaseConnection(oldConn);
			}
			else {
				clearDatabaseConnection();
			}
		}
	}

	/**
	 * Retrieves the min and max value for each parameter of the rating, in the current ratingUnits.
	 * @param conn The database connection to use for lazy ratings and reference ratings
	 * @return The min and max values for each parameter. The outer (Connection conn, first) dimension will be 2, with the first containing
	 *         min values and the second containing max values. The inner (Connection conn, second) dimension will be the number of independent
	 *         parameters for the rating plus one. The first value will be the extent for the first independent parameter, and
	 *         the last value will be the extent for the dependent parameter.
	 */
	public double[][] getRatingExtents(Connection conn) throws RatingException {
		Connection oldConn = this.conn;
		setDatabaseConnection(conn);
		try {
			return getRatingExtents();
		}
		finally {
			if (oldConn != null) {
				setDatabaseConnection(oldConn);
			}
			else {
				clearDatabaseConnection();
			}
		}
	}
	
	/**
	 * Retrieves the min and max value for each parameter of the rating, in the current ratingUnits.
	 * @param conn The database connection to use for lazy ratings and reference ratings
	 * @param ratingTime The time to use in determining the rating extents
	 * @return The min and max values for each parameter. The outer (Connection conn, first) dimension will be 2, with the first containing
	 *         min values and the second containing max values. The inner (Connection conn, second) dimension will be the number of independent
	 *         parameters for the rating plus one. The first value will be the extent for the first independent parameter, and
	 *         the last value will be the extent for the dependent parameter.
	 */
	public double[][] getRatingExtents(Connection conn, long ratingTime) throws RatingException {
		Connection oldConn = this.conn;
		setDatabaseConnection(conn);
		try {
			return getRatingExtents(ratingTime);
		}
		finally {
			if (oldConn != null) {
				setDatabaseConnection(oldConn);
			}
			else {
				clearDatabaseConnection();
			}
		}
	}
	
	/**
	 * Retrieves the effective dates of the rating in milliseconds, one for each contained rating
	 * @param conn The database connection to use for lazy ratings and reference ratings
	 */
	public long[] getEffectiveDates(Connection conn) {
		Connection oldConn = this.conn;
		setDatabaseConnection(conn);
		try {
			return getEffectiveDates();
		}
		finally {
			if (oldConn != null) {
				setDatabaseConnection(oldConn);
			}
			else {
				clearDatabaseConnection();
			}
		}
	}
	
	/**
	 * Retrieves the creation dates of the rating in milliseconds, one for each contained rating
	 * @param conn The database connection to use for lazy ratings and reference ratings
	 */
	public long[] getCreateDates(Connection conn) {
		Connection oldConn = this.conn;
		setDatabaseConnection(conn);
		try {
			return getCreateDates();
		}
		finally {
			if (oldConn != null) {
				setDatabaseConnection(oldConn);
			}
			else {
				clearDatabaseConnection();
			}
		}
	}

	/**
	 * Finds the dependent value for a single independent value.  The rating must be for a single independent parameter.
	 * @param conn The database connection to use for lazy ratings and reference ratings
	 * @param indVal The independent value to rate.
	 * @return The dependent value
	 * @throws RatingException
	 */
	public double rate(Connection conn, double indVal) throws RatingException {
		Connection oldConn = this.conn;
		setDatabaseConnection(conn);
		try {
			return rate(indVal);
		}
		finally {
			if (oldConn != null) {
				setDatabaseConnection(oldConn);
			}
			else {
				clearDatabaseConnection();
			}
		}
	}

	/**
	 * Finds the dependent value for a set of independent values. The rating must be for as many independent parameters as there are arguments.
	 * @param conn The database connection to use for lazy ratings and reference ratings
	 * @param indVals The independent parameters to rate
	 * @return The dependent value
	 * @throws RatingException
	 */
	public double rateOne(Connection conn, double... indVals) throws RatingException {
		Connection oldConn = this.conn;
		setDatabaseConnection(conn);
		try {
			return rateOne(indVals);
		}
		finally {
			if (oldConn != null) {
				setDatabaseConnection(oldConn);
			}
			else {
				clearDatabaseConnection();
			}
		}
	}

	/**
	 * Finds the dependent value for a set of independent values. The rating must be for as many independent parameters as there are arguments.
	 * @param conn The database connection to use for lazy ratings and reference ratings
	 * @param indVals The independent parameters to rate
	 * @return The dependent value
	 * @throws RatingException
	 */
	public double rateOne2(Connection conn, double[] indVals) throws RatingException {
		Connection oldConn = this.conn;
		setDatabaseConnection(conn);
		try {
			return rateOne(indVals);
		}
		finally {
			if (oldConn != null) {
				setDatabaseConnection(oldConn);
			}
			else {
				clearDatabaseConnection();
			}
		}
	}

	/**
	 * Finds multiple dependent values for multiple single independent values.  The rating must be for a single independent parameter.
	 * @param conn The database connection to use for lazy ratings and reference ratings
	 * @param indVals The independent values to rate
	 * @return The dependent values
	 * @throws RatingException
	 */
	public double[] rate(Connection conn, double[] indVals) throws RatingException {
		Connection oldConn = this.conn;
		setDatabaseConnection(conn);
		try {
			return rate(indVals);
		}
		finally {
			if (oldConn != null) {
				setDatabaseConnection(oldConn);
			}
			else {
				clearDatabaseConnection();
			}
		}
	}

	/**
	 * Finds multiple dependent values for multiple sets of independent values.  The rating must be for as many independent
	 * parameters as the length of each independent parameter set.
	 * @param conn The database connection to use for lazy ratings and reference ratings
	 * @param indVals The independent values to rate. Each set of independent values must be the same length.
	 * @return The dependent values
	 * @throws RatingException
	 */
	public double[] rate(Connection conn, double[][] indVals) throws RatingException {
		Connection oldConn = this.conn;
		setDatabaseConnection(conn);
		try {
			return rate(indVals);
		}
		finally {
			if (oldConn != null) {
				setDatabaseConnection(oldConn);
			}
			else {
				clearDatabaseConnection();
			}
		}
	}

	/**
	 * Finds the dependent value for a single independent value at a specified time.  The rating must be for a single independent parameter.
	 * @param conn The database connection to use for lazy ratings and reference ratings
	 * @param valTime The time associated with the value to rate, in Java milliseconds
	 * @param indVal The independent value to rate
	 * @return The dependent value
	 * @throws RatingException
	 */
	public double rate(Connection conn, long valTime, double indVal) throws RatingException {
		Connection oldConn = this.conn;
		setDatabaseConnection(conn);
		try {
			return rate(valTime, indVal);
		}
		finally {
			if (oldConn != null) {
				setDatabaseConnection(oldConn);
			}
			else {
				clearDatabaseConnection();
			}
		}
	}

	/**
	 * Finds the dependent value for a set of independent values. The rating must be for as many independent parameters as there are arguments.
	 * @param conn The database connection to use for lazy ratings and reference ratings
	 * @param valTime The time associated with the set of value to rate, in Java milliseconds
	 * @param indVals The independent parameters to rate
	 * @return The dependent value
	 * @throws RatingException
	 */
	public double rateOne(Connection conn, long valTime, double... indVals) throws RatingException {
		Connection oldConn = this.conn;
		setDatabaseConnection(conn);
		try {
			return rateOne(valTime, indVals);
		}
		finally {
			if (oldConn != null) {
				setDatabaseConnection(oldConn);
			}
			else {
				clearDatabaseConnection();
			}
		}
	}

	/**
	 * Finds the dependent value for a set of independent values. The rating must be for as many independent parameters as there are arguments.
	 * @param conn The database connection to use for lazy ratings and reference ratings
	 * @param valTime The time associated with the set of value to rate, in Java milliseconds
	 * @param indVals The independent parameters to rate
	 * @return The dependent value
	 * @throws RatingException
	 */
	public double rateOne2(Connection conn, long valTime, double[] indVals) throws RatingException {
		Connection oldConn = this.conn;
		setDatabaseConnection(conn);
		try {
			return rateOne(valTime, indVals);
		}
		finally {
			if (oldConn != null) {
				setDatabaseConnection(oldConn);
			}
			else {
				clearDatabaseConnection();
			}
		}
	}

	/**
	 * Finds multiple dependent values for multiple single independent values at a specified time.  The rating must be for a single independent parameter.
	 * @param conn The database connection to use for lazy ratings and reference ratings
	 * @param valTime The time associated with the values to rate, in Java milliseconds
	 * @param indVals The independent values to rate
	 * @return The dependent values
	 * @throws RatingException
	 */
	public double[] rate(Connection conn, long valTime, double[] indVals) throws RatingException {
		Connection oldConn = this.conn;
		setDatabaseConnection(conn);
		try {
			return rate(valTime, indVals);
		}
		finally {
			if (oldConn != null) {
				setDatabaseConnection(oldConn);
			}
			else {
				clearDatabaseConnection();
			}
		}
	}

	/**
	 * Finds multiple dependent values for multiple single independent and times.  The rating must be for a single independent parameter.
	 * @param conn The database connection to use for lazy ratings and reference ratings
	 * @param valTimes The times associated with the values to rate, in Java milliseconds
	 * @param indVals The independent values to rate
	 * @return The dependent values
	 * @throws RatingException
	 */
	public double[] rate(Connection conn, long[] valTimes, double[] indVals) throws RatingException {
		Connection oldConn = this.conn;
		setDatabaseConnection(conn);
		try {
			return rate(valTimes, indVals);
		}
		finally {
			if (oldConn != null) {
				setDatabaseConnection(oldConn);
			}
			else {
				clearDatabaseConnection();
			}
		}
	}

	/**
	 * Finds multiple dependent values for multiple sets of independent values at a specified time.  The rating must be for as many independent
	 * parameters as the length of each independent parameter set.
	 * @param conn The database connection to use for lazy ratings and reference ratings
	 * @param valTime The time associated with the values to rate, in Java milliseconds
	 * @param indVals The independent values to rate. Each set of independent values must be the same length.
	 * @return The dependent values
	 * @throws RatingException
	 */
	public double[] rate(Connection conn, long valTime, double[][] indVals) throws RatingException {
		Connection oldConn = this.conn;
		setDatabaseConnection(conn);
		try {
			return rate(valTime, indVals);
		}
		finally {
			if (oldConn != null) {
				setDatabaseConnection(oldConn);
			}
			else {
				clearDatabaseConnection();
			}
		}
	}

	/**
	 * Finds multiple dependent values for multiple sets of independent values and times.  The rating must be for as many independent
	 * parameters as the length of each independent parameter set.
	 * @param conn The database connection to use for lazy ratings and reference ratings
	 * @param valTimes The time associated with the values to rate, in Java milliseconds
	 * @param indVals The independent values to rate. Each set of independent values must be the same length.
	 * @return The dependent values
	 * @throws RatingException
	 */
	public double[] rate(Connection conn, long[] valTimes, double[][] indVals) throws RatingException {
		Connection oldConn = this.conn;
		setDatabaseConnection(conn);
		try {
			return rate(valTimes, indVals);
		}
		finally {
			if (oldConn != null) {
				setDatabaseConnection(oldConn);
			}
			else {
				clearDatabaseConnection();
			}
		}
	}
	
	/**
	 * Rates the values in the specified TimeSeriesContainer to generate a resulting TimeSeriesContainer. The rating must be for a single independent parameter.
	 * @param conn The database connection to use for lazy ratings and reference ratings
	 * @param tsc The TimeSeriesContainer of independent values.
	 * @return The TimeSeriesContainer of dependent values.
	 * @throws RatingException
	 */
	public TimeSeriesContainer rate(Connection conn, TimeSeriesContainer tsc) throws RatingException {
		Connection oldConn = this.conn;
		setDatabaseConnection(conn);
		try {
			return rate(tsc);
		}
		finally {
			if (oldConn != null) {
				setDatabaseConnection(oldConn);
			}
			else {
				clearDatabaseConnection();
			}
		}
	}
	
	/**
	 * Rates the values in the specified TimeSeriesContainer objects to generate a resulting TimeSeriesContainer. The rating must be for as many independent parameters as the length of tscs.
	 * @param conn The database connection to use for lazy ratings and reference ratings
	 * @param tscs The TimeSeriesContainers of independent values, one for each independent parameter.
	 * @return The TimeSeriesContainer of dependent values.
	 * @throws RatingException
	 */
	public TimeSeriesContainer rate(Connection conn, TimeSeriesContainer[] tscs) throws RatingException {
		Connection oldConn = this.conn;
		setDatabaseConnection(conn);
		try {
			return rate(tscs);
		}
		finally {
			if (oldConn != null) {
				setDatabaseConnection(oldConn);
			}
			else {
				clearDatabaseConnection();
			}
		}
	}
	
	/**
	 * Rates the values in the specified TimeSeriesMath to generate a resulting TimeSeriesMath. The rating must be for a single independent parameter.
	 * @param conn The database connection to use for lazy ratings and reference ratings
	 * @param tsm The TimeSeriesMath of independent values.
	 * @return The TimeSeriesMath of dependent values.
	 * @throws RatingException
	 */
	public TimeSeriesMath rate(Connection conn, TimeSeriesMath tsm) throws RatingException {
		Connection oldConn = this.conn;
		setDatabaseConnection(conn);
		try {
			return rate(tsm);
		}
		finally {
			if (oldConn != null) {
				setDatabaseConnection(oldConn);
			}
			else {
				clearDatabaseConnection();
			}
		}
	}
	
	/**
	 * Rates the values in the specified TimeSeriesMath objects to generate a resulting TimeSeriesMath. The rating must be for as many independent parameters as the length of tscs.
	 * @param conn The database connection to use for lazy ratings and reference ratings
	 * @param tsms The TimeSeriesMaths of independent values, one for each independent parameter.
	 * @return The TimeSeriesMath of dependent values.
	 * @throws RatingException
	 */
	public TimeSeriesMath rate(Connection conn, TimeSeriesMath[] tsms) throws RatingException {
		Connection oldConn = this.conn;
		setDatabaseConnection(conn);
		try {
			return rate(tsms);
		}
		finally {
			if (oldConn != null) {
				setDatabaseConnection(oldConn);
			}
			else {
				clearDatabaseConnection();
			}
		}
	}

	/**
	 * Finds the independent value for a single independent value.  The rating must be for a single independent parameter.
	 * @param conn The database connection to use for lazy ratings and reference ratings
	 * @param depVal The dependent value to rate.
	 * @return The independent value
	 * @throws RatingException
	 */
	public double reverseRate(Connection conn, double depVal) throws RatingException {
		Connection oldConn = this.conn;
		setDatabaseConnection(conn);
		try {
			return reverseRate(depVal);
		}
		finally {
			if (oldConn != null) {
				setDatabaseConnection(oldConn);
			}
			else {
				clearDatabaseConnection();
			}
		}
	}

	/**
	 * Finds multiple independent values for multiple single independent values.  The rating must be for a single independent parameter.
	 * @param conn The database connection to use for lazy ratings and reference ratings
	 * @param depVals The dependent values to rate
	 * @return The independent values
	 * @throws RatingException
	 */
	public double[] reverseRate(Connection conn, double[] depVals) throws RatingException {
		Connection oldConn = this.conn;
		setDatabaseConnection(conn);
		try {
			return reverseRate(depVals);
		}
		finally {
			if (oldConn != null) {
				setDatabaseConnection(oldConn);
			}
			else {
				clearDatabaseConnection();
			}
		}
	}

	/**
	 * Finds the independent value for a single independent value at a specified time.  The rating must be for a single independent parameter.
	 * @param conn The database connection to use for lazy ratings and reference ratings
	 * @param valTime The time associated with the value to rate, in Java milliseconds
	 * @param depVal The dependent value to rate
	 * @return The independent value
	 * @throws RatingException
	 */
	public double reverseRate(Connection conn, long valTime, double depVal) throws RatingException {
		Connection oldConn = this.conn;
		setDatabaseConnection(conn);
		try {
			return reverseRate(valTime, depVal);
		}
		finally {
			if (oldConn != null) {
				setDatabaseConnection(oldConn);
			}
			else {
				clearDatabaseConnection();
			}
		}
	}

	/**
	 * Finds multiple independent values for multiple single independent values at a specified time.  The rating must be for a single independent parameter.
	 * @param conn The database connection to use for lazy ratings and reference ratings
	 * @param valTime The time associated with the values to rate, in Java milliseconds
	 * @param depVals The dependent values to rate
	 * @return The independent values
	 * @throws RatingException
	 */
	public double[] reverseRate(Connection conn, long valTime, double[] depVals) throws RatingException {
		Connection oldConn = this.conn;
		setDatabaseConnection(conn);
		try {
			return reverseRate(valTime, depVals);
		}
		finally {
			if (oldConn != null) {
				setDatabaseConnection(oldConn);
			}
			else {
				clearDatabaseConnection();
			}
		}
	}

	/**
	 * Finds multiple independent values for multiple single independent and times.  The rating must be for a single independent parameter.
	 * @param conn The database connection to use for lazy ratings and reference ratings
	 * @param valTimes The times associated with the values to rate, in Java milliseconds
	 * @param depVals The dependent values to rate
	 * @return The independent values
	 * @throws RatingException
	 */
	public double[] reverseRate(Connection conn, long[] valTimes, double[] depVals) throws RatingException {
		Connection oldConn = this.conn;
		setDatabaseConnection(conn);
		try {
			return reverseRate(valTimes, depVals);
		}
		finally {
			if (oldConn != null) {
				setDatabaseConnection(oldConn);
			}
			else {
				clearDatabaseConnection();
			}
		}
	}

	/**
	 * Rates the values in the specified TimeSeriesContainer to generate a resulting TimeSeriesContainer. The rating must be for a single independent parameter.
	 * @param conn The database connection to use for lazy ratings and reference ratings
	 * @param tsc The TimeSeriesContainer of dependent values.
	 * @return The TimeSeriesContainer of independent values.
	 * @throws RatingException
	 */
	public TimeSeriesContainer reverseRate(Connection conn, TimeSeriesContainer tsc) throws RatingException {
		Connection oldConn = this.conn;
		setDatabaseConnection(conn);
		try {
			return reverseRate(tsc);
		}
		finally {
			if (oldConn != null) {
				setDatabaseConnection(oldConn);
			}
			else {
				clearDatabaseConnection();
			}
		}
	}

	/**
	 * Rates the values in the specified TimeSeriesMath to generate a resulting TimeSeriesMath. The rating must be for a single independent parameter.
	 * @param conn The database connection to use for lazy ratings and reference ratings
	 * @param tsm The TimeSeriesMath of dependent values.
	 * @return The TimeSeriesMath of independent values.
	 * @throws RatingException
	 */
	public TimeSeriesMath reverseRate(Connection conn, TimeSeriesMath tsm) throws RatingException {
		Connection oldConn = this.conn;
		setDatabaseConnection(conn);
		try {
			return reverseRate(tsm);
		}
		finally {
			if (oldConn != null) {
				setDatabaseConnection(oldConn);
			}
			else {
				clearDatabaseConnection();
			}
		}
	}
	/**
	 * Outputs the rating set as an XML instance
	 * @param indent the text use for indentation
	 * @return the XML text
	 * @throws RatingException
	 */
	public String toXmlString(CharSequence indent) throws RatingException {
		return getData().toXml(indent, 0, true);
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
		storeToDatabase(conn, overwriteExisting, true);
	}
	/**
	 * Stores the rating set to a CWMS database
	 * @param conn The connection to the CWMS database
	 * @param overwriteExisting Flag specifying whether to overwrite any existing rating data
	 * @param inlcludeTemplate Flag specifying whether to include the rating template in the XML
	 * @throws RatingException
	 */
	public void storeToDatabase(Connection conn, boolean overwriteExisting, boolean includeTemplate) throws RatingException {
		if (dbrating != null) {
			//-----------------//
			// quietly succeed //
			//-----------------//
		}
		else {
			synchronized(this) {
				RatingSet.storeToDatabase(conn, getData().toXml("", 0, includeTemplate, false), overwriteExisting);
			}
		}
	}
	/* (non-Javadoc)
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */
	@Override
	public void update(java.util.Observable arg0, Object arg1) {
		refreshRatings();
		observationTarget.setChanged();
		observationTarget.notifyObservers();
	}
	/**
	 * Adds an Observer to this RatingSet. The Observer will be notified of any changes to this RatingSet
	 * @param o The Observer object to add
	 * @see java.util.Observer
	 */
	public void addObserver(Observer o) {
		synchronized(observationTarget) {
			observationTarget.addObserver(o);
		}
	}
	/**
	 * Deletes an Observer from this RatingSet. The Observer will no longer be notified of any changes to this RatingSet
	 * @param o The Observer object to delete
	 * @see java.util.Observer
	 */
	public void deleteObserver(Observer o) {
		synchronized(observationTarget) {
			observationTarget.deleteObserver(o);
		}
	}
	/**
	 * Retrieves a RatingSetContainer containing the data of this object. 
	 * @return The RatingSetContainer
	 */
	public RatingSetContainer getData() {
		synchronized(this) {
			if (dbrating != null) {
				ReferenceRatingContainer rrc = new ReferenceRatingContainer();
				if (ratingSpec != null) {
					rrc.ratingSpecContainer = (RatingSpecContainer)ratingSpec.getData();
				}
				return rrc;
			}
			else {
				RatingSetContainer rsc = new RatingSetContainer();
				if (ratingSpec != null) {
					rsc.ratingSpecContainer = (RatingSpecContainer)ratingSpec.getData();
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
		}
	}
	/**
	 * Sets the data from this object from a RatingSetContainer
	 * @param rsc The RatingSetContainer with the data
	 * @throws RatingException
	 */
	public void setData(RatingSetContainer rsc) throws RatingException {
		boolean isLazy = false;
		synchronized(this) {
			try {
				removeAllRatings();
				if (rsc.ratingSpecContainer == null) {
					ratingSpec = null;
				}
				else {
					setRatingSpec(new RatingSpec(rsc.ratingSpecContainer));
				}
				if (rsc instanceof ReferenceRatingContainer) {
					dbrating = new ReferenceRating((ReferenceRatingContainer)rsc);
					dbrating.parent = this;
				}
				else {
					if (rsc.abstractRatingContainers == null) {
						throw new RatingObjectDoesNotExistException("RatingSetContainer contains no ratings.");
					}
					else {
						for (int i = 0; i < rsc.abstractRatingContainers.length; ++i) {
							if (rsc.abstractRatingContainers[i] instanceof TableRatingContainer) {
								if (((TableRatingContainer)rsc.abstractRatingContainers[i]).values == null) {
									isLazy = true;
								}
							}
							addRating(rsc.abstractRatingContainers[i].newRating());
						}
					}
					if (isLazy) setLazy();
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
	}
	/**
	 * Sets the data from this object from an XML instance
	 * @param rsc The RatingSetContainer with the data
	 * @throws RatingException
	 */
	public void setData(String xmlText) throws RatingException {
		setData(new RatingSetContainer(xmlText).clone()); // clone might return a ReferenceRatingContainer
	}
	/**
	 * Sets the data from this object from a CWMS database connection
	 * @param loadMethod The method used to load the object from the database. If null, the value of the property
	 *        "hec.data.cwmsRating.RatingSet.databaseLoadMethod" is used. If both the argument and property value
	 *        are null (or if an invalid value is specified) the Lazy method will be used.
	 *        <table border>
	 *          <tr>
	 *            <th>Value (case insensitive)</th>
	 *            <th>Interpretation</th>
	 *          </tr>
	 *          <tr>
	 *            <td>Eager</td>
	 *            <td>Ratings for all effective times are loaded initially</td>
	 *            <td>Lazy</td>
	 *            <td>No ratings are loaded initially - each rating is only loaded when it is needed</td>
	 *            <td>Reference</td>
	 *            <td>No ratings are loaded ever - values are passed to database to be rated</td>
	 *          </tr>
	 *        </table>
	 * @param conn The connection to a CWMS database
	 * @param officeId The identifier of the office owning the rating. If null, the office associated with the connect user is used. 
	 * @param ratingSpecId The rating specification identifier
	 * @param startTime The earliest time to retrieve, as interpreted by inEffectTimes, in milliseconds.  If null, no earliest limit is set.
	 * @param endTime The latest time to retrieve, as interpreted by inEffectTimes, in milliseconds.  If null, no latest limit is set.
	 * @param dataTimes Determines how startTime and endTime are interpreted.
	 *        <table border>
	 *          <tr>
	 *            <th>Value</th>
	 *            <th>Interpretation</th>
	 *          </tr>
	 *          <tr>
	 *            <td>false</td>
	 *            <td>The time window specifies the extent of when the ratings became effective</td>
	 *            <td>true</td>
	 *            <td>Time time window specifies the time extent of data rate</td>
	 *          </tr>
	 *        </table>
	 * @return The new RatingSet object
	 * @throws RatingException
	 */
	public void setData(
			DatabaseLoadMethod loadMethod,
			Connection conn, 
			String officeId, 
			String ratingSpecId, 
			Long startTime, 
			Long endTime,
			boolean dataTimes)
			throws RatingException {

		if (loadMethod == null) {
			String propval = System.getProperty("hec.data.cwmsRating.RatingSet.databaseLoadMethod", "LAZY").toUpperCase();
			try {
				loadMethod = DatabaseLoadMethod.valueOf(propval);
			}
			catch (IllegalArgumentException e) {
				throw new RatingException(
						"Invalid value of hec.data.cwmsRating.RatingSet.databaseLoadMethod (\""
						+ propval
						+ "\"), must be \"EAGER\", \"LAZY\", \"REFERENCE\", or unset, which defaults to \"LAZY\"");
			}
		}
		String xml = RatingSet.getXmlFromDatabase(loadMethod, conn, officeId, ratingSpecId, startTime, endTime, dataTimes);
		setData(xml);
		switch (loadMethod) {
		case LAZY :
		case REFERENCE :
			try {
				PreparedStatement stmt = conn.prepareStatement("select cwms_util.get_user_id, cwms_util.user_office_id from dual");
				ResultSet rs = stmt.executeQuery();
				rs.next();
				String dbUserId = rs.getString(1);
				String dbOfficeId = rs.getString(2);
				rs.close();
				stmt.close();
				this.setDbInfo(conn.getMetaData().getURL(), dbUserId, dbOfficeId);
			} catch (Exception e) {
				if (e instanceof RatingException) throw (RatingException)e;
				throw new RatingException(e);
			}
		default :
			break;
		}
	}
	/**
	 * Sets the database connection for this RatingSet and any constituent RatingSet objects
	 * @param conn the connection
	 */
	public synchronized void setDatabaseConnection(Connection conn) {
		this.conn = conn;
		if (dbrating == null) {
			for (AbstractRating rating : activeRatings.values()) {
				if (rating instanceof UsgsStreamTableRating) {
					RatingSet shifts = ((UsgsStreamTableRating)rating).shifts;
					if (shifts != null) shifts.setDatabaseConnection(conn);
				}
				else if (rating instanceof VirtualRating) {
					SourceRating[] sourceRatings  = ((VirtualRating)rating).sourceRatings;
					if (sourceRatings != null) {
						for (SourceRating sourceRating : ((VirtualRating)rating).sourceRatings) {
							if (sourceRating.ratings != null) sourceRating.ratings.setDatabaseConnection(conn);
						}
					}
				}
				else if (rating instanceof TransitionalRating) {
					SourceRating[] sourceRatings  = ((TransitionalRating)rating).sourceRatings;
					if (sourceRatings != null) {
						for (SourceRating sourceRating : ((TransitionalRating)rating).sourceRatings) {
							if (sourceRating.ratings != null) sourceRating.ratings.setDatabaseConnection(conn);
						}
					}
				}
			}
		}
	}
	/**
	 * Retrieves the database info required to retrieve a database connection
	 * @return the database info required to retrieve a database connection
	 * @throws RatingException
	 */
	public synchronized DbInfo getDbInfo() throws RatingException {
		if (dbInfo == null) return null;
		return new DbInfo(dbInfo.getUrl(), dbInfo.getUserName(), dbInfo.getOfficeId());
	}
	/**
	 * Sets the database info required to retrieve a database connection
	 * @param dbInfo the database info required to retrieve a database connection
	 * @throws RatingException
	 */
	public synchronized void setDbInfo(DbInfo dbInfo) throws RatingException {
		this.dbInfo = dbInfo;
		if (dbrating == null) {
			for (AbstractRating rating : activeRatings.values()) {
				if (rating instanceof UsgsStreamTableRating) {
					RatingSet shifts = ((UsgsStreamTableRating)rating).shifts;
					if (shifts != null) shifts.setDbInfo(dbInfo);
				}
				else if (rating instanceof VirtualRating) {
					SourceRating[] sourceRatings  = ((VirtualRating)rating).sourceRatings;
					if (sourceRatings != null) {
						for (SourceRating sourceRating : ((VirtualRating)rating).sourceRatings) {
							if (sourceRating.ratings != null) sourceRating.ratings.setDbInfo(dbInfo);
						}
					}
				}
				else if (rating instanceof TransitionalRating) {
					SourceRating[] sourceRatings  = ((TransitionalRating)rating).sourceRatings;
					if (sourceRatings != null) {
						for (SourceRating sourceRating : ((TransitionalRating)rating).sourceRatings) {
							if (sourceRating.ratings != null) sourceRating.ratings.setDbInfo(dbInfo);
						}
					}
				}
			}
			for (AbstractRating rating : ratings.values()) {
				if (rating instanceof UsgsStreamTableRating) {
					RatingSet shifts = ((UsgsStreamTableRating)rating).shifts;
					if (shifts != null) shifts.setDbInfo(dbInfo);
				}
				else if (rating instanceof VirtualRating) {
					SourceRating[] sourceRatings  = ((VirtualRating)rating).sourceRatings;
					if (sourceRatings != null) {
						for (SourceRating sourceRating : ((VirtualRating)rating).sourceRatings) {
							if (sourceRating.ratings != null) sourceRating.ratings.setDbInfo(dbInfo);
						}
					}
				}
				else if (rating instanceof TransitionalRating) {
					SourceRating[] sourceRatings  = ((TransitionalRating)rating).sourceRatings;
					if (sourceRatings != null) {
						for (SourceRating sourceRating : ((TransitionalRating)rating).sourceRatings) {
							if (sourceRating.ratings != null) sourceRating.ratings.setDbInfo(dbInfo);
						}
					}
				}
			}
		}
	}
	/**
	 * Sets the database info required to retrieve a database connection
	 * @param url the database URL
	 * @param userName the database user name
	 * @param officeId the database office 
	 * @throws RatingException
	 */
	public synchronized void setDbInfo(String url, String userName, String officeId) throws RatingException {
		setDbInfo(new DbInfo(url, userName, officeId));
	}
	/**
	 * Clears the database connection for this RatingSet and any constituent RatingSet objects
	 * @param conn the connection
	 */
	public synchronized void clearDatabaseConnection() {
		if (dbrating == null) {
			for (AbstractRating rating : activeRatings.values()) {
				if (rating instanceof UsgsStreamTableRating) {
					RatingSet shifts = ((UsgsStreamTableRating)rating).shifts;
					if (shifts != null) shifts.clearDatabaseConnection();
				}
				else if (rating instanceof VirtualRating) {
					SourceRating[] sourceRatings  = ((VirtualRating)rating).sourceRatings;
					if (sourceRatings != null) {
						for (SourceRating sourceRating : ((VirtualRating)rating).sourceRatings) {
							if (sourceRating.ratings != null) sourceRating.ratings.clearDatabaseConnection();
						}
					}
				}
				else if (rating instanceof TransitionalRating) {
					SourceRating[] sourceRatings  = ((TransitionalRating)rating).sourceRatings;
					if (sourceRatings != null) {
						for (SourceRating sourceRating : ((TransitionalRating)rating).sourceRatings) {
							if (sourceRating.ratings != null) sourceRating.ratings.clearDatabaseConnection();
						}
					}
				}
			}
			conn = null;
		}
	}
	/**
	 * Retrieves a TextContainer containing the data of this object, suitable for storing to DSS. 
	 * @return The TextContainer
	 * @throws RatingException 
	 */
	public TextContainer getDssData() throws RatingException {
		synchronized(this) {
			try {
				if (dbrating != null) {
					throw new RatingException("Reference ratings cannot return DSS Data.");
				}
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
	}
	/**
	 * Sets the data from this object from a TextContainer (as read from DSS)
	 * @param tc The TextContainer with the data
	 * @throws RatingException
	 */
	public void setData(TextContainer tc) throws RatingException {
		synchronized(this) {
			if (dbrating != null) {
				throw new RatingException("Cannot set data for reference ratings.");
			}
			String[] lines = tc.text.split("\\n");
			String className = getClass().getName();
			for (int i = 0; i < lines.length-1; ++i) {
				if (lines[i].equals(className)) {
					int extra = lines[i+1].length() % 4;
					int last = lines[i+1].length() - extra;
					String compressedXml = lines[i+1].substring(0, last);
					String uncompressed = null;
					try {
						uncompressed = TextUtil.uncompress(compressedXml, "base64");
					} catch (Exception e) {
						throw new RatingException(e);
					}
					removeAllRatings();
					setData(uncompressed);
					return;
				}
			}
			throw new RatingException("Invalid text for RatingSet");
		}
	}
	/**
	 * Returns whether this object has any vertical datum info
	 * @return whether this object has any vertical datum info
	 */
	public boolean hasVerticalDatum() {
		if (dbrating != null) {
			try {
				return getNativeVerticalDatum() != null;
			}
			catch (VerticalDatumException e) {
				logger.warning(e.getMessage());
				return false;
			}
		}
		return getData().hasVerticalDatum();
	}
	protected void setLazy() {
		isLazy = true;
		for(AbstractRating r : getRatings()) {
			if (r instanceof VirtualRating) {
				for (SourceRating sr : ((VirtualRating)r).getSourceRatings()) {
					if (sr.ratings != null) {
						sr.ratings.setLazy();
					}
				}
			}
			else if (r instanceof TransitionalRating) {
				for (SourceRating sr : ((TransitionalRating)r).getSourceRatings()) {
					if (sr.ratings != null) {
						sr.ratings.setLazy();
					}
				}
			}
		}
	}
	protected boolean hasNullValues() {
		for(AbstractRating r : getRatings()) {
			if (r instanceof TableRating) {
				TableRating tr = (TableRating)r;
				if (tr.values == null) return true;
			}
			else if (r instanceof VirtualRating) {
				for (SourceRating sr : ((VirtualRating)r).getSourceRatings()) {
					if (sr.ratings != null) {
						if (sr.ratings.hasNullValues()) return true;
					}
				}
			}
			else if (r instanceof TransitionalRating) {
				for (SourceRating sr : ((TransitionalRating)r).getSourceRatings()) {
					if (sr.ratings != null) {
						if (sr.ratings.hasNullValues()) return true;
					}
				}
			}
		}
		return false;
	}
	public String getNativeVerticalDatum(Connection conn) throws VerticalDatumException {
		setDatabaseConnection(conn);
		try {
			return getNativeVerticalDatum();
		}
		finally {
			clearDatabaseConnection();
		}
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#getNativeVerticalDatum()
	 */
	@Override
	public String getNativeVerticalDatum() throws VerticalDatumException {
		if (dbrating != null) {
			ConnectionInfo ci = null;
			try {
				ci = getConnectionInfo();
				conn = ci.getConnection();
				synchronized(conn) {
					PreparedStatement stmt = conn.prepareStatement("select vertical_datum from cwms_v_loc where location_id = :1 and unit_system = 'EN'");
					stmt.setString(1, ratingSpec.locationId);
					ResultSet rs = stmt.executeQuery();
					rs.next();
					String vertical_datum = rs.getString(1);
					rs.close();
					stmt.close();
					return vertical_datum;
				}
			}
			catch (Exception e) {
				throw new VerticalDatumException(e);
			}
			finally {
				if (ci != null && ci.wasRetrieved()) {
					try {
						releaseConnection(ci);
					} catch (RatingException e) {
						throw new VerticalDatumException(e);
					}
				}
			}
		}
		return getData().getNativeVerticalDatum();
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#getCurrentVerticalDatum()
	 */
	@Override
	public String getCurrentVerticalDatum() throws VerticalDatumException {
		return getData().getCurrentVerticalDatum();
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#isCurrentVerticalDatumEstimated()
	 */
	@Override
	public boolean isCurrentVerticalDatumEstimated() throws VerticalDatumException {
		return getData().isCurrentVerticalDatumEstimated();
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#toNativeVerticalDatum()
	 */
	@Override
	public boolean toNativeVerticalDatum() throws VerticalDatumException {
		RatingSetContainer rsc = getData();
		boolean change = rsc.toNativeVerticalDatum();
		if (change) {
			try {
				setData(rsc);
			}
			catch (RatingException e) {
				throw new VerticalDatumException(e);
			}
		}
		return change;
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#toNGVD29()
	 */
	@Override
	public boolean toNGVD29() throws VerticalDatumException {
		RatingSetContainer rsc = getData();
		boolean change = rsc.toNGVD29();
		if (change) {
			try {
				setData(rsc);
			}
			catch (RatingException e) {
				throw new VerticalDatumException(e);
			}
		}
		return change;
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#toNAVD88()
	 */
	@Override
	public boolean toNAVD88() throws VerticalDatumException {
		RatingSetContainer rsc = getData();
		boolean change = rsc.toNAVD88();
		if (change) {
			try {
				setData(rsc);
			}
			catch (RatingException e) {
				throw new VerticalDatumException(e);
			}
		}
		return change;
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#toVerticalDatum(java.lang.String)
	 */
	@Override
	public boolean toVerticalDatum(String datum) throws VerticalDatumException {
		RatingSetContainer rsc = getData();
		boolean change = rsc.toVerticalDatum(datum);
		if (change) {
			try {
				setData(rsc);
			}
			catch (RatingException e) {
				throw new VerticalDatumException(e);
			}
		}
		return change;
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#forceVerticalDatum(java.lang.String)
	 */
	@Override
	public boolean forceVerticalDatum(String datum) throws VerticalDatumException {
		boolean change;
		if (dbrating == null) {
			RatingSetContainer rsc = getData();
			change = rsc.forceVerticalDatum(datum);
			if (change) {
				try {
					setData(rsc);
				}
				catch (RatingException e) {
					throw new VerticalDatumException(e);
				}
			}
		}
		else {
			change = dbrating.forceVerticalDatum(datum);
		}
		return change;
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#getCurrentOffset()
	 */
	@Override
	public double getCurrentOffset() throws VerticalDatumException {
		if (dbrating == null) {
			return getData().getCurrentOffset();
		}
		else {
			return dbrating.getCurrentOffset();
		}
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#getCurrentOffset(java.lang.String)
	 */
	@Override
	public double getCurrentOffset(String unit) throws VerticalDatumException {
		if (dbrating == null) {
			return getData().getCurrentOffset(unit);
		}
		else {
			return dbrating.getCurrentOffset(unit);
		}
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#getNGVD29Offset()
	 */
	@Override
	public double getNGVD29Offset() throws VerticalDatumException {
		if (dbrating == null) {
			return getData().getNGVD29Offset();
		}
		else {
			return dbrating.getNGVD29Offset();
		}
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#getNGVD29Offset(java.lang.String)
	 */
	@Override
	public double getNGVD29Offset(String unit) throws VerticalDatumException {
		if (dbrating == null) {
			return getData().getNGVD29Offset(unit);
		}
		else {
			return dbrating.getNGVD29Offset(unit);
		}
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#getNAVD88Offset()
	 */
	@Override
	public double getNAVD88Offset() throws VerticalDatumException {
		if (dbrating == null) {
			return getData().getNAVD88Offset();
		}
		else {
			return dbrating.getNAVD88Offset();
		}
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#getNAVD88Offset(java.lang.String)
	 */
	@Override
	public double getNAVD88Offset(String unit) throws VerticalDatumException {
		if (dbrating == null) {
			return getData().getNAVD88Offset(unit);
		}
		else {
			return dbrating.getNAVD88Offset(unit);
		}
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#isNGVD29OffsetEstimated()
	 */
	@Override
	public boolean isNGVD29OffsetEstimated() throws VerticalDatumException {
		if (dbrating == null) {
			return getData().isNGVD29OffsetEstimated();
		}
		else {
			return dbrating.isNGVD29OffsetEstimated();
		}
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#isNAVD88OffsetEstimated()
	 */
	@Override
	public boolean isNAVD88OffsetEstimated() throws VerticalDatumException {
		if (dbrating == null) {
			return getData().isNAVD88OffsetEstimated();
		}
		else {
			return dbrating.isNAVD88OffsetEstimated();
		}
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#getVerticalDatumInfo()
	 */
	@Override
	public String getVerticalDatumInfo() throws VerticalDatumException {
		if (dbrating == null) {
			return getData().getVerticalDatumInfo();
		}
		else {
			return dbrating.getVerticalDatumInfo();
		}
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#setVerticalDatumInfo(java.lang.String)
	 */
	@Override
	public void setVerticalDatumInfo(String xmlStr) throws VerticalDatumException {
		if (dbrating == null) {
			RatingSetContainer rsc = getData();
			rsc.setVerticalDatumInfo(xmlStr);
			try {
				setData(rsc);
			}
			catch (RatingException e) {
				throw new VerticalDatumException(e);
			}
		}
		else {
			dbrating.setVerticalDatumInfo(xmlStr);
		}
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		boolean same =  obj == this 
				|| (obj instanceof RatingSet 
						&& (((RatingSet)obj).dbInfo == null) == (dbInfo == null)
						&& (((RatingSet)obj).conn == null) == (conn == null)
						&& (((RatingSet)obj).dbrating == null) == (dbrating == null)
						&& (dbInfo == null || ((RatingSet)obj).dbInfo.equals(dbInfo))
						&& ((RatingSet)obj).allowUnsafe == allowUnsafe
						&& ((RatingSet)obj).warnUnsafe == warnUnsafe
						&& ((RatingSet)obj).defaultValueTime == defaultValueTime
						&& ((RatingSet)obj).ratingTime == ratingTime
						&& getData().equals(((RatingSet)obj).getData())
						&& (((RatingSet)obj).activeRatings == null) == (activeRatings == null));
		if (same) {
			if (activeRatings != null) {
				if (((RatingSet)obj).activeRatings.size() != activeRatings.size()) return false;
				for (Long key : activeRatings.keySet()) {
					if (!((RatingSet)obj).activeRatings.containsKey(key)) return false;
					if(!((RatingSet)obj).activeRatings.get(key).equals(activeRatings.get(key))) return false;
				}
			}
		}
		return same;
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#hashCode()
	 */
	@Override
	public int hashCode() {
		int hashCode = getClass().getName().hashCode()
				+ 3 * (dbInfo == null ? 1 : dbInfo.hashCode())
				+ 5 * (conn == null ? 1 : 5)
				+ 7 * (dbrating == null ? 1 : 7)
				+ 11 * (allowUnsafe ? 1 : 11)
				+ 13 * (warnUnsafe ? 1 : 13)
				+ 17 * (int)defaultValueTime
				+ 19 * (int)ratingTime
				+ getData().hashCode();
		if (activeRatings != null) {
			Iterator<AbstractRating> it = activeRatings.values().iterator();
			for (int i = 0; it.hasNext(); ++i) {
				hashCode += 23 * i * it.next().hashCode();
			}
		}
		return hashCode;
	}
	
	private void refreshRatings() {
		synchronized(this) {
			if (dbrating == null) {
				if (isLazy) {
					//-------------------------------------------------------------------//
					// first update ratings from active ratings (if rating still exists) //
					//-------------------------------------------------------------------//
					for (Long key : activeRatings.keySet()) {
						if (ratings.containsKey(key)) {
							ratings.put(key, activeRatings.get(key));
						}
					}
				}
				//---------------------------------------------//
				// now rebuild both ratings and active ratings //
				//---------------------------------------------//
				AbstractRating[] ratingArray = ratings.values().toArray(new AbstractRating[ratings.size()]);
				ratings.clear();
				activeRatings.clear();
				for (AbstractRating rating : ratingArray) {
					long effectiveDate = rating.getEffectiveDate();
					ratings.put(effectiveDate, rating);
					if (rating.isActive() && rating.getCreateDate() < ratingTime) {
						activeRatings.put(effectiveDate, rating);
					}
					rating.deleteObserver(this);
					rating.addObserver(this);
				}
				if (observationTarget != null) {
					observationTarget.setChanged();
					observationTarget.notifyObservers();
				}
			}
		}
	}
	/**
	 * Validates the rating set
	 * @throws RatingException if the rating set is not valid
	 */
	private void validate() throws RatingException {
		synchronized(this) {
			if (dbrating != null) return;
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
					if (units != null) {
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
	}
}
