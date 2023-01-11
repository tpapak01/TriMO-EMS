//  MOEAD.java
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

package jmetal.metaheuristics.moead;

import jmetal.core.*;
import jmetal.encodings.variable.ArrayReal;
import jmetal.encodings.variable.Binary;
import jmetal.encodings.variable.MOKP_BinarySolution;
import jmetal.problems.MOKP_Problem;
import jmetal.util.JMException;
import jmetal.util.PseudoRandom;
import jmetal.util.comparators.DominanceComparator;
import jmetal.util.Ranking;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Comparator;
import java.util.StringTokenizer;
import java.util.Vector;

public class MOEAD extends Algorithm {

  private int populationSize_;
  /**
   * Stores the population
   */
  private SolutionSet population_;
  /**
   * Z vector (ideal point)
   */
  double[] z_;
  /**
   * Lambda vectors
   */
  //Vector<Vector<Double>> lambda_ ; 
  double[][] lambda_;
  /**
   * T: neighbour size
   */
  int T_;
  /**
   * Neighborhood
   */
  int[][] neighborhood_;
  /**
   * Normalize
   */
  boolean normalize_;
  /**
   * delta: probability that parent solutions are selected from neighbourhood
   */
  double delta_;
  /**
   * nr: maximal number of solutions replaced by each child solution
   */
  int nr_;
  Solution[] indArray_;
  String functionType_;
  int evaluations_;
  /**
   * Operators
   */
  Operator crossover_;
  Operator mutation_;

  String dataDirectory_;

  //thalis
  private static final Comparator dominance_ = new DominanceComparator();
  String rpType_;
  SolutionSet previousPareto;

  /** 
   * Constructor
   * @param problem Problem to solve
   */
  public MOEAD(Problem problem) {
    super (problem) ;

    functionType_ = "TCHE1";

  } // DMOEA

  public static int execution;

