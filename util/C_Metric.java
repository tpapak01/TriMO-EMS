//  ExtractParetoFront.java
//
//  Author:
//       Juan J. Durillo <durillo@lcc.uma.es>
//
//  Copyright (c) 2012 Juan J. Durillo
//
//  This program is free software: you can redistribute it and/or modify
//  it under the terms of the GNU Lesser General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU Lesser General Public License for more details.
// 
//  You should have received a copy of the GNU Lesser General Public License
//  along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jmetal.util;

import java.io.*;
import java.util.*;

/**
 *  This class extract the Pareto front among a set of dominated and 
 *  non-dominated solutions
 */

public class C_Metric {

	String fileNameA_;
	String fileNameB_;
	int dimensions_;
	List<Point> points_ = new LinkedList<Point>(); // the final non-dominated list to be filled
	boolean minimization_problem = true;

	public int num_of_dominated_B;
	public int nds_B;

	private class Point {
		double [] vector_;

		public Point (double [] vector) {
			vector_ = vector;
		}

		public Point (int size) {
			vector_ = new double[size];
			for (int i = 0; i < size; i++)
				vector_[i] = 0.0f;
		}

	}

	/**
	 * @author Juan J. Durillo
	 * Creates a new instance
	 */
	public C_Metric(String nameA, String nameB, int dimensions) {
		fileNameA_ = nameA;
		fileNameB_ = nameB;
		dimensions_ = dimensions;
		loadInstance();
	} // ReadInstance


	/**
	 * Read the points instance from file 
	 */
	public void loadInstance()  {

		//Begin by creating list A
		try {
			File archivo = new File(fileNameA_);
			FileReader fr = null;
			BufferedReader br = null;        
			fr = new FileReader (archivo);
			br = new BufferedReader(fr);

			// File reading
			String line;
			int lineCnt = 0;
			line = br.readLine(); // reading the first line (special case)     

			while (line!=null) {
				StringTokenizer st = new StringTokenizer(line);
				try {
					Point auxPoint = new Point(dimensions_);
					// fill up solution with dimensions
					for (int i = 0; i < dimensions_; i++) {
						auxPoint.vector_[i] = new Double(st.nextToken());
					}
					// decide whether solution is non-dominated == will be added
					points_.add(auxPoint);

					line = br.readLine();
					lineCnt++;
				} catch (NumberFormatException e) {
					System.err.println("Number in a wrong format in line "+lineCnt);
					System.err.println(line);
					line = br.readLine();
					lineCnt++;
				} catch (NoSuchElementException e2) {
					System.err.println("Line "+lineCnt+" does not have the right number of objectives");
					System.err.println(line);
					line = br.readLine();
					lineCnt++;
				}
			}
			br.close();
		} catch (FileNotFoundException e3) {
			System.err.println("The file " + fileNameA_+ " has not been found in your file system");
		}  catch (IOException e3) {
			System.err.println("The file " + fileNameA_+ " has not been found in your file system");
		}

		// continue with testing B through the already created list A
		try {
			File archivo = new File(fileNameB_);
			FileReader fr = null;
			BufferedReader br = null;
			fr = new FileReader (archivo);
			br = new BufferedReader(fr);

			// File reading
			String line;
			int lineCnt = 0;
			line = br.readLine(); // reading the first line (special case)

			while (line!=null) {
				StringTokenizer st = new StringTokenizer(line);
				try {
					Point auxPoint = new Point(dimensions_);
					// fill up solution with dimensions
					for (int i = 0; i < dimensions_; i++) {
						auxPoint.vector_[i] = new Double(st.nextToken());
					}
					// decide whether solution is non-dominated == will be added
					add(auxPoint);

					line = br.readLine();
					lineCnt++;
				} catch (NumberFormatException e) {
					System.err.println("Number in a wrong format in line "+lineCnt);
					System.err.println(line);
					line = br.readLine();
					lineCnt++;
				} catch (NoSuchElementException e2) {
					System.err.println("Line "+lineCnt+" does not have the right number of objectives");
					System.err.println(line);
					line = br.readLine();
					lineCnt++;
				}
			}
			br.close();
		} catch (FileNotFoundException e3) {
			System.err.println("The file " + fileNameA_+ " has not been found in your file system");
		}  catch (IOException e3) {
			System.err.println("The file " + fileNameA_+ " has not been found in your file system");
		}

	} // loadInstance


