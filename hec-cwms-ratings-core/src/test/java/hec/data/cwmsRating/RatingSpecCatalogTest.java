package hec.data.cwmsRating;

import java.util.Arrays;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;

import org.junit.jupiter.api.Test;

import hec.data.RatingException;
import hec.data.RoundingException;
import hec.data.location.LocationTemplate;

import static org.junit.jupiter.api.Assertions.*;

class RatingSpecCatalogTest
{

	@Test
	void testCtorNoArg()
	{
		RatingSpecCatalog catalog = new RatingSpecCatalog();
		assertNotNull(catalog);
		assertEquals(0, catalog.size());
	}

	@Test
	void testCtor() throws RatingException, RoundingException
	{
		// Specs are hard to build manually.
		RatingSpec spec1 = new RatingSpec();
		spec1.setOfficeId("SWT");
		spec1.setTemplateId("Stage;Flow.BASE");
		spec1.setLocationId("Location1");
		spec1.setParametersId("Stage;Flow");
		spec1.setIndRoundingSpecs(new String[]{"2222233332"});
		spec1.setDepRoundingSpec("2222233332");
		spec1.setInRangeMethod(RatingConst.RatingMethod.LINEAR);
		spec1.setOutRangeLowMethod(RatingConst.RatingMethod.NEAREST);
		spec1.setOutRangeHighMethod(RatingConst.RatingMethod.NEAREST);
		spec1.setInRangeMethods(new RatingConst.RatingMethod[]{RatingConst.RatingMethod.LINEAR});
		spec1.setInRangeMethods(new RatingConst.RatingMethod[]{RatingConst.RatingMethod.LINEAR});
		spec1.setOutRangeLowMethods(new RatingConst.RatingMethod[]{RatingConst.RatingMethod.LINEAR});
		spec1.setOutRangeHighMethods(new RatingConst.RatingMethod[]{RatingConst.RatingMethod.LINEAR});

		RatingSpec spec1b = new RatingSpec();
		spec1b.setOfficeId("SWT");
		spec1b.setLocationId("Location1");
		spec1b.setTemplateId("Opening,Elev;Flow.Linear");
		spec1b.setParametersId("Opening,Elev;Flow");
		spec1b.setIndRoundingSpecs(new String[]{"2222233332","2222233332"});
		spec1b.setDepRoundingSpec("2222233332");
		spec1b.setInRangeMethod(RatingConst.RatingMethod.LINEAR);
		spec1b.setOutRangeLowMethod(RatingConst.RatingMethod.NEAREST);
		spec1b.setOutRangeHighMethod(RatingConst.RatingMethod.NEAREST);
		spec1b.setInRangeMethods(new RatingConst.RatingMethod[]{RatingConst.RatingMethod.LINEAR, RatingConst.RatingMethod.LINEAR});
		spec1b.setOutRangeLowMethods(new RatingConst.RatingMethod[]{RatingConst.RatingMethod.LINEAR, RatingConst.RatingMethod.LINEAR});
		spec1b.setOutRangeHighMethods(new RatingConst.RatingMethod[]{RatingConst.RatingMethod.LINEAR, RatingConst.RatingMethod.LINEAR});

		RatingSpec spec2 = new RatingSpec();
		spec2.setOfficeId("SWT");
		spec2.setLocationId("Location2");
		spec2.setTemplateId("Stage;Flow.Linear");
		spec2.setParametersId("Stage;Flow");
		spec2.setIndRoundingSpecs(new String[]{"2222233332"});
		spec2.setDepRoundingSpec("2222233332");
		spec2.setInRangeMethod(RatingConst.RatingMethod.LINEAR);
		spec2.setInRangeMethods(new RatingConst.RatingMethod[]{RatingConst.RatingMethod.LINEAR});
		spec2.setOutRangeLowMethod(RatingConst.RatingMethod.NEAREST);
		spec2.setOutRangeHighMethod(RatingConst.RatingMethod.NEAREST);
		spec2.setOutRangeLowMethods(new RatingConst.RatingMethod[]{RatingConst.RatingMethod.LINEAR});
		spec2.setOutRangeHighMethods(new RatingConst.RatingMethod[]{RatingConst.RatingMethod.LINEAR});

		RatingSpecCatalog catalog = new RatingSpecCatalog(Arrays.asList(spec1, spec1b, spec2));
		assertNotNull(catalog);
		assertEquals(3, catalog.size());

		NavigableMap<LocationTemplate, Map<RatingTemplate, Set<RatingSpec>>> specifications = catalog.getSpecifications();
		assertNotNull(specifications);

		Map<RatingTemplate, Set<RatingSpec>> loc1Map = catalog.getSpecifications(
				new LocationTemplate("SWT", "Location1"));
		assertNotNull(loc1Map);
		assertFalse(loc1Map.isEmpty());
		assertEquals(2, loc1Map.size());

	}

	@Test
	void testBuilder() throws RatingException, RoundingException
	{
		RatingSpecCatalog.Builder builder = new RatingSpecCatalog.Builder();

		for(int i = 0; i < 10; i++)
		{
			RatingSpec spec = new RatingSpec();
			spec.setOfficeId("SWT");
			spec.setTemplateId("Stage;Flow.BASE");
			spec.setLocationId("Location" + i);
			spec.setParametersId("Stage;Flow");
			spec.setIndRoundingSpecs(new String[]{"2222233332"});
			spec.setDepRoundingSpec("2222233332");
			spec.setInRangeMethod(RatingConst.RatingMethod.LINEAR);
			spec.setOutRangeLowMethod(RatingConst.RatingMethod.NEAREST);
			spec.setOutRangeHighMethod(RatingConst.RatingMethod.NEAREST);
			spec.setInRangeMethods(new RatingConst.RatingMethod[]{RatingConst.RatingMethod.LINEAR});
			spec.setInRangeMethods(new RatingConst.RatingMethod[]{RatingConst.RatingMethod.LINEAR});
			spec.setOutRangeLowMethods(new RatingConst.RatingMethod[]{RatingConst.RatingMethod.LINEAR});
			spec.setOutRangeHighMethods(new RatingConst.RatingMethod[]{RatingConst.RatingMethod.LINEAR});

			builder.addRatingSpec(spec);
		}

		RatingSpecCatalog catalog = builder.build();
		assertNotNull(catalog);
		assertEquals(10, catalog.size());
	}

}