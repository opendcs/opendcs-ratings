/**
 * 
 */
package hec.data.cwmsRating.io;

import java.sql.Connection;
import java.util.Arrays;

import hec.lang.Const;

/**
 * @author Mike Perryman
 * 
 * Holds the state of a RatingSet object. This is used to replicate state via getData()/setData().
 */
public class RatingSetStateContainer {
	/**
	 * The database connection object
	 */
	public Connection conn = null;
	/**
	 * Flag specifying whether the connection is stored in the rating object (false) or was retrieved and should be released when done (true)
	 */
	public boolean wasRetrieved = false;
	/**
	 * URL of database connection
	 */
	public String dbUrl = null;
	/**
	 * User name of database connection
	 */
	public String dbUserName = null;
	/**
	 * Office of database connection
	 */
	public String dbOfficeId = null;
	/**
	 * The specified units of the data to rate
	 */
	public String[] dataUnits = null;
	/**
	 * A time to associate with all values that don't specify their own times.  This time, along with the rating
	 * effective dates, is used to determine which ratings to use to rate values.
	 */
	public long defaultValueTime = Const.UNDEFINED_TIME;
	/**
	 * A time used to allow the rating of values with information that was known at a specific time. No ratings
	 * with a creation date after this time will be used to rate values.
	 */
	public long ratingTime = Long.MAX_VALUE;
	@Override
	public int hashCode() {
		int hashCode = getClass().getName().hashCode();
		if (conn       != null) hashCode +=  3 * conn.hashCode();
		if (wasRetrieved      ) hashCode +=  5;
		if (dbUrl      != null) hashCode +=  7 * dbUrl.hashCode();
		if (dbUserName != null) hashCode += 11 * dbUserName.hashCode();
		if (dbOfficeId != null) hashCode += 13 * dbOfficeId.hashCode();
		if (dataUnits  != null) hashCode += 17 * dataUnits.hashCode();
		hashCode += 19 * defaultValueTime;
		hashCode += 23 * ratingTime;
		return hashCode;
	}
	@Override
	public boolean equals(Object obj) {
		boolean result = obj == this;
		if (!result) {
			RatingSetStateContainer rssc = (RatingSetStateContainer)obj;
			test:
			do {
				if ((rssc.conn == null) != (conn == null) || (rssc.conn != null && !rssc.conn.equals(conn))) break; 
				if (rssc.wasRetrieved != wasRetrieved) break;
				if ((rssc.dbUrl == null) != (dbUrl == null) || (rssc.dbUrl != null && !rssc.dbUrl.equals(dbUrl))) break;
				if ((rssc.dbUserName == null) != (dbUserName == null) || (rssc.dbUserName != null && !rssc.dbUserName.equals(dbUserName))) break;
				if ((rssc.dbOfficeId == null) != (dbOfficeId == null) || (rssc.dbOfficeId != null && !rssc.dbOfficeId.equals(dbOfficeId))) break;
				if (rssc.dataUnits != null) {
					if (dataUnits == null) break;
					if (rssc.dataUnits.length != dataUnits.length) break;
					for (int i = 0; i < dataUnits.length; ++i) {
						if (!rssc.dataUnits[i].equals(dataUnits[i])) break test;
					}
				}
				else if (dataUnits != null) break;
				if (rssc.defaultValueTime != defaultValueTime) break;
				if (rssc.ratingTime != ratingTime) break;
				result = true;
			} while (false);
		}
		return result;
	}
	@Override
	protected Object clone() {
		RatingSetStateContainer rssc = new RatingSetStateContainer();
		rssc.conn = conn;
		rssc.wasRetrieved = wasRetrieved;
		rssc.dbUrl = dbUrl;
		rssc.dbUserName = dbUserName;
		rssc.dbOfficeId = dbOfficeId;
		if (dataUnits != null) rssc.dataUnits = Arrays.copyOf(dataUnits, dataUnits.length);
		rssc.defaultValueTime = defaultValueTime;
		rssc.ratingTime = ratingTime;
		return rssc;
	}

}
