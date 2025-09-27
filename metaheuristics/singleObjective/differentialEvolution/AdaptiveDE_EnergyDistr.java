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

package jmetal.metaheuristics.singleObjective.differentialEvolution;

import jmetal.core.*;
import jmetal.encodings.variable.ArrayReal;
import jmetal.metaheuristics.singleObjective.geneticAlgorithm.Fast_CostDistr;
import jmetal.metaheuristics.trilevel.UpperLevelCostDistr_AdaptiveDE;
import jmetal.metaheuristics.trilevel.UpperLevelCostDistr_Fast;
import jmetal.operators.crossover.DifferentialEvolutionCrossover;
import jmetal.problems.EnergyDistr;
import jmetal.util.EuclideanDist;
import jmetal.util.JMException;
import jmetal.util.PseudoRandom;
import jmetal.util.wrapper.XReal;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

/** 
 * Class implementing a generational genetic algorithm
 */
public class AdaptiveDE_EnergyDistr extends Algorithm {

  private EnergyDistr problemEnergyDistr;
  private int populationSize;
  private SolutionSet population;
  int evaluations;
  private static String problemPath = "C:\\Users\\emine\\source\\repos\\SmartHome3\\SmartHome3\\wwwroot\\";
  private static boolean useADEforUpper;

  private static double[] Fpool = new double[] {0.4, 0.5, 0.6};
  private static double[] CRpool = new double[] {0.7, 0.8, 0.9};
  private static double[] Kpool = new double[] {0.4, 0.5, 0.6};

 /**
  *
  * Constructor
  * Create a new GGA instance.
  * @param problem Problem to solve.
  */
  public AdaptiveDE_EnergyDistr(Problem problem, String dataPath, boolean inputUseADEforUpper){
    super(problem);
    problemEnergyDistr = (EnergyDistr) problem;
    if (!dataPath.equals("-")) { problemPath = dataPath; }
    useADEforUpper = inputUseADEforUpper;
  } // GGA
  
