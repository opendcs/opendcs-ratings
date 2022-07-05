package hec.data.cwmsRating;

import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import hec.data.location.LocationTemplate;

public class RatingSpecCatalog
{
	private final Map<LocationTemplate, NavigableMap<RatingTemplate, NavigableSet<RatingSpec>>>
			_locSpecMap = new TreeMap<>(LocationTemplate.LocationComparator);

	NavigableMap<RatingTemplate, NavigableSet<RatingSpec>> getSpecifications(LocationTemplate locRef){
		return this._locSpecMap.getOrDefault(locRef, new TreeMap<>());
	}

	Map<LocationTemplate, NavigableMap<RatingTemplate, NavigableSet<RatingSpec>>> getSpecifications(){
		return this._locSpecMap;
	}

	public void add(RatingSpec spec) {
		LocationTemplate locationRef = new LocationTemplate(spec.getOfficeId(), spec.getLocationId());

		NavigableMap<RatingTemplate, NavigableSet<RatingSpec>> templateSpecMap =
				this._locSpecMap.computeIfAbsent(locationRef, k -> new TreeMap<>());

		NavigableSet<RatingSpec> ratingSpecSet =
				templateSpecMap.computeIfAbsent(spec, k -> new TreeSet<>());
		ratingSpecSet.add(spec);
	}

	public int size() {
		int size = 0;
		Set<Map.Entry<LocationTemplate, NavigableMap<RatingTemplate, NavigableSet<RatingSpec>>>> entrySet = this._locSpecMap.entrySet();
		for (Map.Entry<LocationTemplate, NavigableMap<RatingTemplate, NavigableSet<RatingSpec>>> entry : entrySet) {
			NavigableMap<RatingTemplate, NavigableSet<RatingSpec>> templateSpecMap = entry.getValue();
			size += templateSpecMap.size();
		}

		return size;
	}
}
