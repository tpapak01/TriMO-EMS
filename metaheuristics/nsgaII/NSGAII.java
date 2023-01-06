//  NSGAII.java
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

package jmetal.metaheuristics.nsgaII;

import jmetal.core.*;
import jmetal.encodings.variable.Binary;
import jmetal.problems.MOKP_Problem;
import jmetal.qualityIndicator.QualityIndicator;
import jmetal.util.Distance;
import jmetal.util.JMException;
import jmetal.util.Ranking;
import jmetal.util.comparators.CrowdingComparator;

/** 
 *  Implementation of NSGA-II.
 *  This implementation of NSGA-II makes use of a QualityIndicator object
 *  to obtained the convergence speed of the algorithm. This version is used
 *  in the paper:
 *     A.J. Nebro, J.J. Durillo, C.A. Coello Coello, F. Luna, E. Alba 
 *     "A Study of Convergence Speed in Multi-Objective Metaheuristics." 
 *     To be presented in: PPSN'08. Dortmund. September 2008.
 */

public class NSGAII extends Algorithm {

  private int populationSize_;
  /**
   * Stores the population
   */
  private SolutionSet population_;
  private int evaluations_;

  /**
   * Constructor
   * @param problem Problem to solve
   */
  public NSGAII(Problem problem) {
    super (problem) ;
  } // NSGAII

  public static int execution;

  /**   
   * Runs the NSGA-II algorithm.
   * @return a <code>SolutionSet</code> that is a set of non dominated solutions
   * as a result of the algorithm execution
   * @throws JMException 
   */
  public SolutionSet execute() throws JMException, ClassNotFoundException {
    int maxEvaluations;

    //QualityIndicator indicators; // QualityIndicator object
    //int requiredEvaluations; // Use in the example of use of the
    // indicators object (see below)

    SolutionSet offspringPopulation;
    SolutionSet union;

    Operator mutationOperator;
    Operator crossoverOperator;
    Operator selectionOperator;

    Distance distance = new Distance();

    //Read the parameters
    populationSize_ = ((Integer) getInputParameter("populationSize")).intValue();
    maxEvaluations = ((Integer) getInputParameter("maxEvaluations")).intValue();
    //indicators = (QualityIndicator) getInputParameter("indicators");

    //Initialize the variables
    population_ = new SolutionSet(populationSize_);
    evaluations_ = 0;

    //requiredEvaluations = 0;

    //Read the operators
    mutationOperator = operators_.get("mutation");
    crossoverOperator = operators_.get("crossover");
    selectionOperator = operators_.get("selection");

    // Create the initial solutionSet
    initPopulation();

    //used for convergence observation
    int threshold = 0;
    int iteration = 0;

    // Generations 
    while (evaluations_ < maxEvaluations) {

      // Create the offSpring solutionSet      
      offspringPopulation = new SolutionSet(populationSize_);
      Solution[] parents = new Solution[2];
      for (int i = 0; i < (populationSize_ / 2); i++) {
        if (evaluations_ < maxEvaluations) {
          //obtain parents
          parents[0] = (Solution) selectionOperator.execute(population_);
          parents[1] = (Solution) selectionOperator.execute(population_);
          Solution[] offSpring = (Solution[]) crossoverOperator.execute(parents);
          mutationOperator.execute(offSpring[0]);
          mutationOperator.execute(offSpring[1]);
          problem_.evaluate(offSpring[0]);
          //problem_.evaluateConstraints(offSpring[0]);
          problem_.evaluate(offSpring[1]);
          //problem_.evaluateConstraints(offSpring[1]);
          offspringPopulation.add(offSpring[0]);
          offspringPopulation.add(offSpring[1]);
          evaluations_ += 2;
        } // if                            
      } // for

      // Create the solutionSet union of solutionSet and offSpring
      union = population_.union(offspringPopulation);

      // Ranking the union
      Ranking ranking = new Ranking(union);

      int remain = populationSize_;
      int index = 0;
      SolutionSet front = null;
      population_.clear();

      // Obtain the next front
      front = ranking.getSubfront(index);

      while ((remain > 0) && (remain >= front.size())) {
        //Assign crowding distance to individuals (for later in tournament selection)
        distance.crowdingDistanceAssignment(front, problem_.getNumberOfObjectives());
        //Add the individuals of this front
        for (int k = 0; k < front.size(); k++) {
          population_.add(front.get(k));
        } // for

        //Decrement remain
        remain = remain - front.size();

        //Obtain the next front
        index++;
        if (remain > 0) {
          front = ranking.getSubfront(index);
        }
      } // while

      // Remain is less than front(index).size, insert only the best solutions
      // by sorting the front first
      if (remain > 0) {  // front contains individuals to insert                        
        distance.crowdingDistanceAssignment(front, problem_.getNumberOfObjectives());
        front.sort(new CrowdingComparator());
        for (int k = 0; k < remain; k++) {
          population_.add(front.get(k));
        }
      }

      /*
      // This piece of code shows how to use the indicator object into the code
      // of NSGA-II. In particular, it finds the number of evaluations required
      // by the algorithm to obtain a Pareto front with a hypervolume higher
      // than the hypervolume of the true Pareto front.
      if ((indicators != null) &&
          (requiredEvaluations == 0)) {
        double HV = indicators.getHypervolume(population);
        if (HV >= (0.98 * indicators.getTrueParetoFrontHypervolume())) {
          requiredEvaluations = evaluations;
        } // if
      } // if
       */

      if (evaluations_ > threshold && execution < 10){
        threshold += 500;
        Ranking myRanking = new Ranking(population_);
        SolutionSet paretoFront = myRanking.getSubfront(0);
        paretoFront.printObjectivesToFile("LowerLevelParetoVisualNSGAII/" + execution + "_FUN_" + iteration++);
      }

    } // while

    execution++;

    // Return as output parameter the required evaluations
    //setOutputParameter("evaluations", requiredEvaluations);


    // Return the first non-dominated front
    Ranking ranking = new Ranking(population_);
    SolutionSet paretoFront = ranking.getSubfront(0);
    paretoFront.printFeasibleFUN("FUN_NSGAII") ;

    return paretoFront;
  } // execute

