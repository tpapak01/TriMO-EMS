//  BitFlipMutation.java
//
//  Author:
//       Antonio J. Nebro <antonio@lcc.uma.es>
//       Juan J. Durillo <durillo@lcc.uma.es>
//
//  Copyright (c) 2011 Antonio J. Nebro, Juan J. Durillo
//
//  This program is free software: you can redistribute it and/or modify
//  it under the terms of the GNU Lesser General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU Lesser General Public License for more details.
// 
//  You should have received a copy of the GNU Lesser General Public License
//  along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jmetal.operators.mutation;

import jmetal.core.Problem;
import jmetal.core.Solution;
import jmetal.encodings.solutionType.BinaryRealSolutionType;
import jmetal.encodings.solutionType.BinarySolutionType;
import jmetal.encodings.solutionType.IntSolutionType;
import jmetal.encodings.variable.Binary;
import jmetal.encodings.variable.MOKP_BinarySolution;
import jmetal.problems.MOKP_Problem;
import jmetal.util.Configuration;
import jmetal.util.JMException;
import jmetal.util.PseudoRandom;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * This class implements a bit flip mutation operator.
 * NOTE: the operator is applied to binary or integer solutions, considering the
 * whole solution as a single encodings.variable.
 */
public class BitFlipMutation extends Mutation {
  /**
   * Valid solution types to apply this operator 
   */
  private static final List VALID_TYPES = Arrays.asList(BinarySolutionType.class,
      BinaryRealSolutionType.class,
		  MOKP_BinarySolution.class,
      IntSolutionType.class) ;

  private Double mutationProbability_ = null ;
  private static MOKP_Problem problemMOKP = null;         // The problem to solve
  
	/**
	 * Constructor
	 * Creates a new instance of the Bit Flip mutation operator
	 */
	public BitFlipMutation(HashMap<String, Object> parameters) {
		super(parameters) ;
  	if (parameters.get("probability") != null)
  		mutationProbability_ = (Double) parameters.get("probability") ;
  	if (parameters.get("problem") != null)
		problemMOKP = (MOKP_Problem) parameters.get("problem") ;
	}

	/**
	 * Perform the mutation operation
	 * @param probability Mutation probability
	 * @param solution The solution to mutate
	 * @throws JMException
	 */
	public void doMutation(double probability, Solution solution) throws JMException {
		try {
			if ((solution.getType().getClass() == BinarySolutionType.class) ||
					(solution.getType().getClass() == BinaryRealSolutionType.class) ||
					(solution.getType().getClass() == MOKP_BinarySolution.class)) {

				for (int i = 0; i < solution.getDecisionVariables().length; i++) {
					Binary bin = (Binary) solution.getDecisionVariables()[i];
					int numOfBits = bin.getNumberOfBits();
					for (int j = 0; j < numOfBits; j++) {
						if (PseudoRandom.randDouble() < probability) {
							bin.bits_.flip(j);
						}
					}
				}

				for (int i = 0; i < solution.getDecisionVariables().length; i++) {
					((Binary) solution.getDecisionVariables()[i]).decode();
				}
			} // if
			else { // Integer representation
				for (int i = 0; i < solution.getDecisionVariables().length; i++)
					if (PseudoRandom.randDouble() < probability) {
						int value = PseudoRandom.randInt(
								(int)solution.getDecisionVariables()[i].getLowerBound(),
								(int)solution.getDecisionVariables()[i].getUpperBound());
						solution.getDecisionVariables()[i].setValue(value);
					} // if
			} // else
		} catch (ClassCastException e1) {
			Configuration.logger_.severe("BitFlipMutation.doMutation: " +
					"ClassCastException error" + e1.getMessage());
			Class cls = java.lang.String.class;
			String name = cls.getName();
			throw new JMException("Exception in " + name + ".doMutation()");
		}
	} // doMutation

	public void doCustomMutation(double probability, Solution solution) throws JMException {
		try {
			boolean[] pref_vector = problemMOKP.getUserPreferenceVector();
			int numberOfUsers = problemMOKP.getNumberOfUsers();
			int numberOfConstraints_ = problemMOKP.getNumberOfConstraints();
			int numberOfItems = problemMOKP.getNumberOfItems();

			for (int v = 0; v < solution.getDecisionVariables().length; v++) {
				Binary bin = (Binary) solution.getDecisionVariables()[v];
				int[] covered = solution.getDeviceToPreferenceMapping();
				int[] coveredReverse = solution.getReverseDeviceToPreferenceMapping();


				for (int u = 0; u < numberOfUsers; u++) { // for each user

					int userIndex = u * numberOfConstraints_;
					int l = 0;
					for (int i = userIndex; i < userIndex + numberOfConstraints_; i++) { // for each objective
						int itemIndex = i * numberOfItems;
						for (int j = itemIndex; j < itemIndex + numberOfItems; j++) { // for each bit
							if (PseudoRandom.randDouble() < probability) {
								//just delete current mapping
								if (bin.getIth(j)) {
									int preferenceCovered = covered[j];
									covered[j] = -1;
									coveredReverse[preferenceCovered] = -1;
								} else {
									//preferred
									if (pref_vector[j]) {
										covered[j] = j;
										coveredReverse[j] = j;
									//not preferred
									} else {
										//find the satisfied slot, if it exists. If not, turn device OFF
										int misplacement = 1;
										int selected = -1;
										int behind = l-1;
										int front = l+1;
										while (behind >= 0 || front < numberOfConstraints_){
											if (behind >= 0) {
												int new_position = j-misplacement*numberOfItems;
												boolean pref_behind = pref_vector[new_position];
												if (pref_behind && coveredReverse[new_position] == -1) {
													selected = new_position;
													break;
												}
												behind--;
											}
											if (front < numberOfConstraints_) {
												int new_position = j+misplacement*numberOfItems;
												boolean pref_front = pref_vector[new_position];
												if (pref_front && coveredReverse[new_position] == -1) {
													selected = new_position;
													break;
												}
												front++;
											}
											misplacement++;
										}
										if (selected != -1) {
											//now that we found someone to cover, add satisfaction and secure that
											//position so that noone else claims it in the future
											covered[j] = selected;
											coveredReverse[selected] = j;
										} else {
											//no slot found means ON-device does not belong here
											bin.bits_.flip(j);
										}

									} //end of "not preferred so must find device to satisfy"
								}
								bin.bits_.flip(j);
							} // end of probability check
						} // for j
						l++;
					} // for i
				} //for u


			}

		} catch (ClassCastException e1) {
			Configuration.logger_.severe("BitFlipMutation.doMutation: " +
					"ClassCastException error" + e1.getMessage());
			Class cls = java.lang.String.class;
			String name = cls.getName();
			throw new JMException("Exception in " + name + ".doMutation()");
		}
	}

	/**
	 * Executes the operation
	 * @param object An object containing a solution to mutate
	 * @return An object containing the mutated solution
	 * @throws JMException 
	 */
	public Object execute(Object object) throws JMException {
		Solution solution = (Solution) object;

		if (!VALID_TYPES.contains(solution.getType().getClass())) {
			Configuration.logger_.severe("BitFlipMutation.execute: the solution " +
					"is not of the right type. The type should be 'Binary', " +
					"'BinaryReal' or 'Int', but " + solution.getType() + " is obtained");

			Class cls = java.lang.String.class;
			String name = cls.getName();
			throw new JMException("Exception in " + name + ".execute()");
		} // if 

		if (problemMOKP == null)
			doMutation(mutationProbability_, solution);
		else
			doCustomMutation(mutationProbability_, solution);
		return solution;
	} // execute
} // BitFlipMutation
