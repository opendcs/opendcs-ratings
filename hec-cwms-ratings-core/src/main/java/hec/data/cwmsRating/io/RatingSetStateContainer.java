/*
 * Copyright (c) 2021. Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved. HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 *
 */

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
	 * Flag specifying whether this rating set allows "risky" behavior such as using mismatched units, unknown parameters, etc.
	 */
	public boolean allowUnsafe = true;
	/**
	 * Flag specifying whether this rating set outputs messges about "risky" behavior such as using mismatched units, unknown parameters, etc.
	 */
	public boolean warnUnsafe = true;
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
		if (dbUrl      != null) hashCode +=  7 * dbUrl.hashCode();
		if (dbUserName != null) hashCode += 11 * dbUserName.hashCode();
		if (dbOfficeId != null) hashCode += 13 * dbOfficeId.hashCode();
		if (dataUnits  != null) hashCode += 17 * dataUnits.hashCode();
		if (allowUnsafe       ) hashCode += 19;
		if (warnUnsafe        ) hashCode += 23;
		hashCode += 29 * defaultValueTime;
		hashCode += 31 * ratingTime;
		return hashCode;
	}
	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		RatingSetStateContainer rssc = (RatingSetStateContainer)obj;
		if ((rssc.conn == null) != (conn == null) || (rssc.conn != null && !rssc.conn.equals(conn))) return false; 
		if ((rssc.dbUrl == null) != (dbUrl == null) || (rssc.dbUrl != null && !rssc.dbUrl.equals(dbUrl))) return false;
		if ((rssc.dbUserName == null) != (dbUserName == null) || (rssc.dbUserName != null && !rssc.dbUserName.equals(dbUserName))) return false;
		if ((rssc.dbOfficeId == null) != (dbOfficeId == null) || (rssc.dbOfficeId != null && !rssc.dbOfficeId.equals(dbOfficeId))) return false;
		if (rssc.dataUnits != null) {
			if (dataUnits == null) return false;
			if (rssc.dataUnits.length != dataUnits.length) return false;
			for (int i = 0; i < dataUnits.length; ++i) {
				if (!rssc.dataUnits[i].equals(dataUnits[i])) return false;
			}
		}
		else if (dataUnits != null) return false;
		if (rssc.allowUnsafe != allowUnsafe) return false;
		if (rssc.warnUnsafe != warnUnsafe) return false;
		if (rssc.defaultValueTime != defaultValueTime) return false;
		if (rssc.ratingTime != ratingTime) return false;
		return true;
	}
	@Override
	protected Object clone() {
		RatingSetStateContainer rssc = new RatingSetStateContainer();
		rssc.conn = conn;
		rssc.dbUrl = dbUrl;
		rssc.dbUserName = dbUserName;
		rssc.dbOfficeId = dbOfficeId;
		if (dataUnits != null) rssc.dataUnits = Arrays.copyOf(dataUnits, dataUnits.length);
		rssc.allowUnsafe = allowUnsafe;
		rssc.warnUnsafe = warnUnsafe;
		rssc.defaultValueTime = defaultValueTime;
		rssc.ratingTime = ratingTime;
		return rssc;
	}

}