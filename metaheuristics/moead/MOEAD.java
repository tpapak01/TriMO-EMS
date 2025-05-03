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
import jmetal.encodings.variable.Binary;
import jmetal.problems.MOKP_Problem;
import jmetal.qualityIndicator.QualityIndicator;
import jmetal.util.JMException;
import jmetal.util.PseudoRandom;

import jmetal.util.RankingOnlyFirst;
import jmetal.util.comparators.DominanceComparator;

import java.io.*;
import java.util.Comparator;
import java.util.Vector;

public class MOEAD extends Algorithm {

  public static boolean JUnits = false;

  private MOKP_Problem problemMOKP;
  int evaluations_;
  SolutionSet initPopSolution_;

  /**
   * Z vector (ideal point)
   */
  static double[] z_;
  /**
   * Lambda vectors
   */
  //Vector<Vector<Double>> lambda_ ; 
  static double[][] lambda_;
  public double[][] getLambda_(){
    return lambda_;
  }
  private static double[] nadirObjectiveValue;
  static int T_;
  static int[][] neighborhood_;
  static boolean normalize_;
  static double delta_;
  static int nr_;
  static String functionType_;
  static String dataDirectory_;
  private static final Comparator dominance_ = new DominanceComparator();

  public MOEAD(Problem problem, Algorithm algorithm){
    super(problem);
    problemMOKP = (MOKP_Problem) problem;

    this.setInputParameter("populationSize", algorithm.getInputParameter("populationSize"));
    this.setInputParameter("maxEvaluations", algorithm.getInputParameter("maxEvaluations"));

    this.setInputParameter("dataDirectory",
            "/Users/emine/IdeaProjects/JMETALHOME/data/MOEAD_parameters/Weight");

    this.setInputParameter("T", algorithm.getInputParameter("T"));
    this.setInputParameter("delta", algorithm.getInputParameter("delta")) ;
    this.setInputParameter("nr", algorithm.getInputParameter("nr")) ;

    this.setInputParameter("repairAfterCrossoverMutation",algorithm.getInputParameter("repairAfterCrossoverMutation"));

    this.setInputParameter("rpType",algorithm.getInputParameter("rpType"));
    this.setInputParameter("normalize",algorithm.getInputParameter("normalize"));

    this.setInputParameter("lambdaComparator", algorithm.getInputParameter("lambdaComparator"));

    this.setInputParameter("indicators", algorithm.getInputParameter("indicators")) ;

    /* Add the operators to the algorithm*/
    this.addOperator("crossover", algorithm.getOperator("crossover"));
    this.addOperator("mutation", algorithm.getOperator("mutation"));
  }

  /** 
   * Constructor
   * @param problem Problem to solve
   */
  public MOEAD(Problem problem) {
    super (problem) ;
    problemMOKP = (MOKP_Problem) problem;
    functionType_ = "TCHE1";
    nadirObjectiveValue = problemMOKP.getNadirObjectiveValue();
    /*
    try {
          evalsWriter = new FileWriter("LowerLevelParetoVisual/evals.txt");
          spreadWriter = new FileWriter("LowerLevelParetoVisual/spread.txt");
          hypervolumeWriter = new FileWriter("LowerLevelParetoVisual/hypervolume.txt");
          ndsWriter = new FileWriter("LowerLevelParetoVisual/nds.txt");
    } catch (IOException e) {
          e.printStackTrace();
    }
     */
  }

    //statistical analysis
    private static int execution = 0;
    private static double evals_mean = 0;
    private static double spread_mean = 0;
    private static double hypervolume_mean = 0;
    private static double nds_mean = 0;
    private static FileWriter evalsWriter;
    private static FileWriter spreadWriter;
    private static FileWriter hypervolumeWriter;
    private static FileWriter ndsWriter;
    public static double conv = 0.01;  //0.000001;  0.001

