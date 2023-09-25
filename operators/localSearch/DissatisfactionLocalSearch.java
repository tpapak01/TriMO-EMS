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
import jmetal.core.Problem;
import jmetal.core.Solution;
import jmetal.core.SolutionSet;
import jmetal.encodings.variable.Binary;
import jmetal.operators.mutation.Mutation;
import jmetal.problems.MOKP_Problem;
import jmetal.util.JMException;
import jmetal.util.comparators.DominanceComparator;
import jmetal.util.comparators.OverallConstraintViolationComparator;

import java.util.Comparator;
import java.util.HashMap;

/**
 * This class implements an local search operator based in the use of a 
 * mutation operator. An archive is used to store the non-dominated solutions
 * found during the search.
 */
public class DissatisfactionLocalSearch extends LocalSearch {

  /**
   * Stores the problem to solve
   */
  private MOKP_Problem problemMOKP;

  private int improvementRounds_ ;
  private double temperature_;
  private double cooldown_;
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
  public DissatisfactionLocalSearch(HashMap<String, Object> parameters) {
  	super(parameters) ;
  	if (parameters.get("problem") != null) {
      problemMOKP = (MOKP_Problem) parameters.get("problem");
      nadirObjectiveValue = problemMOKP.getNadirObjectiveValue();
      z_ = problemMOKP.getZenithObjectiveValue();
    }
    if (parameters.get("improvementRounds") != null)
      improvementRounds_ = (Integer) parameters.get("improvementRounds") ;
    if (parameters.get("temperature") != null)
      temperature_ = (Double) parameters.get("temperature") ;
    if (parameters.get("cooldown") != null)
      cooldown_ = (Double) parameters.get("cooldown") ;
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
    int i = 0;
    int best = 0;
    Solution solution = (Solution)object;

    int rounds = improvementRounds_;
    double temperature = temperature_;
    double cooldown = temperature_ * cooldown_;
    Solution finalSolution = null;

    do {
      i++;
      Solution mutatedSolution = new Solution(solution);
      double[] lambda = solution.getLambda();

      do {
        //mutationOperator_.setParameter("temperature", temperature);
        double[][] positionsChanged = (double[][]) mutationOperator_.execute(mutatedSolution);

        if (positionsChanged[0][1] == 0)
          break;

        for (int k=0; k<positionsChanged.length; k++) {
          double[] newposition = positionsChanged[k];
          if (newposition[1] == 0) //step == 0
            break;
          double[] oldVals = new double[problemMOKP.getNumberOfObjectives()];
          double oldFitness = fitnessFunction(mutatedSolution, lambda);
          for (int o=0; o<oldVals.length; o++)
            oldVals[o] = solution.getObjective(o);
          problemMOKP.partiallyEvaluateD(mutatedSolution, newposition);
          double newFitness = fitnessFunction(mutatedSolution, lambda);
          if (newFitness < oldFitness){
            Binary bin  = (Binary)mutatedSolution.getDecisionVariables()[0];
            bin.bits_.flip((int)newposition[0]);
            problemMOKP.partiallyUpdate(mutatedSolution, newposition);
          } else {
            for (int o=0; o<oldVals.length; o++)
               solution.setObjective(o, oldVals[o]);
          }

        }


        temperature -= cooldown;
      } while (temperature_> 0);

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

    } while (i < rounds);
    return finalSolution;
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
