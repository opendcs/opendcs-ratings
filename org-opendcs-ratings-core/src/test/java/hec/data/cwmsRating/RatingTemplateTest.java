/*
 * MIT License
 *
 * Copyright (c) 2022 Hydrologic Engineering Center
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.opendcs.ratings;

import static org.junit.jupiter.api.Assertions.assertThrows;


import org.opendcs.ratings.io.RatingTemplateContainer;
import org.junit.jupiter.api.Test;

/**
 *
 */
final class RatingTemplateTest {
    @Test
    public void testInvalidRatingMethods() throws RatingException {
        RatingConst.RatingMethod[] ratingMethods =
            {RatingConst.RatingMethod.NEAREST, RatingConst.RatingMethod.NEAREST, RatingConst.RatingMethod.NEAREST};
        assertThrows(RatingException.class, () -> new RatingTemplate("SWT", "Stage;Flow.Test", ratingMethods, ratingMethods, ratingMethods, "test"));

		RatingTemplateContainer container = new RatingTemplateContainer();
		container.depParam = "Flow";
		container.indParams = new String[]{"Stage"};
		container.inRangeMethods = new String[]{RatingConst.RatingMethod.NEAREST.toString()};
		container.officeId = "SWT";
		container.outRangeHighMethods = new String[]{RatingConst.RatingMethod.NEAREST.toString()};
		container.outRangeLowMethods = new String[]{RatingConst.RatingMethod.NEAREST.toString()};
		container.parametersId = "Stage;Flow";
		container.templateDescription = "test";
		container.templateId = "Stage;Flow.Test";
		container.templateVersion = "Test";
		RatingTemplate ratingTemplate = new RatingTemplate();
		assertThrows(RatingException.class, () -> ratingTemplate.setData(container, false));


		RatingTemplateContainer validContainer = new RatingTemplateContainer();
		validContainer.depParam = "Flow";
		validContainer.indParams = new String[]{"Stage"};
		validContainer.inRangeMethods = new String[]{RatingConst.RatingMethod.NEXT.toString()};
		validContainer.officeId = "SWT";
		validContainer.outRangeHighMethods = new String[]{RatingConst.RatingMethod.NEAREST.toString()};
		validContainer.outRangeLowMethods = new String[]{RatingConst.RatingMethod.NEAREST.toString()};
		validContainer.parametersId = "Stage;Flow";
		validContainer.templateDescription = "test";
		validContainer.templateId = "Stage;Flow.Test";
		validContainer.templateVersion = "Test";
		RatingTemplate validTemplate = new RatingTemplate(validContainer);
		assertThrows(RatingException.class, () -> validTemplate.setInRangeMethods( new RatingConst.RatingMethod[]{RatingConst.RatingMethod.NEAREST}));
    }
}
