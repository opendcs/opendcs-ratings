/**
 * 
 */
package hec.data.cwmsRating;

import static hec.data.cwmsRating.RatingConst.SEPARATOR2;
import static hec.data.cwmsRating.RatingConst.SEPARATOR3;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import hec.data.RatingException;
import hec.data.RatingRuntimeException;
import hec.data.Units;
import hec.data.UnitsConversionException;
import hec.data.cwmsRating.io.AbstractRatingContainer;
import hec.data.cwmsRating.io.SourceRatingContainer;
import hec.data.cwmsRating.io.VirtualRatingContainer;
import hec.lang.Observable;
import hec.util.TextUtil;

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
	
	protected boolean isNormalized = false;

	String[][]inputs = null;

	String[] outputs = null;
	
	protected static void parseConnectionPoint(String connectionPoint, int[] results) {
//		Matcher m = p.matcher(connectionPoint);
//		if (m.matches()) {
//			try {
//				results[0] = Integer.parseInt(m.group(2))-1;
//			}
//			catch (Throwable t) {
//				results[0] = -1;
//			}
//			try {
//				results[1] = Integer.parseInt(m.group(4))-1;
//			} 
//			catch (Throwable t) {
//				results[1] = -1;
//			}
//		}
		//----------------------------------------------------//
		// NOTE : The code below is much faster than the code //
		//        above but is restricted 9 source ratings    //
		//        and 9 independent parameters for each (so   //
		//        not *really* a restriction).                //
		//----------------------------------------------------//
		results[0] = results[1] = -1;
		switch (connectionPoint.charAt(0)) {
		case 'I' :
			results[1] = connectionPoint.codePointAt(1) - '1';
			break;
		case 'R' :
			results[0] = connectionPoint.codePointAt(1) - '1';
			switch (connectionPoint.charAt(2)) {
			case 'I' :
				results[1] = connectionPoint.codePointAt(3) - '1';
				break;
			}
			break;
		}
	}
	/**
	 * Returns the COMPLETE connections string for the connection map and dependent parameter connection
	 * @param connMap the connection map
	 * @param depParamConn the dependent parameter connection
	 * @return
	 */
	public static String getConnectionsComplete(Map<String, Set<String>> connMap, String depParamConn) {
		Set<Set<String>> conns = new HashSet<Set<String>>();
		for (String conn1 : connMap.keySet()) {
			for (String conn2 : connMap.get(conn1)) {
				Set<String> set = new HashSet<String>();
				set.add(conn1);
				set.add(conn2);
				conns.add(set);
			}
		}
		String[][] connArray = new String[conns.size()][];
		int i = 0;
		for (Set<String> conn : conns) {
			connArray[i] = conn.toArray(new String[2]);
			Arrays.sort(connArray[i]);
			if (connArray[i][0].endsWith("D")) {
				String temp = connArray[i][0];
				connArray[i][0] = connArray[i][1];
				connArray[i][1] = temp;
			}
			++i;
		}
		Arrays.sort(connArray, new Comparator<String[]>(){
			@Override
			public int compare(String[] arg0, String[] arg1) {
				int result = arg0[0].compareTo(arg1[0]);
				if (result == 0) {
					result = arg0[1].compareTo(arg1[1]);
				}
				return result;
			}
		});
		StringBuilder sb = new StringBuilder();
		for (String[] conn : connArray) {
			if (conn[0].indexOf('I') == 0) {
				sb.append(conn[1]).append("=").append(conn[0]).append(",");
			}
			else {
				sb.append(conn[0]).append("=").append(conn[1]).append(",");
			}
		}
		sb.append("D=").append(depParamConn);
		return sb.toString();
	}
	/**
	 * Returns the NORMALIZED connections string for the connection map and dependent parameter connection
	 * @param connMap the connection map
	 * @param depParamConn the dependent parameter connection
	 * @return
	 */
	public static String getConnectionsNormalized(Map<String, Set<String>> connMap, String depParamConn) {
		String completeConnections = VirtualRating.getConnectionsComplete(connMap, depParamConn);
		Map<String, String> skipped = new HashMap<String, String>();
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String pair : TextUtil.split(completeConnections, ",")) {
			String[] parts = TextUtil.split(pair, "=");
			if (parts[1].charAt(0) == 'I') { 
				if (!skipped.containsKey(parts[1])) {
					skipped.put(parts[1], parts[0]);
					continue;
				}
			}
			else if (parts[0].equals("D")) {
				continue;
			}
			if (first) {
				first = false;
			}
			else {
				sb.append(",");
			}
			sb.append(pair);
		}
		StringBuilder re_added = new StringBuilder();
		if (skipped.size() > 1) {
			boolean done = false;
			String[] inputs = skipped.keySet().toArray(new String[skipped.size()]);
			String[] conns = skipped.values().toArray(new String[skipped.size()]);
			skipped.clear();
			for (int i = 0; i < conns.length; ++i) {
				skipped.put(conns[i], inputs[i]);
			}
			do {
				conns = skipped.keySet().toArray(new String[skipped.size()]);
				inputs = skipped.values().toArray(new String[skipped.size()]);
				Arrays.sort(inputs);
				Arrays.sort(conns);
				done = true;
				for (int i = 0; i < conns.length; ++i) {
					if (inputs[i] != skipped.get(conns[i])) {
						if (re_added.length() > 0) {
							re_added.append(",");
						}
						re_added.append(conns[i]).append("=").append(skipped.get(conns[i]));
						skipped.remove(conns[i]);
						done = false;
						break;
					}
				}
			} while(!done);
		}
		if (re_added.length() > 0) {
			re_added.append(",").append(sb);
			sb = re_added;
		}
		if (sb.length() == 0) {
			//---------------------------------------------//
			// special case - don't allow null connections //
			//---------------------------------------------//
			sb.append("R1I1=I1");
		}
		return sb.toString();
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
	 * Public constructor from XML text
	 * @param xmlText The XML text to initialize from
	 * @throws RatingException
	 */
	public VirtualRating(String xmlText) throws RatingException {
		setData(new VirtualRatingContainer(xmlText));
	}
	/**
	 * performs common initialization tasks
	 */
	protected void init() {
		synchronized(this) {
			if (observationTarget == null) observationTarget = new Observable();
		}
	}
	/**
	 * @return the connections string
	 */
	public String getConnections() {
		synchronized(this) {
			return connectionsString;
		}
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
		synchronized(this) {
			if (connections == null || connections.length() == 0) {
				this.connectionsString = null;
				this.connectionsMap = null;
			}
			else {
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
				if (connMap.containsKey("D")) {
					depParamConn = connMap.get("D").iterator().next();
				}
				else {
					depParamConn = null;
				}
				//--------------------------------------------//
				// take note of unspecified connection points //
				//--------------------------------------------//
				List<String> unconnectedList = new ArrayList<String>();
				String connectionPoint = null;
				for (int r = 0; r < sourceRatings.length; ++r) {
					for (int p = 0; p < sourceRatings[r].getIndParamCount(); ++p) {
						connectionPoint = "R"+(r+1)+"I"+(p+1);
						if (!connMap.containsKey(connectionPoint)) {
							unconnectedList.add(connectionPoint);
						}
					}
					connectionPoint = "R"+(r+1)+"D";
					if (!connMap.containsKey(connectionPoint)) {
						unconnectedList.add(connectionPoint);
					}
				}
				//-------------------------------------------------------------//
				// add the default connections from the independent parameters //
				//-------------------------------------------------------------//
				List<String> automaticConnections = new Vector<String>();
				if (unconnectedList.size() > (depParamConn == null ? 1 : 0)) {
					for (int i = 0; i < Math.min(unconnectedList.size(), getIndParamCount()); ++i) {
						String indParam = "I"+(i+1);
						connectionPoint = unconnectedList.get(0);
						if (!connMap.containsKey(connectionPoint)) {
							connMap.put(connectionPoint, new HashSet<String>());
						}
						connMap.get(connectionPoint).add(indParam);
						if (!connMap.containsKey(indParam)) {
							automaticConnections.add(indParam);
							connMap.put(indParam, new HashSet<String>());
						}
						connMap.get(indParam).add(connectionPoint);
						unconnectedList.remove(connectionPoint);
					}
				}
				if (depParamConn == null) {
					automaticConnections.add("D");
				}
				if (unconnectedList.size() > (depParamConn == null ? 1 : 0)) {
					StringBuilder sb = new StringBuilder("Virtual rating is under-connected : \n\tunconnected internal parameters are: ");
					boolean first = true;
					for (String unconnected : unconnectedList) {
						sb.append(first ? "" : ", ").append(unconnected);
						first = false;
					}
					sb.append("\n\tautomatic connections are: ");
					for (int i = 0; i < automaticConnections.size(); ++i) {
						sb.append(i > 0 ? ", " : "").append(automaticConnections.get(i));
					}
					throw new RatingException(sb.toString());
				}
				//---------------------------------------------------------------//
				// verify that all independent params are accounted for and that //
				// we have exactly one unspecified connection point remaining    //
				//---------------------------------------------------------------//
				for (int i = 0; i < getIndParamCount(); ++i) {
					if (!connMap.containsKey("I"+(i+1))) {
						throw new RatingException(String.format("Independent parameter %s is not connected", i+1));
					}
				}
				//------------------------------------------------------------------------//
				// verify that all independent parameters lead to the dependent parameter //
				//------------------------------------------------------------------------//
				if (depParamConn == null) {
					depParamConn = unconnectedList.get(0);
				}
				StringBuffer sb = new StringBuffer();
				int[] cpInfo = {-1, -1};
				int r = cpInfo[0];
				int p = cpInfo[1];
				for (int i = 0; i < this.getIndParamCount(); ++i) {
					String indParam = "I"+(i+1);
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
				isNormalized = false;
			}
		}
	}
	/**
	 * Arranges source ratings in deterministic order and removes unnecessary external connections
	 * @throws RatingException 
	 */
	public void normalize() throws RatingException {
		synchronized(this) {
			//---------------------------------------//
			// determine the new source rating order //
			//---------------------------------------//
			Pattern pat = Pattern.compile("(R|I)(\\d+)(D|I(\\d+))");
			int[] newPos = new int[sourceRatings.length];
			Arrays.fill(newPos, -1);
			//
			// put the rating for external dependent connection last
			//
			Matcher m = pat.matcher(depParamConn);
			if (!m.matches()) {
				throw new RatingException("Unexpected dependent parameter connection: " + depParamConn);
			}
			int dep = Integer.parseInt(m.group(2)) - 1;
			newPos[dep] = sourceRatings.length-1;
			//
			// put the ratings for external independent connections first
			//
			int nextPos = 0;
			for (int i = 0; i < getIndParamCount(); ++i) {
				String input = "I"+(i+1);
				Set<String> set = connectionsMap.get(input);
				if (set != null) {
					String[] conns = set.toArray(new String[set.size()]);
					if (conns.length > 1) {
						// this is an expensive sort, but assures the same order based on source rating names 
						Arrays.sort(conns, new Comparator<String>() {
							@Override
							public int compare(String arg0, String arg1) {
								Matcher m = pat.matcher(arg0);
								if (m.matches()) {
									int i = Integer.parseInt(m.group(2)) - 1;
									m = pat.matcher(arg1);
									if (m.matches()) {
										int j = Integer.parseInt(m.group(2)) - 1;
										return sourceRatings[i].getName().compareTo(sourceRatings[j].getName());
									}
									else {
										// shouldn't happen
										return arg0.compareTo(arg1);
									}
								}
								else {
									// shouldn't happen
									return arg0.compareTo(arg1);
								}
							}
						});
					}
					for (int j = 0; j < conns.length; ++j) {
						m = pat.matcher(conns[j]);
						if (!m.matches()) {
							throw new RatingException("Unexpected independent parameter connection for "+input+": " + conns[0]);
						}
						int ind = Integer.parseInt(m.group(2)) - 1;
						if (newPos[ind] == -1) {
							newPos[ind]= nextPos++;
							break;
						}
					}
				}
			}
			//
			// put ratings for remaining connections in alphabetical order
			//
			Object[][] sourcePos = new Object[sourceRatings.length][2];
			for (int i = 0; i < sourceRatings.length; ++i) {
				sourcePos[i][0] = sourceRatings[i].getName();
				sourcePos[i][1] = new Integer(i);
			}
			Arrays.sort(sourcePos, new Comparator<Object[]>() {
				@Override
				public int compare(Object[] arg0, Object[] arg1) {
					String s0 = (String)arg0[0];
					String s1 = (String)arg1[0];
					return s0.compareTo(s1);
				}
			});
			for (int i = 0; i < sourcePos.length; ++i) {
				int pos = (Integer)sourcePos[i][1];
				if (newPos[pos] == -1) {
					newPos[pos] = nextPos++;
				}
			}
			//------------------------------------//
			// copy the connections map to modify //
			//------------------------------------//
			SourceRating[] newSourceRatings = new SourceRating[sourceRatings.length];
			Map<String, Set<String>> newConnectionsMap = new HashMap<String, Set<String>>();
			for (String conn1 : connectionsMap.keySet()) {
				HashSet<String> set = new HashSet<String>();
				for (String conn2 : (HashSet<String>)connectionsMap.get(conn1)) {
					set.add(conn2);
				}
				newConnectionsMap.put(conn1, set);
			}
			//------------------------------//
			// first pass - update position //
			//------------------------------//
			//
			// first update the keys
			//
			for (int i = 0; i < newPos.length; ++i) {
				newSourceRatings[newPos[i]] = sourceRatings[i];
				String _old = "R"+(i+1);
				String _new = "r"+(newPos[i]+1);
				for (String conn1 : newConnectionsMap.keySet().toArray(new String[newConnectionsMap.size()])) {
					if (conn1.startsWith(_old)) {
						HashSet<String>set = (HashSet<String>)newConnectionsMap.get(conn1);
						newConnectionsMap.remove(conn1);
						conn1 = conn1.replaceAll(_old, _new);
						newConnectionsMap.put(conn1, set);
					}
				}
			}
			//
			// now update the sets 
			//
			for (int i = 0; i < newPos.length; ++i) {
				String _old = "R"+(i+1);
				String _new = "r"+(newPos[i]+1);
				for (String conn1 : newConnectionsMap.keySet().toArray(new String[newConnectionsMap.size()])) {
					HashSet<String>set = (HashSet<String>)newConnectionsMap.get(conn1);
					if (set == null) {
						continue;
					}
					for (String conn2 : set.toArray(new String[set.size()])) {
						if (conn2.startsWith(_old)) {
							set.remove(conn2);
							set.add(conn2.replaceAll(_old, _new));
							newConnectionsMap.put(conn1, set);
						}
					}
				}
			}
			//----------------------------------//
			// second pass - restore upper case //
			//----------------------------------//
			for (String conn1 : newConnectionsMap.keySet().toArray(new String[newConnectionsMap.size()])) {
				Set<String> set = newConnectionsMap.get(conn1);
				for (String conn2 : set.toArray(new String[set.size()])) {
					set.remove(conn2);
					set.add(conn2.toUpperCase());
				}
				newConnectionsMap.remove(conn1);
				conn1 = conn1.toUpperCase();
				newConnectionsMap.put(conn1, set);
			}
			String _old = "R"+(dep+1);
			String _new = "R"+(newPos[dep]+1);
			String newDepParamConn = depParamConn.replaceAll(_old, _new);
			//------------------------------------------------//
			// store the new order, mappings, and connections //
			//------------------------------------------------//
			String newConnections = VirtualRating.getConnectionsNormalized(newConnectionsMap, newDepParamConn);
			setSourceRatings(newSourceRatings);
			setConnections(newConnections);
			connectionsMap = newConnectionsMap;
			depParamConn = newDepParamConn;
			isNormalized = true;
		}
	}
	/**
	 * @returns whether this virtual rating has been normalized
	 */
	public boolean isNormalized() {
		return isNormalized;
	}
	/**
	 * @return a normalized copy of this virtual rating
	 * @throws RatingException
	 */
	public VirtualRating normalizedCopy() throws RatingException {
		try {
			VirtualRating vr = new VirtualRating(getData());
			vr.normalize();
			return vr;
		}
		catch (Exception e) {
			throw new RatingException("Cannot create normalized copy.", e);
		}
	}
	/**
	 * @return a connections string that explicitly declares all external connections
	 */
	public String getConnectionsComplete() {
		return VirtualRating.getConnectionsComplete(this.connectionsMap, this.depParamConn);
	}
	/**
	 * @return a copy of the source ratings array 
	 */
	public SourceRating[] getSourceRatings() {
		synchronized(this) {
			return sourceRatings == null ? null : Arrays.copyOf(sourceRatings, sourceRatings.length);
		}
	}
	/**
	 * @return an array of source rating names
	 */
	public String[] getSourceRatingNames() {
		String[] names = null;
		if (sourceRatings != null) {
			names = new String[sourceRatings.length];
			for (int i = 0; i < sourceRatings.length; ++i) {
				names[i] = sourceRatings[i].getName();
			}
		}
		return names;
	}
	/**
	 * Set the source ratings array
	 * @param sources
	 * @throws RatingException 
	 */
	public void setSourceRatings(SourceRating[] sources) throws RatingException {
		synchronized(this) {
			sourceRatings = sources == null ? null : Arrays.copyOf(sources, sources.length);
			if (sourceRatings == null) {
				inputs = null;
				outputs = null;
			}else {
				inputs = new String[sourceRatings.length][];
				outputs = new String[sourceRatings.length];
				for (int i = 0; i < sourceRatings.length; ++i) {
					try {
						inputs[i] = new String[sourceRatings[i].getIndParamCount()];
						outputs[i] = "R"+(i+1)+"D";
						for (int j = 0; j < sourceRatings[i].getIndParamCount(); ++j) {
							inputs[i][j] = "R"+(i+1)+"I"+(j+1);
						}
					}
					catch (RatingException e) {
						throw new RatingRuntimeException(e);
					}
					sourceRatings[i].addObserver(this);
				}
			}
			findCycles();
			isNormalized = false;
		}
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
		synchronized(this) {
			String endingPoint = null;
			Set<String> connections = map.get(startingPoint);
			if (connections == null || startingPoint.equals("D")) {
				endingPoint = startingPoint;
			}
			else {
				int[] cpInfo = {-1, -1};
				VirtualRating.parseConnectionPoint(startingPoint, cpInfo);
				int startingPointRating = cpInfo[0];
				int startingPointPoint = cpInfo[1];
				List<String> endingPoints = new ArrayList<String>();
				for (Iterator<String> it = connections.iterator(); it.hasNext();) {
					String connectionPoint = it.next();
					if (connectionPoint.equals("D")) {
						endingPoint = startingPoint;
						break;
					}
					String connectionPoint1 = null;
					VirtualRating.parseConnectionPoint(connectionPoint, cpInfo);
					int r = cpInfo[0];
					int p = cpInfo[1];
					if (r < 0) {
						//-------------------------------------------------------------------------------//
						// starting point is the output of an internal rating that is connected to an    //
						// input of the virtual rating - enumerate the inputs of the internal rating     //
						// to find the first one not connected or connected to the virtual rating output //
						//-------------------------------------------------------------------------------//
						if (startingPointPoint == -1) {
							String partialInput = "R"+(startingPointRating+1)+"I";
							for (int i = 0; i < 9; ++i) {
								String input = partialInput+(i+1);
								if (!map.containsKey(input) || (map.get(input).size() == 1 && map.get(input).toArray(new String[1])[0].equals("D"))) {
									endingPoint = input;
									break;
								}
							}
						}
						if (endingPoint == null) {
							throw new RatingException(String.format("Connection path from %s leads to independent parameter", startingPoint));
						}
						else {
							break;
						}
					}
					else if (r >= sourceRatings.length) {
						throw new RatingException(String.format("Connection path from %s specifies rating beyond source rating count", startingPoint));
					}
					else if (r == startingPointRating) {
						throw new RatingException("Connection path specifies a connection from one rating to itself");
					}
					if (p < 0) {
						if (sourceRatings[r].mathExpression != null) {
							throw new RatingException(String.format("Connection path from %s would reverse throgh a math expression", startingPoint));
						}
						else if (sourceRatings[r].getIndParamCount() > 1) {
							throw new RatingException(String.format("Connection path from %s would reverse through a multiple-independent-parameter rating", startingPoint));
						}
						connectionPoint1 = "R"+(r+1)+"I1";
					}
					else {
						connectionPoint1 = "R"+(r+1)+"D";
					}
					endingPoints.add(walkConnections(map, connectionPoint1));
					if (endingPoints.size() > 1 && !endingPoints.get(endingPoints.size()-1).equals(endingPoints.get(0))) {
						throw new RatingException(String.format("Connection point %s leads to multiple termination points", connectionPoint1));
					}
					endingPoint = endingPoints.get(0);
				}
			}
			return endingPoint;
		}
	}
	/**
	 * Finds cyclical rating references in source ratings
	 * @throws RatingException if cyclic reference is found
	 */
	public void findCycles() throws RatingException {
		ArrayList<String> specIds = new ArrayList<String>();
		findCycles(specIds);
	}
	/**
	 * Finds cyclical rating references in source ratings
	 * @throws RatingException if cyclic reference is found
	 */
	protected void findCycles(ArrayList<String> specIds) throws RatingException {
		String specId = getOfficeId()+"/"+getRatingSpecId();
		if (specIds.contains(specId)) {
			StringBuilder sb = new StringBuilder("Cycle detected in source ratings. Cycle path is:");
			for (String pathSpecId : specIds) {
				sb.append(pathSpecId.equals(specId) ? "\n -->" : "\n    ").append(pathSpecId);
			}
			sb.append("\n -->").append(specId);
			throw new RatingException(sb.toString());
		}
		specIds.add(specId);
		for (SourceRating sr : sourceRatings) {
			for (AbstractRating ar : sr.getRatingSet().getRatings()) {
				if (ar instanceof VirtualRating) {
					((VirtualRating)ar).findCycles((ArrayList<String>)specIds.clone());
				}
				if (ar instanceof TransitionalRating) {
					((TransitionalRating)ar).findCycles((ArrayList<String>)specIds.clone());
				}
			}
		}
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
		long[] valTimes = new long[indVals[0].length];
		Arrays.fill(valTimes, valTime);
		return rate(valTimes, indVals);
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#rate(long[], double[][])
	 */
	@Override
	public double[] rate(long[] valTimes, double[][] indVals) throws RatingException {
		synchronized(this) {
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
				cpValues.put("I"+(i+1), indVals[i]);
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
					for (String dest : dests) {
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
					int paramCount = sourceRatings[r].getIndParamCount();
					for (p = 0; p < paramCount; ++p) {
						if (!cpValues.containsKey("R"+(r+1)+"I"+(p+1))) {
							break;
						}
					}
					if (p == paramCount) {
						//------------------------------------------------------------------//
						// forward rate and remove source rating inputs from the population //
						//------------------------------------------------------------------//
						int len = cpValues.get(inputs[r][0]).length;
						double[][] _indVals = new double[len][];
						for (int i = 0; i < len; ++i) {
							_indVals[i] = new double[paramCount];
							for (p = 0; p < paramCount; ++p) {
								double[] vals = cpValues.get(inputs[r][p]);
								_indVals[i][p] = vals[i]; 
							}
						}
						double[] results = sourceRatings[r].rate(valTimes, _indVals);
						cpValues.put(outputs[r], results);
						for (p = 0; p < paramCount; ++p) {
							cpValues.remove(inputs[r][p]);
						}
					}
					else if (cpValues.containsKey(outputs[r])) {
						//------------------------------------------------------------------//
						// reverse rate and remove source rating inputs from the population //
						//------------------------------------------------------------------//
						double[] depVals = cpValues.get(outputs[r]);
						cpValues.put(inputs[r][0], sourceRatings[r].reverseRate(valTimes, depVals));
						cpValues.remove(outputs[r]);
					}
				}
			}
			return cpValues.get(depParamConn);
		}
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#getData()
	 */
	@Override
	public VirtualRatingContainer getData() {
		VirtualRatingContainer vrc = new VirtualRatingContainer();
		getData(vrc);
		return vrc;
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#getData(hec.data.cwmsRating.io.AbstractRatingContainer)
	 */
	@Override
	protected void getData(AbstractRatingContainer arc) {
		synchronized(this) {
			if (!(arc instanceof VirtualRatingContainer)) {
				throw new UnsupportedOperationException("Virtual Ratings only support Virtual Rating Containers.");
			}
			VirtualRatingContainer vrc = (VirtualRatingContainer)arc;
			super.getData(vrc);
			vrc.connections = connectionsString;
			if (sourceRatings != null && sourceRatings.length > 0) {
				vrc.sourceRatings = new SourceRatingContainer[sourceRatings.length];
				for (int i = 0; i < sourceRatings.length; ++i) {
					vrc.sourceRatings[i] = sourceRatings[i].getData();
				}
			}
		}
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#setData(hec.data.cwmsRating.io.AbstractRatingContainer)
	 */
	@Override
	public void setData(AbstractRatingContainer arc) throws RatingException {
		synchronized(this) {
			if (!(arc instanceof VirtualRatingContainer)) {
				throw new RatingException("Expected VirtualRatingContainer, got " + arc.getClass().getName());
			}
			super._setData(arc);
			VirtualRatingContainer vrc = (VirtualRatingContainer)arc;
			if (vrc.sourceRatings != null && vrc.sourceRatings.length > 0) {
				SourceRating[] sourceRatings = new SourceRating[vrc.sourceRatings.length];
				for (int i = 0; i < vrc.sourceRatings.length; ++i) {
					sourceRatings[i] = new SourceRating(vrc.sourceRatings[i]);
				}
				setSourceRatings(sourceRatings);
			}
			setConnections(vrc.connections);
			findCycles();
		}
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#reverseRate(long[], double[])
	 */
	@Override
	public double[] reverseRate(long[] valTimes, double[] depVals) throws RatingException {
		synchronized(this) {
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
					ratedConn = "R"+(r+1)+"I1";
				}
				else {
					double[][] values = new double[cpValues[unrated].length][];
					for (int i = 0; i < cpValues[unrated].length; ++i) {
						values[i] = new double[] {cpValues[unrated][i]};
					}
					cpValues[rated] = sourceRatings[r].rate(valTimes, values);
					ratedConn = "R"+(r+1)+"D";
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
		synchronized(this) {
			super.setDataUnitsId(dataUnitsId);
			dataUnits = TextUtil.split(dataUnitsId.replace(SEPARATOR2, SEPARATOR3), SEPARATOR3);
		}
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#setDataUnits(java.lang.String[])
	 */
	@Override
	public void setDataUnits(String[] units) throws RatingException {
		synchronized(this) {
			super.setDataUnits(units);
			dataUnits = units == null ? null : Arrays.copyOf(units, units.length);
		}
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		return obj == this || (obj != null && obj.getClass() == getClass() && getData().equals(((VirtualRating)obj).getData()));
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#hashCode()
	 */
	@Override
	public int hashCode() {
		return getClass().getName().hashCode() + getData().hashCode();
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#storeToDatabase(java.sql.Connection, boolean)
	 */
	@Override
	public void storeToDatabase(Connection conn, boolean overwriteExisting) throws RatingException {
		if(!isNormalized()) {
			normalizedCopy().storeToDatabase(conn, overwriteExisting);
		}
		else {
			super.storeToDatabase(conn, overwriteExisting);
		}
	}
}
