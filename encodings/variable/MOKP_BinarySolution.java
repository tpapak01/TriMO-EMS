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


public class MOKP_BinarySolution extends BinarySolutionType {
	private static Random r = new Random();
    private int numberOfItems;
    private int [][] p; // profit of items
    private int [][] w; // weight of items
    private double[] capacity ; // capacity of each  knapsack .
    private double [] profPerWeight;
    private int [] selectIndex;
    
    public MOKP_BinarySolution(Problem problem, int numberOfItems, int [][] p, int [][] w, double[] capacity) {
        super(problem);       
        this.numberOfItems = numberOfItems;
        this.p = p;
        this.w = w;
        this.capacity = capacity;     
        profPerWeight = new double [numberOfItems] ;
        selectIndex = new int [numberOfItems] ;
                
        for (int j = 0; j < numberOfItems;j++) {
        	profPerWeight[j] = - 1e30;
        	selectIndex[j]   = j;

        	//for each item, if the profit per kilo (given the specific bucket)
            //is greater than that for any another bucket, then overwrite the top
            //value of that item per kilo
        	for (int i = 0; i < problem_.getNumberOfConstraints();i++) {
        		double val = ((double)p[i][j])/w[i][j];
        		
        		if (val > profPerWeight[j]) {
        			profPerWeight[j] = val;
        		}
        	} // for i
   
        } // for j

        //selectIndex[5]=1 means that item 5 has the lowest value per kilo of all items
        QuickSort(profPerWeight, selectIndex, 0, numberOfItems-1);
        
//        for(int i = 0; i < numberOfItems ; i++) {
//        	System.out.println("profPerWeight[i]= " + profPerWeight[i] + ",index[i]=" + selectIndex[i]);
//        }
    }    
    
    @Override
    public Variable[] createVariables() {
        Variable[] vars = new Variable[problem_.getNumberOfVariables()];

        for (int i = 0; i < vars.length; i++) {
            Binary bin = new Binary(numberOfItems);
            
            for (int j = 0; j < bin.getNumberOfBits(); j++) {
                bin.setIth(j, r.nextBoolean());                
            }
           
            vars[i] = bin;                        
        }
        
        return vars;        
    }    


     // Update solution according to probability

    public void updateProduct(Solution sol,double [] prob) {    	 
  	  Variable[] vars = sol.getDecisionVariables();
  	  double updateRate = 0.01;
//	  System.out.println(updateRate);
		 
	  	for (int i = 0; i < vars.length; i++) {			
	  		Binary bin = ((Binary)vars[i]);
	  		
			for (int j = 0; j < numberOfItems; j++){ //
				
	        	 if (PseudoRandom.randDouble() < updateRate) {	

	        		 if (PseudoRandom.randDouble() < prob[j]) {
	        			 bin.setIth(j, true);
	        		 } else {
//	        			 bin.bits_.flip(j);
	        			 bin.setIth(j, false);
//	        			 System.out.println( prob[j]);
	        		 }

	        	 }
			}
	        	 
		}	
  } // updateProduct
    

     // Repair a solution

    public void repair(Solution solution) {
    	Variable[] vars = solution.getDecisionVariables();
        Binary bin = (Binary) vars[0];
        boolean voliatedConstrant = false;
        
        do {
        	voliatedConstrant = false;
        	
        	for (int i = 0; i < problem_.getNumberOfConstraints();i++) { // for each constraint
        		
        		  int sumWeight = 0;   	    	  
	   	    	  for(int j = 0; j < numberOfItems; j++) { // for each bit
	   	    		  if (bin.getIth(j) == true) {
	   	    			  sumWeight = sumWeight +  w[i][j];
	   	    		  }
	   	    	  }
   	    	  
	   	    	  if (sumWeight > capacity[i])  {	   	  
		   	    		voliatedConstrant = true;
		   	    		break;
	   	    	  }
	   	    	  
        	} // for i
        	
        	if (voliatedConstrant == true) {
        		// Repair
        		for (int j = 0; j < numberOfItems; j++){
        			int pos = selectIndex[j];
        			
        			if (bin.getIth(pos) == true) {
        				bin.setIth(pos, false);
        				break;
        			}// if
        		} // for 
        		
        	} // if 
        	
        } while (voliatedConstrant) ;
        
    }

    /**
     * Quick sort procedure (ascending order)
     * What it actually sorts is the idx array, where
     * idx[i] shows the hierarchical position of item i,
     * e.g. idx[5]=1 means that item 5 has the lowest value per kilo of all items
     *
     * @param array
     * @param idx
     * @param from
     * @param to
     */
    public static void QuickSort(double[] array, int[] idx, int from, int to) {
        if (from >= to) return;

        if (from < to) {
            double temp = array[to];
            int tempIdx = idx[to];
            int i = from - 1;
            for (int j = from; j < to; j++) {
                if (array[j] <= temp) {
                    i++;
                    double tempValue = array[j];
                    array[j] = array[i];
                    array[i] = tempValue;
                    int tempIndex = idx[j];
                    idx[j] = idx[i];
                    idx[i] = tempIndex;
                }
            }
            array[to] = array[i + 1];
            array[i + 1] = temp;
            idx[to] = idx[i + 1];
            idx[i + 1] = tempIdx;


            if (i-1 > from)
                QuickSort(array, idx, from, i);

            if (to > i + 2)
                QuickSort(array, idx, i + 1, to);
        }
    }
}




