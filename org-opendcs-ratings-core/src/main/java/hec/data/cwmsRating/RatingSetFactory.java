/*
* Where Applicable, Copyright 2026 OpenDCS Consortium and/or its contributors
* 
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software 
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations 
* under the License.
*/

package org.opendcs.ratings;


import org.opendcs.ratings.io.RatingSetContainer;

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
