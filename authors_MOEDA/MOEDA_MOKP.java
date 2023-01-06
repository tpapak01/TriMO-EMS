/**
 * MOEDA.java
 * MOEDA for binary problems,分布估计
 */

package jmetal.authors_MOEDA;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import jmetal.core.Algorithm;
import jmetal.core.Operator;
import jmetal.core.Problem;
import jmetal.core.Solution;
import jmetal.core.SolutionSet;
import jmetal.encodings.variable.Binary;
import jmetal.encodings.variable.MOKP_BinarySolution;
import jmetal.metaheuristics.moead.Utils;
//import jmetal.myutils.CaptureConvergence;
//import jmetal.myutils.RecordMetricDuringRun;
import jmetal.util.JMException;
import jmetal.util.PseudoRandom;
import jmetal.util.comparators.DominanceComparator;
import jmetal.util.Ranking;

public class MOEDA_MOKP extends Algorithm {
	
	private int         populationSize_;
	private SolutionSet population_;  // Population repository
	
	double[]   zp_;					  // Zp vector (nadir point = big)
	int[][]    neighborhood_; 		  // Neighborhood matrix
	double[][] lambda_; 			  // Lambda vectors
	
	int    T_;     					  // Neighborhood size
	int    nr_;    					  // Maximal number of solutions replaced by each child solution
	double delta_; 					  // Probability that parent solutions are selected from neighborhood
	double theta_; 
	int    evaluations_; 			  // Counter for the number of function evaluations
	int    maxEvaluations;
	
	double [] gen_min; // minimum point in current population
	double [] gen_max; // maximum point in current population
	
	int [] solutionVisited;
	int [][] numberOfTrue;
	int numerOfBits;
	
	Solution[] indArray_;
	String functionType_;
	String rpType_;
	
	Operator crossover_;
	Operator mutation_;
	Operator localSearch_;
	
	String dataDirectory_;
	private static final Comparator dominance_ = new DominanceComparator();
	  
	//CaptureConvergence captureConvergence_; // our class
    //boolean convergenceFlag = false;
    
	//RecordMetricDuringRun rmdr; // our class
    //boolean rmdrFlag = true;
    
	/**
	 * Constructor
	 * 
	 * @param problem to solve
	 */
	public MOEDA_MOKP(Problem problem) {
		super(problem);

//		functionType_ = this.getInputParameter("functionType").toString();	
//		rpType_       = this.getInputParameter("rpType").toString();	
//		System.out.println(functionType_);
//		if (convergenceFlag== true) 
//			  captureConvergence_ = new CaptureConvergence(problem.getName(), "MOEDA" + functionType_ + rpType_,
//					  problem.getNumberOfObjectives());
//	
//		if (rmdrFlag== true) {
//			rmdr = new RecordMetricDuringRun(problem.getName(), "MOEDA" + functionType_ + rpType_);
//		}
		
	} // MOEDA

