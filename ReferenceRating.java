/**
 * 
 */
package hec.data.cwmsRating;

import static hec.data.cwmsRating.RatingConst.SEPARATOR1;
import static hec.data.cwmsRating.RatingConst.SEPARATOR2;
import static hec.data.cwmsRating.RatingConst.SEPARATOR3;
import static hec.lang.Const.UNDEFINED_LONG;
import static hec.lang.Const.UNDEFINED_TIME;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import hec.data.DataSetIllegalArgumentException;
import hec.data.IRating;
import hec.data.IVerticalDatum;
import hec.data.Parameter;
import hec.data.RatingException;
import hec.data.Units;
import hec.data.VerticalDatumException;
import hec.data.cwmsRating.RatingSet.ConnectionInfo;
import hec.data.cwmsRating.io.ReferenceRatingContainer;
import hec.hecmath.TimeSeriesMath;
import hec.io.TimeSeriesContainer;
import hec.io.VerticalDatumContainer;
import hec.util.TextUtil;


/**
 * A rating that references rating data and methods in a live database.  The method calls on an object of this class
 * are executed in the database, with parameters and return values being communicated between the object and the 
 * database as necessary.  This object is analogous to the RatingSet class in that multiple ratings under the same
 * rating specification may be used, depending on their effective dates. 
 *  
 * @author Mike Perryman
 */
public class ReferenceRating implements IRating, IVerticalDatum {

	protected static final Logger logger = Logger.getLogger(ReferenceRating.class.getPackage().getName());
	
	long defaultValueTime = UNDEFINED_TIME;
	
	long ratingTime = Long.MAX_VALUE;
	
	protected String ratingSpecId = null;
	
	protected long ratingSpecCode = UNDEFINED_LONG;
	
	protected String locationId = null;
	
	protected String templateId = null;
	
	protected String parametersId = null;
	
	protected String templateVersion = null;
	
	protected String specificationVersion = null;
	
	protected String officeId = null;
	
	protected String[] parameters = null;
	
	protected int[] elevPositions = null;
	
	protected String[] ratingUnits = null;
	
	protected String[] dataUnits = null;
	
	protected VerticalDatumContainer vdc = null;
	
	protected TimeSeriesRater tsRater = null;
	
	protected RatingSet parent = null;
	
	protected ReferenceRating() {} // no public default constructor
	
	public ReferenceRating(ReferenceRatingContainer rrc) throws RatingException {
		setData(rrc);
		resetRatingTime();
	}
	/**
	 * Method to create a reference rating from the database
	 * @param conn The database connection
	 * @param officeId The office identifier
	 * @param ratingSpecId The rating specification identifier
	 * @return A new ReferenceRating object
	 * @throws RatingException
	 */
	public static ReferenceRating fromDatabase(Connection conn, String officeId, String ratingSpecId) throws RatingException {
		return new ReferenceRating(conn, officeId, ratingSpecId);
	}
	
