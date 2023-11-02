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
import jmetal.util.wrapper.XReal;

import java.util.*;

/**
 * This class implements a bit flip mutation operator.
 * NOTE: the operator is applied to binary or integer solutions, considering the
 * whole solution as a single encodings.variable.
 */
public class DissatisfactionMutation extends Mutation {
  /**
   * Valid solution types to apply this operator
   */
	  private static final List VALID_TYPES = Arrays.asList(BinarySolutionType.class,
		  BinaryRealSolutionType.class,
			  MOKP_BinarySolution.class,
		  IntSolutionType.class) ;

	  private int mutationRepeats_ = 0;
	  private MOKP_Problem problem = null;         // The problem to solve

	/**
	 * Constructor
	 * Creates a new instance of the Bit Flip mutation operator
	 */
	public DissatisfactionMutation(HashMap<String, Object> parameters) {
		super(parameters) ;
  	if (parameters.get("repeats") != null)
  		mutationRepeats_ = (int) parameters.get("repeats") ;
  	if (parameters.get("problem") != null)
		problem = (MOKP_Problem) parameters.get("problem") ;
	}

	/**
	 * Perform the mutation operation
	 * @param solution The solution to mutate
	 * @throws JMException
	 */

	public double[][] doMutation(/*double temperature, */Solution solution) throws JMException {
		try {
			boolean[] pref_vector = problem.getUserPreferenceVector();
			int numberOfUsers = problem.getNumberOfUsers();
			int numberOfConstraints = problem.getNumberOfConstraints();
			int numberOfItems = problem.getNumberOfItems();
			int[] covered = solution.getDeviceToPreferenceMapping();
			int[] coveredReverse = solution.getReverseDeviceToPreferenceMapping();
			double[] w = problem.getWeightOfItems();
			XReal costs = problem.getCostOfUsage();
			double[][] positionsChanged = new double[mutationRepeats_][3];
			int index = 0;

			for (int i = 0; i < solution.getDecisionVariables().length; i++) {
				Binary bin = (Binary) solution.getDecisionVariables()[i];
				int numOfBits = bin.getNumberOfBits();

				for (int r=0; r<mutationRepeats_; r++){
					int tries = 0;
                    int preferencePosition;
                    do {
						preferencePosition = PseudoRandom.randInt(0, numOfBits-1);
						tries++;
						if (tries > 20)
							break;
                    } while (coveredReverse[preferencePosition] != -1 || !pref_vector[preferencePosition]);

					if (tries > 20)
						break;

                    int u, c, it;
                    u = preferencePosition / (numberOfConstraints * numberOfItems);
                    c = preferencePosition % (numberOfConstraints * numberOfItems) / numberOfItems;
					int userAndTime = u * (numberOfConstraints * numberOfItems) + c * numberOfItems;
                    it = (userAndTime == 0 ? preferencePosition : (preferencePosition % userAndTime));

                    int positionToMake1 = -1;
                    int newtimeslot = -1;
                    boolean goLeft = false;
                    boolean exitLoop = false;
                    int step = 0;
                    while(true) {
						if (goLeft == false) {
							if (exitLoop)
								break;
							exitLoop = true;
							step++;
						}
						if (goLeft) {
							newtimeslot = c-step;
							if (newtimeslot >= 0) {
								positionToMake1 = preferencePosition - step * numberOfItems;
								exitLoop = false;
							}
						}
                    	if (!goLeft) {
							newtimeslot = c+step;
							if (newtimeslot < numberOfConstraints) {
								positionToMake1 = preferencePosition + step * numberOfItems;
								exitLoop = false;
							}
						}
						goLeft = !goLeft;
						if (positionToMake1 != -1 &&
								!pref_vector[positionToMake1] && covered[positionToMake1] == -1)
							break;
                    }

					if (!exitLoop) {
						double cost = w[it] * costs.getValue(newtimeslot);
						//if (cost <= temperature) {
							positionsChanged[index][0] = positionToMake1; //position
							positionsChanged[index][1] = step; //step
							positionsChanged[index][2] = cost; //cost
							index++;

							coveredReverse[preferencePosition] = positionToMake1;
							covered[positionToMake1] = preferencePosition;
						//}
					}

                }
			}
			return positionsChanged;

		} catch (ClassCastException e1) {
			Configuration.logger_.severe("BitFlipMutation.doMutation: " +
					"ClassCastException error" + e1.getMessage());
			Class cls = String.class;
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
			Configuration.logger_.severe("DissatisfactionMutation.execute: the solution " +
					"is not of the right type. The type should be 'Binary', " +
					"'BinaryReal' or 'Int', but " + solution.getType() + " is obtained");

			Class cls = String.class;
			String name = cls.getName();
			throw new JMException("Exception in " + name + ".execute()");
		} // if 

		//double temperature = (double) this.getParameter("temperature") ;
		return doMutation(/*temperature, */solution);
	} // execute
} // BitFlipMutation
