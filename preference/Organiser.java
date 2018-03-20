
/*
 *   Copyright 2015 Cheikh BA <cheikh.ba.sn@gmail.com>
 *
 *   This file is part of WALO.
 *
 *   WALO is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Lesser General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   WALO is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public License,
 *   along with WALO.  If not, see <http://www.gnu.org/licenses/>.
*/


/*
 * Created on 14.05.2014
 * Organiser: to store PCD for concrete services according to the user's preferences.
 * 
 * @author Cheikh BA
 */

package preference;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import datalog.DatalogQuery;
import datalog.Predicate;
import datalog.PredicateElement;
import minicon.MCD;
import minicon.Mapping;
import minicon.Rewriting;

public class Organiser {
	
private static Hashtable<Predicate, LinkedHashMap <String, List<MCD>>> organiser;
	
	public static void createOrganiser (List<MCD> mcds, DatalogQuery query){
		LinkedHashMap <String, List<MCD>> abstractService; 
		List<MCD> coverageDomain;
		
		organiser = new  Hashtable<Predicate, LinkedHashMap <String, List<MCD>>>();
		
		
		
		for (int i = 0; i < query.getPredicates().size(); i++){ 
			Predicate subGoal = query.getPredicates().get(i); 			
			coverageDomain = getCoverageDomain(subGoal, mcds); 			
			abstractService = new LinkedHashMap<String, List<MCD>>(); 
			abstractService.putAll(rankMCDS(coverageDomain)); 
			organiser.put(subGoal, abstractService);
		}		
		
	}
	
	public static Hashtable<Predicate, LinkedHashMap <String, List<MCD>>> getOrganiser (){
		return organiser;
	}
	
	public static List<Rewriting> getDesiredNumberOfRewritings (DatalogQuery query, long desiredNumberOfRewritings){ 
		List<Rewriting> rewritings = new ArrayList<Rewriting>();
				
		try {
			setDesiredNumberOfRewriting(rewritings, new ArrayList<MCD>(), query.getPredicates(), query, desiredNumberOfRewritings);
		} catch (Exception e) {
			// The desired number of rewriting is reached
		}
		
		return rewritings;	
	}
	
	private static void setDesiredNumberOfRewriting(List<Rewriting> rewritings, List<MCD> rewritingPrefix, List<Predicate> subGoals, DatalogQuery query, long desiredNumberOfRewritings) throws Exception{
		if (rewritings.size() >= desiredNumberOfRewritings)
			throw new Exception("The desired number of rewriting is reached ...");
		
		if (subGoals.size() == 0){
			if (isRewriting(rewritingPrefix, query))
				rewritings.add(new Rewriting(rewritingPrefix, query));
		} else {
			List<MCD> sortedCoverageDomain = getSortedCoverageDomain(subGoals.get(0));
			for (int i = 0 ; i < sortedCoverageDomain.size(); i++){
				MCD mcd = sortedCoverageDomain.get(i);
				List<MCD> newRewritingPrefix = new ArrayList<MCD>(rewritingPrefix);
				newRewritingPrefix.add(mcd);
				List<Predicate> remainingPredicatesToCover = getRemainingPredicatesToCover(mcd, subGoals);
				setDesiredNumberOfRewriting(rewritings, newRewritingPrefix, remainingPredicatesToCover, query, desiredNumberOfRewritings);
			}
		}
	}
	
	private static List<Predicate> getRemainingPredicatesToCover(MCD mcd, List<Predicate> subGoals){
		List<Predicate> remainingPredicatesToCover = new ArrayList<Predicate>();
		for (int i = 0; i < subGoals.size(); i++){
			if (! mcd.getSubgoals().contains(subGoals.get(i)))
				remainingPredicatesToCover.add(subGoals.get(i));
		}
		
		return remainingPredicatesToCover;
	}
	
	private static List<MCD> getSortedCoverageDomain (Predicate abstractService){
		if (abstractService.getSortedCoverageDomain() != null) // If it's already computed !
			return abstractService.getSortedCoverageDomain();
		
		LinkedHashMap<String, List<MCD>> rankedPCDs = organiser.get(abstractService);
		List<MCD> result = new ArrayList<MCD>();
		String rank;
		Iterator <String> iterator = rankedPCDs.keySet().iterator();

		while (iterator.hasNext()){
					rank = iterator.next();
					result.addAll(rankedPCDs.get(rank));
				}	
		if (abstractService.getSortedCoverageDomain() == null)
			abstractService.setSortedCoverageDomain(result);
		return result;
	}
		
