package jmetal.metaheuristics.bilevel;

import jmetal.core.*;
import jmetal.metaheuristics.moead.MOEAD;
import jmetal.metaheuristics.singleObjective.geneticAlgorithm.gGA;
import jmetal.operators.crossover.DifferentialEvolutionCrossover;
import jmetal.operators.crossover.SBXCrossover;
import jmetal.operators.crossover.SinglePointCrossover;
import jmetal.operators.mutation.BitFlipMutation;
import jmetal.operators.mutation.Mutation;
import jmetal.operators.mutation.UniformMutation;
import jmetal.operators.selection.BinaryTournament;
import jmetal.operators.selection.Selection;
import jmetal.problems.MOKP_Problem;
import jmetal.problems.ProblemFactory;
import jmetal.problems.UpperLevel_Problem;
import jmetal.qualityIndicator.QualityIndicator;
import jmetal.util.Configuration;
import jmetal.util.JMException;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

public class LowerLevelMOEAD {


    public static Logger logger_ ;      // Logger object
    public static FileHandler fileHandler_ ; // FileHandler object

    /**
     * @param args Command line arguments. The first (optional) argument specifies
     *      the problem to solve.
     * @throws JMException
     * @throws IOException
     * @throws SecurityException
     * Usage: three options
     *      - jmetal.metaheuristics.moead.MOEAD_main
     *      - jmetal.metaheuristics.moead.MOEAD_main problemName
     *      - jmetal.metaheuristics.moead.MOEAD_main problemName ParetoFrontFile
     * @throws ClassNotFoundException

     */
    public static void main(String [] args) throws JMException, SecurityException, IOException, ClassNotFoundException {

        Problem   problem   ;         // The problem to solve
        Algorithm algorithm ;         // The algorithm to use
        Operator  crossover ;         // Crossover operator
        Operator  mutation  ;         // Mutation operator
        //Operator  selection ;         // Selection operator

        //int bits ; // Length of bit string in the OneMax problem
        HashMap  parameters ; // Operator parameters

        //thalis
        problem = new UpperLevel_Problem("knapsack_2_3to1");
        //thalis comment
        //int bits = 512 ;
        //problem = new OneMax("Binary", bits);

        //problem = new Sphere("Real", 10) ;
        //problem = new Easom("Real") ;
        //problem = new Griewank("Real", 10) ;

        algorithm = new gGA(problem) ; // Generational GA
        //algorithm = new ssGA(problem); // Steady-state GA
        //algorithm = new scGA(problem) ; // Synchronous cGA
        //algorithm = new acGA(problem) ;   // Asynchronous cGA

        /* Algorithm parameters*/
        //algorithm.setInputParameter("populationSize",4); //must be even number
        //algorithm.setInputParameter("maxEvaluations", 2500);
        algorithm.setInputParameter("populationSize",500); //must be even number
        algorithm.setInputParameter("maxEvaluations", 25000);


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
        parameters = new HashMap() ;
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
        Double perturbationIndex = 0.5 ;
        Double mutationProbability = 1.0/problem.getNumberOfVariables() ;
        parameters = new HashMap() ;
        parameters.put("probability", mutationProbability) ;
        parameters.put("perturbation", perturbationIndex) ;
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
        algorithm.addOperator("crossover",crossover);
        algorithm.addOperator("mutation",mutation);
        algorithm.addOperator("selection",selection);

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

        //SolutionSet setY = lowerLevelEvaluate();

    } //main

