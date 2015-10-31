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
public class Composition {

    public Composition() {

    }

    public double[][] composition(Object obj[], String[] selected, double lower_weight, String path) {

        /*
         obj[0] = data[][]; --
         obj[1] = rows;
         obj[2] = columns;
         obj[3] = variables[]; --
         obj[4] = dmuNames[]; --
         */
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

        double[] average = new double[rows];

        double[][] results_array = new double[rows][3];

        try {
            // CREATE THE LINEAR PROBLEM
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
                    if (k <= varX) {
                        constrA[k] = -line[k - 1];
                    }
                    if (k > varX && k <= varX + varZ) {
                        constrA[k] = line[k - 1];
                        constrB[k] = -line[k - 1];
                    }
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

                //CREATE
                for (int k = 0; k < cols; k++) {
                    line[k] = data[i][k];
                }

                for (int k = 1; k < cols + 1; k++) {

                    //fill the objective with X
                    if (k <= varX) {
                        objective[k] = -line[k - 1];
                    }
                    // Zs equal zero
                    if (k > varX && k <= varX + varZ) {
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

                //solver.writeLp("model" + i + ".lp");
                // solve the problem
                solver.solve();

                double[] var = solver.getPtrVariables();

                double help1 = 0;
                double help2 = 0;

                for (int t = 0; t < varX; t++) {
                    help1 = help1 + var[t] * line[t];
                }

                eff1[i] = 1 / help1;

                for (int u = varX + varZ; u < cols; u++) {
                    help2 = help2 + var[u] * line[u];
                }

                eff2[i] = help2;

                average[i] = (eff1[i] + eff2[i]) / 2;

                //Delete the redundant constraint
                solver.delConstraint(cols + 2 * rows + 1);

                //results.composition_results(eff1[i], eff2[i], i);
                //res.create_results_file(null, data);
            }

            // delete the problem and free memory
            solver.deleteLp();

            //
            for (int v = 0; v < rows; v++) {

                results_array[v][0] = eff1[v];
                results_array[v][1] = eff2[v];
                results_array[v][2] = average[v];

            }

            //Object for writing results in .xlsx file
//            OpenXlsx results = new OpenXlsx();
//            results.create_xlsx_file(path, "composition", results_array);

        } catch (LpSolveException e) {
            e.printStackTrace();
            e.getMessage();
        }

        return results_array;

    }

}