	protected ReferenceRating(Connection conn, String officeId, String ratingSpecId) throws RatingException {
		this.ratingSpecId = ratingSpecId;
		this.officeId = officeId.toUpperCase();
		synchronized(conn) {
			String sql = "select rating_spec_code from cwms_v_rating_spec where office_id = :1 and upper(rating_id) = upper(:2)"; 
			try {
				PreparedStatement stmt = conn.prepareStatement(sql);
				stmt.setString(1, this.officeId);
				stmt.setString(2, this.ratingSpecId);
				ResultSet rs = stmt.executeQuery();
				try {
					rs.next();
					this.ratingSpecCode = rs.getLong(1);
				}
				catch (SQLException e) {
					throw new RatingException(String.format("No such rating: %s/%s", this.officeId, this.ratingSpecId));
				}
				finally {
					rs.close();
					stmt.close();
				}
			}
			catch (SQLException e) {
				throw new RatingException(e);
			}

			
			String[] parts = TextUtil.split(ratingSpecId, SEPARATOR1);
			if (parts.length != 4) {
				throw new RatingException(String.format("Invalid rating specification: %s", ratingSpecId));
			}
			locationId = parts[0];
			parametersId = parts[1];
			templateVersion = parts[2];
			specificationVersion = parts[3];
			templateId = TextUtil.join(SEPARATOR1, parametersId, templateVersion);
			parameters = TextUtil.split(parametersId.replace(SEPARATOR2, SEPARATOR3), SEPARATOR3);
			ratingUnits = new String[parameters.length];
			try {
				for (int i = 0; i < parameters.length; ++i) {
					ratingUnits[i] = (new Parameter(parameters[i])).getUnitsStringForSystem(Units.ENGLISH_ID);
				}
			}
			catch (DataSetIllegalArgumentException e) {
				throw new IllegalArgumentException(e);
			}
			List<Integer> elevPosList = new ArrayList<Integer>();
			for (int i = 0; i < parameters.length; ++i) {
				try {
					new Parameter(parameters[i]);
				}
				catch (DataSetIllegalArgumentException e) {
					throw new RatingException(e);
				}
				if (parameters[i].toUpperCase().startsWith("ELEV")) {
					elevPosList.add(i);
				}
			}
			elevPositions = new int[elevPosList.size()];
			for (int i = 0; i < elevPosList.size(); ++i) {
				elevPositions[i] = elevPosList.get(i);
			}
			if (elevPositions.length > 0) {
				sql = ""
					+ "begin"
					+ ":1 := cwms_loc.get_vertical_datum_info_f("
					+ "p_location_id => :2,"
					+ "p_unit        => :3,"
					+ "p_office_id   => :4);"
					+ "end;";
				try {
					CallableStatement call = conn.prepareCall(sql);
					call.registerOutParameter(1, Types.VARCHAR);
					call.setString(2, locationId);
					call.setString(3, "ft");
					call.setString(4, officeId);
					call.execute();
					String verticalDatumInfo = call.getString(1);
					call.close();
					if (verticalDatumInfo != null) {
						vdc = new VerticalDatumContainer(verticalDatumInfo);
					}
				}
				catch (SQLException | VerticalDatumException e) {
					AbstractRating.logger.warning(String.format("Vertical datum initialzation failed: %s", e.getMessage()));
				}
			}
			resetRatingTime();
		}
	}
	/**
	 * @return a the current database connection plus a flag specifying whether it was retrieved using the DbInfo
	 * @throws RatingException
	 */
	protected ConnectionInfo getConnection() throws RatingException {
		synchronized(this) {
			if (parent != null) {
				return parent.getConnectionInfo();
			}
			else {
				throw new RatingException("ReferenceRating object has no parent RatingSet object to use for database connections");
			}			
		}
	}
	/**
	 * Releases a database connection that was retrieved using the DbInfo
	 * @param ci The database connection information
	 * @throws RatingException
	 */
	protected void releaseConnection(ConnectionInfo ci) throws RatingException {
		if (ci != null && ci.wasRetrieved()) {
			parent.releaseConnection(ci);
			parent.conn = null;
		}
	}
	
