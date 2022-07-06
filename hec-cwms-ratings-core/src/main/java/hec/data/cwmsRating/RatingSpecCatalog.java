package hec.data.cwmsRating;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import hec.data.RatingException;
import hec.data.cwmsRating.io.RatingSpecContainer;
import hec.data.location.LocationTemplate;

public class RatingSpecCatalog
{
	private final NavigableMap<LocationTemplate, Map<RatingTemplate, Set<RatingSpec>>> locSpecMap;

	public RatingSpecCatalog()
	{
		locSpecMap = new TreeMap<>();
	}

	public RatingSpecCatalog(Collection<RatingSpec> specs) throws RatingException
	{
		locSpecMap = unmodifiableOf(buildLocSpecMap(specs));
	}

	Map<RatingTemplate, Set<RatingSpec>> getSpecifications(LocationTemplate locRef){
		return Collections.unmodifiableMap(this.locSpecMap.getOrDefault(locRef, new LinkedHashMap<>()));
	}

	NavigableMap<LocationTemplate, Map<RatingTemplate, Set<RatingSpec>>> getSpecifications(){
		return this.locSpecMap;
	}

	private static NavigableMap<LocationTemplate, Map<RatingTemplate, Set<RatingSpec>>> buildLocSpecMap(Collection<RatingSpec> specs){
		NavigableMap<LocationTemplate, Map<RatingTemplate, Set<RatingSpec>>> locSpecMap = new TreeMap<>(LocationTemplate.LocationComparator);
		if(specs != null)
		{
			for(RatingSpec spec : specs)
			{
				LocationTemplate locationRef = new LocationTemplate(spec.getOfficeId(), spec.getLocationId());

				Map<RatingTemplate, Set<RatingSpec>> templateSpecMap =
						locSpecMap.computeIfAbsent(locationRef, k -> new LinkedHashMap<>());

				Set<RatingSpec> ratingSpecSet =
						templateSpecMap.computeIfAbsent(spec, k -> new LinkedHashSet<>());
				ratingSpecSet.add(spec);
			}
		}
		return locSpecMap;
	}

	private static NavigableMap<LocationTemplate, Map<RatingTemplate, Set<RatingSpec>>> unmodifiableOf(NavigableMap<LocationTemplate, Map<RatingTemplate, Set<RatingSpec>>> input)
			throws RatingException
	{
		NavigableMap<LocationTemplate, Map<RatingTemplate, Set<RatingSpec>>> locSpecMap = new TreeMap<>(LocationTemplate.LocationComparator);

		Set<Map.Entry<LocationTemplate, Map<RatingTemplate, Set<RatingSpec>>>> entries = input.entrySet();
		for(final Map.Entry<LocationTemplate, Map<RatingTemplate, Set<RatingSpec>>> entry : entries)
		{
			LocationTemplate copy = new LocationTemplate(entry.getKey());
			locSpecMap.put(copy, unmodifiableOf(entry.getValue()));
		}

		return Collections.unmodifiableNavigableMap(locSpecMap);
	}

	private static Map<RatingTemplate, Set<RatingSpec>> unmodifiableOf(Map<RatingTemplate, Set<RatingSpec>> value)
			throws RatingException
	{
		Map<RatingTemplate, Set<RatingSpec>> templateSpecMap = new LinkedHashMap<>();
		Set<Map.Entry<RatingTemplate, Set<RatingSpec>>> entries = value.entrySet();
		for(final Map.Entry<RatingTemplate, Set<RatingSpec>> entry : entries)
		{
			RatingTemplate copy = new RatingTemplate(entry.getKey().getData());
			templateSpecMap.put(copy, unmodifiableOf(entry.getValue()));
		}
		return Collections.unmodifiableMap(templateSpecMap);
	}

	private static Set<RatingSpec> unmodifiableOf(Set<RatingSpec> specs) throws RatingException
	{
		LinkedHashSet<RatingSpec> retval = new LinkedHashSet<>();
		if(specs != null){
			for(RatingSpec spec : specs){
				RatingSpec copy = new RatingSpec((RatingSpecContainer) spec.getData());
				retval.add(copy);
			}
		}

		return Collections.unmodifiableSet(retval);
	}


	public int size() {
		int size = 0;
		Set<Map.Entry<LocationTemplate, Map<RatingTemplate, Set<RatingSpec>>>> entrySet = this.locSpecMap.entrySet();
		for (Map.Entry<LocationTemplate, Map<RatingTemplate, Set<RatingSpec>>> entry : entrySet) {
			Map<RatingTemplate, Set<RatingSpec>> templateSpecMap = entry.getValue();
			size += templateSpecMap.size();
		}

		return size;
	}


	public static class Builder{

		final LinkedHashSet<RatingSpec> ratingSpecs = new LinkedHashSet<>();

		public RatingSpecCatalog build() throws RatingException
		{
			return new RatingSpecCatalog(ratingSpecs);
		}

		public Builder addRatingSpec(RatingSpec spec){
			ratingSpecs.add(spec);
			return this;
		}

		public Builder addRatingSpecs(Collection<RatingSpec> specs){
			if(specs != null){
				ratingSpecs.addAll(specs);
			}
			return this;
		}

	}
}
