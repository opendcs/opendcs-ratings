package hec.data.cwmsRating;

public class RatingOutOfRangeException extends RatingException{
	
	public enum OutOfRangeEnum{OUT_OF_RANGE_LOW, OUT_OF_RANGE_HIGH}
	
	OutOfRangeEnum _type;
	
	public RatingOutOfRangeException(OutOfRangeEnum type)
	{
		super(getOutOfRangeMessage(type));
		_type = type;
	}
	
	private static String getOutOfRangeMessage(OutOfRangeEnum type)
	{
		switch(type){	
			case OUT_OF_RANGE_LOW:
				return "Value is below the range of the curve";
			case OUT_OF_RANGE_HIGH:
				return "Value is above the range of the curve";
			default:
				return "Value is not in the range of the rating curve";
		}
	}
	
	public OutOfRangeEnum getOutOfRangeType()
	{
		return _type;
	}
}