	protected void setData(ReferenceRatingContainer rrc) throws RatingException {
		if (rrc.ratingSpecContainer != null) {
			ratingSpecId = rrc.ratingSpecContainer.specId;
			officeId = rrc.ratingSpecContainer.specOfficeId == null ? rrc.ratingSpecContainer.officeId : rrc.ratingSpecContainer.specOfficeId;
			if (rrc.hasVerticalDatum()) {
				try {
					setVerticalDatumInfo(rrc.getVerticalDatumInfo());
				} catch (VerticalDatumException e) {
					throw new RatingException(e);
				}
			}
			String[] parts = TextUtil.split(ratingSpecId, SEPARATOR1);
			if (parts.length != 4) {
				throw new RatingException(String.format("Invalid rating specification: %s", ratingSpecId));
			}
			locationId = parts[0];
			parametersId = parts[1];
			templateVersion = parts[2];
			specificationVersion = parts[3];
			templateId = TextUtil.join(SEPARATOR1, parametersId, templateVersion);
			parameters = TextUtil.split(parametersId.replace(SEPARATOR2, SEPARATOR3), SEPARATOR3);
			ratingUnits = new String[parameters.length];
			try {
				for (int i = 0; i < parameters.length; ++i) {
					ratingUnits[i] = (new Parameter(parameters[i])).getUnitsStringForSystem(Units.ENGLISH_ID);
				}
			}
			catch (DataSetIllegalArgumentException e) {
				throw new IllegalArgumentException(e);
			}
			List<Integer> elevPosList = new ArrayList<Integer>();
			for (int i = 0; i < parameters.length; ++i) {
				try {
					new Parameter(parameters[i]);
				}
				catch (DataSetIllegalArgumentException e) {
					throw new RatingException(e);
				}
				if (parameters[i].toUpperCase().startsWith("ELEV")) {
					elevPosList.add(i);
				}
			}
			elevPositions = new int[elevPosList.size()];
			for (int i = 0; i < elevPosList.size(); ++i) {
				elevPositions[i] = elevPosList.get(i);
			}
		}
	}
	protected void populateRatingSpecCode() throws RatingException {
		ConnectionInfo ci = getConnection();
		Connection conn = ci.getConnection();
		synchronized(conn) {
			String sql = "select rating_spec_code from cwms_v_rating_spec where office_id = :1 and upper(rating_id) = upper(:2)"; 
			try {
				PreparedStatement stmt = conn.prepareStatement(sql);
				stmt.setString(1, this.officeId);
				stmt.setString(2, this.ratingSpecId);
				ResultSet rs = stmt.executeQuery();
				try {
					rs.next();
					ratingSpecCode = rs.getLong(1);
				}
				catch (SQLException e) {
					throw new RatingException(String.format("No such rating: %s/%s", this.officeId, this.ratingSpecId));
				}
				finally {
					rs.close();
					stmt.close();
				}
			}
			catch (SQLException e) {
				throw new RatingException(e);
			}
			finally {
				if (ci.wasRetrieved()) {
					releaseConnection(ci);
				}
			}
		}
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		int hashCode = getClass().getName().hashCode();
		try {
			hashCode += getConnection().hashCode();
		} catch (RatingException e) {}
		return hashCode;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		boolean result = obj == this;
		if (!result) {
			if (obj instanceof ReferenceRating) {
				ReferenceRating other = (ReferenceRating)obj;
				try {
					ConnectionInfo ci1 = getConnection();
					ConnectionInfo ci2 = other.getConnection();
					Connection conn1 = ci1.getConnection();
					Connection conn2 = ci2.getConnection();
					if (conn2 == conn1) return true;
					DatabaseMetaData md1 = conn1.getMetaData();
					DatabaseMetaData md2 = conn2.getMetaData();
					if (!md1.getURL().equalsIgnoreCase(md2.getURL())) return false;
					if (!md1.getUserName().equalsIgnoreCase(md2.getUserName())) return false;
					return true;
				} catch (Exception e) {
					result = other.hashCode() == hashCode();
				}
			}
		}
		return result;
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#getNativeVerticalDatum()
	 */
	@Override
	public String getNativeVerticalDatum() throws VerticalDatumException {
		checkVDC();
		return vdc.getNativeVerticalDatum();
	}

	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#getCurrentVerticalDatum()
	 */
	@Override
	public String getCurrentVerticalDatum() throws VerticalDatumException {
		checkVDC();
		return vdc.getCurrentVerticalDatum();
	}

	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#isCurrentVerticalDatumEstimated()
	 */
	@Override
	public boolean isCurrentVerticalDatumEstimated() throws VerticalDatumException {
		checkVDC();
		return vdc.isCurrentVerticalDatumEstimated();
	}

	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#toNativeVerticalDatum()
	 */
	@Override
	public boolean toNativeVerticalDatum() throws VerticalDatumException {
		checkVDC();
		return vdc.toNativeVerticalDatum();
	}

	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#toNGVD29()
	 */
	@Override
	public boolean toNGVD29() throws VerticalDatumException {
		checkVDC();
		return vdc.toNGVD29();
	}

	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#toNAVD88()
	 */
	@Override
	public boolean toNAVD88() throws VerticalDatumException {
		checkVDC();
		return vdc.toNAVD88();
	}

	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#toVerticalDatum(java.lang.String)
	 */
	@Override
	public boolean toVerticalDatum(String datum) throws VerticalDatumException {
		checkVDC();
		return vdc.toVerticalDatum(datum);
	}

	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#forceVerticalDatum(java.lang.String)
	 */
	@Override
	public boolean forceVerticalDatum(String datum) throws VerticalDatumException {
		checkVDC();
		return vdc.forceVerticalDatum(datum);
	}
	
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#getCurrentOffset()
	 */
	@Override
	public double getCurrentOffset() throws VerticalDatumException {
		checkVDC();
		return vdc.getCurrentOffset();
	}

	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#getCurrentOffset(java.lang.String)
	 */
	@Override
	public double getCurrentOffset(String unit) throws VerticalDatumException {
		checkVDC();
		return vdc.getCurrentOffset(unit);
	}

	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#getNGVD29Offset()
	 */
	@Override
	public double getNGVD29Offset() throws VerticalDatumException {
		checkVDC();
		return vdc.getNGVD29Offset();
	}

	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#getNGVD29Offset(java.lang.String)
	 */
	@Override
	public double getNGVD29Offset(String unit) throws VerticalDatumException {
		checkVDC();
		return vdc.getNGVD29Offset(unit);
	}

	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#getNAVD88Offset()
	 */
	@Override
	public double getNAVD88Offset() throws VerticalDatumException {
		checkVDC();
		return vdc.getNAVD88Offset();
	}

	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#getNAVD88Offset(java.lang.String)
	 */
	@Override
	public double getNAVD88Offset(String unit) throws VerticalDatumException {
		checkVDC();
		return vdc.getNAVD88Offset(unit);
	}

	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#isNGVD29OffsetEstimated()
	 */
	@Override
	public boolean isNGVD29OffsetEstimated() throws VerticalDatumException {
		checkVDC();
		return vdc.isNGVD29OffsetEstimated();
	}

	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#isNAVD88OffsetEstimated()
	 */
	@Override
	public boolean isNAVD88OffsetEstimated() throws VerticalDatumException {
		checkVDC();
		return vdc.isNAVD88OffsetEstimated();
	}

	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#getVerticalDatumInfo()
	 */
	@Override
	public String getVerticalDatumInfo() throws VerticalDatumException {
		checkVDC();
		return vdc.getVerticalDatumInfo();
	}

	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#setVerticalDatumInfo(java.lang.String)
	 */
	@Override
	public void setVerticalDatumInfo(String initStr) throws VerticalDatumException {
		checkVDC();
		vdc.setVerticalDatumInfo(initStr);
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#getName()
	 */
	@Override
	public String getName() {
		return ratingSpecId;
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#setName(java.lang.String)
	 */
	@Override
	public void setName(String name) throws RatingException {
		throw new RatingException("Cannot set name of a reference rating");
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#getRatingParameters()
	 */
	@Override
	public String[] getRatingParameters() {
		return parameters;
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#getRatingUnits()
	 */
	@Override
	public String[] getRatingUnits() {
		return ratingUnits;
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#getDataUnits()
	 */
	@Override
	public String[] getDataUnits() {
		return dataUnits == null ? ratingUnits : dataUnits;
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#setDataUnits(java.lang.String[])
	 */
	@Override
	public void setDataUnits(String[] units) throws RatingException {
		synchronized(this) {
			ConnectionInfo ci = null;
			try {
				if (units == null) {
					dataUnits = null;
					return;
				}
				if (units.length != parameters.length) {
					throw new RatingException(String.format("Expected %d units, got %d instead.", parameters.length, units.length));
				}
				ci = getConnection();
				Connection conn = ci.getConnection();
				synchronized(conn) {
					CallableStatement call = conn.prepareCall("begin :1 := cwms_util.convert_units(1.0, :2, :3); end;");
					call.registerOutParameter(1, 0x65 /*OracleTypes.BINARY_DOUBLE*/);
					for (int i = 0; i < units.length; ++i) {
						call.setString(2, ratingUnits[i]);
						call.setString(3, units[i]);
						try {
							call.execute();
						}
						catch (SQLException e) {
							if (e.getMessage().indexOf("Cannot convert") != -1) {
								throw new RatingException(String.format("Invalid unit \"%s\" specified for parameter \"%s\"", units[i], parameters[i]));
							}
						}
					}
					call.close();
				}
				dataUnits = Arrays.copyOf(units, units.length);
			}
			catch (Exception e) {
				if (e instanceof RatingException) throw (RatingException)e;
				throw new RatingException(e);
			}
			finally {
				releaseConnection(ci);
			}
		}
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#getDefaultValueTime()
	 */
	@Override
	public long getDefaultValueTime() {
		synchronized(this) {
			return defaultValueTime == UNDEFINED_TIME ? System.currentTimeMillis() : defaultValueTime;
		}
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#setDefaultValueTime(long)
	 */
	@Override
	public void setDefaultValueTime(long defaultValueTime) {
		synchronized(this) {
			this.defaultValueTime = defaultValueTime;
		}
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#resetDefaultValuetime()
	 */
	@Override
	public void resetDefaultValuetime() {
		synchronized(this) {
			defaultValueTime = UNDEFINED_TIME;
		}
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#getRatingTime()
	 */
	@Override
	public long getRatingTime() {
		synchronized(this) {
			return ratingTime;
		}
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#setRatingTime(long)
	 */
	@Override
	public void setRatingTime(long ratingTime) {
		synchronized(this) {
			this.ratingTime = ratingTime;
		}
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#resetRatingTime()
	 */
	@Override
	public void resetRatingTime() {
		synchronized(this) {
			ratingTime = System.currentTimeMillis() + 100 * 365 * 86400 * 1000;
		}
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#getRatingExtents()
	 */
	@Override
	public double[][] getRatingExtents() throws RatingException {
		synchronized(this) {
			return getRatingExtents(getRatingTime());
		}
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#getRatingExtents(long)
	 */
	@Override
	public double[][] getRatingExtents(long ratingTime) throws RatingException {
		synchronized(this) {
			ConnectionInfo ci = getConnection();
			Connection conn = ci.getConnection();
			try {
				synchronized(conn) {
					Class<?> ratingObjClass = Class.forName("cwmsdb.CwmsRatingJdbc");
					Object cwmsRatingObj = ratingObjClass.getConstructor(Connection.class).newInstance(conn); 
					if (cwmsRatingObj == null) {
						throw new RatingException("No database rating implemenation found");
					}
					Method extentsMethod = ratingObjClass.getMethod(
							"getRatingExtents",
							String.class,
							String.class,
							boolean.class,
							long.class,
							double[][][].class,
							String[][].class,
							String[][].class);
					
					int paramCount = this.parameters.length;
					double[][][] extents = new double[][][] {new double[2][paramCount]};
					String[][] parameters = new String[][] {new String[paramCount]};
					String[][] units = new String[][] {new String[paramCount]};
					
					extentsMethod.invoke(
							cwmsRatingObj,
							ratingSpecId, 
							officeId, 
							true, 
							ratingTime, 
							extents, 
							parameters,
							units);
					
					return extents[0];
				}
			}
			catch (Exception e) {
				if (e instanceof RatingException) throw (RatingException)e;
				throw new RatingException(e);
			}
			finally {
				releaseConnection(ci);
			}
		}
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#getEffectiveDates()
	 */
	@Override
	public long[] getEffectiveDates() {
		synchronized(this) {
			long[] dates = null;
			ConnectionInfo ci = null;
			try {
				ci = getConnection();
				Connection conn = ci.getConnection();
				synchronized(conn) {
					if (ratingSpecCode == UNDEFINED_LONG) {
						populateRatingSpecCode();
					}
					String sql = ""
							+ "select cwms_util.to_millis(v.effective_date)"
							+ "  from cwms_v_rating v"
							+ " where aliased_item is null"
							+ "   and rating_spec_code = :1"
							+ " order by 1";
					PreparedStatement stmt = conn.prepareStatement(sql);
					stmt.setLong(1, ratingSpecCode);
					ResultSet rs = stmt.executeQuery();
					List<Long> dateList = new ArrayList<Long>();
					while (rs.next()) {
						dateList.add(rs.getLong(1));
					}
					rs.close();
					stmt.close();
					dates = new long[dateList.size()];
					for (int i = 0; i < dateList.size(); ++i) {
						dates[i] = dateList.get(i);
					}
				}
			}
			catch (Exception e) {
				if (logger.isLoggable(Level.WARNING)) {
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					e.printStackTrace(pw);
					logger.log(Level.WARNING, sw.toString());
				}
			}
			finally {
				try {
					releaseConnection(ci);
				} 
				catch (RatingException e) {
					if (logger.isLoggable(Level.WARNING)) {
						StringWriter sw = new StringWriter();
						PrintWriter pw = new PrintWriter(sw);
						e.printStackTrace(pw);
						logger.log(Level.WARNING, sw.toString());
					}
				}
			}
			return dates;
		}
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#getCreateDates()
	 */
	@Override
	public long[] getCreateDates() {
		synchronized(this) {
			long[] dates = null;
			ConnectionInfo ci = null;
			try {
				ci = getConnection();
				Connection conn = ci.getConnection();
				if (conn == null) {
					throw new RatingException("Not currently connected to a database. Either use a method with a Connection parameter or call setConnection(Connection)");
				}			
				synchronized(conn) {
					if (ratingSpecCode == UNDEFINED_LONG) {
						populateRatingSpecCode();
					}
					String sql = ""
							+ "select cwms_util.to_millis(v.create_date)"
							+ "  from cwms_v_rating v"
							+ " where aliased_item is null"
							+ "   and rating_spec_code = :1"
							+ " order by 1";
					PreparedStatement stmt = conn.prepareStatement(sql);
					stmt.setLong(1, ratingSpecCode);
					ResultSet rs = stmt.executeQuery();
					List<Long> dateList = new ArrayList<Long>();
					while (rs.next()) {
						dateList.add(rs.getLong(1));
					}
					rs.close();
					stmt.close();
					dates = new long[dateList.size()];
					for (int i = 0; i < dateList.size(); ++i) {
						dates[i] = dateList.get(i);
					}
				}
			}
			catch (Exception e) {
				if (logger.isLoggable(Level.WARNING)) {
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					e.printStackTrace(pw);
					logger.log(Level.WARNING, sw.toString());
				}
			}
			finally {
				try {
					releaseConnection(ci);
				} 
				catch (RatingException e) {
					if (logger.isLoggable(Level.WARNING)) {
						StringWriter sw = new StringWriter();
						PrintWriter pw = new PrintWriter(sw);
						e.printStackTrace(pw);
						logger.log(Level.WARNING, sw.toString());
					}
				}
			}
			return dates;
		}
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#rate(double)
	 */
	@Override
	public double rate(double indVal) throws RatingException {
		return rate(getDefaultValueTime(), indVal);
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#rateOne(double[])
	 */
	@Override
	public double rateOne(double... indVals) throws RatingException {
		long[] valTimes = new long[] {getDefaultValueTime()};
		double[][] _indVals = new double[indVals.length][];
		for (int i = 0; i < indVals.length; ++i) {
			_indVals[i] = new double[] {indVals[i]};
		}
		return rate(valTimes, _indVals)[0];
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#rateOne2(double[])
	 */
	@Override
	public double rateOne2(double[] indVals) throws RatingException {
		long[] valTimes = new long[] {getDefaultValueTime()};
		double[][] _indVals = new double[1][indVals.length];
		for (int i = 0; i < indVals.length; ++i) {
			_indVals[0][i] = indVals[i];
		}
		return rate(valTimes, _indVals)[0];
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#rate(double[])
	 */
	@Override
	public double[] rate(double[] indVals) throws RatingException {
		return rate(getDefaultValueTime(), indVals);
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#rate(double[][])
	 */
	@Override
	public double[] rate(double[][] indVals) throws RatingException {
		return rate(getDefaultValueTime(), indVals);
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#rate(long, double)
	 */
	@Override
	public double rate(long valTime, double indVal) throws RatingException {
		long[] valTimes = new long[] {valTime};
		double[][] indVals = new double[][] {{indVal}};
		return rate(valTimes, indVals)[0];
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#rateOne(long, double[])
	 */
	@Override
	public double rateOne(long valTime, double... indVals) throws RatingException {
		return rateOne2(valTime, indVals);
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#rateOne2(long, double[])
	 */
	@Override
	public double rateOne2(long valTime, double[] indVals) throws RatingException {
		long[] valTimes = new long[] {valTime};
		double[][] _indVals = new double[1][indVals.length];
		for (int i = 0; i < indVals.length; ++i) {
			_indVals[0][i] = indVals[i];
		}
		return rate(valTimes, _indVals)[0];
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#rate(long, double[])
	 */
	@Override
	public double[] rate(long valTime, double[] indVals) throws RatingException {
		long[] valTimes = new long[indVals.length];
		Arrays.fill(valTimes, valTime);
		return rate(valTimes, indVals);
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#rate(long[], double[])
	 */
	@Override
	public double[] rate(long[] valTimes, double[] indVals) throws RatingException {
		double[][] _indVals = new double[indVals.length][];
		for (int i = 0; i < indVals.length; ++i) {
			_indVals[i] = new double[] {indVals[i]};
		}
		return rate(valTimes, _indVals);
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#rate(long, double[][])
	 */
	@Override
	public double[] rate(long valTime, double[][] indVals) throws RatingException {
		long[] valTimes = new long[indVals.length];
		Arrays.fill(valTimes, valTime);
		return rate(valTimes, indVals);
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#rate(long[], double[][])
	 */
	@Override
	public double[] rate(long[] valTimes, double[][] indVals) throws RatingException {
		//------------------------------------//
		// rearrange values for database call //
		//------------------------------------//
		int dim1 = indVals.length;
		int dim2 = indVals[0].length;
		double [][] dbIndVals = new double[dim2][dim1];
		for (int i = 0; i < dim2; ++i) {
			for (int j = 0; j < dim1; ++j) {
				dbIndVals[i][j] = indVals[j][i];
			}
		}
		synchronized(this) {
			ConnectionInfo ci = getConnection();
			Connection conn = ci.getConnection();
			try {
				synchronized(conn) {
					Class<?> ratingObjClass = Class.forName("cwmsdb.CwmsRatingJdbc");
					Object cwmsRatingObj = ratingObjClass.getConstructor(Connection.class).newInstance(conn); 
					if (cwmsRatingObj == null) {
						throw new RatingException("No database rating implemenation found");
					}
					Method rateMethod = ratingObjClass.getMethod(
							"rate",
							String.class,
							String.class,
							String[].class,
							double[][].class,
							long[].class,
							long.class);
					
					double[] results = (double[])rateMethod.invoke(
							cwmsRatingObj,
							ratingSpecId, 
							officeId, 
							dataUnits == null ? ratingUnits : dataUnits, 
							dbIndVals, 
							valTimes, 
							ratingTime);
					
					return results;
				}
			}
			catch (Exception e) {
				if (e instanceof RatingException) throw (RatingException)e;
				throw new RatingException(e);
			}
			finally {
				releaseConnection(ci);
			}
		}
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#rate(hec.io.TimeSeriesContainer)
	 */
	@Override
	public TimeSeriesContainer rate(TimeSeriesContainer tsc) throws RatingException {
		TimeSeriesContainer[] tscs = {tsc};
		return rate(tscs);
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#rate(hec.io.TimeSeriesContainer[])
	 */
	@Override
	public TimeSeriesContainer rate(TimeSeriesContainer[] tscs) throws RatingException {
		synchronized(this) {
			if (tsRater == null) {
				tsRater = new TimeSeriesRater(this);
			}
			return tsRater.rate(tscs);
		}
	}
	/* (non-Javadoc)
	 * @see hec.data.IRating#rate(hec.hecmath.TimeSeriesMath)
	 */
	@Override
	public TimeSeriesMath rate(TimeSeriesMath tsm) throws RatingException {
		try {
			return new TimeSeriesMath(rate((TimeSeriesContainer)tsm.getData()));
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
		TimeSeriesContainer[] tscs = new TimeSeriesContainer[tsms.length];
		try {
			for (int i = 0; i < tsms.length; ++i) {
				tscs[i] = (TimeSeriesContainer)tsms[i].getData();
			}
			return new TimeSeriesMath(rate(tscs));
		}
		catch (Throwable t) {
			if (t instanceof RatingException) throw (RatingException) t;
			throw new RatingException(t);
		}
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#reverseRate(double)
	 */
	@Override
	public double reverseRate(double depVal) throws RatingException {
		long[] valTimes = {getDefaultValueTime()};
		double[] depVals = {depVal};
		return reverseRate(valTimes, depVals)[0];
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#reverseRate(double[])
	 */
	@Override
	public double[] reverseRate(double[] depVals) throws RatingException {
		long[] valTimes = new long[depVals.length];
		Arrays.fill(valTimes, getDefaultValueTime());
		return reverseRate(valTimes, depVals);
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#reverseRate(long, double)
	 */
	@Override
	public double reverseRate(long valTime, double depVal) throws RatingException {
		long[] valTimes = {valTime};
		double[] depVals = {depVal};
		return reverseRate(valTimes, depVals)[0];
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#reverseRate(long, double[])
	 */
	@Override
	public double[] reverseRate(long valTime, double[] depVals) throws RatingException {
		long[] valTimes = new long[depVals.length];
		Arrays.fill(valTimes, valTime);
		return reverseRate(valTimes, depVals);
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#reverseRate(long[], double[])
	 */
	@Override
	public double[] reverseRate(long[] valTimes, double[] depVals)	throws RatingException {
		synchronized(this) {
			ConnectionInfo ci = getConnection();
			Connection conn = ci.getConnection();
			try {
				synchronized(conn) {
					Class<?> ratingObjClass = Class.forName("cwmsdb.CwmsRatingJdbc");
					Object cwmsRatingObj = ratingObjClass.getConstructor(Connection.class).newInstance(conn); 
					Method rateMethod = ratingObjClass.getMethod(
							"reverseRate",
							String.class,
							String.class,
							String[].class,
							double[].class,
							long[].class,
							long.class);
					
					double[] results = (double[])rateMethod.invoke(
							cwmsRatingObj,
							ratingSpecId, 
							officeId, 
							dataUnits == null ? ratingUnits : dataUnits, 
							depVals, 
							valTimes, 
							ratingTime);
					
					return results;
				}
			}
			catch (Exception e) {
				if (e instanceof RatingException) throw (RatingException)e;
				throw new RatingException(e);
			}
			finally {
				releaseConnection(ci);
			}
		}
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#reverseRate(hec.io.TimeSeriesContainer)
	 */
	@Override
	public TimeSeriesContainer reverseRate(TimeSeriesContainer tsc) throws RatingException {
		synchronized(this) {
			if (tsRater == null) {
				tsRater = new TimeSeriesRater(this);
			}
			return tsRater.reverseRate(tsc);
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
	 * @see hec.data.IRating#getIndParamCount()
	 */
	@Override
	public int getIndParamCount() throws RatingException {
		synchronized(this) {
			return parameters.length - 1;
		}
	}
	
	protected void checkVDC() throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Rating has no vertical datum information.");
	}

}
