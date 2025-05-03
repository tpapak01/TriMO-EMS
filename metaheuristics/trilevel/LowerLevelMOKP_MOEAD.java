package jmetal.metaheuristics.trilevel;

import jmetal.core.*;
import jmetal.metaheuristics.moead.MOEAD;
import jmetal.operators.crossover.PartiallyMappedHUXCrossover;
import jmetal.operators.mutation.BitFlipMutation;
import jmetal.problems.MOKP_Problem;
import jmetal.qualityIndicator.QualityIndicator;
import jmetal.util.Configuration;
import jmetal.util.JMException;
import jmetal.util.comparators.LambdaComparator;
import jmetal.util.comparators.ObjectiveComparator;
import jmetal.util.wrapper.XReal;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

public class LowerLevelMOKP_MOEAD {

    public static Logger logger_ ;      // Logger object
    public static FileHandler fileHandler_ ; // FileHandler object
    public static MOKP_Problem problem;         // The problem to solve
    public static Algorithm algorithm ;         // The algorithm to use
    public static int popSize;
    private static String problemPath = "/Users/emine/IdeaProjects/JMETALHOME/HV_Optimal/"; // The path of the files

    public Algorithm alg_moead;
    private MOKP_Problem problemMOKP;
    public MOKP_Problem getProblemMOKP(){
        return problemMOKP;
    }

    public static void initializeAlgorithm(Problem lowerLevelProblem, String dataPath, String paretoFileName) throws SecurityException, IOException {

        if (!dataPath.equals("-")) { problemPath = dataPath; }

        Operator crossover ;         // Crossover operator
        Operator  mutation  ;         // Mutation operator

        QualityIndicator indicators; // Object to get quality indicators

        HashMap parameters ; // Operator parameters

        // Logger object and file to store log messages
        logger_      = Configuration.logger_ ;
        fileHandler_ = new FileHandler("MOEAD.log");
        logger_.addHandler(fileHandler_) ;

        //thalis
        problem = (MOKP_Problem) lowerLevelProblem;

        String paretoName = "OPTIMAL_PARETO";
        if (!paretoFileName.equals("-")) { paretoName = paretoFileName; }
        indicators = new QualityIndicator(problem,
                problemPath + paretoName) ;
        
        algorithm = new MOEAD(problem);

        // Algorithm parameters
        popSize = 300;
        algorithm.setInputParameter("populationSize", popSize);
        algorithm.setInputParameter("maxEvaluations",1000000);

        algorithm.setInputParameter("dataDirectory",
                "/Users/emine/IdeaProjects/JMETALHOME/data/MOEAD_parameters/Weight");

        //thalis
        algorithm.setInputParameter("T", 11) ; // number of neighbours per individual
        algorithm.setInputParameter("delta", 1.0) ; // 1 = parents always from neighbourhood = MOEAD
        algorithm.setInputParameter("nr", 10) ; // maximal number of solutions that can be updated in "updateProblem"

        // Crossover operator
        //thalis
        parameters = new HashMap();
        double crossoverProbability = 1.0;
        parameters.put("probability", crossoverProbability);
        //crossover = new PartiallyMappedTwoPointCrossover(parameters);
        crossover = new PartiallyMappedHUXCrossover(parameters);
        algorithm.setInputParameter("repairAfterCrossoverMutation",0);

        // Mutation operator
        parameters = new HashMap();
        double mutationProbability = 0.01;
        parameters.put("probability", mutationProbability);
        parameters.put("repair", 1);
        parameters.put("problem", problem);
        mutation = new BitFlipMutation(parameters);

        algorithm.addOperator("crossover",crossover);
        algorithm.addOperator("mutation",mutation);

        algorithm.setInputParameter("rpType","Ideal"); // reference point z_ type, Ideal (best) or Nadir (worst)
        algorithm.setInputParameter("normalize",false);

        /* Comparator */
        Comparator lambdaComparator;
        if (problem.isMaxmized())
            lambdaComparator = new LambdaComparator(0, false) ; // Single objective comparator
        else lambdaComparator = new LambdaComparator(0, true) ; // Single objective comparator
        algorithm.setInputParameter("lambdaComparator", lambdaComparator);

        // Add the indicator object to the algorithm
        algorithm.setInputParameter("indicators", indicators) ;
    }

    public LowerLevelMOKP_MOEAD(){
        problemMOKP = new MOKP_Problem(problem);
        alg_moead = new MOEAD(problemMOKP, algorithm);
    }

    public void receiveParams(XReal y, Solution solution) {

        problemMOKP.setCostOfUsage(y);
        if (solution.isMarked())
            alg_moead.setInputParameter("initPopSolution", null);
        else {
            SolutionSet chosen = solution.getLL_Transfer_pop();
            SolutionSet transfer = new SolutionSet(chosen.size());
            for (int x=0; x<transfer.getCapacity(); x++){
                transfer.add(chosen.get(x));
            }
            alg_moead.setInputParameter("initPopSolution", transfer);
        }
    }

    public SolutionSet execute() throws JMException, ClassNotFoundException {
        // Execute the Algorithm
        SolutionSet population = alg_moead.execute();
        return population;
    }

}
