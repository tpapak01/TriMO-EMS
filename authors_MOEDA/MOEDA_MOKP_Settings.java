//  MOEDA_Settings.java 

package jmetal.authors_MOEDA;

import java.util.HashMap;

import jmetal.core.Algorithm;
import jmetal.core.Problem;
import jmetal.experiments.Settings;
import jmetal.operators.crossover.Crossover;
import jmetal.operators.crossover.SinglePointCrossover;
import jmetal.operators.mutation.BitFlipMutation;
import jmetal.operators.mutation.Mutation;
import jmetal.operators.selection.BinaryTournament;
import jmetal.operators.selection.Selection;
import jmetal.util.JMException;


/**
 * Settings class of algorithm VaEA (real encoding)
 */
public class MOEDA_MOKP_Settings extends Settings {
  public int populationSize_                 ;
  public int maxEvaluations_                 ;
  public int maxGenerations_;
  public double mutationProbability_         ;
  public double crossoverProbability_        ;
  public double theta_                       ;
  public int SATtimeout_                    ; // runtime for SAT 
  public long iteratorTimeout_               ; // runtime for the whole algorithm
	
  public String dataDirectory_  ;
  public int T_        ;
  public double delta_ ;
  public int nr_    ;
  String functionType_; // scalar function type
  String rpType_;      // reference point type, Ideal or Nadir
  
  // For Permutation variable
  public MOEDA_MOKP_Settings(Problem problem) {
	    super(problem.getName()) ;   
	    problem_ = problem;
	    
	    // Default experiments.settings
	    if (problem_.getNumberOfObjectives() == 2) {
	    	populationSize_              = 100   ; 
	    } else if (problem_.getNumberOfObjectives() == 3) {
	    	populationSize_              = 105   ;
	    } else if (problem_.getNumberOfObjectives() == 4) {
	    	populationSize_              = 120   ;
	    } else if (problem_.getNumberOfObjectives() == 6) {
	    	populationSize_              = 126   ;
	    } else if (problem_.getNumberOfObjectives() == 8) {
	    	populationSize_              = 120   ;
	    } else if (problem_.getNumberOfObjectives() == 10) {
	    	populationSize_              = 220   ;
	    } else {
	    	populationSize_              = 100   ; 
	    }
	        
		maxGenerations_              = Integer.MAX_VALUE; 
	    dataDirectory_ =  "/Users/emine/IdeaProjects/JMETALHOME/data/MOEAD_parameters/Weight" ;
	    
	    crossoverProbability_        = 1.0;	  
	 	mutationProbability_         = 0.01;	 	
	    maxEvaluations_              = 400000;	    
	 	
	    // ------------Should be modified------------------
	    T_ = 10;
	    delta_ = 1.0;
	    nr_ = T_;  
	    theta_ = 5.0; // used in PBI
	    
	    // Note the algorithm name in the study class should be modified accordingly
	    functionType_ = "TCHE1"; // scalar function type
	    rpType_  = "Ideal";      // reference point type, Ideal or Nadir	  
	    System.out.println("MOEDA+" + functionType_ + "+" + rpType_ );
	 // ------------Should be modified------------------
	    
	 } // MOEDA_Settings
  /**
   * Configure MOEDA_VaEA with user-defined parameter experiments.settings
   * @return A MOEDA_VaEA algorithm object
   * @throws jmetal.util.JMException
   */
  public Algorithm configure() throws JMException {
    Algorithm algorithm ;
    Selection  selection ;
    Crossover  crossover ;
    Mutation   mutation  ;

    HashMap  parameters ; // Operator parameters

    // Creating the algorithm.
    algorithm = new MOEDA_MOKP(problem_) ; 

    
    // Algorithm parameters
    algorithm.setInputParameter("maxEvaluations",maxEvaluations_);
    algorithm.setInputParameter("maxGenerations",maxGenerations_);
    algorithm.setInputParameter("maxRunTimeMS",iteratorTimeout_);
    algorithm.setInputParameter("populationSize",populationSize_);   
    algorithm.setInputParameter("normalize",true);  
    
    algorithm.setInputParameter("dataDirectory", dataDirectory_) ;
    algorithm.setInputParameter("T", T_) ;
    algorithm.setInputParameter("delta", delta_) ;
    algorithm.setInputParameter("nr", nr_) ;
    algorithm.setInputParameter("theta",theta_);   
    algorithm.setInputParameter("functionType",functionType_);   
    algorithm.setInputParameter("rpType",rpType_);   
    
	/**
	 *  Mutation and Crossover for binary codification
	 */
    // Mutation and Crossover for Real codification 
    parameters = new HashMap();
    parameters.put("probability", crossoverProbability_);
    crossover = new SinglePointCrossover(parameters);
//    crossover = new UniformCrossover(parameters);
    
    parameters = new HashMap();
    parameters.put("probability", mutationProbability_);
    mutation = new BitFlipMutation(parameters);
       
    
//    parameters = new HashMap();    
//    parameters.put("probability", mutationProbability_);
//    mutation = new SATVaEA_NewMutation(parameters,  problem_,10000);
    
    // Selection Operator, binary tournament 
    parameters = new HashMap();
//    selection = new ConstraintBinaryTournament(parameters);
//    selection = new RandomSelection(parameters);
    selection = new BinaryTournament(parameters);
    
//    // Selection Operator
//    parameters = new HashMap() ;
//    selection = new RandomSelection(parameters);//
        
    // Add the operators to the algorithm
    algorithm.addOperator("crossover",crossover);
    algorithm.addOperator("mutation",mutation);
    algorithm.addOperator("selection",selection);

    return algorithm ;
  } // configure
  
} // MOEDA_Settings
