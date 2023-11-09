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

package jmetal.operators.localSearch;

import jmetal.core.Operator;
import jmetal.core.Solution;
import jmetal.operators.mutation.Mutation;
import jmetal.problems.MOKP_Problem;
import jmetal.util.JMException;
import jmetal.util.PseudoRandom;
import jmetal.util.comparators.DominanceComparator;

import java.util.Comparator;
import java.util.HashMap;

/**
 * This class implements an local search operator based in the use of a 
 * mutation operator. An archive is used to store the non-dominated solutions
 * found during the search.
 */
public class CostsLocalSearch extends LocalSearch {

  /**
   * Stores the problem to solve
   */
  private MOKP_Problem problemMOKP;

  private int improvementRounds_ ;
  private int cooldownRounds_;
  private double[] nadirObjectiveValue;
  private double[] z_;

  private Comparator dominanceComparator_ ;

  /**
   * Stores the mutation operator
   */
  private Operator mutationOperator_;

  /**
  * Constructor.
  * Creates a new local search object.
  * @param parameters The parameters

  */
  public CostsLocalSearch(HashMap<String, Object> parameters) {
  	super(parameters) ;
  	if (parameters.get("problem") != null) {
      problemMOKP = (MOKP_Problem) parameters.get("problem");
      nadirObjectiveValue = problemMOKP.getNadirObjectiveValue();
      z_ = problemMOKP.getZenithObjectiveValue();
    }
    if (parameters.get("improvementRounds") != null)
      improvementRounds_ = (Integer) parameters.get("improvementRounds") ;
    if (parameters.get("cooldownRounds") != null)
      cooldownRounds_ = (Integer) parameters.get("cooldownRounds") ;
    if (parameters.get("mutation") != null)
  	  mutationOperator_ = (Mutation) parameters.get("mutation") ;  		


    dominanceComparator_  = new DominanceComparator();
  } //Mutation improvement

  @Override
  public int getEvaluations() {
    return 0;
  }

  /**
   * Executes the local search. The maximum number of iterations is given by 
   * the param "improvementRounds", which is in the parameter list of the 
   * operator. The archive to store the non-dominated solutions is also in the 
   * parameter list.
   * @param object Object representing a solution
   * @return An object containing the new improved solution
 * @throws JMException 
   */
  public Object execute(Object object) throws JMException {

    Solution original = (Solution)object;
    problemMOKP.fillUpCovered(original);

    int cooldownRounds = cooldownRounds_;

    //Solution finalSolution = null;
    //int rounds = improvementRounds_;
    //int i = 0;
    //int best = 0;
    //do {
      //i++;
      double[] lambda = original.getLambda();
      Solution solution = new Solution(original, lambda);
      double currentFitness = fitnessFunction(solution, lambda);

      for (int cr=0; cr<cooldownRounds; cr++) {
        double temperature = 1.0 - ( (cr+1.0) / (double) cooldownRounds );

        //find positions to be changed
        Solution mutatedSolution = new Solution(solution, lambda);
        double[][] positionsChanged = (double[][]) mutationOperator_.execute(mutatedSolution);
        if (positionsChanged[0][0] == positionsChanged[0][1])
          continue;

        for (int k=0; k<positionsChanged.length; k++) {
          double[] infoOnSingleChange = positionsChanged[k];
          if (infoOnSingleChange[0] == infoOnSingleChange[1]) //same positions means no change found
            break;
          problemMOKP.partiallyEvaluateC(mutatedSolution, infoOnSingleChange); //mutatedSolution gets new obj values
        }

        //calculate new fitness
        double newFitness = fitnessFunction(mutatedSolution, lambda);

        //immediately choose if dominant
        int flagDominate;
        if (problemMOKP.isMaxmized() == false)
          flagDominate = dominanceComparator_.compare(mutatedSolution, solution);
        else flagDominate = dominanceComparator_.compare(solution, mutatedSolution);

        if (flagDominate == 0) { // Non-dominated

          if (newFitness < currentFitness) {
            solution = mutatedSolution;
            currentFitness = newFitness;
          } else {
            double chance = 1.0 / (1.0 + Math.exp((newFitness - currentFitness) * 50 / temperature));
            double rand = PseudoRandom.randDouble();
            if (temperature < 0.8){
              int a = 2;
            }
            if (chance > rand) {
              solution = mutatedSolution;
              currentFitness = newFitness;
            }
          }

        } else if (flagDominate == -1) {// mutated is better
            solution = mutatedSolution;
            currentFitness = newFitness;
        }

      } //cooldownRoundOver

      /*
      if (finalSolution == null) finalSolution = mutatedSolution;
      else {
        best = dominanceComparator_.compare(mutatedSolution, finalSolution);
        if (best == -1) // This is: Mutated is best
          finalSolution = mutatedSolution;
        else if (best == 1) {
          int a = 2;
        } // This is: Original is best == //delete mutatedSolution
        else {
          int a = 2;
        }// mutatedSolution and original are non-dominated, put mutatedSolution in archive
      }

       */

    //} while (i < rounds);
    //return finalSolution;
      return solution;
  } // execute

  double fitnessFunction(Solution individual, double[] lambda) {
    double fitness;
    double maxFun = -1.0e+30;

    for (int n = 0; n < problemMOKP.getNumberOfObjectives(); n++) {
      double diff = Math.abs((individual.getObjective(n) / nadirObjectiveValue[n]) - z_[n]);

      double feval;
      // make sure the multiplication with λ doesn't result in an absolute zero
      if (lambda[n] == 0) {
        feval = 0.0001 * diff;
      } else {
        feval = lambda[n] * diff;
      }

      //is this the maximum difference found so far?
      if (feval > maxFun) {
        maxFun = feval;
      }
    } // for

    fitness = maxFun;

    return fitness;
  } // fitnessEvaluation

}
