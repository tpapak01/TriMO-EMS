//  HUXCrossover.java
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
import jmetal.encodings.variable.Binary;
import jmetal.encodings.variable.MOKP_BinarySolution;
import jmetal.util.Configuration;
import jmetal.util.JMException;
import jmetal.util.PseudoRandom;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * This class allows to apply a HUX crossover operator using two parent
 * solutions.
 * NOTE: the operator is applied to the first encodings.variable of the solutions, and
 * the type of the solutions must be Binary or BinaryReal
 */
public class PartiallyMappedHUXCrossover extends Crossover{

	/**
   * Valid solution types to apply this operator
   */
  private static final List VALID_TYPES = Arrays.asList(BinarySolutionType.class,
                                                    MOKP_BinarySolution.class,
  		                                            BinaryRealSolutionType.class) ;

  private Double probability_ = null ;
  /**
   * Constructor
   * Create a new instance of the HUX crossover operator.
   */
  public PartiallyMappedHUXCrossover(HashMap<String, Object> parameters) {
    super(parameters) ;

  	if (parameters.get("probability") != null)
  		probability_ = (Double) parameters.get("probability") ;
  } // HUXCrossover


   /**
   * Constructor
   * Create a new intance of the HUX crossover operator.
   */
   //public HUXCrossover(Properties properties) {
   // this();
   //} // HUXCrossover



  /**
   * Perform the crossover operation
   * @param probability Crossover probability
   * @param parent1 The first parent
   * @param parent2 The second parent
   * @return An array containing the two offsprings
   * @throws JMException
   */
  public Solution[] doCrossover(double   probability,
                                Solution parent1,
                                Solution parent2) throws JMException {
    Solution [] offSpring = new Solution[2];
    offSpring[0] = new Solution(parent1);
    int[] covered0 = offSpring[0].getDeviceToPreferenceMapping();
    int[] coveredReverse0 = offSpring[0].getReverseDeviceToPreferenceMapping();
    offSpring[1] = new Solution(parent2);
    int[] covered1 = offSpring[1].getDeviceToPreferenceMapping();
    int[] coveredReverse1 = offSpring[1].getReverseDeviceToPreferenceMapping();

    try {
      if (PseudoRandom.randDouble() < probability) {
        for (int var = 0; var < parent1.getDecisionVariables().length; var++) {

          Binary offSpring0, offSpring1;
          offSpring0 =
                  (Binary) parent1.getDecisionVariables()[var].deepCopy();
          offSpring1 =
                  (Binary) parent2.getDecisionVariables()[var].deepCopy();

          for (int i = 0; i < offSpring0.getNumberOfBits(); i++) {
              if (PseudoRandom.randDouble() < 0.5) {

                boolean offspring0_set = offSpring0.getIth(i);
                boolean offspring1_set = offSpring1.getIth(i);
                int covered0_current = covered0[i];
                int covered1_current = covered1[i];

                //set offspring 0
                //neighbour turned on device
                if (offspring1_set){
                  if (offspring0_set) {
                    //erase past
                    int pastPreferenceCovered = covered0[i];
                    coveredReverse0[pastPreferenceCovered] = -1;
                  } else {
                    offSpring0.bits_.set(i, true);
                  }
                  //cover new and delete other spot covering the new
                  int newPreferenceCovered = covered1_current;
                  covered0[i] = newPreferenceCovered;
                  if (coveredReverse0[newPreferenceCovered] != -1) {
                    int previousCoverer = coveredReverse0[newPreferenceCovered];
                    covered0[previousCoverer] = -1;
                    offSpring0.bits_.set(previousCoverer, false);
                  }
                  coveredReverse0[newPreferenceCovered] = i;
                  //neighbour has not turned on device
                } else {
                  if (offspring0_set) {
                    //erase past
                    int pastPreferenceCovered = covered0[i];
                    coveredReverse0[pastPreferenceCovered] = -1;
                    covered0[i] = -1;
                    offSpring0.bits_.set(i, false);
                  }
                }

                //set offspring 1
                //neighbour turned on device
                if (offspring0_set){
                  if (offspring1_set) {
                    //erase past
                    int pastPreferenceCovered = covered1[i];
                    coveredReverse1[pastPreferenceCovered] = -1;
                  } else {
                    offSpring1.bits_.set(i, true);
                  }
                  //cover new and delete other spot covering the new
                  int newPreferenceCovered = covered0_current;
                  covered1[i] = newPreferenceCovered;
                  if (coveredReverse1[newPreferenceCovered] != -1) {
                    int previousCoverer = coveredReverse1[newPreferenceCovered];
                    covered1[previousCoverer] = -1;
                    offSpring1.bits_.set(previousCoverer, false);
                  }
                  coveredReverse1[newPreferenceCovered] = i;
                  //neighbour has not turned on device
                } else {
                  if (offspring1_set) {
                    //erase past
                    int pastPreferenceCovered = covered1[i];
                    coveredReverse1[pastPreferenceCovered] = -1;
                    covered1[i] = -1;
                    offSpring1.bits_.set(i, false);
                  }
                }
              }
          }

          offSpring[0].getDecisionVariables()[var] = offSpring0;
          offSpring[1].getDecisionVariables()[var] = offSpring1;

        }

        //7. Decode the results
        for (int i = 0; i < offSpring[0].getDecisionVariables().length; i++)
        {
          ((Binary)offSpring[0].getDecisionVariables()[i]).decode();
          ((Binary)offSpring[1].getDecisionVariables()[i]).decode();
        }
      }
    }catch (ClassCastException e1) {

      Configuration.logger_.severe("HUXCrossover.doCrossover: Cannot perfom " +
          "SinglePointCrossover ") ;
      Class cls = String.class;
      String name = cls.getName();
      throw new JMException("Exception in " + name + ".doCrossover()") ;
    }
    return offSpring;
  } // doCrossover


  /**
  * Executes the operation
  * @param object An object containing an array of two solutions
  * @return An object containing the offSprings
  */
  public Object execute(Object object) throws JMException {
    Solution [] parents = (Solution [])object;

    if (parents.length < 2)
    {
      Configuration.logger_.severe("HUXCrossover.execute: operator needs two " +
          "parents");
      Class cls = String.class;
      String name = cls.getName();
      throw new JMException("Exception in " + name + ".execute()") ;
    }

    if (!(VALID_TYPES.contains(parents[0].getType().getClass())  &&
        VALID_TYPES.contains(parents[1].getType().getClass())) ) {

      Configuration.logger_.severe("HUXCrossover.execute: the solutions " +
          "are not of the right type. The type should be 'Binary' of " +
          "'BinaryReal', but " +
          parents[0].getType() + " and " +
          parents[1].getType() + " are obtained");

      Class cls = String.class;
      String name = cls.getName(); 
      throw new JMException("Exception in " + name + ".execute()") ;

    } // if 
        
    Solution [] offSpring = doCrossover(probability_,
                                        parents[0],
                                        parents[1]);
    
    for (int i = 0; i < offSpring.length; i++)
    {
      offSpring[i].setCrowdingDistance(0.0);
      offSpring[i].setRank(0);
    } 
    
    return offSpring;
  } // execute
} // HUXCrossover