  /**
   *
   */
  public void initPopulation() throws JMException, ClassNotFoundException {

    int numberOfUsers = ((MOKP_Problem) problem_).getNumberOfUsers();
    int numberOfItems = ((MOKP_Problem) problem_).getNumberOfItems();
    int numOfConstraints = problem_.getNumberOfConstraints();
    int numOfBits = numberOfUsers * numberOfItems * numOfConstraints;

    for (int i = 0; i < populationSize_; i++) {
      Solution newSolution = new Solution(problem_);


      //1) zero devices
      if (i == 0) {
        newSolution.setDecisionVariables(updateSolution(numOfBits, false));
      }

      /*
      //2) all devices
      if (i == 1) {
        newSolution.setDecisionVariables(updateSolution(numOfBits, true));
      }

       */

      //3) exactly what the users want

      if (i == 2) {
        boolean[][][] pref = ((MOKP_Problem) problem_).getUserPreferences();
        Variable[] vars = new Variable[problem_.getNumberOfVariables()];
        for (int v = 0; v < vars.length; v++) {
          Binary bin = new Binary(numOfBits);

          for (int u = 0; u < numberOfUsers; u++) { // for each user
            int userIndex = u * numOfConstraints;
            int l = 0;
            for (int p = userIndex; p < userIndex +  numOfConstraints; p++) { // for each objective
              int startingIndex = p * numberOfItems;
              int k = 0;
              for (int j = startingIndex; j < startingIndex + numberOfItems; j++) { // for each bit
                bin.setIth(j, pref[u][l][k]);
                k++;
              }
              l++;
            } // for p
          } // for u

          vars[v] = bin;
        }

        newSolution.setDecisionVariables(vars);
      }

      ((MOKP_Problem) problem_).repair(newSolution);

      problem_.evaluate(newSolution);
      evaluations_++;
      population_.add(newSolution) ;
    } // for
  }

  public Variable[] updateSolution(int numOfBits, Boolean val) {
    Variable[] vars = new Variable[problem_.getNumberOfVariables()];
    for (int j = 0; j < vars.length; j++) {
      Binary bin = new Binary(numOfBits);
      for (int k = 0; k < numOfBits; k++) {
        bin.setIth(k, val);
      }
      vars[j] = bin;
    }
    return vars;
  }



} // NSGA-II




