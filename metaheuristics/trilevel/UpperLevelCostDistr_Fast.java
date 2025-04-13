package jmetal.metaheuristics.trilevel;

import jmetal.core.*;
import jmetal.metaheuristics.singleObjective.geneticAlgorithm.Fast_CostDistr;
import jmetal.operators.crossover.SBXCrossover;
import jmetal.operators.mutation.UniformMutation;
import jmetal.operators.selection.BinaryTournament;
import jmetal.operators.selection.Selection;
import jmetal.problems.CostDistr;
import jmetal.problems.MOKP_Problem;
import jmetal.util.JMException;
import jmetal.util.comparators.ObjectiveComparator;
import jmetal.util.wrapper.XReal;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

public class UpperLevelCostDistr_Fast {


    public static Logger logger_;      // Logger object
    public static FileHandler fileHandler_; // FileHandler object
    private static String problemPath = "C:\\Users\\emine\\source\\repos\\SmartHome3\\SmartHome3\\wwwroot\\";
    public static Algorithm algorithm ;
    public static CostDistr problemCostDistr;         // The problem to solve

    /**
     * @throws JMException
     * @throws IOException
     * @throws SecurityException      Usage: three options
     *                                - jmetal.metaheuristics.moead.MOEAD_main
     *                                - jmetal.metaheuristics.moead.MOEAD_main problemName
     *                                - jmetal.metaheuristics.moead.MOEAD_main problemName ParetoFrontFile
     * @throws ClassNotFoundException
     */
    public static void initializeAlgorithm(Problem upperLevelProblem, Problem lowerLevelProblem,
                                           String dataPath, String paretoFileName)
            throws JMException, SecurityException, IOException, ClassNotFoundException {

        Operator crossover;         // Crossover operator
        Operator mutation;         // Mutation operator
        //Operator  selection ;         // Selection operator

        //int bits ; // Length of bit string in the OneMax problem
        HashMap parameters; // Operator parameters

        //initialize Lower Level algorithm
        LowerLevelMOKP_MOEAD.initializeAlgorithm(lowerLevelProblem, dataPath, paretoFileName);

        //thalis
        problemCostDistr = (CostDistr) upperLevelProblem;
        //thalis comment
        //int bits = 512 ;
        //problem = new OneMax("Binary", bits);

        //problem = new Sphere("Real", 10) ;
        //problem = new Easom("Real") ;
        //problem = new Griewank("Real", 10) ;

        algorithm = new Fast_CostDistr(problemCostDistr, problemPath); // Generational GA
        //algorithm = new ssGA(problem); // Steady-state GA
        //algorithm = new scGA(problem) ; // Synchronous cGA
        //algorithm = new acGA(problem) ;   // Asynchronous cGA

        /* Algorithm parameters*/
        //algorithm.setInputParameter("populationSize",4); //must be even number
        //algorithm.setInputParameter("maxEvaluations", 2500);
        algorithm.setInputParameter("populationSize", 100); //must be even number
        algorithm.setInputParameter("maxEvaluations", 1000000);

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

        /* Crossover operator */
        //thalis
        parameters = new HashMap();
        //parameters.put("probability", CR_) ;
        //parameters.put("distributionIndex", F_) ;
        crossover = new SBXCrossover(parameters);

        /* Mutation Operator */
        //thalis comment
        /*
        parameters = new HashMap() ;
        parameters.put("probability", 1.0/bits) ;
        mutation = MutationFactory.getMutationOperator("BitFlipMutation", parameters);
         */
        //thalis
        Double perturbationIndex = 0.5;
        Double mutationProbability = 1.0 / problemCostDistr.getNumberOfVariables();
        parameters = new HashMap();
        parameters.put("probability", mutationProbability);
        parameters.put("perturbation", perturbationIndex);
        mutation = new UniformMutation(parameters);

        /* Comparator */
        Comparator comparator;
        if (problemCostDistr.isMaxmized())
            comparator = new ObjectiveComparator(0, true) ; // Single objective comparator
        else comparator = new ObjectiveComparator(0) ; // Single objective comparator
        algorithm.setInputParameter("comparator", comparator);

        /* Selection Operator */
        //thalis comment
        /*
        parameters = null ;
        selection = SelectionFactory.getSelectionOperator("BinaryTournament", parameters) ;
         */
        //thalis
        parameters.put("comparator", comparator);
        Selection selection = new BinaryTournament(parameters);

        /* Add the operators to the algorithm*/
        algorithm.addOperator("crossover", crossover);
        algorithm.addOperator("mutation", mutation);
        algorithm.addOperator("selection", selection);



    } //main

    public static SolutionSet evaluate(XReal costOfBuying) throws JMException, SecurityException, ClassNotFoundException {

        problemCostDistr.setCostOfBuying(costOfBuying);
        /* Execute the Algorithm */
        SolutionSet population = algorithm.execute();
        return population;
    }

}