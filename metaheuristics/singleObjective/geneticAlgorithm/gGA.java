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

package jmetal.metaheuristics.singleObjective.geneticAlgorithm;

import jmetal.core.*;
import jmetal.encodings.variable.ArrayReal;
import jmetal.encodings.variable.MOKP_BinarySolution;
import jmetal.problems.CostDistr;
import jmetal.util.JMException;
import jmetal.util.comparators.ObjectiveComparator;

import java.util.Comparator;

/** 
 * Class implementing a generational genetic algorithm
 */
public class gGA extends Algorithm {

  private int populationSize;
  private SolutionSet population;
  int evaluations;
  
 /**
  *
  * Constructor
  * Create a new GGA instance.
  * @param problem Problem to solve.
  */
  public gGA(Problem problem){
    super(problem) ;
  } // GGA
  
 /**
  * Execute the GGA algorithm
 * @throws JMException 
  */
  public SolutionSet execute() throws JMException, ClassNotFoundException {

    int maxEvaluations ;

    SolutionSet offspringPopulation ;

    Operator    mutationOperator  ;
    Operator    crossoverOperator ;
    Operator    selectionOperator ;
    
    Comparator  comparator        ;
    //thalis
    if (problem_.isMaxmized())
      comparator = new ObjectiveComparator(0, true) ; // Single objective comparator
    else comparator = new ObjectiveComparator(0) ; // Single objective comparator
    
    // Read the params
    populationSize = ((Integer)this.getInputParameter("populationSize")).intValue();
    maxEvaluations = ((Integer)this.getInputParameter("maxEvaluations")).intValue();                
   
    // Initialize the variables
    population          = new SolutionSet(populationSize) ;   
    offspringPopulation = new SolutionSet(populationSize) ;
    
    evaluations  = 0;                

    // Read the operators
    mutationOperator  = this.operators_.get("mutation");
    crossoverOperator = this.operators_.get("crossover");
    selectionOperator = this.operators_.get("selection");  

    // Create the initial population
    //thalis comment
    /*
    Solution newIndividual;
    for (int i = 0; i < populationSize; i++) {
      newIndividual = new Solution(problem_);                    
      problem_.evaluate(newIndividual);            
      evaluations++;
      population.add(newIndividual);
    } //for

     */

    //initPopulation();
    int[] producedRE = ((CostDistr) problem_).getProducedRE();
    initPopulationCostDistr(producedRE);
     
    // Sort population
    population.sort(comparator) ;
    while (evaluations < maxEvaluations) {
      if ((evaluations % 10) == 0) {
        System.out.println(evaluations + ": " + population.get(0).getObjective(0)) ;
      } //

      // Copy the best two individuals to the offspring population
      offspringPopulation.add(new Solution(population.get(0))) ;	
      offspringPopulation.add(new Solution(population.get(1))) ;	
        
      // Reproductive cycle
      for (int i = 0 ; i < (populationSize / 2 - 1) ; i ++) {
        // Selection
        Solution [] parents = new Solution[2];

        parents[0] = (Solution)selectionOperator.execute(population);
        parents[1] = (Solution)selectionOperator.execute(population);
 
        // Crossover
        Solution [] offspring = (Solution []) crossoverOperator.execute(parents);
          
        // Mutation
        mutationOperator.execute(offspring[0]);
        mutationOperator.execute(offspring[1]);

        // Evaluation of the new individual
        problem_.evaluate(offspring[0]);            
        problem_.evaluate(offspring[1]);
          
        evaluations +=2;
    
        // Replacement: the two new individuals are inserted in the offspring
        //                population
        offspringPopulation.add(offspring[0]) ;
        offspringPopulation.add(offspring[1]) ;
      } // for
      
      // The offspring population becomes the new current population
      population.clear();
      for (int i = 0; i < populationSize; i++) {
        population.add(offspringPopulation.get(i)) ;
      }
      offspringPopulation.clear();
      population.sort(comparator) ;
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
  public void initPopulation() throws JMException, ClassNotFoundException {
    for (int i = 0; i < populationSize; i++) {
      Solution newSolution = new Solution(problem_);

      problem_.evaluate(newSolution);
      evaluations++;
      population.add(newSolution) ;
    } // for
  } // initPopulation

  public void initPopulationCostDistr(int[] producedRE) throws JMException, ClassNotFoundException {
    double[] costsToSend = new double[problem_.getNumberOfVariables()];
    int RE_min = Integer.MAX_VALUE;
    int RE_max = Integer.MIN_VALUE;
    for (int j = 0; j < problem_.getNumberOfVariables(); j++) {
      if (producedRE[j] < RE_min){
        RE_min = producedRE[j];
      }
      if (producedRE[j] > RE_max){
        RE_max = producedRE[j];
      }
    }

    for (int i = 0; i < populationSize; i++) {
      Solution newSolution = new Solution(problem_);

      /*
      //1) normalized costs to send - MIN MAX = LIMITS (so 0 and 1 exist)
      if (i == 0) {
        for (int j = 0; j < problem_.getNumberOfVariables(); j++) {
          costsToSend[j] = 1 - (((double) producedRE[j] - RE_min) / (RE_max - RE_min));
        }
        newSolution.setDecisionVariables(updateSolution(costsToSend));
      }

      //2) deduct RE from upper limit
      if (i == 1) {
        for (int j = 0; j < problem_.getNumberOfVariables(); j++) {
          costsToSend[j] = problem_.getUpperLimit(j) - producedRE[j];
        }
        newSolution.setDecisionVariables(updateSolution(costsToSend));
      }

      //3) deduct RE from double the upper limit
      if (i == 2) {
        for (int j = 0; j < problem_.getNumberOfVariables(); j++) {
          costsToSend[j] = problem_.getUpperLimit(j)*2 - producedRE[j];
        }
        newSolution.setDecisionVariables(updateSolution(costsToSend));
      }

      //4) deduct RE from triple the upper limit
      if (i == 3) {
        for (int j = 0; j < problem_.getNumberOfVariables(); j++) {
          costsToSend[j] = problem_.getUpperLimit(j)*3 - producedRE[j];
        }
        newSolution.setDecisionVariables(updateSolution(costsToSend));
      }

      //5) deduct RE from quadruple the upper limit
      if (i == 4) {
        for (int j = 0; j < problem_.getNumberOfVariables(); j++) {
          costsToSend[j] = problem_.getUpperLimit(j)*4 - producedRE[j];
        }
        newSolution.setDecisionVariables(updateSolution(costsToSend));
      }

      //6) normalized costs to send, but PERCENTAGE (MIN MAX = LIMITS (so 0 and 100 exist)
      if (i == 5) {
        for (int j = 0; j < problem_.getNumberOfVariables(); j++) {
          costsToSend[j] = (1 - (((double) producedRE[j] - RE_min) / (RE_max - RE_min))) * 100;
        }
        newSolution.setDecisionVariables(updateSolution(costsToSend));
      }

      //7) deduct RE from upper limit, then make PERCENTAGE
      if (i == 6) {
        double multiplier = 100 / problem_.getUpperLimit(0);
        for (int j = 0; j < problem_.getNumberOfVariables(); j++) {
          costsToSend[j] = (problem_.getUpperLimit(j) - producedRE[j]) * multiplier;
        }
        newSolution.setDecisionVariables(updateSolution(costsToSend));
      }

       */

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