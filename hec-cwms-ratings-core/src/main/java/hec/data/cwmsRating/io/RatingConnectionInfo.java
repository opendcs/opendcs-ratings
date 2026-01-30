/*
 * Copyright (c) 2022
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */

package hec.data.cwmsRating.io;


import hec.data.cwmsRating.RatingException;

public interface RatingConnectionInfo {

    <T> T getConnectionInfo(Class<T> type) throws RatingException;
}
