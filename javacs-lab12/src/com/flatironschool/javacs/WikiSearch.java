package com.flatironschool.javacs;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import redis.clients.jedis.Jedis;


/**
 * Represents the results of a search query.
 *
 */
public class WikiSearch {
	
	// map from URLs that contain the term(s) to relevance score
	private Map<String, Integer> map;

	/**
	 * Constructor.
	 * 
	 * @param map
	 */
	public WikiSearch(Map<String, Integer> map) {
		this.map = map;
	}
	
	/**
	 * Looks up the relevance of a given URL.
	 * 
	 * @param url
	 * @return
	 */
	public Integer getRelevance(String url) {
		Integer relevance = map.get(url);
		return relevance==null ? 0: relevance;
	}
	
	/**
	 * Prints the contents in order of term frequency.
	 * 
	 * @param map
	 */
	private void print() {
		List<Entry<String, Integer>> entries = sort();
		for (Entry<String, Integer> entry: entries) {
			System.out.println(entry);
		}
	}
	
	private Map<String, Integer> getMap(){
		return map;
	}
	
	/**
	 * Computes the union of two search results.
	 * 
	 * @param that
	 * @return New WikiSearch object.
	 */
	public WikiSearch or(WikiSearch that) {
        // TODO
		Map<String, Integer> union = new HashMap<String, Integer>();
		
		// add all urls from this WikiSearch
		for (String url : map.keySet()) {
			union.put(url, totalRelevance(map.get(url), that.getRelevance(url)));
		}
		
		// add remaining urls from that that have not been added yet
		for (Entry<String, Integer> entry : that.getMap().entrySet()) {
			if (!union.containsKey(entry.getKey())) {
				union.put(entry.getKey(), entry.getValue());
			}
		}
		return new WikiSearch(union);
	}
	
	/**
	 * Computes the intersection of two search results.
	 * 
	 * @param that
	 * @return New WikiSearch object.
	 */
	public WikiSearch and(WikiSearch that) {
        // TODO
		
		Map<String, Integer> intersection = new HashMap<String, Integer>();
		
		for (String url : map.keySet()) {
			// that also contains same url
			if (that.getRelevance(url) != 0) { 
				intersection.put(url, totalRelevance(map.get(url), that.getRelevance(url)));
			}
		}
		return new WikiSearch(intersection);
	}
	
	/**
	 * Computes the intersection of two search results.
	 * 
	 * @param that
	 * @return New WikiSearch object.
	 */
	public WikiSearch minus(WikiSearch that) {
        // TODO
		
		Map<String, Integer> diff = new HashMap<String, Integer>();
		
		for (String url : map.keySet()) {
			if (that.getRelevance(url) == 0) {
				diff.put(url, map.get(url));
			}
		}
		
		return new WikiSearch(diff);
	}
	
	/**
	 * Computes the relevance of a search with multiple terms.
	 * 
	 * @param rel1: relevance score for the first search
	 * @param rel2: relevance score for the second search
	 * @return
	 */
	protected int totalRelevance(Integer rel1, Integer rel2) {
		// simple starting place: relevance is the sum of the term frequencies.
		return rel1 + rel2;
	}

	/**
	 * Sort the results by relevance.
	 * 
	 * @return List of entries with URL and relevance.
	 */
	public List<Entry<String, Integer>> sort() {
        // TODO
		
		List<Entry<String, Integer>> sorted = new ArrayList<Entry<String, Integer>>();
		
		// entries to sort
		Set<Entry<String, Integer>> entries = map.entrySet();
		
		// relevances
		Collection<Integer> relevance = map.values();
	
		// map is tied to entries and relevance, 
		// changes in one will cause changes in the other 
		// (ex. deletion in relevances will delete pairs in map that contain same value)
				
		ArrayList<Integer> sortedRel = new ArrayList<Integer>();
		for (Integer i : relevance) {
			sortedRel.add(i);
		}	
		Collections.sort(sortedRel);

		for (int i = 0; i < sortedRel.size(); i++) {
			for (Entry<String, Integer> entry : entries) {
				if (entry.getValue() == sortedRel.get(i) && !sorted.contains(entry))
					sorted.add(entry);
			}
		}
		
		/* I like this way better than creating an ArrayList for sorted relevances, 
		 * but removing i from relevance also removes  it from map, and then 
		 * the items in map get deleted
		while (!relevance.isEmpty()) {
			int i = Collections.min(relevance);
			
			for (Entry<String, Integer> entry : entries) {
				if (entry.getValue() == i && !sorted.contains(entry)) 
					sorted.add(entry);
			}
			relevance.remove(i);
		}
		*/
		
		return sorted;
	}

	/**
	 * Performs a search and makes a WikiSearch object.
	 * 
	 * @param term
	 * @param index
	 * @return
	 */
	public static WikiSearch search(String term, JedisIndex index) {
		Map<String, Integer> map = index.getCounts(term);
		return new WikiSearch(map);
	}

	public static void main(String[] args) throws IOException {
		
		// make a JedisIndex
		Jedis jedis = JedisMaker.make();
		JedisIndex index = new JedisIndex(jedis); 
		
		// search for the first term
		String term1 = "java";
		System.out.println("Query: " + term1);
		WikiSearch search1 = search(term1, index);
		search1.print();
		
		// search for the second term
		String term2 = "programming";
		System.out.println("Query: " + term2);
		WikiSearch search2 = search(term2, index);
		search2.print();
		
		// compute the intersection of the searches
		System.out.println("Query: " + term1 + " AND " + term2);
		WikiSearch intersection = search1.and(search2);
		intersection.print();

		// compute the union of the searches
		System.out.println("Query: " + term1 + " OR " + term2);
		WikiSearch union = search1.or(search2);
		union.print();
	}
}
