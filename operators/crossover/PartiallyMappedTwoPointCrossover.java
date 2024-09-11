//  TwoPointCrossover.java
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

package jmetal.operators.crossover;

import jmetal.core.Solution;
import jmetal.encodings.solutionType.BinaryRealSolutionType;
import jmetal.encodings.solutionType.BinarySolutionType;
import jmetal.encodings.solutionType.IntSolutionType;
import jmetal.encodings.variable.Binary;
import jmetal.encodings.variable.MOKP_BinarySolution;
import jmetal.util.Configuration;
import jmetal.util.JMException;
import jmetal.util.PseudoRandom;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;

/**
 * This class allows to apply a Two Point crossover operator using two parent
 * solutions.
 */
public class PartiallyMappedTwoPointCrossover extends Crossover {
  /**
   * Valid solution types to apply this operator
   */
  private static final List VALID_TYPES = Arrays.asList(BinarySolutionType.class,
  		                                            BinaryRealSolutionType.class,
                                                    MOKP_BinarySolution.class,
  		                                            IntSolutionType.class) ;

  private Double crossoverProbability_ = null;

  /**
   * Constructor
   * Creates a new instance of the two point crossover operator
   */
  public PartiallyMappedTwoPointCrossover(HashMap<String, Object> parameters) {
  	super(parameters) ;
  	if (parameters.get("probability") != null)
  		crossoverProbability_ = (Double) parameters.get("probability") ;
  } // PartiallyMappedCrossoverCustom