	public SolutionSet execute() throws JMException, ClassNotFoundException {
		// ------------读取function和rp的类型，生成相应的文件---------------------
		functionType_ = this.getInputParameter("functionType").toString();	
		rpType_       = this.getInputParameter("rpType").toString();	

		/*
		if (convergenceFlag== true) 
			  captureConvergence_ = new CaptureConvergence(problem_.getName(), "MOEDA" + functionType_ + rpType_,
					  problem_.getNumberOfObjectives());
	
		if (rmdrFlag== true) {
//			rmdr = new RecordMetricDuringRun(problem_.getName(), "MOEDA" + functionType_ + rpType_);
			rmdr = new RecordMetricDuringRun(problem_.getName(), "MOEDA+" + functionType_ + "+"+ rpType_);
		}
		// ------------读取function和rp的类型，生成相应的文件（end）---------------------

					
		long elapsed = 0, start = System.currentTimeMillis(); // start
		boolean timeRecorded1 = false, timeRecorded2 = false; // whether the TT50 and TT100 have been recorded


		 */

		evaluations_ = 0;	
		
		maxEvaluations = ((Integer) this.getInputParameter("maxEvaluations")).intValue();
		populationSize_ = ((Integer) this.getInputParameter("populationSize")).intValue();
		Long maxRunTimeMS = ((Long) this.getInputParameter("maxRunTimeMS")).longValue();

		
		// ----------------------------------------------------------
		// Used only for random weight vectors, should be modified
//		while (populationSize_ % 4 > 0)
//			populationSize_ += 1;	
		// ----------------------------------------------------------
		
		dataDirectory_ = this.getInputParameter("dataDirectory").toString();		
	    T_ = ((Integer) this.getInputParameter("T")).intValue();
	    nr_ = ((Integer) this.getInputParameter("nr")).intValue();
	    delta_ = ((Double) this.getInputParameter("delta")).doubleValue();
	    theta_ = ((Double) this.getInputParameter("theta")).doubleValue();
	    
		population_ = new SolutionSet(populationSize_);
		indArray_ = new Solution[problem_.getNumberOfObjectives()];
		
	    double alpha = 0.0;
	    
		neighborhood_ = new int[populationSize_][T_];

		zp_ = new double[problem_.getNumberOfObjectives()];
		lambda_ = new double[populationSize_][problem_.getNumberOfObjectives()];
		
		gen_min = new double [problem_.getNumberOfObjectives()];
		gen_max = new double [problem_.getNumberOfObjectives()];
		
		for (int j=0; j < problem_.getNumberOfObjectives(); j++) {
			gen_min[j] = 1e+30;
			gen_max[j]= -1e+30;
		} // Initialize gen_min and gen_max, only for normalization
		
		crossover_ = operators_.get("crossover"); 	// default: single-point crossover
		mutation_ = operators_.get("mutation"); 	// default: bit-flip mutation
		
		//long initTime = System.currentTimeMillis();
		
		// STEP 1. Initialization		
		// STEP 1.1. Compute Euclidean distances between weight vectors and find T
		initUniformWeight();
		//initRandomWeight();
		initNeighborhood();

		// STEP 1.2. Initialize population
		initPopulation();

		//The following section is only used to obtain a probability for mutation purposes (updateProduct)
		//------------- Obtain the length of the binary and initialize two arrays--------
		Binary temp = (Binary)((population_.get(0).getDecisionVariables()[0]));
		numerOfBits = temp.getNumberOfBits();

		solutionVisited = new int [populationSize_]; // For each sub-problem, the number of solutions
		numberOfTrue = new int [populationSize_][numerOfBits]; // 
		
		// 初始化两个数组
		for (int i = 0; i < populationSize_; i++ ) {
			solutionVisited[i] = 1; 
			
			Binary bin = (Binary)((population_.get(i).getDecisionVariables()[0]));
			
			for (int j = 0; j < numerOfBits; j++) {
				
				if (bin.getIth(j) == true) {
					numberOfTrue[i][j] ++;
				}
			}
		}
		//------------- Obtain the length of the binary and initialize two arrays--------
		
		// STEP 1.3. Normalize population
		normalize_pop(population_);
		//replaces original "initIdealPoint"
		initialize_RP();
			
		//if (convergenceFlag== true)
		//	captureConvergence_.runCaptureConvergence(0, population_);
		
		// STEP 2. Update
		do {		
			
			int[] permutation = new int[populationSize_];
			Utils.randomPermutation(permutation, populationSize_);

			for (int i = 0; i < populationSize_; i++) {
				int n = permutation[i];
				int type;
				double rnd = PseudoRandom.randDouble();

				// STEP 2.1. Mating selection based on probability
				if (rnd < delta_) // if (rnd < realb)
				{
					type = 1; // neighborhood
				} else {
					type = 2; // whole population
				}
				
				Vector<Integer> p = new Vector<Integer>();
				matingSelection(p, n, 2, type);

				//also used specifically to obtain a probability for mutation purposes, just like
				//numberofBits and solutionVisited
				alpha = (0.0 - 1.0) *  (double)evaluations_/ maxEvaluations + 1.0;
//				alpha = (0.0 - 1.0) * (double)elapsed / maxRunTimeMS + 1.0; 	
//				alpha = 1.0;
//				alpha = 0.5;	
//				System.out.println("alpha =" + alpha);

				double [] prob;
				double [] prob1 = new double[numerOfBits];
				double [] prob2 = new double[numerOfBits];
				
                for (int j = 0; j < numerOfBits;j ++) {                	
                	prob1[j] = 0.5 * alpha + (double) numberOfTrue[p.get(0)][j] / solutionVisited[p.get(0)] * (1 - alpha);
                	prob2[j] = 0.5 * alpha + (double) numberOfTrue[p.get(1)][j] / solutionVisited[p.get(1)] * (1 - alpha); 

                }     

                double rndSel =  PseudoRandom.randDouble();
                
                if (rndSel < 0.5) 
                	prob = prob1;
                else 
                	prob = prob2;                
      
				/**
				 * --------------------Use crossover and mutation (or update)--------------------
				 */
				Solution[] parents = new Solution[2];
				parents[0] = population_.get(p.get(0));
				parents[1] = population_.get(p.get(1));
				
				Solution[] children = (Solution[]) crossover_.execute(parents);

				Solution child = null;
				
				if (rndSel < 0.5) {
					child = children[0];
	            } else 
	            	child = children[1];
				 
				// Apply mutation
//				mutation_.execute(child); 
				//	Apply update			
				((MOKP_BinarySolution)(problem_.getSolutionType())).updateProduct(child, prob);
				 // --------------------Use crossover and mutation (end)--------------------
								
				// Evaluation
				problem_.evaluate(child);
				evaluations_++;		
				
				//if (convergenceFlag== true)
				//	captureConvergence_.runCaptureConvergence(evaluations_, population_);
				
				normalize_ind(child);
				//same as "updateReference" in original
				update_RP(child);
				
				// STEP 2.5. Update of solutions
				updateProblem(child, n, type);

				if (evaluations_>= maxEvaluations) {
					break;
				}
				
			} // for
			
			normalize_pop (population_);
			initialize_RP();
			
			//elapsed = System.currentTimeMillis() - start;
			
			// record time
			/*
			if (rmdrFlag== true && (timeRecorded1 == false || timeRecorded2 == false)) { 
				
				int feasibleNo = 0;
				for (int i = 0; i < population_.size();i++) {
					if (population_.get(i).getNumberOfViolatedConstraint() == 0) {
						feasibleNo ++;
					}
				}
				double rate = (double)feasibleNo/populationSize_ ;
				
				if (timeRecorded1 == false && rate >= 0.5) 	{		
//					rmdr.writeMetric("TT50", elapsed/1000.0);
					rmdr.writeMetric("TT50", evaluations_);
					timeRecorded1 = true;
				}
			
				if (timeRecorded2 == false && rate >= 1.0) {				
//					rmdr.writeMetric("TT100", elapsed/1000.0);
					rmdr.writeMetric("TT100", evaluations_);
					timeRecorded2 = true;
				}
				
			}

			 */
			
		} while ( evaluations_ < maxEvaluations);

		/*
		if (rmdrFlag== true && timeRecorded1 == false) {
			rmdr.writeMetric("TT50", -1.0);			
		}
		
		if (rmdrFlag== true && timeRecorded2 == false) {
			rmdr.writeMetric("TT100", -1.0);
		}
		
	    System.out.println("RunTimeMS: " + elapsed);
        System.out.println("evaluations: " + evaluations_);

		 */
                
        // Only feasible solutions		 
		SolutionSet feasibleSet = new SolutionSet(population_.size());		
		for (int i = 0; i < population_.size();i++) {
			feasibleSet.add(new Solution(population_.get(i)));
		}
		
        // At last remove identical solutions 
        SolutionSet finalSet = new SolutionSet(feasibleSet.size());        
        finalSet.add(feasibleSet.get(0));        
      
	    for (int i = 1; i < feasibleSet.size(); i++) {      
			      
			Solution sol = feasibleSet.get(i);
			boolean existEqual = false;
			
			for (int j = 0; j < finalSet.size();j++) {
				if (equalSolution(sol, finalSet.get(j))) {
					existEqual = true;
					break;	    		
				}
			}
			
			if (existEqual == true) continue;
		
			finalSet.add(feasibleSet.get(i));	 
			
		} // for			    
	   

	    // Find non-dominated solutions
	    Ranking ranking = new Ranking(finalSet);
        System.out.println("# Non-dominated feasible solutions in MOEDA = " + ranking.getSubfront(0).size());         
    
        // return population_ 的原因是在RunExperiment类中有进行同样的处理。上述处理仅为了便于观察性能
        return population_;	
	}