  public SolutionSet execute() throws JMException, ClassNotFoundException {
    int maxEvaluations;
    int populationSize;
    SolutionSet population;
    SolutionSet extPopulation;
    Operator crossover;
    Operator mutation;

    QualityIndicator indicators; // QualityIndicator object

    evaluations_ = 0;
    maxEvaluations = ((Integer) this.getInputParameter("maxEvaluations")).intValue();
    populationSize = ((Integer) this.getInputParameter("populationSize")).intValue();
    int repairAfterCrossoverMutation = ((Integer) this.getInputParameter("repairAfterCrossoverMutation")).intValue();
    indicators = (QualityIndicator) getInputParameter("indicators");

    //thalis
    String rpType = this.getInputParameter("rpType").toString();
    Comparator  lambdaComparator;

    dataDirectory_ = this.getInputParameter("dataDirectory").toString();
    //System.out.println("POPSIZE: "+ populationSize_) ;

    population = new SolutionSet(populationSize);
    extPopulation = new SolutionSet(populationSize*2);
    double prevHypervolume = 0;

    T_ = ((Integer) this.getInputParameter("T")).intValue();
    nr_ = ((Integer) this.getInputParameter("nr")).intValue();
    delta_ = ((Double) this.getInputParameter("delta")).doubleValue();
    normalize_ = ((Boolean) this.getInputParameter("normalize")).booleanValue();
    initPopSolution_ = ((SolutionSet) this.getInputParameter("initPopSolution"));

/*
    T_ = (int) (0.1 * populationSize_);
    delta_ = 0.9;
    nr_ = (int) (0.01 * populationSize_);
*/
    neighborhood_ = new int[populationSize][T_];

    z_ = new double[problem_.getNumberOfObjectives()];
    lambda_ = new double[populationSize][problem_.getNumberOfObjectives()];

    //Read the operators
    lambdaComparator = (Comparator) this.getInputParameter("lambdaComparator");
    crossover = operators_.get("crossover"); // default: DE crossover
    mutation = operators_.get("mutation");  // default: polynomial mutation

    // STEP 1. Initialization
    // STEP 1.1. Compute euclidean distances between weight vectors and find T
    initUniformWeight(populationSize);
    
    initNeighborhood(populationSize);

    // STEP 1.2. Initialize population
    initPopulation(population, populationSize, lambdaComparator);

    // STEP 1.3. Initialize z_
    initIdealPoint(population, populationSize, rpType);

    //used for convergence observation
    int threshold = 0;
    int iteration = 0;
    //used for solution injection
    boolean passedOnce = false;
    //used for convergence
    boolean converged = false;

    // STEP 2. Update
    do {
      int[] permutation = new int[populationSize];
      Utils.randomPermutation(permutation, populationSize);

      //if (evaluations_ > threshold && initPopSolution_ != null) {
      //  extPopulation = updateExternal(population, extPopulation);
      //  extPopulation.printObjectivesToFile("LowerLevelParetoEvolution/" + execution + "_FUN_" + iteration++);
      //}

      for (int i = 0; i < populationSize; i++) {
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
        //thalis
        Solution[] parents = new Solution[2];
        //thalis comment
        //Solution[] parents = new Solution[3];

        parents[0] = population.get(parents_index.get(0));
        parents[1] = population.get(parents_index.get(1));
        //thalis comment
        //parents[2] = population_.get(n);

        //thalis
        // produce 2 offspring by performing crossover on the 2 parents
        Solution[] children = (Solution[]) crossover.execute(parents);

        Solution child;
        boolean randomOffspring = false;
        if (randomOffspring) {

          // randomly select 1 of the 2 produced offspring
          double rndSel = PseudoRandom.randDouble();
          if (rndSel < 0.5)
            child = children[0];
          else
            child = children[1];

          //thalis comment
          // Apply crossover, DE by default
          //child = (Solution) crossover_.execute(new Object[]{population_.get(n), parents});

          // Apply mutation, we keep the original bit flipping for now, not the "updateProduct"
          mutation.execute(child);

          if (repairAfterCrossoverMutation == 1)
            problemMOKP.repair(child);

          // Evaluation
          problem_.evaluate(child);
          evaluations_++;

        } else { // Best offspring
          mutation.execute(children[0]);
          mutation.execute(children[1]);

          if (repairAfterCrossoverMutation == 1) {
            problemMOKP.repair(children[0]);
            problemMOKP.repair(children[1]);
          }

          problem_.evaluate(children[0]);
          evaluations_++;
          problem_.evaluate(children[1]);
          evaluations_++;

          int flagDominate;
          if (problem_.isMaxmized() == false)
            flagDominate = dominance_.compare(children[0], children[1]);
          else flagDominate = dominance_.compare(children[1], children[0]);

          if (flagDominate == -1)
            child = children[0];
          else if (flagDominate == 1)
            child = children[1];
          else {
            double rndSel = PseudoRandom.randDouble();
            if (rndSel < 0.5)
              child = children[0];
            else
              child = children[1];
          }

        } // end of dilemma

        // STEP 2.3. Repair. Not necessary, no constraints

        // STEP 2.4. Update z_
        // NOT NEEDED BECAUSE Z* IS ALREADY OPTIMAL
        // updateReference(child);

        // STEP 2.5. Update of solutions
        updateProblem(population, child, n, type);

      } // for

      extPopulation = updateExternal(population, extPopulation);

      //convergence check
      if (evaluations_ > threshold){
        threshold += 5000;
        //sort based on LAMBDA
        extPopulation.sort(lambdaComparator);
        double hypervolume = indicators.getHypervolume(extPopulation);
        double diff = hypervolume - prevHypervolume;
        if (diff < conv){
          converged = true;
        }
        prevHypervolume = hypervolume;

        //print the evolution of the Pareto front over N generations
        //if (converged == false && initPopSolution_ != null)
        //  extPopulation.printObjectivesToFile("LowerLevelParetoEvolution/" + execution + "_FUN_" + iteration++);
      }

    } while (evaluations_ < maxEvaluations && converged == false);

    execution++;

    //thalis

    //register Spent Energy of top solutions - used for both algorithm and Platform
    for (int i = 0; i < extPopulation.size(); i++) {
      problemMOKP.calculateSpentEnergy(extPopulation.get(i));
    }
    if (JUnits){
      for (int i = 0; i < extPopulation.size(); i++) {
        problemMOKP.JUnits(extPopulation.get(i));
      }
    }

    //print final Pareto Front to file, and calculate/print hypervolume and spread (Delta)
    /*
    Ranking finalRanking = new Ranking(finalSet);
    SolutionSet finalParetoFront = finalRanking.getSubfront(0);
    finalParetoFront.printObjectivesToFile("LowerLevelParetoVisual/WithoutLocalSearch/" + (execution) + "_FUN");

    double spread = indicators.getSpread(finalParetoFront);
    double hypervolume = indicators.getHypervolume(finalParetoFront);
    double nds = finalParetoFront.size();
    try {
        evals_mean += evaluations_;
        evalsWriter.write(evaluations_ + "\n");

        spread_mean += spread;
        spreadWriter.write(spread + "\n");

        hypervolume_mean += hypervolume;
        hypervolumeWriter.write(hypervolume + "\n");

        nds_mean += nds;
        ndsWriter.write(nds + "\n");

        if (execution == 20) {
            evals_mean = evals_mean / 20;
            spread_mean = spread_mean / 20;
            hypervolume_mean = hypervolume_mean / 20;
            nds_mean = nds_mean / 20;
            evalsWriter.write(evals_mean + "\n");
            spreadWriter.write(spread_mean + "\n");
            hypervolumeWriter.write(hypervolume_mean + "\n");
            ndsWriter.write(nds_mean + "\n");
            evalsWriter.close();
            spreadWriter.close();
            hypervolumeWriter.close();
            ndsWriter.close();
        }

    } catch(Exception e){}


     */

    return extPopulation;
  }

