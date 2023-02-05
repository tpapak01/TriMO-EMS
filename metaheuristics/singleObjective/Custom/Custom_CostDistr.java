//  gGA.java
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

package jmetal.metaheuristics.singleObjective.Custom;

import jmetal.core.*;
import jmetal.encodings.variable.ArrayReal;
import jmetal.problems.CostDistr;
import jmetal.util.JMException;
import jmetal.util.wrapper.XReal;

import java.util.Arrays;
import java.util.Comparator;

/** 
 * Class implementing a generational genetic algorithm
 */
public class Custom_CostDistr extends Algorithm {

  private int populationSize;
  private SolutionSet population;
  int evaluations;

 /**
  *
  * Constructor
  * Create a new GGA instance.
  * @param problem Problem to solve.
  */
  public Custom_CostDistr(Problem problem){
    super(problem) ;
  } // GGA
  
 /**
  * Execute the GGA algorithm
 * @throws JMException 
  */
  public SolutionSet execute() throws JMException, ClassNotFoundException {

    int maxEvaluations ;
    int numOfVars = problem_.getNumberOfVariables();

    Comparator  comparator        ;
    
    // Read the params
    populationSize = ((Integer)this.getInputParameter("populationSize")).intValue();
    maxEvaluations = ((Integer)this.getInputParameter("maxEvaluations")).intValue();                
   
    // Initialize the variables
    population          = new SolutionSet(populationSize) ;
    
    evaluations  = 0;                

    // Read the operators
    comparator = (Comparator) this.getInputParameter("comparator");

    //initPopulation();
    CostDistr prob = ((CostDistr) problem_);
    int[] producedRE = Arrays.copyOf(prob.getProducedRE(),numOfVars);
    int totalProducedRE = prob.getTotalProducedRE();
    initPopulationCostDistr(producedRE, totalProducedRE);

    while (evaluations < maxEvaluations) {

      population.sort(comparator);
        
      // Reproductive cycle: keep adding 2 offspring to the offspring population until it reaches the max size
      for (int i = 0 ; i < populationSize; i++) {

        Solution sol = population.get(i);
        Solution newSol = new Solution(sol);

        double[] spentEnergy = Arrays.copyOf(sol.getSpentEnergy(),numOfVars);
        double[] costsToSend = new double[numOfVars];
        XReal solDecisionVars = new XReal(sol);
        for (int j = 0; j < numOfVars; j++) {

          double currentValue = solDecisionVars.getValue(j);

          // 1) LINEAR
          /*
          if (spentEnergy[j] < producedRE[j]){
            costsToSend[j] =  currentValue - 0.05;
          } else if (spentEnergy[j] > producedRE[j]) {
            costsToSend[j] = currentValue + 0.05;
          } else {
            costsToSend[j] =  currentValue;
          }

           */

          // 2) PROPORTIONAL
          if (producedRE[j] == 0) {
            producedRE[j] = 1;
          }
          if (spentEnergy[j] == 0) {
            spentEnergy[j] = 1;
          }

          if (spentEnergy[j] != producedRE[j]){
            double proportion = spentEnergy[j] / (double) producedRE[j];
            costsToSend[j] = currentValue * proportion;
          } else {
            costsToSend[j] =  currentValue;
          }

          //Normalize
          if (costsToSend[j] > 1.0)
            costsToSend[j] = 1.0;
          else if (costsToSend[j] < 0)
            costsToSend[j] = 0;
          else costsToSend[j] = Math.round(costsToSend[j]*100.0) / 100.0;

        }
        newSol.setDecisionVariables(updateSolution(costsToSend));

        // Evaluation of the new individuals
        double previousVal = sol.getObjective(0);
        problem_.evaluate(newSol);
          
        evaluations++;

        if (newSol.getObjective(0) < previousVal){
          population.replace(i, newSol);
        }

      } // for

    } // while
    
    // Return a population with the best individual
    SolutionSet resultPopulation = new SolutionSet(1) ;
    resultPopulation.add(population.get(0)) ;
    
    System.out.println("Evaluations: " + evaluations ) ;
    return resultPopulation ;
  } // execute

  /**
   *
   */
  public void initPopulation(int[] producedRE) throws JMException, ClassNotFoundException {
    for (int i = 0; i < populationSize; i++) {
      Solution newSolution = new Solution(problem_);

      problem_.evaluate(newSolution);
      evaluations++;
      population.add(newSolution) ;
    } // for
  } // initPopulation

  public void initPopulationCostDistr(int[] producedRE, int totalProducedRE) throws JMException, ClassNotFoundException {
    double[] costsToSend = new double[problem_.getNumberOfVariables()];
    //int RE_min = Integer.MAX_VALUE;
    //int RE_max = Integer.MIN_VALUE;

    for (int j = 0; j < problem_.getNumberOfVariables(); j++) {
      /*
      if (producedRE[j] < RE_min){
        RE_min = producedRE[j];
      }
      if (producedRE[j] > RE_max){
        RE_max = producedRE[j];
      }

       */
    }

    for (int i = 0; i < populationSize; i++) {
      Solution newSolution = new Solution(problem_);

      //1) normalized costs to send - MIN MAX = LIMITS (so 0 and 1 exist)
      if (i == 0) {
        for (int j = 0; j < problem_.getNumberOfVariables(); j++) {
          costsToSend[j] = 1 - ((double)producedRE[j]/(double)totalProducedRE);
          costsToSend[j] = Math.round(costsToSend[j]*100.0) / 100.0;
        }
        newSolution.setDecisionVariables(updateSolution(costsToSend));
      }


      problem_.evaluate(newSolution);
      evaluations++;
      population.add(newSolution) ;
    } // for
  } // initPopulation

  public Variable[] updateSolution(double[] costsToSend) throws JMException {
    Variable [] variables = new Variable[1];
    ArrayReal arrayReal = new ArrayReal(problem_.getNumberOfVariables(), problem_);
    for (int j=0; j<problem_.getNumberOfVariables(); j++)
      arrayReal.setValue(j, costsToSend[j]);
    variables[0] = arrayReal;
    return variables;
  }


} // gGA