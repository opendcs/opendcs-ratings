/**
 * 
 */
package hec.data.cwmsRating;

import static hec.data.cwmsRating.RatingConst.SEPARATOR2;
import static hec.data.cwmsRating.RatingConst.SEPARATOR3;
import hec.data.RatingException;
import hec.data.Units;
import hec.data.UnitsConversionException;
import hec.data.cwmsRating.io.AbstractRatingContainer;
import hec.data.cwmsRating.io.SourceRatingContainer;
import hec.data.cwmsRating.io.VirtualRatingContainer;
import hec.lang.Observable;
import hec.util.TextUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rating that is comprised of other ratings and math expressions connected 
 * in such a way to form a new rating
 * 
 * @author Mike Perryman
 */
public class VirtualRating extends AbstractRating {
	
	/**
	 * The string specifying how the inputs, source ratings, and output are connected
	 */
	protected String connectionsString = null;
	
	protected Map<String, Set<String>> connectionsMap = null;
	
	protected SourceRating[] sourceRatings = null;
	
	protected String[] dataUnits;
	
	protected String[] ratingUnits;
	
	protected String depParamConn = null;

	protected static void parseConnectionPoint(String connectionPoint, int[] results) {
		final Pattern p = Pattern.compile("(R(\\d+))?(I(\\d+)|D)");
		Matcher m = p.matcher(connectionPoint);
		if (m.matches()) {
			try {
				results[0] = Integer.parseInt(m.group(2))-1;
			}
			catch (Throwable t) {
				results[0] = -1;
			}
			try {
				results[1] = Integer.parseInt(m.group(4))-1;
			} 
			catch (Throwable t) {
				results[1] = -1;
			}
		}
	}
	/**
	 * Default constructor
	 */
	public VirtualRating() {
		init();
	}
	/**
	 * Constructor from VirtualRatingContainer object
	 * @param vrc The VirtualRatingContainer object to construct from
	 * @throws RatingException
	 */
	public VirtualRating(VirtualRatingContainer vrc) throws RatingException {
		init();
		setData(vrc);
	}
	/**
	 * performs common initialization tasks
	 */
	protected void init() {
		if (observationTarget == null) observationTarget = new Observable();
	}
	/**
	 * Sets the connections map from a connections string. Also sets the rating units
	 * @param connections The connections string to set the connections map from
	 * @throws RatingException
	 */
	public void setConnections(String connections) throws RatingException {
		//----------------------------------------------------------------//
		// map the specified connections - map both directions because we //
		// don't yet know which direction they will be used, or even if   //
		// they can be used bi-directionally (i.e., reverseRate())        //
		//----------------------------------------------------------------//
		Map<String, Set<String>> connMap = new HashMap<String, Set<String>>();
		for (String pair : connections.trim().toUpperCase().split("\\s*,\\s*")) {
			String[] parts = pair.split("\\s*=\\s*");
			if (parts.length != 2) {
				throw new RatingException("Invalid connections string: " + connections);
			}
			if (!connMap.containsKey(parts[0])) {
				connMap.put(parts[0], new HashSet<String>());
			}
			if (!connMap.get(parts[0]).add(parts[1])) {
				AbstractRating.logger.warning(String.format("Connection %s specified more than once", pair));
			}
			if (!connMap.containsKey(parts[1])) {
				connMap.put(parts[1], new HashSet<String>());
			}
			if (!connMap.get(parts[1]).add(parts[0])) {
				AbstractRating.logger.warning(String.format("Connection %s specified more than once", pair));
			}
		}
		//--------------------------------------------//
		// take note of unspecified connection points //
		//--------------------------------------------//
		List<String> unconnectedList = new ArrayList<String>();
		String connectionPoint = null;;
		for (int r = 0; r < sourceRatings.length; ++r) {
			for (int p = 0; p < sourceRatings[r].getIndParamCount(); ++p) {
				connectionPoint = String.format("R%dI%d", r+1, p+1);
				if (!connMap.containsKey(connectionPoint)) {
					unconnectedList.add(connectionPoint);
				}
			}
			connectionPoint = String.format("R%dD", r+1);
			if (!connMap.containsKey(connectionPoint)) {
				unconnectedList.add(connectionPoint);
			}
		}
		//-------------------------------------------------------------//
		// add the default connections from the independent parameters //
		//-------------------------------------------------------------//
		Set<String> unconnectedSet = new HashSet<String>(unconnectedList);
		for (int i = 0; i < Math.min(unconnectedList.size(), getIndParamCount()); ++i) {
			String indParam = String.format("I%d", i+1);
			connectionPoint = unconnectedList.get(i);
			connMap.put(connectionPoint, new HashSet<String>());
			connMap.get(connectionPoint).add(indParam);
			if (!connMap.containsKey(indParam)) {
				connMap.put(indParam, new HashSet<String>());
			}
			connMap.get(indParam).add(connectionPoint);
			unconnectedSet.remove(connectionPoint);
		}
		if (unconnectedSet.size() < 1) {
			throw new RatingException("Virtual rating is over-connected");
		}
		else if (unconnectedSet.size() > 1) {
			throw new RatingException("Virtual rating is under-connected");
		}
		//---------------------------------------------------------------//
		// verify that all independent params are accounted for and that //
		// we have exactly one unspecified connection point remaining    //
		//---------------------------------------------------------------//
		for (int i = 0; i < getIndParamCount(); ++i) {
			if (!connMap.containsKey(String.format("I%d", i+1))) {
				throw new RatingException(String.format("Independent parameter %s is not connected", i+1));
			}
		}
		//------------------------------------------------------------------------//
		// verify that all independent parameters lead to the dependent parameter //
		//------------------------------------------------------------------------//
		depParamConn = unconnectedSet.iterator().next();
		StringBuffer sb = new StringBuffer();
		int[] cpInfo = {-1, -1};
		int r = cpInfo[0];
		int p = cpInfo[1];
		for (int i = 0; i < this.getIndParamCount(); ++i) {
			String indParam = String.format("I%d", i+1);
			if (!depParamConn.equals(walkConnections(connMap, indParam))) {
				throw new RatingException(String.format("Connection path does not connect rating independent parameter %d with rating dependend parameter (%s)", i+1, depParamConn));
			}
			parseConnectionPoint(connMap.get(indParam).iterator().next(), cpInfo);
			r = cpInfo[0];
			p = cpInfo[1] < 0 ? p = sourceRatings[r].getIndParamCount() : cpInfo[1];
			sb.append(i == 0 ? "" : SEPARATOR3).append(sourceRatings[r].ratingUnits[p]);
		}
		parseConnectionPoint(depParamConn, cpInfo);
		r = cpInfo[0];
		p = cpInfo[1] < 0 ? p = sourceRatings[r].ratingUnits.length-1 : cpInfo[1];
		sb.append(SEPARATOR2).append(sourceRatings[r].ratingUnits[p]);
		setRatingUnitsId(sb.toString());
		connectionsMap = connMap;
		connectionsString = connections;
	}
	/**
	 * Traverses a connections path from a specified starting location to the end, and returns the end point of the path
	 * @param map The connections path to traverse
	 * @param startingPoint The connection point to start traversing from
	 * @return The end point of the connections path starting from the starting location
	 * @throws RatingException
	 */
	protected String walkConnections(Map<String, Set<String>>map, String startingPoint)
			throws RatingException {
		String endingPoint = null;
		Set<String> connections = map.get(startingPoint);
		if (connections == null) {
			endingPoint = startingPoint;
		}
		else {
			int[] cpInfo = {-1, -1};
			VirtualRating.parseConnectionPoint(startingPoint, cpInfo);
			int startingPointRating = cpInfo[0];
			List<String> endingPoints = new ArrayList<String>();
			for (Iterator<String> it = connections.iterator(); it.hasNext();) {
				String connectionPoint = it.next();
				String connectionPoint1 = null;
				VirtualRating.parseConnectionPoint(connectionPoint, cpInfo);
				int r = cpInfo[0];
				int p = cpInfo[1];
				if (r < 0) {
					throw new RatingException("Connection path leads to independent parameter");
				}
				else if (r >= sourceRatings.length) {
					throw new RatingException("Connection path specifies rating beyond source rating count");
				}
				else if (r == startingPointRating) {
					throw new RatingException("Connection path specifies a connection from one rating to itself");
				}
				if (p < 0) {
					if (sourceRatings[r].mathExpression != null) {
						throw new RatingException("Connection path would reverse throgh a math expression");
					}
					else if (sourceRatings[r].getIndParamCount() > 1) {
						throw new RatingException("Connection path would reverse through a multiple-independent-parameter rating");
					}
					connectionPoint1 = String.format("R%dI1", r+1);
				}
				else {
					connectionPoint1 = String.format("R%dD", r+1);
				}
				endingPoints.add(walkConnections(map, connectionPoint1));
				if (endingPoints.size() > 1 && !endingPoints.get(endingPoints.size()-1).equals(endingPoints.get(0))) {
					throw new RatingException(String.format("Connection point %s leads to multiple termination points"));
				}
				endingPoint = endingPoints.get(0);
			}
		}
		return endingPoint;
	}
	
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.ICwmsRating#toXmlString(java.lang.CharSequence, int)
	 */
	@Override
	public String toXmlString(CharSequence indent, int indentLevel)
			throws RatingException {
		return getData().toXml(indent, indentLevel);
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#getRatingExtents(long)
	 */
	@Override
	public double[][] getRatingExtents(long ratingTime) throws RatingException {
		throw new RatingException("getRatingExtents is not supported for virtual ratings");
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#rate(double)
	 */
	@Override
	public double rate(double indVal) throws RatingException {
		return rate(defaultValueTime, indVal);
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#rateOne(double[])
	 */
	@Override
	public double rateOne(double... indVals) throws RatingException {
		return rateOne2(indVals);
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#rateOne(double[])
	 */
	@Override
	public double rateOne2(double[] indVals) throws RatingException {
		long[] valTimes = new long[] {defaultValueTime};
		double[][] _indVals = new double[indVals.length][];
		for (int i = 0; i < indVals.length; ++i) {
			_indVals[i] = new double[] {indVals[i]};
		}
		return rate(valTimes, _indVals)[0];
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#rate(double[])
	 */
	@Override
	public double[] rate(double[] indVals) throws RatingException {
		return rate(defaultValueTime, indVals);
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#rate(double[][])
	 */
	@Override
	public double[] rate(double[][] indVals) throws RatingException {
		return rate(defaultValueTime, indVals);
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
	public double rateOne(long valTime, double... indVals)
			throws RatingException {
		return rateOne2(valTime, indVals);
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#rateOne(long, double[])
	 */
	@Override
	public double rateOne2(long valTime, double[] indVals) throws RatingException {
		long[] valTimes = new long[] {valTime};
		double[][] _indVals = new double[indVals.length][];
		for (int i = 0; i < indVals.length; ++i) {
			_indVals[i] = new double[] {indVals[i]};
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
		return rate(valTimes, new double[][] {indVals});
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
		if (indVals.length != getIndParamCount()) {
			throw new RatingException(String.format("Expected %d value sets, got %d", getIndParamCount(), indVals.length));
		}
		if (valTimes == null) {
			throw new RatingException("No value times supplied");
		}
		//------------------------------------------------------------------------//
		// populate connection point values with the independent parameter values //
		//------------------------------------------------------------------------//
		Map<String, double[]> cpValues = new HashMap<String, double[]>();
		String[] dataUnits = this.dataUnits == null ? getRatingUnits() : this.dataUnits;
		int[] cpInfo = {-1, -1};
		int r = cpInfo[0];
		int p = cpInfo[1];
		for (int i = 0; i < indVals.length; ++i) {
			if (indVals[i] == null) {
				throw new RatingException(String.format("Independent paramter %d has no values", i+1));
			}
			if (indVals[i].length != valTimes.length) {
				throw new RatingException("Inconsistent times and values arrays");
			}
			cpValues.put(String.format("I%d", i+1), indVals[i]);
		}
		//-----------------------------------------------------------------------//
		// process the map until we are left with the dependent parameter values //
		//-----------------------------------------------------------------------//
		Set<String> sources = new HashSet<String>(cpValues.keySet());
		while (!cpValues.containsKey(depParamConn)) {
			//---------------------------------------------------------------------//
			// populate connected connection points, converting units if necessary //
			//---------------------------------------------------------------------//
			String[] populated = cpValues.keySet().toArray(new String[0]);
			for (String source : populated) {
				VirtualRating.parseConnectionPoint(source, cpInfo);
				r = cpInfo[0];
				p = cpInfo[1] < 0 ? sourceRatings[r].dataUnits.length-1 : cpInfo[1];
				String srcUnit = r < 0 ? dataUnits[p] : sourceRatings[r].dataUnits[p];
				Set<String> dests = connectionsMap.get(source);
				for (Iterator<String> d = dests.iterator(); d.hasNext();) {
					String dest = d.next();
					if (sources.contains(dest)) {
						continue; // prevent reversing onto a previous source
					}
					sources.add(source);
					VirtualRating.parseConnectionPoint(dest, cpInfo);
					r = cpInfo[0];
					p = cpInfo[1] < 0 ? sourceRatings[r].dataUnits.length-1 : cpInfo[1];
					String dstUnit = sourceRatings[r].dataUnits[p];
					if (dstUnit.equals(srcUnit)) {
						cpValues.put(dest, cpValues.get(source));
					}
					else {
						double[] srcVals = cpValues.get(source); 
						double[] dstVals = Arrays.copyOf(srcVals, srcVals.length);
						try {
							Units.convertUnits(dstVals, srcUnit, dstUnit);
						}
						catch (UnitsConversionException e) {
							throw new RatingException(e);
						}
						cpValues.put(dest, dstVals);
					}
				}
			}
			//---------------------------------------------------------------//
			// remove the source connection point values from the population //
			//---------------------------------------------------------------//
			for (String source : sources) {
				cpValues.remove(source);
			}
			//-----------------------------------------------------------------//
			// process any source ratings that have all their inputs populated //
			//-----------------------------------------------------------------//
			for (r = 0; r < sourceRatings.length; ++r) {
				int count = sourceRatings[r].getIndParamCount();
				for (p = 0; p < count; ++p) {
					if (!cpValues.containsKey(String.format("R%dI%d", r+1, p+1))) {
						break;
					}
				}
				if (p == count) {
					//------------------------------------------------------------------//
					// forward rate and remove source rating inputs from the population //
					//------------------------------------------------------------------//
					double[][] _indVals = new double[count][];
					for (p = 0; p < count; ++p) {
						_indVals[p] = cpValues.get(String.format("R%dI%d", r+1, p+1));
					}
					cpValues.put(String.format("R%dD", r+1), sourceRatings[r].rate(valTimes, _indVals));
					for (p = 0; p < count; ++p) {
						cpValues.remove(String.format("R%dI%d", r+1, p+1));
					}
				}
				else if (cpValues.containsKey(String.format("R%dD", r+1))) {
					//------------------------------------------------------------------//
					// reverse rate and remove source rating inputs from the population //
					//------------------------------------------------------------------//
					double[] depVals = cpValues.get(String.format("R%dD", r+1));
					cpValues.put(String.format("R%dI1", r+1), sourceRatings[r].reverseRate(valTimes, depVals));
					cpValues.remove(String.format("R%dD", r+1));
				}
			}
		}
		return cpValues.get(depParamConn);
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#getData()
	 */
	@Override
	public AbstractRatingContainer getData() {
		VirtualRatingContainer vrc = new VirtualRatingContainer();
		getData(vrc);
		return vrc;
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#getData(hec.data.cwmsRating.io.AbstractRatingContainer)
	 */
	@Override
	protected void getData(AbstractRatingContainer arc) {
		if (!(arc instanceof VirtualRatingContainer)) {
			throw new UnsupportedOperationException("Virtual Ratings only support Virtual Rating Containers.");
		}
		VirtualRatingContainer vrc = (VirtualRatingContainer)arc;
		super.getData(vrc);
		vrc.connections = connectionsString;
		vrc.sourceRatings = new SourceRatingContainer[sourceRatings.length];
		for (int i = 0; i < sourceRatings.length; ++i) {
			vrc.sourceRatings[i] = sourceRatings[i].getData();
		}
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#setData(hec.data.cwmsRating.io.AbstractRatingContainer)
	 */
	@Override
	public void setData(AbstractRatingContainer arc) throws RatingException {
		if (!(arc instanceof VirtualRatingContainer)) {
			throw new RatingException("Expected VirtualRatingContainer, got " + arc.getClass().getName());
		}
		super._setData(arc);
		VirtualRatingContainer vrc = (VirtualRatingContainer)arc;
		sourceRatings = new SourceRating[vrc.sourceRatings.length];
		for (int i = 0; i < vrc.sourceRatings.length; ++i) {
			sourceRatings[i] = new SourceRating(vrc.sourceRatings[i]);
		}
		setConnections(vrc.connections);
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#reverseRate(long[], double[])
	 */
	@Override
	public double[] reverseRate(long[] valTimes, double[] depVals) throws RatingException {
		if (getIndParamCount() > 1) {
			throw new RatingException("Cannot reverse through a rating with more than one independent paramter");
		}
		for (int i = 0; i < sourceRatings.length; ++i) {
			if (sourceRatings[i].mathExpression != null) {
				throw new RatingException("Cannot reverse through a virtual rating that contains a math expression for a source rating");
			}
			if (sourceRatings[i].getIndParamCount() > 1) {
				throw new RatingException("Cannot reverse through a virutual rating that contains a source rating with more than one independent paramter");
			}
		}
		//-------------------//
		// prime the process //
		//-------------------//
		double[][] cpValues = new double[2][];
		int unrated = 1;
		int rated = 0;
		int[] cpInfo = {-1, -1};
		int r = cpInfo[0];
		int p = cpInfo[1];
		String ratedConn = depParamConn;
		String unratedConn = null;
		String terminus = connectionsMap.get("I1").iterator().next();
		String[] dataUnits = this.dataUnits == null ? getRatingUnits() : this.dataUnits;
		String ratedUnit = dataUnits[dataUnits.length-1];
		String unratedUnit = null;
		cpValues[0] = depVals;
		boolean first = true;
		//--------------------------------------------------------------------------------//
		// This is a simpler version of the the process used in rate(long[], double[][]). //
		// In reverse rating we know that there is only one independent value for any/all //
		// source ratings and for the virtual rating as a whole.                          //
		//--------------------------------------------------------------------------------//
		//--------------------------------------------------------------------//
		// assign values and rate until we get to the ending connection point //
		//--------------------------------------------------------------------//
		while (!ratedConn.equals(terminus)) {
			//------------------------------------//
			// swap the rated and unrated indexes //
			//------------------------------------//
			rated = ++rated % 2;
			unrated = ++unrated % 2;
			//---------------------------------------------//
			// get the unrated connection points and units //
			//---------------------------------------------//
			if (first) {
				//---------------------------------------//
				// special operation for first pass only //
				//---------------------------------------//
				first = false;
				unratedConn = ratedConn;
				parseConnectionPoint(depParamConn, cpInfo);
				r = cpInfo[0];
				p = cpInfo[1];
				unratedUnit = sourceRatings[r].dataUnits[p < 0 ? 1 : p];
				ratedUnit = dataUnits[dataUnits.length-1];
			}
			else {
				//------------------//
				// normal operation //
				//------------------//
				parseConnectionPoint(ratedConn, cpInfo);
				r = cpInfo[0];
				p = cpInfo[1];
				if (r < 0) {
					ratedUnit = dataUnits[p];
				}
				else {
					ratedUnit = sourceRatings[r].dataUnits[p < 0 ? 1 : p];
				}
				unratedConn = connectionsMap.get(ratedConn).iterator().next();
				parseConnectionPoint(unratedConn, cpInfo);
				r = cpInfo[0];
				p = cpInfo[1];
				unratedUnit = sourceRatings[r].dataUnits[p < 0 ? 1 : p];
			}
			//----------------------------------------------//
			// convert the unrated values unit if necessary //
			//----------------------------------------------//
			if (!unratedUnit.equals(ratedUnit)) {
				double[] values = Arrays.copyOf(cpValues[unrated], cpValues[unrated].length);
				try {
					Units.convertUnits(values, ratedUnit, unratedUnit);
				}
				catch (UnitsConversionException e) {
					throw new RatingException(e);
				}
				cpValues[unrated] = values;
			}
			//----------------------------------------------------------------//
			// rate the unrated values and updated the rated connection point //
			//----------------------------------------------------------------//
			if (p < 0) {
				cpValues[rated] = sourceRatings[r].reverseRate(valTimes, cpValues[unrated]);
				ratedConn = String.format("R%dI1", r+1);
			}
			else {
				double[][] values = new double[cpValues[unrated].length][];
				for (int i = 0; i < cpValues[unrated].length; ++i) {
					values[i] = new double[] {cpValues[unrated][i]};
				}
				cpValues[rated] = sourceRatings[r].rate(valTimes, values);
				ratedConn = String.format("R%dD", r+1);
			}
		}
		//-------------------------------------------------//
		// perform the final units conversion if necessary //
		//-------------------------------------------------//
		VirtualRating.parseConnectionPoint(terminus, cpInfo);
		r = cpInfo[0];
		p = cpInfo[1];
		ratedUnit = sourceRatings[r].dataUnits[p < 0 ? sourceRatings[r].dataUnits.length-1 : p];
		unratedUnit = dataUnits[0];
		if (!ratedUnit.equals(unratedUnit)) {
			try {
				Units.convertUnits(cpValues[rated], ratedUnit, unratedUnit);
			}
			catch (UnitsConversionException e) {
				throw new RatingException(e);
			}
		}
		return cpValues[rated];
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#getValues(java.lang.Integer)
	 */
	@Override
	public RatingValue[] getValues(Integer defaultInterval) {
		throw new UnsupportedOperationException("getValues is not supported for virtual ratings");
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#getInstance(hec.data.cwmsRating.io.AbstractRatingContainer)
	 */
	@Override
	public AbstractRating getInstance(AbstractRatingContainer ratingContainer) throws RatingException {
		if (!(ratingContainer instanceof VirtualRatingContainer)) {
			throw new UnsupportedOperationException("Virtual Ratings only support Virtual Rating Containers.");
		}
		return new VirtualRating((VirtualRatingContainer)ratingContainer);
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#setDataUnitsId(java.lang.String)
	 */
	@Override
	public void setDataUnitsId(String dataUnitsId) {
		super.setDataUnitsId(dataUnitsId);
		dataUnits = TextUtil.split(dataUnitsId.replace(SEPARATOR2, SEPARATOR3), SEPARATOR3);
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#setDataUnits(java.lang.String[])
	 */
	@Override
	public void setDataUnits(String[] units) throws RatingException {
		super.setDataUnits(units);
		dataUnits = Arrays.copyOf(units, units.length);
	}
}
