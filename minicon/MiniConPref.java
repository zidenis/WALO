
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
 * Created on 10.09.2014 - Cheikh Ba
 * 
 * Based On Programming project - Implementation of MiniCon algorithm (Created on 20.01.2005)
 * 
 */
package minicon;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import preference.Organiser;
import preference.PreferencesFileParser;
import datalog.DatalogQuery;
import datalog.Predicate;

/**
 * 
 * MiniCon is the main class of the implementation of the MiniCon algorithm. It
 * contains the main method to start the program. It uses class InputHandler to
 * obtain parsed user input in form of a MiniCon object. It contains the query
 * and a list of views. Basically, the algorithm consists of three steps: 1.
 * forming the MCDs, 2. combining the MCD, and 3. removing redundant subgoals
 * The last part is optional.
 * 
 * @author Kevin Irmscher
 */
public class MiniConPref {
	
	private static int testID;	
	private static long numberOfRequiredRewritings;

	/** query Object used by algorithm */
	private DatalogQuery query;

	/** list of views used by algorithm */
	private List<DatalogQuery> views;

	/** list of MCDs created by algorithm */
	private List<MCD> mcds;

	/** list of rewritings created by the algorithm */ 
	private List<Rewriting> rewritings;

	/**
	 * MiniCon constructor
	 * 
	 * @param query
	 *            query obtained from the parser
	 * @param views
	 *            list of views obtained from the parser
	 */
	public MiniConPref(DatalogQuery query, List<DatalogQuery> views) {
		this.query = query;
		this.views = views;
		this.mcds = new ArrayList<MCD>();
		this.rewritings = new ArrayList<Rewriting>();
	}

	/**
	 * Main method will be called to start the algorithm. It uses class
	 * InputHandler to handle the arguments provided by parameter args.
	 * InputHandler will return a MiniCon object which contains the query and a
	 * list of views.
	 * 
	 * @param args
	 *            -v : verbose mode (print MCDs);
	 * 
	 * -f FILE.XML ID : read testcase with ID from file;
	 * 
	 * -sql : SQL input mode;
	 * 
	 * -r : remove redundancies
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("MiniCon WALO Algorithm");
		
		//testID = 5;
		//numberOfRequiredRewritings = 4;
		
		if (args.length < 1 || args.length > 2){
			System.out.println("USAGE: java minicon.MiniConPref testId [numberOfRequiredRewritings]");
			System.exit(1);
		}
		if (args.length == 1){
			testID = Integer.valueOf(args[0]).intValue();
			numberOfRequiredRewritings = 999999999999999999L; // all rewritings are desired !
		} else {
			testID = Integer.valueOf(args[0]).intValue();
			numberOfRequiredRewritings = Long.valueOf(args[1]).longValue();
		}
		
		long start = ManagementFactory.getThreadMXBean().getCurrentThreadUserTime();
					
		MiniConPref mc = InputHandlerPref.handleArguments(new String[]{"-f", "testcases.xml", "" + testID});		
		
		if (mc != null) {
			mc.printQuery(); 			// commented for time evaluation
			mc.printViews();			// commented for time evaluation			
			mc.startMiniCon();
			mc.printRewritings();		// commented for time evaluation
			
		}	
		long time = ManagementFactory.getThreadMXBean().getCurrentThreadUserTime() - start;
		System.out.println("Done in: " + time/1000000.0 + " ms");
	}

	/**
	 * The method will execute the actual algorithm. Three method calls will be
	 * performed regarding to the three parts of the algorithm. 1. forming MCDs,
	 * 2. combining MCDs, 3. remove redundancies; the last call depends on
	 * whether argument -r is provided
	 */
	public void startMiniCon() {
				formMCDs();
		
		/*C.BA*/ 
		// set the MCD preferences ...
		try {
			
			PreferencesFileParser.setMCDPreferences (mcds, "preferences.xml", testID); 
			Organiser.createOrganiser(mcds, query); 
				
			rewritings = Organiser.getDesiredNumberOfRewritings(query, numberOfRequiredRewritings);	
	
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("OUPSS !!! " + e);
			System.out.println("mcds.size() = " + mcds.size());
			System.out.println("rewritings.size() = " + rewritings.size());
		}				
			
	}