  /**
   * Perform the crossover operation.
   * @param probability Crossover probability
   * @param parent1 The first parent
   * @param parent2 The second parent
   * @return An array containig the two offsprings
   * @throws JMException
   */
  public Solution[] doCrossover(double probability,
          Solution parent1,
          Solution parent2) throws JMException {
    Solution[] offSpring = new Solution[2];
    offSpring[0] = new Solution(parent1);
    offSpring[1] = new Solution(parent2);
    try {
      if (PseudoRandom.randDouble() < probability) {

        if ((parent1.getType().getClass() == BinarySolutionType.class) ||
            (parent1.getType().getClass() == BinaryRealSolutionType.class ||
                (parent1.getType().getClass() == MOKP_BinarySolution.class))) {

          int[] covered0 = offSpring[0].getDeviceToPreferenceMapping();
          int[] coveredReverse0 = offSpring[0].getReverseDeviceToPreferenceMapping();
          int[] covered1 = offSpring[1].getDeviceToPreferenceMapping();
          int[] coveredReverse1 = offSpring[1].getReverseDeviceToPreferenceMapping();

          int numOfVars = parent1.getDecisionVariables().length;
          for (int var = 0; var < numOfVars; var++) {
            int totalNumberOfBits = ((Binary) parent1.getDecisionVariables()[var]).getNumberOfBits();

            //2. Calculate the point to make the crossover
            int crossoverPoint = PseudoRandom.randInt(0, totalNumberOfBits - 1);

            int crossoverPointEnd = PseudoRandom.randInt(crossoverPoint, totalNumberOfBits - 1);

            //5. Make the crossover into the gene;
            BitSet offSpring0, offSpring1;
            offSpring0 = ((Binary) offSpring[0].getDecisionVariables()[var]).bits_;
            offSpring1 = ((Binary) offSpring[1].getDecisionVariables()[var]).bits_;

            int i = crossoverPoint-1;
            do {
              int i0 = offSpring0.nextSetBit(i+1);
              int i1 = offSpring1.nextSetBit(i+1);
              if ((i0 > crossoverPointEnd && i1 > crossoverPointEnd) || (i0 == -1 && i1 == -1)) break;
              if (i0 == i1) {
                i = i0;
                int covered0_current = covered0[i];
                int covered1_current = covered1[i];
                if (covered0_current != covered1_current) {
                  //erase who you currently cover
                  coveredReverse0[covered0_current] = -1;
                  coveredReverse1[covered1_current] = -1;

                  //i0: cover new and redirect any other spot covering the new to the old
                  covered0[i] = covered1_current;
                  if (coveredReverse0[covered1_current] != -1) {
                    int previousCoverer = coveredReverse0[covered1_current];
                    //covered0[previousCoverer] = -1;
                    //offSpring0.set(previousCoverer, false);
                    covered0[previousCoverer] = covered0_current;
                    coveredReverse0[covered0_current] = previousCoverer;
                  }
                  coveredReverse0[covered1_current] = i;

                  //i1: cover new and redirect any other spot covering the new to the old
                  covered1[i] = covered0_current;
                  if (coveredReverse1[covered0_current] != -1) {
                    int previousCoverer = coveredReverse1[covered0_current];
                    //covered1[previousCoverer] = -1;
                    //offSpring1.set(previousCoverer, false);
                    covered1[previousCoverer] = covered1_current;
                    coveredReverse1[covered1_current] = previousCoverer;
                  }
                  coveredReverse1[covered0_current] = i;
                }
              } else if (i1 == -1 || (i0 != -1 && i0 < i1)) {
                i = i0;
                int covered0_current = covered0[i];

                //i0:
                //erase who you currently cover
                coveredReverse0[covered0_current] = -1;
                covered0[i] = -1;
                offSpring0.set(i, false);

                //i1:
                offSpring1.set(i, true);
                //i1: cover new and delete any other spot covering the new
                covered1[i] = covered0_current;
                if (coveredReverse1[covered0_current] != -1) {
                  int previousCoverer = coveredReverse1[covered0_current];
                  covered1[previousCoverer] = -1;
                  offSpring1.set(previousCoverer, false);
                }
                coveredReverse1[covered0_current] = i;
              } else {
                i = i1;
                int covered1_current = covered1[i];

                //i0:
                offSpring0.set(i, true);
                //i0: cover new and delete any other spot covering the new
                covered0[i] = covered1_current;
                if (coveredReverse0[covered1_current] != -1) {
                  int previousCoverer = coveredReverse0[covered1_current];
                  covered0[previousCoverer] = -1;
                  offSpring0.set(previousCoverer, false);
                }
                coveredReverse0[covered1_current] = i;

                //i1:
                //erase who you currently cover
                coveredReverse1[covered1_current] = -1;
                covered1[i] = -1;
                offSpring1.set(i, false);
              }
            } while (true);

            /*
            //6. Apply the crossover to the other variables
            for (int i = 1; i < numOfVars; i++) {
              offSpring[0].getDecisionVariables()[i] =
                      parent2.getDecisionVariables()[i].deepCopy();

              offSpring[1].getDecisionVariables()[i] =
                      parent1.getDecisionVariables()[i].deepCopy();
            }

             */

          }
          /*
          //7. Decode the results
          for (int i = 0; i < numOfVars; i++) {
            ((Binary) offSpring[0].getDecisionVariables()[i]).decode();
            ((Binary) offSpring[1].getDecisionVariables()[i]).decode();
          }

           */

        } // Binary or BinaryReal
        else { // Integer representation
          int crossoverPoint = PseudoRandom.randInt(0, parent1.numberOfVariables() - 1);
          int valueX1;
          int valueX2;
          for (int i = crossoverPoint; i < parent1.numberOfVariables(); i++) {
            valueX1 = (int) parent1.getDecisionVariables()[i].getValue();
            valueX2 = (int) parent2.getDecisionVariables()[i].getValue();
            offSpring[0].getDecisionVariables()[i].setValue(valueX2);
            offSpring[1].getDecisionVariables()[i].setValue(valueX1);
          } // for
        } // Int representation
      }
    } catch (ClassCastException e1) {
      Configuration.logger_.severe("TwoPointCrossover.doCrossover: Cannot perfom " +
              "TwoPointCrossover");
      Class cls = String.class;
      String name = cls.getName();
      throw new JMException("Exception in " + name + ".doCrossover()");
    }
    return offSpring;
  } // doCrossover

  /**
   * Executes the operation
   * @param object An object containing an array of two solutions
   * @return An object containing an array with the offSprings
   * @throws JMException
   */
  public Object execute(Object object) throws JMException {
    Solution[] parents = (Solution[]) object;

    if (!(VALID_TYPES.contains(parents[0].getType().getClass())  &&
        VALID_TYPES.contains(parents[1].getType().getClass())) ) {

      Configuration.logger_.severe("TwoPointCrossover.execute: the solutions " +
              "are not of the right type. The type should be 'Binary' or 'Int', but " +
              parents[0].getType() + " and " +
              parents[1].getType() + " are obtained");

      Class cls = String.class;
      String name = cls.getName();
      throw new JMException("Exception in " + name + ".execute()");
    } // if

    if (parents.length < 2) {
      Configuration.logger_.severe("TwoPointCrossover.execute: operator " +
              "needs two parents");
      Class cls = String.class;
      String name = cls.getName();
      throw new JMException("Exception in " + name + ".execute()");
    } 
    
    Solution[] offSpring;
    offSpring = doCrossover(crossoverProbability_,
            parents[0],
            parents[1]);

    //-> Update the offSpring solutions
    for (int i = 0; i < offSpring.length; i++) {
      offSpring[i].setCrowdingDistance(0.0);
      offSpring[i].setRank(0);
    }
    return offSpring;
  } // execute
} // TwoPointCrossover
