/**
 * Author: Yi Xiang
 * gzhuxiang_yi@163.com
 */



package jmetal.encodings.variable;

import java.io.FileNotFoundException;
import java.util.Random;

import jmetal.core.Problem;
import jmetal.core.Solution;
import jmetal.core.Variable;
import jmetal.encodings.solutionType.BinarySolutionType;
import jmetal.util.PseudoRandom;
import jmetal.util.Utils;


public class MOKP_BinarySolution extends BinarySolutionType {
	private static Random r = new Random();
    private int numberOfItems;
    private int [][] p; // profit of items
    private int [][] w; // weight of items
    private double[] sackCapacity ; // capacity of each  knapsack .
    private double [] profPerWeight;
    private int [] selectIndex;
    
    public MOKP_BinarySolution(Problem problem, int numberOfItems, int [][] p, int [][] w, double[] sackCapacity) {
        super(problem);       
        this.numberOfItems = numberOfItems;
        this.p = p;
        this.w = w;
        this.sackCapacity = sackCapacity;

        //the below process only seems to play a role in the repair process
        profPerWeight = new double [problem_.getNumberOfConstraints()*numberOfItems] ;
        selectIndex = new int [problem_.getNumberOfConstraints()*numberOfItems] ;

		for (int i = 0; i < problem_.getNumberOfConstraints();i++) { // for each constraint

			int startingIndex = i * numberOfItems;

			int k = 0;
			for (int j = startingIndex; j < startingIndex+numberOfItems; j++) {
				selectIndex[j] = j;

				//for each item, if the profit per kilo (given the specific bucket)
				//is greater than that for any another bucket, then overwrite the top
				//value of that item per kilo
				double val = ((double) p[i][k]) / w[i][k];
				profPerWeight[j] = val;
				k++;
			} // for j
		}

        //selectIndex[5]=1 means that item 5 has the lowest value per kilo of all items
        //Utils.QuickSort(profPerWeight, selectIndex, 0, numberOfItems-1);
		Utils.bubbleSort(profPerWeight, selectIndex);

    }    
    
    @Override
    public Variable[] createVariables() {
        Variable[] vars = new Variable[problem_.getNumberOfVariables()];

        for (int i = 0; i < vars.length; i++) {
            Binary bin = new Binary(numberOfItems * problem_.getNumberOfConstraints());
            
            for (int j = 0; j < bin.getNumberOfBits(); j++) {
                bin.setIth(j, r.nextBoolean());                
            }
           
            vars[i] = bin;                        
        }
        
        return vars;        
    }    


     // Update solution according to probability
     // replaces mutation operator
    public void updateProduct(Solution sol,double [] prob) {    	 
		  Variable[] vars = sol.getDecisionVariables();
		  double updateRate = 0.01;
		 
			for (int i = 0; i < vars.length; i++) {
				Binary bin = ((Binary)vars[i]);

				for (int j = 0; j < bin.getNumberOfBits(); j++){ //

					 if (PseudoRandom.randDouble() < updateRate) {

						 if (PseudoRandom.randDouble() < prob[j]) {
							 bin.setIth(j, true);
						 } else {
							 bin.setIth(j, false);
						 }

					 }
				}

			}
  	} // updateProduct

	// Repair a solution
    public void repair(Solution solution) {
    	Variable[] vars = solution.getDecisionVariables();
        Binary bin = (Binary) vars[0];
        boolean violatedConstraint;
        
        do {
			violatedConstraint = false;

			int i;
        	for (i = 0; i < problem_.getNumberOfConstraints();i++) { // for each constraint

        		  int startingIndex = i * numberOfItems;
        		
        		  int sumWeight = 0;
        		  int k = 0;
	   	    	  for(int j = startingIndex; j < startingIndex+numberOfItems; j++) { // for each bit
	   	    		  if (bin.getIth(j) == true) {
	   	    			  sumWeight = sumWeight +  w[i][k];
	   	    		  }
	   	    		  k++;
	   	    	  }
   	    	  
	   	    	  if (sumWeight > sackCapacity[i])  {
					  violatedConstraint = true;
					  break;
	   	    	  }
	   	    	  
        	} // for i

			//Repair seems to take in account the sorting based on value-per-kilo that happened earlier...
			//I think this tries to fix the solution by flipping a bit from 1 to 0 (included in sack to not included),
			//but it makes sure it flips the bits with the least impact. Start from those with minimal value-to-weight,
			//and move on
        	if (violatedConstraint == true) {
        		// Repair
				int startingIndex = i * numberOfItems;

        		for (int j = startingIndex; j < startingIndex+numberOfItems; j++){
        			int pos = selectIndex[j];
        			
        			if (bin.getIth(pos) == true) {
        				bin.setIth(pos, false);
        				break;
        			}// if
        		} // for 
        		
        	} // if 
        	
        } while (violatedConstraint) ;
        
    }

}