  public boolean equalSolution (Solution sol1, Solution sol2) {
    for (int i = 0; i < sol1.getNumberOfObjectives();i++) {
      if (sol1.getObjective(i) != sol2.getObjective(i))
        return false;
    }

    Variable[] vars1 = sol1.getDecisionVariables();
    Binary bin1 = (Binary) vars1[0];
    Variable[] vars2 = sol2.getDecisionVariables();
    Binary bin2 = (Binary) vars2[0];
    int len = bin1.getNumberOfBits();
    for (int i=0; i<len; i++)
      if (bin1.getIth(i) != bin2.getIth(i))
        return false;
    return true;
    /*
    if (!sol1.getDecisionVariables()[0].toString().equals(
            sol2.getDecisionVariables()[0].toString()
    )) return false;
    return true;
     */

  }

 
  /**
   * initUniformWeight
   */
  public void initUniformWeight(int populationSize) {
    //if ((problem_.getNumberOfObjectives() == 2) && (populationSize_ <= 300)) {
      for (int n = 0; n < populationSize; n++) {
        double a = 1.0 * n / (populationSize - 1);
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
  public void initNeighborhood(int populationSize) {
    double[] x = new double[populationSize];
    int[] idx = new int[populationSize];

    for (int i = 0; i < populationSize; i++) {
      // calculate the distances based on weight vectors
      for (int j = 0; j < populationSize; j++) {
        x[j] = Utils.distVector(lambda_[i], lambda_[j]);
        idx[j] = j;
      }

      // find 'niche' nearest neighboring subproblems to subproblem i
      // This is basically insertion sort, but you only sort the first T positions.
      // In other words, the first T positions contain the individuals with
      // the closest distance to individual i
      Utils.minFastSort(x, idx, populationSize, T_);

      // copy the indexes of the first (closest) T individuals to the neighbours-array of individual i
      System.arraycopy(idx, 0, neighborhood_[i], 0, T_);
    } // for
  } // initNeighborhood

  /**
   * 
   */
  public void initPopulation(SolutionSet population, int populationSize, Comparator lambdaComparator) throws JMException, ClassNotFoundException {

    int numberOfUsers = problemMOKP.getNumberOfUsers();
    int numberOfItems = problemMOKP.getNumberOfItems();
    int numOfConstraints = problem_.getNumberOfConstraints();
    int numOfBits = numberOfUsers * numberOfItems * numOfConstraints;

    if (initPopSolution_ == null) {
      for (int i = 0; i < populationSize; i++) {
        Solution newSolution = new Solution(problem_);

        // zero devices
        if (i == 0) {
          newSolution.setDecisionVariables(updateSolution(numOfBits, false));
        }

        // all devices
        if (i == populationSize - 1) {
          newSolution.setDecisionVariables(updateSolution(numOfBits, true));
        }

        problemMOKP.repair(newSolution);
        newSolution.setLambda(new double[]{lambda_[i][0],lambda_[i][1]});
        problem_.evaluate(newSolution);
        evaluations_++;
        population.add(newSolution);
      } // for
    } else {

      //sort then remove identical lambdas
      initPopSolution_.sort(lambdaComparator);
      for (int i=0; i<initPopSolution_.size()-1; i++){
        Solution sol1 = initPopSolution_.get(i);
        Solution sol2 = initPopSolution_.get(i+1);
        double[] solLambda1 = sol1.getLambda();
        double[] solLambda2 = sol2.getLambda();
        if (solLambda1[0] == solLambda2[0] && solLambda1[1] == solLambda2[1]) {
          double[] lambda = solLambda1;
          if (lambda[0] > lambda[1]) {
            if (sol1.getObjective(0) < sol2.getObjective(0))
              initPopSolution_.remove(i+1);
            else initPopSolution_.remove(i);
          } else {
            if (sol1.getObjective(1) < sol2.getObjective(1))
              initPopSolution_.remove(i+1);
            else initPopSolution_.remove(i);
            i--;
          }
        }
      }

      //FILL POPULATION WHILE ADDING FROM INIT_POP IF LAMBDA MATCHES
      int initPopIndex = 0;
      int initPopSize = initPopSolution_.size();
      Solution toAdd = initPopSolution_.get(initPopIndex);
      double[] lambda = toAdd.getLambda();
      boolean emptiedInitPopSolution = false;
      Solution newSolution;
      for (int i = 0; i < populationSize; i++) {
        if (!emptiedInitPopSolution && lambda_[i][0] == lambda[0] && lambda_[i][1] == lambda[1]){
          newSolution = new Solution(toAdd, lambda);
          //problemMOKP.repair(newSolution); //not needed
          problem_.evaluate(newSolution);
          evaluations_++;
          population.add(newSolution);

          initPopIndex++;
          if (initPopIndex < initPopSize) {
            toAdd = initPopSolution_.get(initPopIndex);
            lambda = toAdd.getLambda();
          } else emptiedInitPopSolution = true;
          continue;
        } else if (i == 0) {
          newSolution = new Solution(problem_);
          newSolution.setDecisionVariables(updateSolution(numOfBits, false));
        } else if (i == populationSize - 1) {
          newSolution = new Solution(problem_);
          newSolution.setDecisionVariables(updateSolution(numOfBits, true));
        } else {
          newSolution = new Solution(problem_);
        }
        problemMOKP.repair(newSolution);
        newSolution.setLambda(new double[]{lambda_[i][0], lambda_[i][1]});
        problem_.evaluate(newSolution);
        evaluations_++;
        population.add(newSolution);
      }
    }
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
  void initIdealPoint(SolutionSet population, int populationSize, String rpType) {
    for(int i = 0; i < problem_.getNumberOfObjectives(); i++)  {
      if (problem_.isMaxmized() == false) {
        if (rpType.equalsIgnoreCase("Ideal")) {
          if (normalize_) z_[i] = +1.1;
          else z_[i] = 0; //zero because we know it's the optimal solution z* for both objectives
        } else {
          System.out.println("MOEDA.initialize_RP: unknown type " + rpType);
          System.exit(-1);
        }
      } else {
        if (rpType.equalsIgnoreCase("Ideal")) {
          if (normalize_) z_[i] = -1.1;
          else z_[i] = -1.0e+30;
        } else {
          System.out.println("MOEDA.initialize_RP: unknown type " + rpType);
          System.exit(-1);
        }
      }

    }

    // NOT NEEDED BECAUSE Z* IS ALREADY OPTIMAL
    /*
    for (int i = 0; i < populationSize; i++) {
      updateReference(population.get(i));
    }
     */
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
          }
        } else {
          if (individual.getNormalizedObjective(n) > z_[n]) {
            z_[n] = individual.getNormalizedObjective(n);
          }
        }
      } else {
        if (problem_.isMaxmized() == false) {
          if (individual.getObjective(n) < z_[n]) {
            z_[n] = individual.getObjective(n);
          }
        } else {
          if (individual.getObjective(n) > z_[n]) {
            z_[n] = individual.getObjective(n);
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
  void updateProblem(SolutionSet population, Solution indiv, int id, int type) {
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

      Solution replacementCandidate = population.get(k);
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
        population.replace(k, new Solution(indiv, lambda_[k]));
        time++;
      }

      // the maximal number of solutions updated is not allowed to exceed 'limit'
      if (time >= nr_) {
        return;
      }
    }
  } // updateProblem

  public double fitnessFunction(Solution individual, double[] lambda) {
    double fitness;
    fitness = 0.0;

    if (functionType_.equals("TCHE1")) {
      double maxFun = -1.0e+30;

      for (int n = 0; n < problem_.getNumberOfObjectives(); n++) {
        double diff;
        if (normalize_)
          diff = Math.abs(individual.getNormalizedObjective(n) - z_[n]);
        else diff = Math.abs((individual.getObjective(n) / nadirObjectiveValue[n]) - z_[n]);

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

  SolutionSet updateExternal(SolutionSet population, SolutionSet external) {
      for (int i = 0; i < population.size(); i++) {
        Solution sol = population.get(i);
        boolean existEqual = false;

        for (int j = 0; j < external.size();j++) {
          if (equalSolution(sol, external.get(j))) {
            existEqual = true;
            break;
          }
        }

        if (existEqual) continue;

        external.add(population.get(i));
      } // for

      //only keep the Pareto front of the solutions
      RankingOnlyFirst toGetOnlyParetoOptimals = new RankingOnlyFirst(external);
      SolutionSet paretoOptimal = toGetOnlyParetoOptimals.getSubfront(0);
      external.clear();
      for (int i = 0; i < paretoOptimal.size(); i++)
        external.add(paretoOptimal.get(i));
      return external;
  }


} // MOEAD