  public SolutionSet execute() throws JMException, ClassNotFoundException {
    int maxEvaluations;

    evaluations_ = 0;
    maxEvaluations = ((Integer) this.getInputParameter("maxEvaluations")).intValue();
    populationSize_ = ((Integer) this.getInputParameter("populationSize")).intValue();

    //thalis
    rpType_ = this.getInputParameter("rpType").toString();
    Comparator  comparator;

    dataDirectory_ = this.getInputParameter("dataDirectory").toString();
    //System.out.println("POPSIZE: "+ populationSize_) ;

    population_ = new SolutionSet(populationSize_);
    indArray_ = new Solution[problem_.getNumberOfObjectives()];

    T_ = ((Integer) this.getInputParameter("T")).intValue();
    nr_ = ((Integer) this.getInputParameter("nr")).intValue();
    delta_ = ((Double) this.getInputParameter("delta")).doubleValue();
    normalize_ = ((Boolean) this.getInputParameter("normalize")).booleanValue();

/*
    T_ = (int) (0.1 * populationSize_);
    delta_ = 0.9;
    nr_ = (int) (0.01 * populationSize_);
*/
    neighborhood_ = new int[populationSize_][T_];

    z_ = new double[problem_.getNumberOfObjectives()];
    //lambda_ = new Vector(problem_.getNumberOfObjectives()) ;
    lambda_ = new double[populationSize_][problem_.getNumberOfObjectives()];

    //Read the operators
    comparator = (Comparator) this.getInputParameter("comparator");
    crossover_ = operators_.get("crossover"); // default: DE crossover
    mutation_ = operators_.get("mutation");  // default: polynomial mutation

    // STEP 1. Initialization
    // STEP 1.1. Compute euclidean distances between weight vectors and find T
    initUniformWeight();
    
    initNeighborhood();

    // STEP 1.2. Initialize population
    initPopulation();

    // STEP 1.3. Initialize z_
    initIdealPoint();

    //used for convergence observation
    int threshold = 0;
    int iteration = 0;
    //used for solution injection
    boolean passedOnce = false;
    //used for convergence
    boolean converged = false;
    Ranking r = new Ranking(population_);
    SolutionSet pareto = r.getSubfront(0);
    pareto.sort(comparator);
    previousPareto = pareto;

    // STEP 2. Update
    do {
      int[] permutation = new int[populationSize_];
      Utils.randomPermutation(permutation, populationSize_);

      for (int i = 0; i < populationSize_; i++) {
        // iterate through the population in random (permutation) order, or the normal order
        int n = permutation[i]; // or int n = i;
        int type;
        //double rnd = PseudoRandom.randDouble();

        // STEP 2.1. Mating selection based on probability
        //if (rnd < delta_) // if (rnd < realb)
        //{
          type = 1;   // neighborhood
        //} else {
        //  type = 2;   // whole population
        //}

        // select 2 parents from the neighbours of the current individual n
        Vector<Integer> parents_index = new Vector<Integer>();
        matingSelection(parents_index, n, 2/*, type*/);

        // STEP 2.2. Reproduction
        Solution child;
        //thalis
        Solution[] parents = new Solution[2];
        //thalis comment
        //Solution[] parents = new Solution[3];

        parents[0] = population_.get(parents_index.get(0));
        parents[1] = population_.get(parents_index.get(1));
        //thalis comment
        //parents[2] = population_.get(n);

        //thalis
        // produce 2 offspring by performing crossover on the 2 parents
        Solution[] children = (Solution[]) crossover_.execute(parents);
        // randomly select 1 of the 2 produced offspring
        double rndSel =  PseudoRandom.randDouble();
        if (rndSel < 0.5) {
          child = children[0];
        } else
          child = children[1];
        //thalis comment
        // Apply crossover, DE by default
        //child = (Solution) crossover_.execute(new Object[]{population_.get(n), parents});

        // Apply mutation, we keep the original bit flipping for now, not the "updateProduct"
        mutation_.execute(child);

        // Evaluation
        problem_.evaluate(child);
        
        evaluations_++;

        // STEP 2.3. Repair. Not necessary, no constraints

        // STEP 2.4. Update z_
        updateReference(child);

        // STEP 2.5. Update of solutions
        updateProblem(child, n, type);
      } // for


      if (evaluations_ > threshold){
        threshold += 500;
        Ranking ranking = new Ranking(population_);
        SolutionSet paretoFront = ranking.getSubfront(0);
        paretoFront.sort(comparator);

        if (paretoFront.size() == previousPareto.size()) {
          converged = true;
          for (int i = 0; i < paretoFront.size(); i++) {
              Solution sol1 = paretoFront.get(i);
              Solution sol2 = previousPareto.get(i);
              for (int j = 0; j < sol1.getNumberOfObjectives(); j++) {
                if (sol1.getObjective(j) != sol2.getObjective(j))
                  converged = false;
                  break;
              }
              if (converged == false) break;
          }
        }
        if (converged == false && execution < 10)
          paretoFront.printObjectivesToFile("LowerLevelParetoVisual/" + execution + "_FUN_" + iteration++);

        previousPareto = paretoFront;
      }

      //solution injection
      /*
      if (evaluations_ > 2000 && !passedOnce){
          passedOnce = true;
          population_.remove(population_.size()-1);

          Solution newSolution = new Solution(problem_);
          int numberOfUsers = ((MOKP_Problem) problem_).getNumberOfUsers();
          int numberOfItems = ((MOKP_Problem) problem_).getNumberOfItems();
          int numOfConstraints = problem_.getNumberOfConstraints();
          int numOfBits = numberOfUsers * numberOfItems * numOfConstraints;
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
          problem_.evaluate(newSolution);

          population_.add(population_.size(), newSolution);
        }

       */

    } while (evaluations_ < maxEvaluations && converged == false);

    execution++;

    //thalis
    // Only feasible solutions
      /*
    SolutionSet feasibleSet = new SolutionSet(population_.size());
    for (int i = 0; i < population_.size();i++) {
        feasibleSet.add(new Solution(population_.get(i)));
    }

       */

    //thalis
    //register Spent Energy of top solutions
    MOKP_Problem mokp_problem = (MOKP_Problem) problem_;
    for (int i = 0; i < population_.size(); i++) {
      mokp_problem.calculateSpentEnergy(population_.get(i));
    }


    //thalis
    // At last remove identical solutions, based not only on objective value, but also decision vector
    SolutionSet finalSet = new SolutionSet(population_.size());
    finalSet.add(population_.get(0));

    for (int i = 1; i < population_.size(); i++) {
      Solution sol = population_.get(i);
      boolean existEqual = false;

      for (int j = 0; j < finalSet.size();j++) {
        if (equalSolution(sol, finalSet.get(j))) {
          existEqual = true;
          break;
        }
      }

      if (existEqual) continue;

      finalSet.add(population_.get(i));

    } // for

    return finalSet;
  }

