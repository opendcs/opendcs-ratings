package hec.data.cwmsRating;

/**
 * This class is used to differentiate Not Monotonically increasing or decreasing errors from regular rating exceptions
 * 
 * @author josh
 */
public class NotMonotonicRatingException extends RatingException
{
    //generated serial verison uid
    private static final long serialVersionUID = 6847236907816935402L;    
    
    public NotMonotonicRatingException(String text)
    {
        super(text);
    }
    
    public NotMonotonicRatingException(Throwable t)
    {
        super(t);
    }
}