	public boolean equalSolution (Solution sol1, Solution sol2) {
		
		for (int i = 0; i < sol1.getNumberOfObjectives();i++) {
			if (sol1.getObjective(i) != sol2.getObjective(i))
				return false;
		}
		
		return true;
	}
	
	/**
	 * Initialize the weight vectors, this function only can read from the 
	 * existing data file, instead of generating itself.
	 * 
	 */
	public void initUniformWeight() {
		String dataFileName;
		dataFileName = "W" + problem_.getNumberOfObjectives() + "D_"
				+ populationSize_ + ".dat";
		
		if ((problem_.getNumberOfObjectives() == 2) && (populationSize_ <= 300)) {
		      for (int n = 0; n < populationSize_; n++) {
		        double a = 1.0 * n / (populationSize_ - 1);
		        lambda_[n][0] = a;
		        lambda_[n][1] = 1 - a;
		      } // for
		} else {// if
		
			try {
				// Open the file
				FileInputStream fis = new FileInputStream(dataDirectory_ + "/"
						+ dataFileName);
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
						j++;
						
					}
					aux = br.readLine();
					i++;
				}
				br.close();
			} catch (Exception e) {
				System.out
						.println("initUniformWeight: failed when reading for file: "
								+ dataDirectory_ + "/" + dataFileName);
				e.printStackTrace();
			}
		}
	} // initUniformWeight

	/**
	   * initRandomWeight
	 * @throws IOException 
	   */
	  public void initRandomWeight() {	

		  if ((problem_.getNumberOfObjectives() == 2) && (populationSize_ <= 300)) {
		      for (int n = 0; n < populationSize_; n++) {
		        double a = 1.0 * n / (populationSize_ - 1);
		        lambda_[n][0] = a;
		        lambda_[n][1] = 1 - a;
		      } // for
		    } // if
		    else {
		      String dataFileName;
		      dataFileName = "W" + problem_.getNumberOfObjectives() + "D_" +
		        populationSize_ + ".rw";
		      String filePath = dataDirectory_ + "/" + dataFileName;
		      
		      File f = new File(filePath);
		      if(f.exists()) {
		    	  System.out.println("Weight " + dataFileName + " exists, loading weights");
		    	  loadWeights(filePath);
		      } else{
		    	  System.out.println("Weight " + dataFileName + " does not exist, generating weights");
		    	  try {
		    		  generateWeights(filePath);
		    	  } catch (Exception e) {
		  			System.err.println("generate Weights fiailed");
			        e.printStackTrace();
		           }
		      }//else
		      
		     } // else 		      
	
	  } // initUniformWeight  
	  
	  
	  public void loadWeights(String path) {
			try {
				// Open the file
				FileInputStream fis = new FileInputStream(path);
				InputStreamReader isr = new InputStreamReader(fis);
				BufferedReader br = new BufferedReader(isr);

				int numberOfObjectives = 0;
				int i = 0;
				int j = 0;
				String aux = br.readLine();
				while (aux != null) {
					StringTokenizer st = new StringTokenizer(aux);
					j = 0;
					numberOfObjectives = st.countTokens();
					while (st.hasMoreTokens()) {
						double value = (new Double(st.nextToken())).doubleValue();
						lambda_[i][j] = value;
//						 System.out.println("lambda["+i+","+j+"] = " + value) ;
						j++;
					}
					aux = br.readLine();
					i++;
				}
				br.close();
			} catch (Exception e) {
				System.err
						.println("initUniformWeight: failed when reading for file: "
								+ path);
				e.printStackTrace();
			}
		}// loadWeights
	  
	  /**
	   * Generate weight vectors
	   * @param path
	   * @throws IOException
	   */
	  public void generateWeights(String path) throws IOException{

			int numberOfObjectives = problem_.getNumberOfObjectives();
			
			List<double[]> weights = new ArrayList<double[]>(5000);

			// create 5000 random weights
			for (int i = 0; i < 5000; i++) {
				double[] weight = new double[numberOfObjectives];
				double sum = 0.0; 
				
				for (int j = 0; j < numberOfObjectives; j++) {
					weight[j] =  PseudoRandom.randDouble();
					sum = sum + weight[j];
				}		
			
				
				for (int j = 0; j < numberOfObjectives; j++) {
					weight[j] /= sum;
				}
				
				weights.add(weight);
			}

			List<double[]> W = new ArrayList<double[]>(populationSize_);
			
			// initialize W with weights (1,0,...,0), (0,1,...,0), ...,
			// (0,...,0,1)
			for (int i = 0; i < numberOfObjectives; i++) {
				double[] weight = new double[numberOfObjectives];
				weight[i] = 1.0;
				W.add(weight);
			}
			
			// fill in remaining weights with the weight vector with the largest
			// distance from the assigned weights
			while (W.size() < populationSize_) {
				double[] weight = null;
				double distance = Double.NEGATIVE_INFINITY;

				for (int i = 0; i < weights.size(); i++) {
					double d = Double.POSITIVE_INFINITY;

					for (int j = 0; j < W.size(); j++) {		
						d = Math.min(d, Utils.distVector(weights.get(i),W.get(j)));
					}
					
					if (d > distance) {
						weight = weights.get(i);
						distance = d;
					}
				}		
				
				W.add(weight);
				weights.remove(weight);
			}

			/**
			 * Write weight into a file 
			 */
			try {
				 FileOutputStream fos   = new FileOutputStream(path)     ;
			     OutputStreamWriter osw = new OutputStreamWriter(fos)    ;
			     BufferedWriter bw      = new BufferedWriter(osw)        ;
			     
			     
				for(int i=0;i< populationSize_;i++) {
					lambda_[i] = W.get(i);
					String aux = "";
					for(int j=0; j< numberOfObjectives;j++){
						aux = aux + Double.toString(lambda_[i][j]) + " ";				
					}		
					bw.write(aux);
					bw.newLine();
				}
				
				bw.close();
			} catch (Exception e) {
				System.err.println("Write weights : failed when reading for file: "
								+ path);
				e.printStackTrace();
			}
			
		}
	/**
	 * Initialize the neighborhood matrix of subproblems, based on the Euclidean
	 * distances between different weight vectors
	 * 
	 */
	public void initNeighborhood() {
		int[] idx  = new int[populationSize_];
		double[] x = new double[populationSize_];

		for (int i = 0; i < populationSize_; i++) {
			/* calculate the distances based on weight vectors */
			for (int j = 0; j < populationSize_; j++) {
				x[j] = Utils.distVector(lambda_[i], lambda_[j]);
				idx[j] = j;
			}
			/* find 'niche' nearest neighboring subproblems */
			Utils.minFastSort(x, idx, populationSize_, T_);

			for (int k = 0; k < T_; k++) {
				neighborhood_[i][k] = idx[k];
			}
		}
	} // initNeighborhood

	/**
	 * Initialize the population, random sampling from the search space
	 * 
	 * @throws JMException
	 * @throws ClassNotFoundException
	 */
	public void initPopulation() throws JMException, ClassNotFoundException {
		
		for (int i = 0; i < populationSize_; i++) {
			Solution newSolution = new Solution(problem_);
			
			problem_.evaluate(newSolution);
			evaluations_++;
			population_.add(newSolution);
		}
	} // initPopulation


	/**
	 * Select the mating parents, depending on the selection 'type'
	 * 
	 * @param list : the set of the indexes of selected mating parents
	 * @param cid  : the id of current subproblem
	 * @param size : the number of selected mating parents
	 * @param type : 1 - neighborhood; otherwise - whole population
	 */
	public void matingSelection(Vector<Integer> list, int cid, int size, int type) {
		int ss;
		int r;
		int p;

		ss = neighborhood_[cid].length;
		while (list.size() < size) {
			if (type == 1) {
				r = PseudoRandom.randInt(0, ss - 1);
				p = neighborhood_[cid][r];
			} else {
				p = PseudoRandom.randInt(0, populationSize_ - 1);
			}
			boolean flag = true;
			for (int i = 0; i < list.size(); i++) {
				if (list.get(i) == p) // p is in the list
				{
					flag = false;
					break;
				}
			}

			if (flag) {
				list.addElement(p);
			}
		}
	} // matingSelection



	// initialise the reference point
	void initialize_RP() {
		int i;
		for(i = 0; i < problem_.getNumberOfObjectives(); i++)  {
			//if Nadir, start big. Else, start small
			if(rpType_.equalsIgnoreCase("Nadir")) {
				zp_[i] = 1.1;
			} else if(rpType_.equalsIgnoreCase("Ideal")){
				zp_[i] = -0.1; // This is very important for improving the performance
			} else {				
				System.out.println("MOEDA.initialize_RP: unknown type " + rpType_);
				System.exit(-1);
			}
			
		}
	}
	
	/**
	 * Update the population by the current offspring
	 * 
	 * @param indiv: current offspring
	 * @param id:    index of current subproblem
	 * @param type:  update solutions in - neighborhood (1) or whole population (otherwise)
	 */
	void updateProblem(Solution indiv, int id, int type) {
		int size;
		int time;

		time = 0;

		if (type == 1) {
			size = neighborhood_[id].length;
		} else {
			size = population_.size();
		}
		int[] perm = new int[size];

		Utils.randomPermutation(perm, size);

		for (int i = 0; i < size; i++) {
			int k;
			if (type == 1) {
				k = neighborhood_[id][perm[i]];
			} else {
				k = perm[i];  // calculate the values of objective function regarding the current subproblem
			}
			
			int flagDominate;

			if (problem_.isMaxmized() == false)
				flagDominate = dominance_.compare(indiv, population_.get(k));
			else flagDominate = dominance_.compare(population_.get(k), indiv);

			
			if (flagDominate == 0) { // Non-dominated 
				double f1, f2;					

				f1 = fitnessFunction(population_.get(k), lambda_[k]);
				f2 = fitnessFunction(indiv, lambda_[k]);
				
				if (problem_.isMaxmized()) {
					//if f2 bigger than f1, f2 is better. Correct
					if (f2 > f1) {
						flagDominate = 1;
					}
				} else {
					//if f2 smaller than f1, f2 is better. Correct
					if (f2 < f1) {
						flagDominate = -1;
					}
				} // if 
			}
			
			if (flagDominate == -1) {// indiv is better
				population_.replace(k, new Solution(indiv));
				time++;
				
				solutionVisited[k] ++;				
				Binary bin = (Binary)((indiv.getDecisionVariables()[0]));
				
				for (int j = 0; j < numerOfBits; j++) {
					
					if (bin.getIth(j) == true) {
						numberOfTrue[k][j] ++;
					}
				} // for j
				
			} // if 
			
						
			/**
			 * ------------------------Code before------------------------------
			 */
//			int nvc_indiv = indiv.getNumberOfViolatedConstraint();
//			int nvc_k = population_.get(k).getNumberOfViolatedConstraint();
//			
//			if (nvc_indiv < nvc_k) {
//				
//				population_.replace(k, new Solution(indiv));
//				time++;
//				
//			} else if (nvc_indiv == nvc_k) {
//				
//				double f1, f2;
//
//				//比较适应值
//				f1 = fitnessFunction(population_.get(k), lambda_[k]);
//				f2 = fitnessFunction(indiv, lambda_[k]);
//
//				if (problem_.isMaxmized() == true && functionType_.equals("WS")) {
//					if (f2 > f1) {
//						population_.replace(k, new Solution(indiv));
//						time++;
//					}
//				}
//				
//				else {
//					if (f2 < f1) {
//						population_.replace(k, new Solution(indiv));
//						time++;
//					}
//				}
//			}		
			//-----------------------Code before (end)------------------------------			 
			
			// the maximal number of solutions updated is not allowed to exceed
			// 'limit'
			if (time >= nr_) {
				return;
			}
		}
	} // updateProblem

	double innerproduct(double[] vec1, double[] vec2) {
		double sum = 0;
		for (int i = 0; i < vec1.length; i++)
			sum += vec1[i] * vec2[i];
		return sum;
	}

	double norm_vector(Vector<Double> x) {
		double sum = 0.0;
		for (int i = 0; i < (int) x.size(); i++)
			sum = sum + x.get(i) * x.get(i);
		return Math.sqrt(sum);
	}

	double norm_vector(double[] z) {
		double sum = 0.0;
		for (int i = 0; i < problem_.getNumberOfObjectives(); i++)
			sum = sum + z[i] * z[i];
		return Math.sqrt(sum);
	}

	double fitnessFunction(Solution individual, double[] lambda) {
		double fitness;
		fitness = 0.0;
		
		if (functionType_.equals("WS")) { //Weighted Sum Approach 
			
			for (int n = 0; n < problem_.getNumberOfObjectives(); n++) {
				fitness = fitness + lambda[n] * individual.getNormalizedObjective(n); // Objectives after normalization
			}
			
		} 
		else if (functionType_.equals("TCHE1")) {
		      double maxFun = -1.0e+30;

		      for (int n = 0; n < problem_.getNumberOfObjectives(); n++) {
		        double diff = individual.getNormalizedObjective(n) - zp_[n]; //Note: No abs 

		        if (problem_.isMaxmized()) {
		        	diff = zp_[n] - individual.getNormalizedObjective(n);
		        }
		        
		        double feval;
		        if (lambda[n] == 0) {
		          feval = 0.0001 * diff;
		        } else {
		          feval = lambda[n] * diff ;
		        }
		       
		        if (feval > maxFun) {
		          maxFun = feval;
		        }
		        
		      } // for

		      fitness = maxFun;
		} // if
		else if (functionType_.equals("TCHE2")) {
		      double maxFun = -1.0e+30;

		      for (int n = 0; n < problem_.getNumberOfObjectives(); n++) {
		        double diff = individual.getNormalizedObjective(n) - zp_[n];
		        
		        if (problem_.isMaxmized() == true) { // For maximization problem
		        	diff =  zp_[n] - individual.getNormalizedObjective(n);
		        }
		        
		        double feval;
		        if (lambda[n] == 0) {
		          feval = diff/0.0001;
		        } else {
		          feval = diff/lambda[n];
		        }
		        if (feval > maxFun) {
		          maxFun = feval;
		        }
		        
		      } // for

	         fitness = maxFun;
	         
		} else if (functionType_.equals("PBI")) {
			    int nobj = problem_.getNumberOfObjectives();
	            
	    		double nd = norm_vector(lambda); 
	    		
	    		for(int i=0; i<nobj; i++) 
	    			lambda[i] = lambda[i]/nd; //A unit vector
	     
	    	    // penalty method  
	    	    // temporary vectors NBI method 
	    		 double[] realA = new double[nobj]; 
	    		 double[] realB = new double[nobj]; 
	     
	    		// difference between current point and reference point 
	    		for(int n=0; n<nobj; n++) {
	    			realA[n] = (individual.getNormalizedObjective(n) - zp_[n]); //
	    			
	    			if (problem_.isMaxmized() == true) { // For maximization problem
	    				realA[n] = (zp_[n] - individual.getNormalizedObjective(n));
			        }
	    		}
	     
	    		// distance along the search direction norm 
//	    		double d1 = Math.abs(innerproduct(realA,lambda)); 
	    		
	    		// 注意： 当d1不取绝对值时，PBI和IPBI是可以统一的。即IPBI相当于zp取天底点
	    		double d1 = innerproduct(realA,lambda); // 不取绝对值
	
	    		// distance to the search direction norm 
	    		for(int n=0; n<nobj; n++) {
	    			realB[n] = (individual.getNormalizedObjective(n) - (zp_[n] + d1*lambda[n]));
	    			
	    			if (problem_.isMaxmized() == true) { // For maximization problem
	    				realB[n] = (individual.getNormalizedObjective(n) - (zp_[n] - d1*lambda[n]));
			        }
	    		}
	    		
	    		double d2 = norm_vector(realB); 	     
	    		fitness =  (d1 + theta_ *d2);   
	    		
		}		
	   else {
			System.out.println("MOEDA.fitnessFunction: unknown type " + functionType_);
			System.exit(-1);
		}
		return fitness;
	} // fitnessEvaluation
	

	public void normalize_pop (SolutionSet pop)	{
		int i, j;
		
		for (j=0; j < problem_.getNumberOfObjectives(); j++) {
//			gen_min[j] = 1e+30;
			gen_max[j]= -1e+30;
	        for (i=0; i < populationSize_; i++)	{
				if (pop.get(i).getObjective(j) < gen_min[j]) {
					gen_min[j] = pop.get(i).getObjective(j);
				}
				if (pop.get(i).getObjective(j)> gen_max[j])	{
	                gen_max[j] = pop.get(i).getObjective(j);
				}				
			}
			
			if (gen_max[j] == gen_min[j])	{
				for (i=0; i < populationSize_; i++){
					pop.get(i).setNormalizedObjective(j, 0.5);
				}
			} else {
				for (i=0; i<populationSize_; i++)	{
					pop.get(i).setNormalizedObjective(j, (pop.get(i).getObjective(j)- gen_min[j]) / (gen_max[j] - gen_min[j]));
					
				}
			} //if	 
			
		}	// for j
		
		// Estimate convex or concave
//		int solutionsBelowLine = 0;
//		
//		for (i = 0; i < populationSize_; i++) {
//			double sumObj = 0.0;
//			
//			for (j = 0; j < problem_.getNumberOfObjectives(); j++) {
//				sumObj = sumObj + pop.get(i).getNormalizedObjective(j);
//			}
//			
//			if (sumObj < 1.0) solutionsBelowLine++;
//			
//		} // for i
//		
//		if (solutionsBelowLine > 0.9 * populationSize_) {
//			this.rpType_ = "Nadir";		
//
//		} else {
//			this.rpType_ = "Ideal";
//		}
	

	} //normalize_pop

	public void normalize_ind (Solution ind)	{
		int j;
		for (j=0; j< problem_.getNumberOfObjectives(); j++)	{
			if (gen_max[j] == gen_min[j]) {
				ind.setNormalizedObjective(j, 0.5);
			}
			else {
				double value = (ind.getObjective(j) - gen_min[j]) / (gen_max[j] - gen_min[j]);
				ind.setNormalizedObjective(j, value);
			}
		}
		return;
	}
	
	// update the reference point
	void update_RP(Solution ind){
		int i;
		
		for(i = 0; i < problem_.getNumberOfObjectives(); i++)   {
			//if Nadir, make it big. Else, make it small
			if(rpType_.equalsIgnoreCase("Nadir")) {
				if(ind.getNormalizedObjective(i) > zp_[i])	{
					zp_[i] = ind.getNormalizedObjective(i);
				}
			} else {
				if(ind.getNormalizedObjective(i) < zp_[i])	{
					zp_[i] = ind.getNormalizedObjective(i);
				}
			}
			
		}
	}
} // MOEDA