 /**
  * Execute the GGA algorithm
 * @throws JMException 
  */
  public SolutionSet execute() throws JMException, ClassNotFoundException {

    int maxEvaluations ;

    Operator    crossoverOperator ;
    Operator    selectionOperator ;
    
    Comparator  comparator        ;
    
    // Read the params
    populationSize = ((Integer)this.getInputParameter("populationSize")).intValue();
    maxEvaluations = ((Integer)this.getInputParameter("maxEvaluations")).intValue();                
   
    // Initialize the variables
    population          = new SolutionSet(populationSize) ;
    
    evaluations  = 0;                

    // Read the operators
    comparator = (Comparator) this.getInputParameter("comparator");
    crossoverOperator = this.operators_.get("crossover");
    selectionOperator = this.operators_.get("selection");  

    initPopulation();
    //double[] producedRE = problemEnergyDistr.getProducedRE();
    //initPopulationEnergyDistr(producedRE);

    // Sort population
    population.sort(comparator) ;

    //Convergence
    int generations_left_for_convergence = 10;
    int converged = generations_left_for_convergence;
    double best_solution = population.get(0).getObjective(0);
    System.out.println("Energy: BEST SOL at init: " + best_solution);

    int stop = 0;
    int generation = 0;

    int flag = PseudoRandom.randInt(1,3);

    while (evaluations < maxEvaluations && converged != 0) {

      HashMap parameters = new HashMap();
      parameters.put("CR", CRpool[flag-1]);
      parameters.put("F", Fpool[flag-1]);
      parameters.put("K", Kpool[flag-1]);
      //parameters.put("DE_VARIANT", "rand/1/bin");
      ((DifferentialEvolutionCrossover) crossoverOperator).updateParameters(parameters);

      Solution winner = population.get(0);
      //System.out.println("q: " + winner.getUL_Optimism());

      // Platform-only
      SolutionSet winnerPareto = winner.getLL_Pareto_pop();
      winnerPareto.printParetoToFile(problemPath + "results\\Paretos\\PARETO_" + (generation));
      SolutionSet onlyWinner = new SolutionSet(1);
      onlyWinner.add(winner);
      onlyWinner.printSelfConsumptionToFile(problemPath + "results\\SelfsInProgress\\SELF_" + (generation), false);
      onlyWinner.printProfitToFile(problemPath + "results\\ProfitInProgress\\PROFIT_" + (generation), false);

      generation++;

      SolutionSet offspringPopulation = new SolutionSet(populationSize) ;

      // Reproductive cycle: keep adding 2 offspring to the offspring population until it reaches the max size
      for (int i = 0 ; i < populationSize; i++) {
        Solution current = population.get(i);

        // Selection: 3 random parents
        // Two parameters are required: the population and the index of the current individual
        Solution[] tempParents = (Solution [])selectionOperator.execute(new Object[]{population, i});
        Solution[] parents = new Solution[4];
        parents[0] = tempParents[0]; parents[1] = tempParents[1]; parents[2] = tempParents[2];
        parents[3] = winner;

        // Crossover.
        // Two parameters are required: the current individual and the array of parents
        Solution offspring = (Solution)crossoverOperator.execute(new Object[]{current, parents}) ;

        //attach to offspring the UL solution of the closest TL solution
        double min_dist = Double.MAX_VALUE;
        int min_index = -1;

        Double[] offspringVar = (((ArrayReal) offspring.getDecisionVariables()[0]).array_);
        for (int j = 0; j < populationSize; j++) {
          Double[] popSolutionVar = (((ArrayReal) population.get(j).getDecisionVariables()[0]).array_);
          double dist = EuclideanDist.distance(offspringVar, popSolutionVar);
          if (dist < min_dist) {
            min_dist = dist;
            min_index = j;
          }
        }
        offspring.setUL_Transfer_pop(population.get(min_index).getUL_Transfer_pop());
        XReal costOfBuying = new XReal(offspring);
        int id = (generation*populationSize) + i + 1;
        Thread thread;
        UpperLevelCostDistr_AdaptiveDE ul_wrapper_ade = null;
        UpperLevelCostDistr_Fast ul_wrapper = null;
        if (useADEforUpper) {
          ul_wrapper_ade = new UpperLevelCostDistr_AdaptiveDE(id, costOfBuying, offspring);
          offspring.setUl_wrapper_ade(ul_wrapper_ade);
          thread = new Thread(ul_wrapper_ade);
        } else {
          ul_wrapper = new UpperLevelCostDistr_Fast(id, costOfBuying, offspring);
          offspring.setUl_wrapper(ul_wrapper);
          thread = new Thread(ul_wrapper);
        }
        offspring.setThread(thread);
        thread.start();

        Object lock;
        if (useADEforUpper) {
          lock = ((AdaptiveDE_CostDistr) ul_wrapper_ade.getAlg_fast()).getLock();
        } else {
          lock = ((Fast_CostDistr) ul_wrapper.getAlg_fast()).getLock();
        }
        synchronized (lock) {
          try {
            lock.wait(); // Wait indefinitely
          } catch (InterruptedException e) {
            System.out.println("Waiter thread: Why did someone interrupt me?");
          }
        }

        // Evaluation of the new individuals
        problem_.evaluate(offspring);
        evaluations++;

        offspringPopulation.add(offspring);

        if (stop == 1){
          break;
        }

      } // for

    // The offspring population is added to the new current population
    SolutionSet popCombined = population.union(offspringPopulation);
    offspringPopulation.clear();
    population.clear();

    // reevaluate combined population to see progress made
    for (int i=0; i<popCombined.size() ; i++){
        problem_.evaluate(popCombined.get(i));
    }

    // sort based on objective and pick the 100 best
    popCombined.sort(comparator);
    for (int i = 0; i < populationSize; i++) {
        population.add(popCombined.get(i));
    }
    // terminate the remaining of the threads
    for (int i = populationSize; i < popCombined.size(); i++) {
        Thread toKill = popCombined.get(i).getThread();
        System.out.println("Thread " + popCombined.get(i).getUl_wrapper().getId() + " terminated");
        toKill.stop();
    }

    if (stop == 1){
      break;
    }

    winner = population.get(0);
    //check for convergence
    if (winner.getObjective(0) > best_solution - 1){
      converged--;
      flag = PseudoRandom.randInt(1,3);
      problemEnergyDistr.setPenaltyFlag(PseudoRandom.randInt(0,2));
    } else {
      best_solution = winner.getObjective(0);
      System.out.println("Energy: BEST SOL at " + evaluations + ": " + best_solution);
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
    if (problemEnergyDistr.getInputCosts() == null) {
      for (int i = 0; i < populationSize; i++) {
        Solution newSolution = new Solution(problem_, true);

        if (i == 0){
          double[] ones = new double[problem_.getNumberOfVariables()];
          Arrays.fill(ones, EnergyDistr.TL_upperLimit);
          newSolution.setDecisionVariables(updateSolution(ones));
        }

        if (i == populationSize-1){
          double[] ones = new double[problem_.getNumberOfVariables()];
          Arrays.fill(ones, 0.0);
          newSolution.setDecisionVariables(updateSolution(ones));
        }

        XReal costOfBuying = new XReal(newSolution);

        Thread thread;
        UpperLevelCostDistr_AdaptiveDE ul_wrapper_ade = null;
        UpperLevelCostDistr_Fast ul_wrapper = null;
        if (useADEforUpper) {
          ul_wrapper_ade = new UpperLevelCostDistr_AdaptiveDE(i+1, costOfBuying, newSolution);
          thread = new Thread(ul_wrapper_ade);
          newSolution.setUl_wrapper_ade(ul_wrapper_ade);
        } else {
          ul_wrapper = new UpperLevelCostDistr_Fast(i+1, costOfBuying, newSolution);
          thread = new Thread(ul_wrapper);
          newSolution.setUl_wrapper(ul_wrapper);
        }

        newSolution.setThread(thread);
        thread.start();

        Object lock;
        if (useADEforUpper) {
          lock = ((AdaptiveDE_CostDistr) ul_wrapper_ade.getAlg_fast()).getLock();
        } else {
          lock = ((Fast_CostDistr) ul_wrapper.getAlg_fast()).getLock();
        }
        synchronized (lock) {
          try {
            lock.wait(); // Wait indefinitely
          } catch (InterruptedException e) {
            System.out.println("Waiter thread: Why did someone interrupt me?");
          }
        }

        problem_.evaluate(newSolution);
        evaluations++;
        population.add(newSolution);
      } // for
    } else {
      for (int i = 0; i < populationSize; i++) {
        Solution newSolution = new Solution(problem_, true);
        newSolution.setDecisionVariables(updateSolution(problemEnergyDistr.getInputCosts()));

        XReal costOfBuying = new XReal(newSolution);

        Thread thread;
        UpperLevelCostDistr_AdaptiveDE ul_wrapper_ade = null;
        UpperLevelCostDistr_Fast ul_wrapper = null;
        if (useADEforUpper) {
          ul_wrapper_ade = new UpperLevelCostDistr_AdaptiveDE(i+1, costOfBuying, newSolution);
          thread = new Thread(ul_wrapper_ade);
          newSolution.setUl_wrapper_ade(ul_wrapper_ade);
        } else {
          ul_wrapper = new UpperLevelCostDistr_Fast(i+1, costOfBuying, newSolution);
          thread = new Thread(ul_wrapper);
          newSolution.setUl_wrapper(ul_wrapper);
        }

        newSolution.setThread(thread);
        thread.start();

        Object lock;
        if (useADEforUpper) {
          lock = ((AdaptiveDE_CostDistr) ul_wrapper_ade.getAlg_fast()).getLock();
        } else {
          lock = ((Fast_CostDistr) ul_wrapper.getAlg_fast()).getLock();
        }
        synchronized (lock) {
          try {
            lock.wait(); // Wait indefinitely
          } catch (InterruptedException e) {
            System.out.println("Waiter thread: Why did someone interrupt me?");
          }
        }

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


} // gGA