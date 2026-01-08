/*
 * Copyright (c) 2022
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */

package hec.data.cwmsRating;


import hec.data.cwmsRating.io.RatingSetContainer;

public final class RatingSetFactory {

    private RatingSetFactory() {
        throw new AssertionError("Utility class");
    }

    public static AbstractRatingSet ratingSet(RatingSetContainer rsc) throws RatingException {
        ConcreteRatingSet concreteRatingSet = new ConcreteRatingSet();
        concreteRatingSet.setData(rsc);
        concreteRatingSet.validate();
        return concreteRatingSet;
    }

    public static AbstractRatingSet ratingSet(RatingSpec ratingSpec, Iterable<AbstractRating> ratings) throws RatingException {
        ConcreteRatingSet concreteRatingSet = new ConcreteRatingSet();
        concreteRatingSet.setRatingSpec(ratingSpec);
        concreteRatingSet.addRatings(ratings);
        concreteRatingSet.validate();
        return concreteRatingSet;
    }

    public static AbstractRatingSet ratingSet(RatingSpec ratingSpec, AbstractRating[] ratings) throws RatingException {
        ConcreteRatingSet concreteRatingSet = new ConcreteRatingSet();
        concreteRatingSet.setRatingSpec(ratingSpec);
        concreteRatingSet.addRatings(ratings);
        concreteRatingSet.validate();
        return concreteRatingSet;
    }

    public static AbstractRatingSet ratingSet(RatingSpec ratingSpec, AbstractRating rating) throws RatingException {
        ConcreteRatingSet concreteRatingSet = new ConcreteRatingSet();
        concreteRatingSet.setRatingSpec(ratingSpec);
        concreteRatingSet.addRating(rating);
        concreteRatingSet.validate();
        return concreteRatingSet;
    }

    public static AbstractRatingSet ratingSet(RatingSpec ratingSpec) throws RatingException {
        ConcreteRatingSet concreteRatingSet = new ConcreteRatingSet();
        concreteRatingSet.setRatingSpec(ratingSpec);
        concreteRatingSet.validate();
        return concreteRatingSet;
    }
}
