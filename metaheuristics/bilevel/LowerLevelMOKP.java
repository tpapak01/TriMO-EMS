package jmetal.metaheuristics.bilevel;

import jmetal.core.*;
import jmetal.metaheuristics.moead.MOEAD;
import jmetal.operators.crossover.SinglePointCrossover;
import jmetal.operators.mutation.BitFlipMutation;
import jmetal.operators.selection.BinaryTournament;
import jmetal.operators.selection.Selection;
import jmetal.problems.MOKP_Problem;
import jmetal.qualityIndicator.QualityIndicator;
import jmetal.util.Configuration;
import jmetal.util.JMException;
import jmetal.util.wrapper.XReal;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

public class LowerLevelMOKP {

    public static Logger logger_ ;      // Logger object
    public static FileHandler fileHandler_ ; // FileHandler object
    public static Problem problem   ;         // The problem to solve
    public static Algorithm algorithm ;         // The algorithm to use

    public static Problem initializeAlgorithm(String problemName) throws SecurityException, IOException {


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
        problem = new MOKP_Problem(problemName  ,"userpreference_5_5to5");
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
        algorithm.setInputParameter("maxEvaluations",10000);
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

        return problem;
    }


    public static SolutionSet evaluate(XReal y) throws JMException, SecurityException, ClassNotFoundException {

        ((MOKP_Problem) problem).setCostOfUsage(y);

        // Execute the Algorithm
        SolutionSet population = algorithm.execute();

        return population;
    }

}
