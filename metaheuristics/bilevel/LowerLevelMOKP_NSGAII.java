package jmetal.metaheuristics.bilevel;

import jmetal.core.*;
import jmetal.metaheuristics.nsgaII.NSGAII;
import jmetal.operators.crossover.PartiallyMappedHUXCrossover;
import jmetal.operators.crossover.PartiallyMappedTwoPointCrossover;
import jmetal.operators.mutation.BitFlipMutation;
import jmetal.operators.selection.SelectionFactory;
import jmetal.problems.MOKP_Problem;
import jmetal.qualityIndicator.QualityIndicator;
import jmetal.util.Configuration;
import jmetal.util.JMException;
import jmetal.util.comparators.ObjectiveComparator;
import jmetal.util.wrapper.XReal;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

public class LowerLevelMOKP_NSGAII {

    public static Logger logger_ ;      // Logger object
    public static FileHandler fileHandler_ ; // FileHandler object
    public static MOKP_Problem problemMOKP;         // The problem to solve
    public static Algorithm algorithm ;         // The algorithm to use
    public static int popSize;
    private static String problemPath = "/Users/emine/IdeaProjects/JMETALHOME/HV_Optimal/"; // The path of the files

    // statistical analysis
    private static int execution = 0;
    private static double time_mean = 0;
    private static FileWriter timeWriter;

    public static void initializeAlgorithm(Problem lowerLevelProblem, String dataPath, String paretoFileName) throws SecurityException, IOException {

        if (!dataPath.equals("-")) { problemPath = dataPath; }

        Operator crossover ;         // Crossover operator
        Operator  mutation  ;         // Mutation operator
        Operator  selection = null; // Selection operator

        QualityIndicator indicators ; // Object to get quality indicators

        HashMap parameters ; // Operator parameters

        // Logger object and file to store log messages
        logger_      = Configuration.logger_ ;
        fileHandler_ = new FileHandler("MOEAD.log");
        logger_.addHandler(fileHandler_) ;

        //thalis
        problemMOKP = (MOKP_Problem) lowerLevelProblem;

        String paretoName = "OPTIMAL_PARETO";
        if (!paretoFileName.equals("-")) { paretoName = paretoFileName; }
        indicators = new QualityIndicator(problemMOKP,
                problemPath + paretoName) ;

        algorithm = new NSGAII(problemMOKP);

        // Algorithm parameters
        popSize = 300;
        algorithm.setInputParameter("populationSize",popSize);
        algorithm.setInputParameter("maxEvaluations",100000);

        // Crossover operator
        //thalis
        parameters = new HashMap();
        double crossoverProbability = 1.0;
        parameters.put("probability", crossoverProbability);
        //crossover = new PartiallyMappedTwoPointCrossover(parameters);
        crossover = new PartiallyMappedHUXCrossover(parameters);
        algorithm.setInputParameter("repairAfterCrossoverMutation",0);
        //crossover = new TwoPointCrossoverCustom(parameters);
        //crossover = new SinglePointCrossover(parameters);
        //crossover = new HUXCrossover(parameters);

        // Mutation operator
        //thalis - authors have replaced this mutation operator with "updateProduct", but not us
        parameters = new HashMap();
        double mutationProbability = 0.01;
        parameters.put("probability", mutationProbability);
        parameters.put("repair", 1);
        parameters.put("problem", problemMOKP);
        mutation = new BitFlipMutation(parameters);
        //mutation = new SwapMutation(parameters);

        // Selection Operator
        parameters = null ;
        try {
            selection = SelectionFactory.getSelectionOperator("BinaryTournament2", parameters);
        } catch (JMException ex) {
            System.out.println("Selection exception: " + ex.toString());
        }

        algorithm.addOperator("crossover",crossover);
        algorithm.addOperator("mutation",mutation);
        algorithm.addOperator("selection",selection);

        /* Comparator */
        Comparator comparator;
        if (problemMOKP.isMaxmized())
            comparator = new ObjectiveComparator(0, true) ; // Single objective comparator
        else comparator = new ObjectiveComparator(0) ; // Single objective comparator
        algorithm.setInputParameter("comparator", comparator);

        // Add the indicator object to the algorithm
        algorithm.setInputParameter("indicators", indicators) ;

        //timeWriter = new FileWriter("LowerLevelParetoVisualNSGAII/time.txt");
    }


    public static SolutionSet evaluate(XReal y, Solution solution) throws JMException, SecurityException, ClassNotFoundException {

        problemMOKP.setCostOfUsage(y);
        if (solution.isMarked())
            algorithm.setInputParameter("initPopSolution", null);
        else algorithm.setInputParameter("initPopSolution", solution.getLL_ND_pop());

        // Execute the Algorithm
        long initTime = System.currentTimeMillis();
        SolutionSet population = algorithm.execute();
        long estimatedTime = System.currentTimeMillis() - initTime;
        /*
        try {
            execution++;
            time_mean += estimatedTime;
            timeWriter.write(estimatedTime + "\n");
            if (execution == 20) {
                time_mean = time_mean / 20;
                timeWriter.write(time_mean + "\n");
                timeWriter.close();
            }
        } catch(Exception e){}

         */


        return population;
    }

}
