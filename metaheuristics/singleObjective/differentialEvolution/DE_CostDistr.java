//  DE.java
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

package jmetal.metaheuristics.singleObjective.differentialEvolution;

import jmetal.core.*;
import jmetal.encodings.variable.ArrayReal;
import jmetal.problems.CostDistr;
import jmetal.problems.MOKP_Problem;
import jmetal.util.JMException;
import jmetal.util.comparators.ObjectiveComparator;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;

/**
 * This class implements a differential evolution algorithm. 
 */
public class DE_CostDistr extends Algorithm {

  /**
  * Constructor
  * @param problem Problem to solve
  */
  public DE_CostDistr(Problem problem){
    super(problem) ;
  } // gDE

  private int populationSize;
  private SolutionSet population;
  int evaluations;

  /**   
   * Runs of the DE algorithm.
   * @return a <code>SolutionSet</code> that is a set of non dominated solutions
   * as a result of the algorithm execution  
    * @throws JMException 
   */  
   public SolutionSet execute() throws JMException, ClassNotFoundException {

     int maxEvaluations ;

     SolutionSet offspringPopulation ;
          
     Operator selectionOperator ;
     Operator crossoverOperator ;
               
     Comparator  comparator ;
     
     // Differential evolution parameters
     int r1    ;
     int r2    ;
     int r3    ;
     int jrand ;

     Solution parent[] ;
     
     //Read the parameters
     populationSize = ((Integer)this.getInputParameter("populationSize")).intValue();
     maxEvaluations  = ((Integer)this.getInputParameter("maxEvaluations")).intValue();

     // Read the operators
     comparator = (Comparator) this.getInputParameter("comparator");
     selectionOperator = operators_.get("selection");   
     crossoverOperator = operators_.get("crossover") ;
     
     //Initialize the variables
     population  = new SolutionSet(populationSize);

     /*
     // Create the initial solutionSet
     Solution newSolution;
     for (int i = 0; i < populationSize; i++) {
       newSolution = new Solution(problem_);                    
       problem_.evaluate(newSolution);            
       problem_.evaluateConstraints(newSolution);
       evaluations++;
       population.add(newSolution);
     } //for

      */

     initPopulation();
   
     // Generations ...
     population.sort(comparator) ;

     //Convergence
     int generations_left_for_convergence = 10;
     int converged = generations_left_for_convergence;
     double best_solution = population.get(0).getObjective(0);

     while (evaluations < maxEvaluations && converged != 0) {
       
       // Create the offSpring solutionSet      
       offspringPopulation  = new SolutionSet(populationSize);        

       // optional: keep best solution
       //offspringPopulation.add(new Solution(population.get(0))) ;
      
       for (int i = 0; i < populationSize; i++) {
         Solution current = population.get(i);

         // Selection: 3 random parents
         // Two parameters are required: the population and the index of the current individual
         parent = (Solution [])selectionOperator.execute(new Object[]{population, i});

         Solution child ;
         
         // Crossover.
         // Two parameters are required: the current individual and the array of parents
         child = (Solution)crossoverOperator.execute(new Object[]{current, parent}) ;

         problem_.evaluate(child) ;

         evaluations++ ;

         // if child better than currently-examined individual, replace him
         if (comparator.compare(current, child) < 0)
           offspringPopulation.add(new Solution(current)) ;
         else
           offspringPopulation.add(child) ;
       }
       
       // The offspring population becomes the new current population
       population.clear();
       for (int i = 0; i < populationSize; i++) {
         population.add(offspringPopulation.get(i)) ;
       }

       offspringPopulation.clear();
       population.sort(comparator) ;

       //check for convergence
       if (population.get(0).getObjective(0) >= best_solution){
         converged--;
       } else {
         best_solution = population.get(0).getObjective(0);
         converged = generations_left_for_convergence;
       }

     } // while
     
     // Return a population with the best individual
     SolutionSet resultPopulation = new SolutionSet(1) ;
     resultPopulation.add(population.get(0)) ;
     
     System.out.println("Evaluations: " + evaluations ) ;
     FileWriter evalsWriter = null;
     try {
       evalsWriter = new FileWriter("EVALS", true);
       evalsWriter.write(evaluations + "\n");
       evalsWriter.close();
     } catch (IOException e) {
       e.printStackTrace();
     }

     return resultPopulation ;
   } // execute

  /**
   *
   */
  public void initPopulation() throws JMException, ClassNotFoundException {

    if (((CostDistr)problem_).getInputCosts() == null) {
      for (int i = 0; i < populationSize; i++) {
        Solution newSolution = new Solution(problem_);

        problem_.evaluate(newSolution);
        evaluations++;
        population.add(newSolution);
      } // for
    } else {
      for (int i = 0; i < populationSize; i++) {
        Solution newSolution = new Solution(problem_);
        newSolution.setDecisionVariables(updateSolution(((CostDistr)problem_).getInputCosts()));
        problem_.evaluate(newSolution);
        evaluations++;
        population.add(newSolution);
      } // for
    }
  } // initPopulation


  public Variable[] updateSolution(double[] costsToSend) throws JMException {
    Variable [] variables = new Variable[1];
    ArrayReal arrayReal = new ArrayReal(problem_.getNumberOfVariables(), problem_);
    for (int j=0; j<problem_.getNumberOfVariables(); j++)
      arrayReal.setValue(j, costsToSend[j]);
    variables[0] = arrayReal;
    return variables;
  }

} // DE
