/*
 * Copyright (c) 2021. Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved. HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 *
 */

package hec.data.cwmsRating;

import java.util.ArrayList;
import java.util.List;

// this if from CWMS_RATING_METHOD
public enum RatingMethodId
{
	// the text for the method, spec low, spec in range, spec high, templ low,
	// templ in range, templ high
	Null("NULL", true, false, false, true, true, true),
	Error("ERROR", true, false, false, true, true, true),
	Linear("LINEAR", true, true, true, true, true, true),
	Logarithmic("LOGARITHMIC", false, false, false, true, true, true),
	LinLog("LIN-LOG", false, false, false, true, true, true),
	LogLin("LOG-LIN", false, false, false, true, true, true),
	Previous("PREVIOUS", false, true, true, false, true, true),
	Next("NEXT", true, true, false, true, true, false),
	Nearest("NEAREST", true, false, true, true, false, true),
	Lower("LOWER", false, true, true, false, true, true),
	Higher("HIGHER", true, true, false, true, true, false),
	Closest("CLOSEST", true, true, true, true, true, true);

	// the booleans are to specify whether for a given type of usage, the rating
	// method is a valid choice. Rating spec values are interpolating between
	// different
	// rating curves so some options don't make sense. All the values make sense
	// for the rating template but I put this here for consistency in case we
	// changed our minde
	String _dbId;
	boolean[] _rangeIdentifier = new boolean[6];
	public static final int SPECLO = 0;
	public static final int SPECINRANGE = 1;
	public static final int SPECHI = 2;
	public static final int TEMPLATELO = 3;
	public static final int TEMPLATEINRANGE = 4;
	public static final int TEMPLATEHI = 5;

	RatingMethodId(String dbId, boolean specLo, boolean specInRange, boolean specHi, boolean templateLo,
		boolean templateInRange, boolean templateHi)
	{
		_dbId = dbId;
		_rangeIdentifier[SPECLO] = specLo;
		_rangeIdentifier[SPECINRANGE] = specInRange;
		_rangeIdentifier[SPECHI] = specHi;
		_rangeIdentifier[TEMPLATELO] = templateLo;
		_rangeIdentifier[TEMPLATEINRANGE] = templateInRange;
		_rangeIdentifier[TEMPLATEHI] = templateHi;
	}

	public String getDbId()
	{
		return _dbId;
	}

	public static RatingMethodId getRatingMethodIdFromDbId(String dbId)
	{
		for (RatingMethodId value : RatingMethodId.values())
		{
			if (value.getDbId().equalsIgnoreCase(dbId)) return value;
		}
		return null;
	}

	/**
	 * @param rangeIdenifierIdx
	 * @return a subset of the rating method ids that are valid for the given
	 *         rangeIdentifierIdx (e.g. SPECLO, TEMPLATEHI etc...)
	 */
	public static RatingMethodId[] getRatingMethodsForRangeIdentifier(int rangeIdentifierIdx)
	{
		if (rangeIdentifierIdx < 0 || rangeIdentifierIdx > Null._rangeIdentifier.length - 1)
			return new RatingMethodId[0];

		List<RatingMethodId> rmidList = new ArrayList<>();
		for (RatingMethodId rmid : RatingMethodId.values())
		{
			if (rmid._rangeIdentifier[rangeIdentifierIdx])
			{
				rmidList.add(rmid);

			}
		}
		return rmidList.toArray(new RatingMethodId[rmidList.size()]);
	}

	public boolean isRatingMethodValidForRangeIdentifier(int rangeIdentifierIdx)
	{
		if (rangeIdentifierIdx < 0 || rangeIdentifierIdx > Null._rangeIdentifier.length - 1) return false;

		return _rangeIdentifier[rangeIdentifierIdx];
	}

}