	public void add(Point point) {
		nds_B++;

		Iterator<Point> iterator = points_.iterator();

		// for all solutions already put in the list we are creating
		while (iterator.hasNext()){
			Point auxPoint = iterator.next();
			int flag;
			if (minimization_problem){
				flag = compareMin(point,auxPoint);
			} else {
				flag = compareMax(point, auxPoint);
			}

			if (flag == 1) { // The solution is dominated
				num_of_dominated_B++;
				return;
			}        
		} // while

		//System.out.println("Solution " + Arrays.toString(point.vector_) + " is undefeated by A!!");

	} // add                   


	public int compareMin(Point one, Point two) {
		int flag1 = 0, flag2 = 0;
		for (int i = 0; i < dimensions_; i++) {
			if (one.vector_[i] < two.vector_[i])
				flag1 = 1;

			if (one.vector_[i] > two.vector_[i])
				flag2 = 1;
		}

		if (flag1 > flag2) // one dominates
		return -1;

		if (flag2 > flag1) // two dominates
			return 1;

		return 0; // both are non dominated
	}

	public int compareMax(Point one, Point two) {
		int flag1 = 0, flag2 = 0;
		for (int i = 0; i < dimensions_; i++) {
			if (one.vector_[i] < two.vector_[i])
				flag2 = 1;

			if (one.vector_[i] > two.vector_[i])
				flag1 = 1;
		}

		if (flag1 > flag2) // one dominates
			return -1;

		if (flag2 > flag1) // two dominates
			return 1;

		return 0; // both are non dominated
	}

	/*
	public void writeParetoFront() {
		try {    
			// Open the file
			FileOutputStream fos   = new FileOutputStream(fileName_+".pf") ;
			OutputStreamWriter osw = new OutputStreamWriter(fos)    ;
			BufferedWriter bw      = new BufferedWriter(osw)        ;

      for (Point auxPoint : points_) {
        String aux = "";

        for (int i = 0; i < auxPoint.vector_.length; i++) {
          aux += auxPoint.vector_[i] + " ";

        }
        bw.write(aux);
        bw.newLine();
      }

			// Close the file
			bw.close();
		}catch (IOException e) {        
			e.printStackTrace();
		}       
	}

	 */

	public static void main(String [] args) throws IOException {
		if (args.length != 3) {
			System.out.println("Wrong number of arguments: ");
			System.out.println("Sintaxt: java C_Metric <fileA> <fileB> <dimensions>");
			System.out.println("\t<file> is a file containing points");
			System.out.println("\t<dimensions> represents the number of dimensions of the problem");
			System.exit(-1) ;
		}
		int dimensions = new Integer(args[2]);

		boolean mode_args = false;
		if (mode_args) {

			C_Metric epf = new C_Metric(args[0], args[1], dimensions);

			double cMetric = (float) epf.num_of_dominated_B / (float) epf.nds_B;
			System.out.println(cMetric);

		} else {
			FileWriter cMetricWriterNSGAII = new FileWriter("LowerLevelParetoVisualNSGAII/cmetric.txt");
			FileWriter cMetricWriterMOEAD = new FileWriter("LowerLevelParetoVisual/cmetric.txt");

			String prefix1 = "LowerLevelParetoVisual/WithoutLocalSearch/";
			String prefix2 = "LowerLevelParetoVisualNSGAII/WithoutLocalSearch/";

			// first calculate C metric of NSGAII (lower == better)
			double cMetricMean = 0;
			for (int i=1; i<=100; i++){
				String file1 = prefix1 + i + "_FUN";
				String file2 = prefix2 + i + "_FUN";

				C_Metric epf = new C_Metric(file1, file2, dimensions);
				double cMetric = (float) epf.num_of_dominated_B / (float) epf.nds_B;
				//System.out.println(cMetric);
				cMetricMean += cMetric;
				cMetricWriterNSGAII.write(cMetric + "\n");
			}
			cMetricMean = cMetricMean / 100;
			cMetricWriterNSGAII.write(cMetricMean + "\n");
			cMetricWriterNSGAII.close();

			// now for MOEAD
			cMetricMean = 0;
			for (int i=1; i<=100; i++){
				String file1 = prefix1 + i + "_FUN";
				String file2 = prefix2 + i + "_FUN";

				C_Metric epf = new C_Metric(file2, file1, dimensions);
				double cMetric = (float) epf.num_of_dominated_B / (float) epf.nds_B;
				//System.out.println(cMetric);
				cMetricMean += cMetric;
				cMetricWriterMOEAD.write(cMetric + "\n");
			}
			cMetricMean = cMetricMean / 100;
			cMetricWriterMOEAD.write(cMetricMean + "\n");
			cMetricWriterMOEAD.close();

		}


	}
}
