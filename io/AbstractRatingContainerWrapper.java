package hec.data.cwmsRating.io;

import hec.io.DataContainer;

import java.io.Serializable;

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
}