  public boolean equalSolution (Solution sol1, Solution sol2) {
    for (int i = 0; i < sol1.getNumberOfObjectives();i++) {
      if (sol1.getObjective(i) != sol2.getObjective(i))
        return false;
    }
    double[] spentEnergy1 = sol1.getSpentEnergy();
    double[] spentEnergy2 = sol2.getSpentEnergy();
    for (int i=0; i<spentEnergy1.length; i++)
      if (spentEnergy1[i] != spentEnergy2[i])
        return false;
    return true;
  }

 
  /**
   * initUniformWeight
   */
  public void initUniformWeight() {
    //if ((problem_.getNumberOfObjectives() == 2) && (populationSize_ <= 300)) {
      for (int n = 0; n < populationSize_; n++) {
        double a = 1.0 * n / (populationSize_ - 1);
        lambda_[n][0] = a;
        lambda_[n][1] = 1 - a;
      } // for
      /*
    } // if
    else {
      String dataFileName;
      dataFileName = "W" + problem_.getNumberOfObjectives() + "D_" +
        populationSize_ + ".dat";
   
      try {
        // Open the file
        FileInputStream fis = new FileInputStream(dataDirectory_ + "/" + dataFileName);
        InputStreamReader isr = new InputStreamReader(fis);
        BufferedReader br = new BufferedReader(isr);

        int i = 0;
        int j = 0;
        String aux = br.readLine();
        while (aux != null) {
          StringTokenizer st = new StringTokenizer(aux);
          j = 0;
          while (st.hasMoreTokens()) {
            double value = (new Double(st.nextToken())).doubleValue();
            lambda_[i][j] = value;
            //System.out.println("lambda["+i+","+j+"] = " + value) ;
            j++;
          }
          aux = br.readLine();
          i++;
        }
        br.close();
      } catch (Exception e) {
        System.out.println("initUniformWeight: failed when reading for file: " + dataDirectory_ + "/" + dataFileName);
        e.printStackTrace();
      }
    } // else

    //System.exit(0) ;

       */
  } // initUniformWeight


  /**
   * 
   */
  public void initNeighborhood() {
    double[] x = new double[populationSize_];
    int[] idx = new int[populationSize_];

    for (int i = 0; i < populationSize_; i++) {
      // calculate the distances based on weight vectors
      for (int j = 0; j < populationSize_; j++) {
        x[j] = Utils.distVector(lambda_[i], lambda_[j]);
        idx[j] = j;
      }

      // find 'niche' nearest neighboring subproblems to subproblem i
      // This is basically insertion sort, but you only sort the first T positions.
      // In other words, the first T positions contain the individuals with
      // the closest distance to individual i
      Utils.minFastSort(x, idx, populationSize_, T_);

      // copy the indexes of the first (closest) T individuals to the neighbours-array of individual i
      System.arraycopy(idx, 0, neighborhood_[i], 0, T_);
    } // for
  } // initNeighborhood

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

      /*
      // all devices
      if (i == 1) {
        newSolution.setDecisionVariables(updateSolution(numOfBits, true));
      }
       */

      // zero devices
      if (i == 0) {
        newSolution.setDecisionVariables(updateSolution(numOfBits, false));
      }

