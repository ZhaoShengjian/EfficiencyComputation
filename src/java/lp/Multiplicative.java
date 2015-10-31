/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lp;

import lpsolve.LpSolve;
import lpsolve.LpSolveException;

/**
 *
 * @author kostas kord
 */
public class Multiplicative {

    public Multiplicative() {

    }

    public double[][] multi(Object obj[], String[] selected, double lower_weight, String path) {

        //Retrieve data
        double[][] data = (double[][]) obj[0];
        int rows = (Integer) obj[1];
        int cols = (Integer) obj[2] - 1;
        System.out.println("Rows: " + rows + " -Columns: " + cols);

        //Variables given from user
        int varX = Integer.parseInt(selected[0]);
        int varZ = Integer.parseInt(selected[1]);

        //line Array, for each line of data
        double[] line = new double[cols];
        double[] objective = new double[cols + 1];
        double[] equal = new double[cols + 1];

        double[] constrA = new double[cols + 1];
        double[] constrB = new double[cols + 1];

        double[] positive = new double[cols + 1];

        double[] eff1 = new double[rows];
        double[] eff2 = new double[rows];

        double[] overeff = new double[rows];

        double[][] results_array = new double[rows][3];

        try {
            // CREATE THE PROBLEM
            // Create a problem with "cols" variables and 0 constraints
            LpSolve solver = LpSolve.makeLp(0, cols);
            solver.setMaxim();

            //Add Constraints
            //Initialize Unitary matrix OK!
            for (int i = 1; i < cols + 1; i++) {
                positive[i] = 1;
                solver.addConstraint(positive, LpSolve.GE, lower_weight);
                positive[i] = 0;
            }

            //Initialize Standard Constraints OK!
            for (int m = 0; m < rows; m++) {
                for (int n = 0; n < cols; n++) {
                    line[n] = data[m][n];
                }
                for (int k = 1; k < cols + 1; k++) {

                    // add -Xs
                    if (k <= varX) {
                        constrA[k] = -line[k - 1];
                    }
                    // +Zs to ConstrA and -Zs to ConstrB
                    if (k > varX && k <= varX + varZ) {
                        constrA[k] = line[k - 1];
                        constrB[k] = -line[k - 1];
                    }
                    // + Ys
                    if (k > varX + varZ) {
                        constrB[k] = line[k - 1];
                    }
                }
                solver.addConstraint(constrA, LpSolve.LE, 0);
                solver.addConstraint(constrB, LpSolve.LE, 0);
            }

            //...for each DMU:
            // Set Objective 
            for (int i = 0; i < rows; i++) {
                //System.arraycopy(data[i], 0, line, 0, cols);

                //CREATE
                for (int k = 0; k < cols; k++) {
                    line[k] = data[i][k];
                }

                for (int k = 1; k < cols + 1; k++) {

                    //Xs equal zero
                    if (k < varX + 1) {
                        equal[k] = line[k - 1];
                    }
                    //fill the objective with Y
                    if (k > varX + varZ) {
                        objective[k] = line[k - 1];
                    }
                }
                // Objective function(s)
                solver.setObjFn(objective);
                solver.addConstraint(equal, LpSolve.EQ, 1);

                // solve the problem
                solver.solve();

                double[] var = solver.getPtrVariables();

                double sum_x = 0;
                double sum_z = 0;
                double sum_y = 0;

                for (int t = 0; t < cols; t++) {
                    if (t < varX) {
                        sum_x = sum_x + var[t] * line[t];
                    }
                    if (t >= varX && t < varX + varZ) {
                        sum_z = sum_z + var[t] * line[t];
                    }
                    if (t >= varX + varZ) {
                        sum_y = sum_y + var[t] * line[t];
                    }
                }

                eff1[i] = sum_z / sum_x;
                eff2[i] = sum_y / sum_z;

                overeff[i] = sum_y;

                //solver.writeLp("model" + i + ".lp");
                //Delete the redundant constraint
                solver.delConstraint(cols + 2 * rows + 1);

                //results.multipicative_results(eff1[i], eff2[i], overeff[i], i);
            }

            // delete the problem and free memory
            solver.deleteLp();

            System.out.println(" ");

            for (int v = 0; v < rows; v++) {
                results_array[v][0] = eff1[v];
                results_array[v][1] = eff2[v];
                results_array[v][2] = overeff[v];

            }

            //Object for writing results in .xml file
//            OpenXlsx results = new OpenXlsx();
//            results.create_xlsx_file(path, "multiplicative", results_array);
            
        } catch (LpSolveException e) {
            //e.printStackTrace();
        }

        return results_array;

    }

}