	/**
	 * The method will create the MCDs that are possible for the given query and
	 * views. Every subgoal of the query will be considered separately. For each
	 * subgoal the algorithm will create all possible mappings to each given
	 * view. For every obtained valid mapping an MCD will be created. Using the
	 * class MCD, it will be checked whether all properties are fulfilled and if
	 * necessary, the MCD will be extended. If the MCD is valid, it will be
	 * added to the list of MCDs. Finally duplicate MCDs will be removed from
	 * the list.
	 */
	private void formMCDs() {

		// subgoal of the query
		List<Predicate> subgoals = query.getPredicates();

		for (Predicate subgoal : subgoals) {
			// System.out.println("\n current subgoal " + subgoal);

			// for every view try to create mappings
			for (DatalogQuery view : views) {

				List<MCDMappings> mappings = createMapping(subgoal, view);

				// for every mapping created check whether properties are
				// fulfilled
				for (MCDMappings map : mappings) {

					// create MCD
					MCD mcd = new MCD(subgoal, query, view, map);

					// MCD can be extend to fulfill properties
					if (mcd.fulfillProperty()) {
						mcds.add(mcd);
					}
				}
			}
		}
		removeDuplicates();
	}

	/**
	 * Called by formMCDs. The given query subgoal is tested if it can be mapped
	 * to every predicate of the view. If a mapping is possible, a new mapping
	 * object is added to the list of mappings.
	 * 
	 * @param subgoal
	 *            current query subgoal
	 * @param view
	 *            current view
	 * @return list of possible mappings
	 */
	private List<MCDMappings> createMapping(Predicate subgoal, DatalogQuery view) {
		List<Predicate> viewPredicates = view.getPredicates();
		List<MCDMappings> mappings = new ArrayList<MCDMappings>();

		for (Predicate viewPred : viewPredicates) {

			if (subgoal.canBeMapped(viewPred)) {
				mappings.add(new MCDMappings(subgoal, viewPred));
			}
		}
		return mappings;
	}

	/**
	 * Called by formMCDs. The method will remove duplicate MCDs. First the
	 * empty list 'noDuplicates' will be created. By iterating through the
	 * member list 'mcds', each MCD will be added to the noDuplicates list only
	 * if there is no duplicate already contained in the list. The reference of
	 * the member list mcds will finally be linked to the list noDuplicates. The
	 * equality of the MCDs is determined by method 'equals' in class MCD.
	 */
	private void removeDuplicates() {

		List<MCD> noDuplicates = new ArrayList<MCD>();

		for (MCD mcd : mcds) {
			boolean contains = false;

			for (MCD noDup : noDuplicates) {
				if (mcd.equals(noDup)) {
					contains = true;
				}
			}
			if (!contains) {
				noDuplicates.add(mcd);
			}
		}
		mcds = noDuplicates;
	}

	/**
	 * Print rewritings
	 */
	private void printRewritings() {
		if (!rewritings.isEmpty()) {
			System.out.println("\nRewriting(s):");
			for (Rewriting rw : rewritings) {
				System.out.println(rw);
			}
		}
	}
	
	/**
	 * Print query provided by user
	 */
	private void printQuery() {
		System.out.println("\nQuery: " + query);
	}

	/**
	 * Print views provided by user
	 */
	private void printViews() {
		for (DatalogQuery view : views) {
			System.out.println("View: " + view);

		}
	}

	/**
	 * Returns list of Rewriting objects created by the algorithm.
	 * 
	 * @return list of Rewriting objects
	 */
	public List<Rewriting> getRewritings() {
		return rewritings;
	}

}