      // exactly what the users want
      if (i == populationSize_-1) {
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

  /**
   * Initialise the reference point
   */
  void initIdealPoint() throws JMException, ClassNotFoundException {
    for(int i = 0; i < problem_.getNumberOfObjectives(); i++)  {
      if (problem_.isMaxmized() == false) {
        if (rpType_.equalsIgnoreCase("Ideal")) {
          if (normalize_) z_[i] = +0.1; else z_[i] = +1.0e+30;
        } else {
          System.out.println("MOEDA.initialize_RP: unknown type " + rpType_);
          System.exit(-1);
        }
      } else {
        if (rpType_.equalsIgnoreCase("Ideal")) {
          if (normalize_) z_[i] = -1.1; else z_[i] = -1.0e+30;
        } else {
          System.out.println("MOEDA.initialize_RP: unknown type " + rpType_);
          System.exit(-1);
        }
      }

      indArray_[i] = new Solution(problem_);
      problem_.evaluate(indArray_[i]);
      evaluations_++;
    }

    for (int i = 0; i < populationSize_; i++) {
      updateReference(population_.get(i));
    }
  }

  /**
   * Just choose *size* random neighbours from the list of neighbours of the individual (for mating purposes)
   */
  public void matingSelection(Vector<Integer> list, int cid, int size/*, int type*/) {
    // list : the set of the indexes of selected mating parents
    // cid  : the id of current subproblem
    // size : the number of selected mating parents
    // type : 1 - neighborhood; otherwise - whole population
    int ss;
    int r;
    int p;

    ss = neighborhood_[cid].length;
    //continue until you get enough (2) neighbours
    while (list.size() < size) {
      //if (type == 1) {
        r = PseudoRandom.randInt(0, ss - 1);
        p = neighborhood_[cid][r];
      //} else {
      //  p = PseudoRandom.randInt(0, populationSize_ - 1);
      //}
      boolean flag = true;
      // now make sure you don't pick the same neighbour twice
      for (int i = 0; i < list.size(); i++) {
        if (list.get(i) == p) // p is in the list
        {
          flag = false;
          break;
        }
      }
      //if not in the list, add it
      if (flag) {
        list.addElement(p);
      }
    }
  } // matingSelection

  /**
   * Update the reference point z_
   * @param individual
   */
  void updateReference(Solution individual) {
    for (int n = 0; n < problem_.getNumberOfObjectives(); n++) {
      if (normalize_) {
        if (problem_.isMaxmized() == false) {
          if (individual.getNormalizedObjective(n) < z_[n]) {
            z_[n] = individual.getNormalizedObjective(n);
            indArray_[n] = individual;
          }
        } else {
          if (individual.getNormalizedObjective(n) > z_[n]) {
            z_[n] = individual.getNormalizedObjective(n);
            indArray_[n] = individual;
          }
        }
      } else {
        if (problem_.isMaxmized() == false) {
          if (individual.getObjective(n) < z_[n]) {
            z_[n] = individual.getObjective(n);
            indArray_[n] = individual;
          }
        } else {
          if (individual.getObjective(n) > z_[n]) {
            z_[n] = individual.getObjective(n);
            indArray_[n] = individual;
          }
        }
      }
    }
  } // updateReference

  /**
   * @param indiv
   * @param id
   * @param type
   */
  void updateProblem(Solution indiv, int id, int type) {
    // indiv: child solution
    // id:   the id of current subproblem
    // type: update solutions in - neighborhood (1) or whole population (otherwise)
    int size;
    int time;

    time = 0;

    //if (type == 1) {
      size = neighborhood_[id].length;
    //} else {
    //  size = population_.size();
    //}
    int[] perm = new int[size];

    Utils.randomPermutation(perm, size);

    // iterate through the neighbourhood of indiv (child) in random (permutation) order
    for (int i = 0; i < size; i++) {
      int k;
      //if (type == 1) {
        k = neighborhood_[id][perm[i]];
      //} else {
      //  k = perm[i];      // calculate the values of objective function regarding the current subproblem
      //}

      //thalis
      int flagDominate;

      Solution replacementCandidate = population_.get(k);
      if (problem_.isMaxmized() == false)
        flagDominate = dominance_.compare(indiv, replacementCandidate);
      else flagDominate = dominance_.compare(replacementCandidate, indiv);

      if (flagDominate == 0) { // Non-dominated
        double f1, f2;

        f1 = fitnessFunction(replacementCandidate, lambda_[k]);
        f2 = fitnessFunction(indiv, lambda_[k]);

        // if f2 smaller than f1, f2 (indiv) is better. We always look for the minimal Tchebycheff,
        // regardless of maximization or minimization problem
        if (f2 < f1) {
          flagDominate = -1;
        }

      }

      if (flagDominate == -1) {// indiv is better
        population_.replace(k, new Solution(indiv));
        time++;
      }

      // the maximal number of solutions updated is not allowed to exceed 'limit'
      if (time >= nr_) {
        return;
      }
    }
  } // updateProblem

  double fitnessFunction(Solution individual, double[] lambda) {
    double fitness;
    fitness = 0.0;

    if (functionType_.equals("TCHE1")) {
      double maxFun = -1.0e+30;

      for (int n = 0; n < problem_.getNumberOfObjectives(); n++) {
        double diff;
        if (normalize_)
          diff = Math.abs(individual.getNormalizedObjective(n) - z_[n]);
        else diff = Math.abs(individual.getObjective(n) - z_[n]);

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
    } // if
    else {
      System.out.println("MOEAD.fitnessFunction: unknown type " + functionType_);
      System.exit(-1);
    }
    return fitness;
  } // fitnessEvaluation


} // MOEAD

