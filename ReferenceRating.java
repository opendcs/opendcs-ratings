/**
 * 
 */
package hec.data.cwmsRating;

import static hec.data.cwmsRating.RatingConst.SEPARATOR1;
import static hec.data.cwmsRating.RatingConst.SEPARATOR2;
import static hec.data.cwmsRating.RatingConst.SEPARATOR3;
import static hec.lang.Const.UNDEFINED_LONG;
import static hec.lang.Const.UNDEFINED_TIME;

import java.lang.reflect.Method;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

import hec.data.DataSetIllegalArgumentException;
import hec.data.IRating;
import hec.data.IVerticalDatum;
import hec.data.Parameter;
import hec.data.RatingException;
import hec.data.Units;
import hec.data.VerticalDatumException;
import hec.data.rating.JDomRatingSpecification;
import hec.data.rating.ParameterValues;
import hec.data.rating.RatingInput;
import hec.data.rating.RatingOutput;
import hec.data.rating.ReverseRatingInput;
import hec.db.DbConnectionException;
import hec.hecmath.TimeSeriesMath;
import hec.io.TimeSeriesContainer;
import hec.io.VerticalDatumContainer;
import hec.lang.Reflection;
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
	
	protected RatingSet ratingSet = null;
	
	protected ReferenceRating() {} // no public default constructor
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
		try {
			if (units.length != parameters.length) {
				throw new RatingException(String.format("Expected %d units, got %d instead.", parameters.length, units.length));
			}
			Connection conn = ratingSet.getConnection();
			if (conn == null) {
				throw new RatingException("No database connections available");
			}
			try {
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
			}
			finally {
				conn.close();
			}
			dataUnits = Arrays.copyOf(units, units.length);
		}
		catch (Exception e) {
			if (e instanceof RatingException) throw (RatingException)e;
			throw new RatingException(e);
		}
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#getDefaultValueTime()
	 */
	@Override
	public long getDefaultValueTime() {
		return defaultValueTime == UNDEFINED_TIME ? System.currentTimeMillis() : defaultValueTime;
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#setDefaultValueTime(long)
	 */
	@Override
	public void setDefaultValueTime(long defaultValueTime) {
		this.defaultValueTime = defaultValueTime;
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#resetDefaultValuetime()
	 */
	@Override
	public void resetDefaultValuetime() {
		defaultValueTime = UNDEFINED_TIME;
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#getRatingTime()
	 */
	@Override
	public long getRatingTime() {
		return ratingTime;
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#setRatingTime(long)
	 */
	@Override
	public void setRatingTime(long ratingTime) {
		this.ratingTime = ratingTime;
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#resetRatingTime()
	 */
	@Override
	public void resetRatingTime() {
		ratingTime = System.currentTimeMillis() + 100 * 365 * 86400 * 1000; 
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#getRatingExtents()
	 */
	@Override
	public double[][] getRatingExtents() throws RatingException {
		return getRatingExtents(getRatingTime());
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#getRatingExtents(long)
	 */
	@Override
	public double[][] getRatingExtents(long ratingTime) throws RatingException {
		double[][][] extents = null;
//		if (ratingExtentsMethod == null) {
//			getRatingExtentsMethod();
//		}
//		extents = new double[1][][];
//		String[][] parameters = new String[1][];
//		String[][] units = new String[1][];
//		try {
//			ratingExtentsMethod.invoke(
//					cwmsRatingObj,
//					ratingSpecId,
//					officeId,
//					true,
//					ratingTime,
//					extents,
//					parameters,
//					units);
//		}
//		catch (IllegalAccessException | IllegalArgumentException
//				| InvocationTargetException e) {
//			throw new RatingException(e);
//		}
		return extents[0];
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#getEffectiveDates()
	 */
	@Override
	public long[] getEffectiveDates() {
		long[] dates = null;
		try {
			Connection conn = ratingSet.getConnection();
			if (conn == null) {
				throw new RatingException("No database connections available");
			}
			try {
				synchronized(conn) {
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
			finally {
				conn.close();
			}
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		return dates;
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#getCreateDates()
	 */
	@Override
	public long[] getCreateDates() {
		long[] dates = null;
		try {
			Connection conn = ratingSet.getConnection();
			if (conn == null) {
				throw new RatingException("No database connections available");
			}
			try {
				synchronized(conn) {
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
			finally {
				conn.close();
			}
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		return dates;
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
		Object cwmsRatingObj;
		Method rateMethod;
		Connection conn = null;
		try {
			conn = ratingSet.getConnection();
		}
		catch (RatingException e1) {}
		if (conn == null) {
			throw new RatingException("No database connections available");
		}
		try {
			synchronized(conn) {
				cwmsRatingObj = Reflection.getConstructor(
						"wcds.dbi.oracle.OracleCwmsRatingDaoImpl", 
						"java.sql.Connection").newInstance(conn);
				rateMethod = cwmsRatingObj.getClass().getMethod(
						"rate",
						Reflection.getClass("hec.data.rating.RatingInput"),
						Reflection.getClass("hec.data.rating.RatingOutput"));
					
					Calendar ratingTime = Calendar.getInstance();
					ratingTime.setTimeZone(TimeZone.getTimeZone("UTC"));
					ratingTime.setTimeInMillis(this.ratingTime);
					Calendar cal = Calendar.getInstance();
					cal.setTimeZone(TimeZone.getTimeZone("UTC"));
					Vector<Date> valueTimes = new Vector<Date>();
					if (valTimes != null) {
						for (long valTime : valTimes) {
							cal.setTimeInMillis(valTime);
							valueTimes.add(cal.getTime());
						}
					}
					Vector<ParameterValues> indValues = new Vector<ParameterValues>();
					String[] indParamIds = TextUtil.split(TextUtil.split(TextUtil.split(templateId, SEPARATOR1)[0], SEPARATOR2)[0], SEPARATOR3);
					for (int i = 0; i < indParamIds.length; ++i) {
						indValues.add(new ParameterValues(new Parameter(indParamIds[i]), indVals[i]));
					}
					Vector<String> indUnits = new Vector<String>();
					String[] units = dataUnits == null ? ratingUnits : dataUnits;
					for (int i = 0; i < units.length-1; ++i) {
						indUnits.add(units[i]);
					}
					RatingInput input = new RatingInput(
							new JDomRatingSpecification(officeId, ratingSpecId),
							ratingTime.getTime(),
							valueTimes,
							indValues,
							indUnits,
							units[units.length-1],
							false);
					RatingOutput output = (RatingOutput)rateMethod.invoke(
							cwmsRatingObj,
							input,
							null); 
					return output.getValues().getDoubleArray();
			}
		}
		catch (Exception e) {
			if (e instanceof RatingException) throw (RatingException)e;
			throw new RatingException(e);
		}
		finally {
			try {conn.close();}
			catch (SQLException e) {}
		}
//		try {
//			rated = (double[])rateMethod.invoke(
//					cwmsRatingObj, 
//					ratingSpecId, 
//					officeId, 
//					getDataUnits(), 
//					indVals, 
//					valTimes, 
//					ratingTime);
//		}
//		catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
//			throw new RatingException(e);
//		}
//		return rated;
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
		if (tsRater == null) {
			tsRater = new TimeSeriesRater(this);
		}
		return tsRater.rate(tscs);
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
		Connection conn = null;
		try {
			conn = ratingSet.getConnection();
		}
		catch (RatingException e1) {}
		if (conn == null) {
			throw new RatingException("No database connections available");
		}
		try {
			synchronized(conn) {
				Class<?> cwmsRatingClass = Class.forName("wcds.dbi.oracle.OracleCwmsRatingDaoImpl");
				Object cwmsRatingObj = cwmsRatingClass.getConstructor(java.sql.Connection.class).newInstance(conn);
				Calendar ratingTime = Calendar.getInstance();
				ratingTime.setTimeZone(TimeZone.getTimeZone("UTC"));
				ratingTime.setTimeInMillis(this.ratingTime);
				Calendar cal = Calendar.getInstance();
				cal.setTimeZone(TimeZone.getTimeZone("UTC"));
				Vector<Date> valueTimes = new Vector<Date>();
				if (valTimes != null) {
					for (long valTime : valTimes) {
						cal.setTimeInMillis(valTime);
						valueTimes.add(cal.getTime());
					}
				}
				String depParamId = TextUtil.split(TextUtil.split(templateId, SEPARATOR1)[0], SEPARATOR2)[1];
				ParameterValues depValues = new ParameterValues(new Parameter(depParamId), depVals);
				String[] units = dataUnits == null ? ratingUnits : dataUnits;
				ReverseRatingInput input = new ReverseRatingInput(
						new JDomRatingSpecification(officeId, ratingSpecId),
						ratingTime.getTime(),
						valueTimes,
						depValues,
						units[1],
						units[0],
						false);
				RatingOutput output = (RatingOutput)cwmsRatingClass.getMethod("reverseRateSimple", ReverseRatingInput.class, RatingOutput.class).invoke(
						cwmsRatingObj,
						input,
						(RatingOutput)null);
				return output.getValues().getDoubleArray();
			}
		}
		catch (Exception e) {
			if (e instanceof RatingException) throw (RatingException)e;
			throw new RatingException(e);
		}
		finally {
			try {conn.close();}
			catch (SQLException e) {}
		}
//		double[] rated = null;
//		if (reverseRateMethod == null) {
//			getReverseRateMethod();
//		}
//		try {
//			rated = (double[])reverseRateMethod.invoke(
//					cwmsRatingObj, 
//					ratingSpecId, 
//					officeId, 
//					getDataUnits(), 
//					depVals, 
//					valTimes, 
//					ratingTime);
//		}
//		catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
//			throw new RatingException(e);
//		}
//		return rated;
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#reverseRate(hec.io.TimeSeriesContainer)
	 */
	@Override
	public TimeSeriesContainer reverseRate(TimeSeriesContainer tsc) throws RatingException {
		if (tsRater == null) {
			tsRater = new TimeSeriesRater(this);
		}
		return tsRater.reverseRate(tsc);
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
		return parameters.length - 1;
	}
	
	protected void checkVDC() throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Rating has no vertical datum information.");
	}

//	protected void getRatingExtentsMethod() throws RatingException {
//		if (cwmsRatingObj == null) {
//			getRatingObj();
//		}
//		try {
//			ratingExtentsMethod = cwmsRatingObj.getClass().getMethod(
//					"getRatingExtents", 
//					Reflection.getClass("hec.data.rating.IRatingExtents"),
//					Reflection.getClass("java.util.Date"));
//		}
//		catch (Exception e) {
//			throw new RatingException(e);
//		}
//	}
}
