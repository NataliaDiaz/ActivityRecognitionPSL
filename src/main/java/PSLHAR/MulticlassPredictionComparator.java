package PSLHAR;

/*
 * This file is part of the PSL software.
 * Copyright 2011-2013 University of Maryland
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import edu.umd.cs.psl.database.Database;
import edu.umd.cs.psl.evaluation.statistics.ConfusionMatrix;
import edu.umd.cs.psl.evaluation.statistics.PredictionStatistics;
import edu.umd.cs.psl.evaluation.statistics.ResultComparator;
import edu.umd.cs.psl.evaluation.statistics.filter.AtomFilter;
import edu.umd.cs.psl.evaluation.statistics.filter.MaxValueFilter;
import edu.umd.cs.psl.model.argument.GroundTerm;
import edu.umd.cs.psl.model.atom.GroundAtom;
import edu.umd.cs.psl.model.predicate.Predicate;
import edu.umd.cs.psl.util.database.Queries;

/**
 * Computes statistics for multiclass prediction.
 * 
 * NOTE: Currently only works with binary predicates of the form (example, label) or (label, example),
 * where example is a single {@link GroundTerm} and label is a single {@link IntegerAttribute}.
 * 
 * @author Ben
 *
 */
public class MulticlassPredictionComparator implements ResultComparator {

	private final Database predDB;
	private Database truthDB;

	/**
	 * Constructor
	 * 
	 * @param predDB Predictions database. Target predicates must be closed.
	 */
	public MulticlassPredictionComparator(Database predDB) {
		this.predDB = predDB;
		this.truthDB = null;
	}
	
	/**
	 * Sets the ground truth database.
	 * 
	 * @param truthDB Ground truth database. Target predicates must be closed.
	 */
	@Override
	public void setBaseline(Database truthDB) {
		this.truthDB = truthDB;
	}

	/**
	 * TODO: Does it make sense to have this method for this class?
	 */
	@Override
	public void setResultFilter(AtomFilter af) {
		// TODO Auto-generated method stub
	}
	
	/**
	 * Returns prediction statistics in the form of a confusion matrix.
	 * 
	 * @param p A predicate
	 * @param labelMap A map indicating the mapping from the label to an index.
	 * @param labelIndex The index of the label in each example's terms.
	 * @return A {@link MulticlassPredictionStatistics}.
	 */
	public PredictionStatistics compare(Predicate p, Map<GroundTerm,Integer> labelMap, int labelIndex) {
		/* Allocate a square confusion matrix. */
		int numClass = labelMap.size();
		int[][] cm = new int[numClass][numClass];
		
		/* Get all predicted atoms using max-score label. */
		Map<Example,Integer> pred = getAllMaxScoreAtoms(predDB, p, labelMap, labelIndex);
		
		/* Get all of the ground truth atoms. */
		Map<Example,Integer> truth = getAllMaxScoreAtoms(truthDB, p, labelMap, labelIndex);
		
		/* Iterate over all prediction and compare to ground truth. */
		for (Entry<Example,Integer> entry : pred.entrySet()) {
			Example ex = entry.getKey();
			System.out.println(p+"    Example: "+entry.toString() +  "  ");//+data.getUniqueID(ex.toString()));
			if (!truth.containsKey(ex))//new UniqueID(Integer.valueOf(ex.toString()))))//ex))
				throw new RuntimeException(p+" Missing ground truth for example " + ex.toString()+" (using labelMap and labelIndex: "+labelMap+" "+labelIndex);
			int predLabel = entry.getValue().intValue();
			int trueLabel = truth.get(ex);
			++cm[trueLabel][predLabel];
		}
		
		return new MulticlassPredictionStatistics(new ConfusionMatrix(cm));
	}
	
	/**
	 * Returns all of the examples for a given predicate, along with their max-scoring labels.
	 * 
	 * @param db
	 * @param p
	 * @param labelMap
	 * @param labelIndex: the index of the parameter containing the label, in te target predicate
	 * @return
	 */
	private Map<Example,Integer> getAllMaxScoreAtoms(Database db, Predicate p, Map<GroundTerm,Integer> labelMap, int labelIndex) {
		Map<Example,Integer> atoms = new HashMap<Example,Integer>();
		AtomFilter maxFilter = new MaxValueFilter(p, labelIndex);
		Iterator<GroundAtom> iter = maxFilter.filter(Queries.getAllAtoms(db, p).iterator());
		while (iter.hasNext()) {
			GroundAtom predAtom = iter.next();
			//if (predAtom.getValue() == 0.0)
				//throw new RuntimeException("Max value "+" for predAtom "+predAtom+" and labelIndex "+labelIndex+" does not exist.");
			GroundTerm[] terms = predAtom.getArguments();
			GroundTerm[] exTerms = new GroundTerm[terms.length-1];
			int i = 0;
			for (int j = 0; j < terms.length; j++) {
				//System.out.println("terms: "+terms[j]);				
				if (j != labelIndex)
					exTerms[i++] = terms[j];
			}
			GroundTerm label = terms[labelIndex];
			atoms.put(new Example(exTerms), labelMap.get(label));
		}
		return atoms;
	}
	
	/**
	 * Wrapper for groupings of ground terms dubbed "examples".
	 * 
	 * TODO: Current version assumes that the example is composed of a single term.
	 * Needs to be changed to generalize this code.
	 * 
	 * @author Ben
	 */
	class Example {
		
		private final GroundTerm[] terms;
		
		public Example(GroundTerm[] terms) {
			this.terms = terms;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Example))
				return false;
			Example other = (Example) obj;
			if (this.terms.length != other.terms.length)
				return false;
			for (int i = 0; i < terms.length; i++) {
				if (!this.terms[i].equals(other.terms[i]))
					return false;
			}
			return true;
		}

		@Override
		public int hashCode() {
			return terms[0].hashCode();
		}
		
		@Override
		public String toString() {
			return terms[0].toString();
		}
	}
}