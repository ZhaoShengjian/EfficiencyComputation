/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lp;

/**
 *
 * @author kostas
 */
import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class XMLhandler {

    public void create_xml(String absolute, String model, double lower_weight, Object[] obj, double[][] results) {

        /*
         obj[0] = data; --
         obj[1] = rows;
         obj[2] = columns;
         obj[3] = variables; --
         obj[4] = dmuNames; --
         */
        double[][] data = (double[][]) obj[0];

        // Rows number in file
        int rows_number = (Integer) obj[1];

        // Columns in file (1-based)
        int col_number = (Integer) obj[2] - 1;
        int res = results.length;
        int results_number = results[0].length;
        int total_col = col_number + results_number;

        String[] dmuNames = (String[]) obj[4];

        String[] var = (String[]) obj[3];
        String[] variables = new String[0];

        /*
         Get the selected model/s and create the matrix "variables" 
         for each model respectively
         */
        if (model.equals("multiplicative") || model.equals("composition")) {

            variables = new String[var.length + 3];
            System.arraycopy(var, 0, variables, 0, var.length);

            variables[col_number + 1] = "e1";
            variables[col_number + 2] = "e2";
            variables[col_number + 3] = "Overall Efficiency";
        }
        if (model.equals("additive")) {

            variables = new String[var.length + 5];
            System.arraycopy(var, 0, variables, 0, var.length);

            variables[col_number + 1] = "weight 1";
            variables[col_number + 2] = "weight 2";
            variables[col_number + 3] = "Overall Efficiency";
            variables[col_number + 4] = "Theta 1";
            variables[col_number + 5] = "Theta 2";

        }

        try {
            DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
            Document document = documentBuilder.newDocument();

            /*
             Create the .xml file. Root element.
             */
            Element root = document.createElement(model + "Results");
            document.appendChild(root);

            /*
             -DMUs: the number of DMUs in the problem
             -columns: total number of columns we need. This is an 1-based number.
             This information will be used by the <script>
             */
            Element info = document.createElement("DMUs");
            int help = rows_number + 1;
            info.appendChild(document.createTextNode("" + help));
            root.appendChild(info);

            info = document.createElement("columns");
            int aux = total_col + 1;
            info.appendChild(document.createTextNode("" + aux));
            root.appendChild(info);

            info = document.createElement("lower_weight");
            info.appendChild(document.createTextNode("" + lower_weight));
            root.appendChild(info);

            Element row;
            Element cell;

            int data_index = 0;

            // Total Rows in the file 0 - 23
            for (int i = 0; i < rows_number + 1; i++) {
                row = document.createElement("row");
                root.appendChild(row);

                //the first ROW: the names and variable names for each column
                if (i == 0) {
                    for (int l = 0; l < variables.length; l++) {
                        cell = document.createElement("cell");
                        cell.appendChild(document.createTextNode(variables[l]));
                        row.appendChild(cell);
                    }

                } else {

                    for (int k = 0; k < total_col; k++) {
                        cell = document.createElement("cell");
                        if (k == 0) {
                            cell.appendChild(document.createTextNode(dmuNames[data_index]));
                            row.appendChild(cell);

                        }
                        if (k > 0 && k < col_number + 1) {
                            cell.appendChild(document.createTextNode("" + data[data_index][k - 1]));
                            row.appendChild(cell);
                        }
                    }

                    for (int t = 0; t < results_number; t++) {
                        cell = document.createElement("cell");
                        cell.appendChild(document.createTextNode("" + results[data_index][t]));
                        row.appendChild(cell);
                    }

                    data_index++;
                }
            }
            // create the xml file
            //transform the DOM Object to an XML File
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource domSource = new DOMSource(document);
            StreamResult streamResult = new StreamResult(new File(absolute + "\\" + model + ".xml"));
            transformer.transform(domSource, streamResult);

            System.out.println("Done creating XML File");

        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (TransformerException tfe) {
            tfe.printStackTrace();
        }

    }

    public void models_selected(String[] models, String absolute) {
        /*
         Create the information.xml file
         */
        try {
            DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
            Document document = documentBuilder.newDocument();

            // root element
            Element root = document.createElement("ModelsSelected");
            document.appendChild(root);

            Element model_info;

            int numberOfModels = 0;
            for (String model : models) {
                model_info = document.createElement("choise");
                model_info.appendChild(document.createTextNode(model));
                root.appendChild(model_info);
                numberOfModels++;
            }

            model_info = document.createElement("number_of_choises");
            model_info.appendChild(document.createTextNode("" + models.length));
            root.appendChild(model_info);

            // create the xml file
            //transform the DOM Object to an XML File
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource domSource = new DOMSource(document);
            StreamResult streamResult = new StreamResult(new File(absolute + "\\information.xml"));
            transformer.transform(domSource, streamResult);

        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (TransformerException tfe) {
            tfe.printStackTrace();
        }
    }

}
