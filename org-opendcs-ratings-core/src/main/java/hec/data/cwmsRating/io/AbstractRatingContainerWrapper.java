/*
 * Copyright (c) 2021. Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved. HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 *
 */

package org.opendcs.ratings.io;

import java.io.Serializable;

import hec.io.DataContainer;

public class AbstractRatingContainerWrapper  extends DataContainer implements Serializable{
	private AbstractRatingContainer _abstractRatingContainer;
	
	public AbstractRatingContainerWrapper(AbstractRatingContainer abstractRatingContainer)
	{
		_abstractRatingContainer = abstractRatingContainer;
	}
	public AbstractRatingContainer getAbstractRatingContainer()
	{
		return _abstractRatingContainer;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		boolean result = obj == this;
		if (!result) {
			do {
				if (obj == null || obj.getClass() != getClass()) break;
				AbstractRatingContainerWrapper other = (AbstractRatingContainerWrapper)obj;
				if ((other._abstractRatingContainer == null) != (_abstractRatingContainer == null)) break;
				if (!(other._abstractRatingContainer).equals(_abstractRatingContainer)) break;
				result = true;
			} while (false);
		}
		return result;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return getClass().getName().hashCode() + (_abstractRatingContainer == null ? 0 : _abstractRatingContainer.hashCode());
	}
	
}
