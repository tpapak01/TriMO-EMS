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
  private MOKP_Problem problem_;

  private int improvementRounds_ ;
  private double temperature_;
  private double cooldown_;

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
  	if (parameters.get("problem") != null)
  		problem_ = (MOKP_Problem) parameters.get("problem") ;
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

      do {
        mutationOperator_.setParameter("temperature", temperature);
        double[][] positionsChanged = (double[][]) mutationOperator_.execute(mutatedSolution);

        if (positionsChanged[1][0] == 0)
          break;

        problem_.partiallyEvaluateD(mutatedSolution, positionsChanged);

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

}