	private static List<MCD> getCoverageDomain(Predicate abstractService, List<MCD> mcds){
		List<MCD> coverageDomain = new LinkedList<MCD>();
		
		for (int i = 0; i < mcds.size(); i++){
			MCD mcd = mcds.get(i);
			List<Predicate> coveredSubGoals = mcd.getSubgoals();
			for (int j = 0; j < coveredSubGoals.size(); j++ ){
				
				if (coveredSubGoals.get(j).equals(abstractService)){
					coverageDomain.add(mcd);
					j = coveredSubGoals.size();
				}
			}			
		}

		return coverageDomain;
	}
	
	private static LinkedHashMap <String, List<MCD>> rankMCDS (List<MCD> mcds){
		LinkedHashMap <String, List<MCD>> rank = new LinkedHashMap <String, List<MCD>>();
		
		if (mcds.size() > 0){
			
			List<MCD> mcdList = new LinkedList<MCD>();
			mcdList.add(0, mcds.get(0)); 
			rank.put("" + mcds.get(0).getRank(), mcdList);
			
			for (int i = 1 ; i < mcds.size(); i++){
				
				if (rank.keySet().contains(mcds.get(i).getRank()+"")){ // le rank existe d�j�
					rank.get(mcds.get(i).getRank()+"").add(mcds.get(i));
				} else {
					mcdList = new LinkedList<MCD>();
					mcdList.add(0, mcds.get(i));
					rank.put("" + mcds.get(i).getRank(), mcdList);
				}
			}
			
		}
		
		sortRankedMCDs(rank); // sort, for each subgoal, the ranks
		
		return rank;
	}
	
	private static void sortRankedMCDs (LinkedHashMap <String, List<MCD>> ranks){
		LinkedHashMap <String, List<MCD>> temp = new LinkedHashMap<String, List<MCD>> ();		
		LinkedList<String> rankValues = new LinkedList<String>();
		for (String rank: ranks.keySet()){
			rankValues.add(rank);
		}
		
		sortDoubleString (rankValues); // decreasing ...
		
		for (int i = 0; i < rankValues.size(); i++)
			temp.put(rankValues.get(i), ranks.get(rankValues.get(i)));
		
		ranks.clear();
		ranks.putAll(temp);
	}
	
	private static void sortDoubleString (List<String> rankValues){ // decreasing ...
		boolean exchange;// = false;
		int n = rankValues.size();		
		
		do {
			exchange = false;
			for (int i = 0 ; i < (n - 1); i++){
				if (Double.valueOf(rankValues.get(i)).doubleValue() < Double.valueOf(rankValues.get(i+1)).doubleValue()){					
					String tampon = rankValues.get(i);
					rankValues.set(i, rankValues.get(i+1));
					rankValues.set(i+1, tampon);
					exchange = true;
				}
			}
			n--;
		} while (exchange);
	}
	
	private static boolean isRewriting(List<MCD> mcds, DatalogQuery query) {
		int countPredicates = 0;

		for (MCD mcd : mcds) {
			countPredicates += mcd.numberOfSubgoals();
		}

		// compare total number of predicates with number of query subgoals
		if (countPredicates != query.numberOfPredicates()) {
			return false;
		}

		// test pairwise disjoint
		for (int i = 0; i < mcds.size(); i++) {
			for (int j = 0; j < mcds.size(); j++) {
				if (i != j) {
					MCD mcd1 = mcds.get(i);
					MCD mcd2 = mcds.get(j);
					if (!mcd1.isDisjoint(mcd2)) {
						return false;
					}
				}
			}
		}

		// x exists in C1 and C2 ==> it must be mapped to the same constant
		for (int i = 0; i < mcds.size(); i++) {
			MCD mcd1 = mcds.get(i);
			Mapping constMap1 = mcd1.mappings.constMap;
			for (int j = 0; j < mcds.size(); j++) {
				if (i != j) {
					MCD mcd2 = mcds.get(j);
					Mapping constMap2 = mcd2.mappings.constMap;
					for (PredicateElement elem : constMap1.arguments) {
						if ((constMap2.containsArgument(elem) && !(constMap1
								.getFirstMatchingValue(elem).equals(constMap2
								.getFirstMatchingValue(elem))))) {
							return false;
						}
					}
				}
			}
		}
		return true;
	}

}