    public static SolutionSet lowerLevelEvaluate() throws JMException, SecurityException, IOException, ClassNotFoundException {
        Problem problem   ;         // The problem to solve
        Algorithm algorithm ;         // The algorithm to use
        Operator crossover ;         // Crossover operator
        Operator  mutation  ;         // Mutation operator

        QualityIndicator indicators ; // Object to get quality indicators

        HashMap parameters ; // Operator parameters

        // Logger object and file to store log messages
        logger_      = Configuration.logger_ ;
        fileHandler_ = new FileHandler("MOEAD.log");
        logger_.addHandler(fileHandler_) ;

        indicators = null ;

        //thalis
        problem = new MOKP_Problem("knapsack_10_5to2"  ,"userpreference_5_5to10");
        //thalis comment, default option
        //problem = new Kursawe("Real", 3);
        //problem = new Kursawe("BinaryReal", 3);
        //problem = new Water("Real");
        //problem = new ZDT1("ArrayReal", 100);
        //problem = new ConstrEx("Real");
        //problem = new DTLZ1("Real");
        //problem = new OKA2("Real") ;


        algorithm = new MOEAD(problem);
        //algorithm = new MOEAD_DRA(problem);

        // Algorithm parameters
        //thalis
        int populationSize;
        if (problem.getNumberOfObjectives() == 2) {
            populationSize              = 100   ;
        } else if (problem.getNumberOfObjectives() == 3) {
            populationSize              = 105   ;
        } else if (problem.getNumberOfObjectives() == 4) {
            populationSize              = 120   ;
        } else if (problem.getNumberOfObjectives() == 6) {
            populationSize              = 126   ;
        } else if (problem.getNumberOfObjectives() == 8) {
            populationSize              = 120   ;
        } else if (problem.getNumberOfObjectives() == 10) {
            populationSize              = 220   ;
        } else {
            populationSize              = 100   ;
        }
        algorithm.setInputParameter("populationSize",populationSize);
        algorithm.setInputParameter("maxEvaluations",400000);
        //thalis comment
        //algorithm.setInputParameter("populationSize",300);
        //algorithm.setInputParameter("maxEvaluations",150000);

        // Directory with the files containing the weight vectors used in
        // Q. Zhang,  W. Liu,  and H Li, The Performance of a New Version of MOEA/D
        // on CEC09 Unconstrained MOP Test Instances Working Report CES-491, School
        // of CS & EE, University of Essex, 02/2009.
        // http://dces.essex.ac.uk/staff/qzhang/MOEAcompetition/CEC09final/code/ZhangMOEADcode/moead0305.rar
        algorithm.setInputParameter("dataDirectory",
                "/Users/emine/IdeaProjects/JMETALHOME/data/MOEAD_parameters/Weight");

        algorithm.setInputParameter("finalSize", 300) ; // used by MOEAD_DRA

        //thalis
        algorithm.setInputParameter("T", 10) ;
        algorithm.setInputParameter("delta", 1.0) ;
        algorithm.setInputParameter("nr", 10) ;
        //theta_ = 5.0; // used in PBI
        //algorithm.setInputParameter("theta", theta_) ;
        //thalis comment
        //algorithm.setInputParameter("T", 20) ;
        //algorithm.setInputParameter("delta", 0.9) ;
        //algorithm.setInputParameter("nr", 2) ;

        // Crossover operator
        //thalis
        parameters = new HashMap();
        double crossoverProbability = 1.0;
        parameters.put("probability", crossoverProbability);
        crossover = new SinglePointCrossover(parameters);
        //thalis comment
        //parameters = new HashMap() ;
        //parameters.put("CR", 1.0) ;
        //parameters.put("F", 0.5) ;
        //crossover = CrossoverFactory.getCrossoverOperator("DifferentialEvolutionCrossover", parameters);

        // Mutation operator
        //thalis - authors have replaced this mutation operator with "updateProduct", but not us
        parameters = new HashMap();
        double mutationProbability = 0.01;
        parameters.put("probability", mutationProbability);
        mutation = new BitFlipMutation(parameters);
        //thalis comment
        //parameters = new HashMap() ;
        //parameters.put("probability", 1.0/problem.getNumberOfVariables()) ;
        //parameters.put("distributionIndex", 20.0) ;
        //mutation = MutationFactory.getMutationOperator("PolynomialMutation", parameters);

        algorithm.addOperator("crossover",crossover);
        algorithm.addOperator("mutation",mutation);

        //thalis - extras
        Selection selection = new BinaryTournament(parameters);
        algorithm.addOperator("selection",selection);
        //algorithm.setInputParameter("functionType","TCHE1"); // scalar function type
        algorithm.setInputParameter("rpType","Ideal"); // reference point z_ type, Ideal or Nadir
        // Ideal by default
        algorithm.setInputParameter("normalize",false);

        // Execute the Algorithm
        long initTime = System.currentTimeMillis();
        SolutionSet population = algorithm.execute();
        long estimatedTime = System.currentTimeMillis() - initTime;

        // Result messages
        logger_.info("Total execution time: "+estimatedTime + "ms");
        logger_.info("Objectives values have been written to file FUN");
        population.printObjectivesToFile("FUN");
        logger_.info("Variables values have been written to file VAR");
        population.printVariablesToFile("VAR");

        if (indicators != null) {
            logger_.info("Quality indicators") ;
            logger_.info("Hypervolume: " + indicators.getHypervolume(population)) ;
            logger_.info("EPSILON    : " + indicators.getEpsilon(population)) ;
            logger_.info("GD         : " + indicators.getGD(population)) ;
            logger_.info("IGD        : " + indicators.getIGD(population)) ;
            logger_.info("Spread     : " + indicators.getSpread(population)) ;
        } // if

        return population;
    }

}
