package jmetal.metaheuristics.bilevel;

import jmetal.core.Algorithm;
import jmetal.core.Operator;
import jmetal.core.Problem;
import jmetal.core.SolutionSet;
import jmetal.metaheuristics.singleObjective.geneticAlgorithm.gGA;
import jmetal.operators.crossover.SBXCrossover;
import jmetal.operators.mutation.UniformMutation;
import jmetal.operators.selection.BinaryTournament;
import jmetal.operators.selection.Selection;
import jmetal.problems.MOKP_Problem;
import jmetal.problems.CostDistr;
import jmetal.util.JMException;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

public class UpperLevelCostDistr {


    public static Logger logger_;      // Logger object
    public static FileHandler fileHandler_; // FileHandler object

    /**
     * @param args Command line arguments. The first (optional) argument specifies
     *             the problem to solve.
     * @throws JMException
     * @throws IOException
     * @throws SecurityException      Usage: three options
     *                                - jmetal.metaheuristics.moead.MOEAD_main
     *                                - jmetal.metaheuristics.moead.MOEAD_main problemName
     *                                - jmetal.metaheuristics.moead.MOEAD_main problemName ParetoFrontFile
     * @throws ClassNotFoundException
     */
    public static void main(String[] args) throws JMException, SecurityException, IOException, ClassNotFoundException {

        Problem problem;         // The problem to solve
        Algorithm algorithm;         // The algorithm to use
        Operator crossover;         // Crossover operator
        Operator mutation;         // Mutation operator
        //Operator  selection ;         // Selection operator

        //int bits ; // Length of bit string in the OneMax problem
        HashMap parameters; // Operator parameters

        String problemName = "idealKnapsack_24_8to2";
        //initialize Lower Level algorithm
        MOKP_Problem lowerLevelProblem = (MOKP_Problem) LowerLevelMOKP.initializeAlgorithm(problemName);

        //thalis
        problem = new CostDistr(problemName, lowerLevelProblem);
        //thalis comment
        //int bits = 512 ;
        //problem = new OneMax("Binary", bits);

        //problem = new Sphere("Real", 10) ;
        //problem = new Easom("Real") ;
        //problem = new Griewank("Real", 10) ;

        algorithm = new gGA(problem); // Generational GA
        //algorithm = new ssGA(problem); // Steady-state GA
        //algorithm = new scGA(problem) ; // Synchronous cGA
        //algorithm = new acGA(problem) ;   // Asynchronous cGA

        /* Algorithm parameters*/
        //algorithm.setInputParameter("populationSize",4); //must be even number
        //algorithm.setInputParameter("maxEvaluations", 2500);
        algorithm.setInputParameter("populationSize", 100); //must be even number
        algorithm.setInputParameter("maxEvaluations", 200);


        //thalis
        algorithm.setInputParameter("dataDirectory",
                "/Users/emine/IdeaProjects/JMETALHOME/data/MOEAD_parameters/Weight");

        /*
        // Mutation and Crossover for Real codification
        parameters = new HashMap() ;
        parameters.put("probability", 0.9) ;
        parameters.put("distributionIndex", 20.0) ;
        crossover = CrossoverFactory.getCrossoverOperator("SBXCrossover", parameters);

        parameters = new HashMap() ;
        parameters.put("probability", 1.0/problem.getNumberOfVariables()) ;
        parameters.put("distributionIndex", 20.0) ;
        mutation = MutationFactory.getMutationOperator("PolynomialMutation", parameters);
        */

        // Mutation and Crossover for Binary codification
        //thalis comment
        /*
        parameters = new HashMap() ;
        parameters.put("probability", 0.9) ;
        crossover = CrossoverFactory.getCrossoverOperator("SinglePointCrossover", parameters);
         */

        // Crossover operator
        //thalis
        parameters = new HashMap();
        //parameters.put("probability", CR_) ;
        //parameters.put("distributionIndex", F_) ;
        crossover = new SBXCrossover(parameters);

        //thalis comment
        /*
        parameters = new HashMap() ;
        parameters.put("probability", 1.0/bits) ;
        mutation = MutationFactory.getMutationOperator("BitFlipMutation", parameters);
         */
        //thalis
        Double perturbationIndex = 0.5;
        Double mutationProbability = 1.0 / problem.getNumberOfVariables();
        parameters = new HashMap();
        parameters.put("probability", mutationProbability);
        parameters.put("perturbation", perturbationIndex);
        mutation = new UniformMutation(parameters);

        /* Selection Operator */
        //thalis comment
        /*
        parameters = null ;
        selection = SelectionFactory.getSelectionOperator("BinaryTournament", parameters) ;
         */
        //thalis
        Selection selection = new BinaryTournament(parameters);

        /* Add the operators to the algorithm*/
        algorithm.addOperator("crossover", crossover);
        algorithm.addOperator("mutation", mutation);
        algorithm.addOperator("selection", selection);

        /* Execute the Algorithm */
        long initTime = System.currentTimeMillis();
        SolutionSet population = algorithm.execute();
        long estimatedTime = System.currentTimeMillis() - initTime;
        System.out.println("Total execution time: " + estimatedTime);

        /* Log messages */
        System.out.println("Objectives values have been writen to file FUN");
        population.printObjectivesToFile("FUN");
        System.out.println("Variables values have been writen to file VAR");
        population.printVariablesToFile("VAR");

        population.printSpentEnergyToFile("SPENT");
        population.printLowerLevelVarsToFile("LL_VAR");
        population.printLowerLevelObjToFile("LL_FUN");


    } //main

}