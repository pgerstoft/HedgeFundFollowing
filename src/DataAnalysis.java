import hr.irb.fastRandomForest.FastRandomForest;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.TreeSet;

import weka.classifiers.*;
import weka.classifiers.bayes.NaiveBayesUpdateable;
import weka.classifiers.functions.Logistic;
import weka.classifiers.functions.SimpleLogistic;
import weka.classifiers.trees.*;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;

import weka.filters.Filter;
import weka.filters.unsupervised.instance.NonSparseToSparse;
 

public class DataAnalysis {

	public DataAnalysis(){
	}
	
	public void compareClassifiers() throws Exception{

		Instances train  = new Instances(new BufferedReader(new FileReader("designMatrixTrain.arff"))); 
		train.setClassIndex(train.numAttributes() - 1);
	    
	    ArffLoader loaderTest = new ArffLoader();
	    Instances test = new Instances(new BufferedReader(new FileReader("designMatrixTest.arff")));//loaderTest.getStructure();
			test.setClassIndex(test.numAttributes() - 1);
		
	    
		
		SimpleLogistic classify = new SimpleLogistic();
//		Classifier classify = new RandomForest();

		
			classify.buildClassifier(train);
			 // evaluate classifier and print some statistics
			System.out.println("Evaluating");
			Evaluation eval = new Evaluation(train);
			 eval.evaluateModel(classify, test);
			 System.out.println(eval.toSummaryString("\nResults\n======\n", false));
		
		int correct = 0;
		int numInstances = 0;
		try {
		
		Instance currentTest  = null;
		System.out.println(loaderTest == null);
		for(Enumeration e = test.enumerateInstances(); e.hasMoreElements(); ){
			currentTest = (Instance) e.nextElement();
				numInstances ++;
					if(currentTest.classValue() == 0){
						correct ++;
					}
//					System.out.print("ID: " + currentTest.stringValue(0) + " ");
//					System.out.print(nb.distributionForInstance(currentTest)[0] + " ");
//					System.out.print("Classifi " +  test.classAttribute().value( (int) nb.classifyInstance(currentTest)) + " " );
//					System.out.println("actual, " +  test.classAttribute().value((int) currentTest.classValue()));
			} 
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("Base Line predicition" + correct*1.0/numInstances*1.0);
	}
	
	
	public void makeDesignMatrixCSV(Quarter predictionQuarter, int numTrainingQuarters){
		
		//TODO move file before you write, input file to makeDesigntemp
		String tempTrainFile = "temp/tempTrainDesignMatrix2.csv";
		String tempTestFile  = "temp/tempTestDesignMatrix2.csv";
		String designMatrixTrain = "designMatrixTrain2.arff";
		String designMatrixTest = "designMatrixTest2.arff";
		//set allParts of writeable to true
		new File(System.getProperty("user.dir")+"/temp/").setWritable(true);
		
		Quarter startQuarter = predictionQuarter;
		for(int i = numTrainingQuarters; i > 0; i--)
			startQuarter = startQuarter.getPreviousQuarter();
		
		System.out.println("Starting Quarter " +  startQuarter);
		
//		System.out.println("Storing Database values into csv file...");
//		new File(tempTrainFile).delete();
//		DB.makeDesignTemp(System.getProperty("user.dir")+"/" + tempTrainFile, startQuarter.getPreviousQuarter(), predictionQuarter);
//		new File(tempTestFile).delete();
//		DB.makeDesignTemp(System.getProperty("user.dir")+"/" + tempTestFile, predictionQuarter.getPreviousQuarter(), predictionQuarter.getNextQuarter());		
//		System.out.println("Pivoting table...");
//		
		boolean isTrainingDesignMatrix = true;
		System.out.println("Training Design Matrix...");
		writeDesignMatrixARFF(tempTrainFile, tempTestFile, designMatrixTrain, isTrainingDesignMatrix);
		System.out.println("Test Design Matrix...");
		writeDesignMatrixARFF(tempTrainFile, tempTestFile, designMatrixTest, !isTrainingDesignMatrix);
		
		
//		new File(tempTrainFile).delete();
//		new File(tempTestFile).delete();
	}
	
	private ArrayList<Double> zeroedOutArrayList(int size) {
		ArrayList<Double> x = new ArrayList<Double>(size);
		for(int i = 0; i < size; i++){
			x.add(0.0); 
		}
		return x;
	}

	
	//write desgin Matrix in sparse arff notation
	
	private void writeDesignMatrix(String tempTrainFile, String tempTestFile, String designMatrixFile, boolean isTrainingDesignMatrix){
//		String designMatrixFileArff1 = designMatrixFile.replace(".csv", ".arff");
//		csvToArff(designMatrixFile, designMatrixFileArff1);
//		arffToSparse(designMatrixFileArff1);
//		if(true)
//		return;
		
		int colNumTicker = 0; //ReadCSVFile.getColNum(line, "TICKER");
		int colNumQuarter = 1;
		int colNumCIK = 2; //ReadCSVFile.getColNum(line, "CIK");
		int colNumPOF = 3; //ReadCSVFile.getColNum(line, "PortionOfFund");
		int colNumReturn = 4; //ReadCSVFile.getColNum(line,"RET"); //TODO
		
		System.out.println("getting CIKS FROM FILE...");
		
		//we only want ciks that can be used in the final model
		TreeSet<CIK> ciks = getCIKSFromFile(tempTestFile, colNumCIK);
		
		System.out.println(ciks.size());
		
		Hashtable<Quarter, Double> spyReturns = new Hashtable<Quarter, Double>();
		
		Hashtable<CIK, Integer> cikToIndex = new Hashtable<CIK, Integer>();
		int index = 0;
		for(Object c: ciks.toArray())
			cikToIndex.put((CIK) c, index++);
		
		ArrayList<Double> portionOfFunds = zeroedOutArrayList(cikToIndex.size());

		String ticker = null, newTicker = null;
		Quarter quarter = null, newQuarter = null;
		double portionOfFund;
		CIK cik;
		boolean ret;
		String line;
		
		BufferedReader in = null;
		BufferedWriter out = null;
		
		try{
			out = new BufferedWriter(new FileWriter(designMatrixFile));
			
			if(isTrainingDesignMatrix)
				in = new BufferedReader(new FileReader(tempTrainFile));
			else
				in = new BufferedReader(new FileReader(tempTestFile));
						

			//header
			out.write("Ticker, ");
			out.write("Quarter, ");
			CIK tempCIK = null;
			
			for(Object c : ciks.toArray()){
				tempCIK = (CIK) c;
				out.write("POF"+tempCIK.toString()+", ");
			}
			
			out.write("Return \n");
			int count = 0;
			while((line = in.readLine()) != null){
				count++;
				if(count >  200)
					break;
				newTicker = ReadCSVFile.getValue(line, colNumTicker);
				
				newQuarter = new Quarter(ReadCSVFile.getValue(line, colNumQuarter));
				ticker = ticker == null ? newTicker : ticker;
				quarter = quarter == null ? newQuarter : quarter;

				
				if(!ticker.equals(newTicker) || !quarter.equals(newQuarter)){ //is new Quarter || is new Ticker
					if(!spyReturns.containsKey(quarter))
						spyReturns.put(quarter, DB.getThreeMonthStockReturn(DB.getCusipFromTicker("SPY", quarter), quarter));
					System.out.println(spyReturns.get(quarter));
					ret = new Double(ReadCSVFile.getValue(line, colNumReturn)) - spyReturns.get(quarter) > 0 ? true : false;
										
					out.write(ticker + ","  + quarter + "," +  portionOfFunds.toString().replaceAll("[\\[\\]]", "") + "," + ret+"\n");
					
					portionOfFunds = zeroedOutArrayList(cikToIndex.size());
					ticker = newTicker;
					quarter = newQuarter;
					cik = new CIK(ReadCSVFile.getValue(line, colNumCIK));
					portionOfFund = new Double(ReadCSVFile.getValue(line, colNumPOF));
					if(cikToIndex.get(cik) != null)
						portionOfFunds.set(cikToIndex.get(cik), portionOfFund);
				}else{
					cik = new CIK(ReadCSVFile.getValue(line, colNumCIK));
					portionOfFund = new Double(ReadCSVFile.getValue(line, colNumPOF));
					if(cikToIndex.get(cik) != null)
						portionOfFunds.set(cikToIndex.get(cik), portionOfFund);
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(in != null){
				try {
					in.close();
				} catch (IOException e) {
				}
			}
			if(out != null){
				try {
					out.close();
				} catch (IOException e) {
				}
			}
		}
		String designMatrixFileArff = designMatrixFile.replace(".arff", "2.arff");
		csvToArff(designMatrixFile, designMatrixFileArff);
		arffToSparse(designMatrixFileArff);
	}
	
	private void writeDesignMatrixARFF(String tempTrainFile, String tempTestFile, String designMatrixFile, boolean isTrainingDesignMatrix){

		
		int colNumTicker = 0; 
		int colNumQuarter = 1;
		int colNumCIK = 2; 
		int colNumPOF = 3; 
		int colNumReturn = 4; 
		
		System.out.println("getting CIKS FROM FILE...");
		
		//we only want ciks that can be used in the final model
//		TreeSet<CIK> ciks = getCIKSFromFile(tempTestFile, colNumCIK);
		
		TreeSet<CIK> ciks = getCIKSFromFile(tempTrainFile, colNumCIK);	
		
		System.out.println(ciks.size());
		
		TreeSet<String> tickers;
		if(isTrainingDesignMatrix)
			tickers = getTickersFromFile(tempTrainFile, colNumTicker);
		else
			tickers = getTickersFromFile(tempTestFile, colNumTicker);
				
		Hashtable<Quarter, Double> spyReturns = new Hashtable<Quarter, Double>();				
		
		
		Hashtable<Integer, String> portionOfFunds = new Hashtable<Integer, String>();

		String ticker = null, newTicker = null;
		Quarter quarter = null, newQuarter = null;
		double portionOfFund;
		CIK cik;
		boolean ret;
		String line;
		
		BufferedReader in = null;
		BufferedWriter out = null;
		
		try{
			out = new BufferedWriter(new FileWriter(designMatrixFile));
			
			if(isTrainingDesignMatrix)
				in = new BufferedReader(new FileReader(tempTrainFile));
			else
				in = new BufferedReader(new FileReader(tempTestFile));
						

//			if(isTrainingDesignMatrix)
//				out.write("@relation designMatrixTrain\n");
//			else
//				out.write("@relation designMatrixTest\n");
//			
//			out.write("@attribute Ticker ");
//			out.write("{"+ tickers.toString().replaceAll("[\\[\\]]", "")+ "}");
//			out.write("\n");

			CIK tempCIK = null;
			
			Hashtable<CIK, Integer> cikToIndex = new Hashtable<CIK, Integer>();
			int index = 0;

			for(Object c : ciks.toArray()){
				tempCIK = (CIK) c;
				if(index == 0)
					System.out.println(tempCIK);
				cikToIndex.put(tempCIK, index);
				
//				out.write("@attribute \'POF"+tempCIK.toString()+"\'  numeric\n");
				index++;
			}
			
			//The predicted variable
//			out.write("@attribute Class  {true,false}\n");
//			
//			out.write("@data\n");
			while((line = in.readLine()) != null){

				newTicker = ReadCSVFile.getValue(line, colNumTicker);
				
				newQuarter = new Quarter(ReadCSVFile.getValue(line, colNumQuarter));
				ticker = ticker == null ? newTicker : ticker;
				quarter = quarter == null ? newQuarter : quarter;

				
				if(!ticker.equals(newTicker) || !quarter.equals(newQuarter)){ //is new Quarter || is new Ticker
					if(!spyReturns.containsKey(quarter))
						spyReturns.put(quarter, DB.getThreeMonthStockReturn(DB.getCusipFromTicker("SPY", quarter), quarter));
					System.out.println(spyReturns.get(quarter));
					ret = new Double(ReadCSVFile.getValue(line, colNumReturn)) - spyReturns.get(quarter) > 0 ? true : false;
										
					out.write("");
//					out.write("0 " + ticker + ",");

					if(portionOfFunds.size()>0){
						ArrayList<Integer> keys = new ArrayList<Integer>();
						for(Enumeration<Integer> e= portionOfFunds.keys(); e.hasMoreElements();){
							keys.add(e.nextElement());
						}
						Collections.sort(keys);
						for(int key: keys){
//							out.write((key+1) + " " + portionOfFunds.get(key)  +",");
						}
					}
					
					
					
					out.write(" " + portionOfFunds.size()+ ",");
					out.write(""+(new Double(ReadCSVFile.getValue(line, colNumReturn)) - spyReturns.get(quarter)));
//					out.write((cikToIndex.size()+1)+ " " + ret);
					out.write(" \n");
					portionOfFunds = new Hashtable<Integer, String>();
					ticker = newTicker;
					quarter = newQuarter;
					cik = new CIK(ReadCSVFile.getValue(line, colNumCIK));
					portionOfFund = new Double(ReadCSVFile.getValue(line, colNumPOF));
					if(cikToIndex.get(cik) != null)
						portionOfFunds.put((cikToIndex.get(cik)), ""+ portionOfFund);
				}else{
					cik = new CIK(ReadCSVFile.getValue(line, colNumCIK));
					portionOfFund = new Double(ReadCSVFile.getValue(line, colNumPOF));
					if(cikToIndex.get(cik) != null)
						portionOfFunds.put((cikToIndex.get(cik)), ""+ portionOfFund);
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(in != null){
				try {
					in.close();
				} catch (IOException e) {
				}
			}
			if(out != null){
				try {
					out.close();
				} catch (IOException e) {
				}
			}
		}
		
		
	}
	
//TODO:	public void pivotTable(String file, int top, int[] sides, int y)
	
	public void arffToSparse(String arffFile){
		try{
			Instances source  = new Instances(new BufferedReader(new FileReader(arffFile))); 
		    NonSparseToSparse sp = new NonSparseToSparse(); 
		    sp.setInputFormat(source); 
		    Instances newData = Filter.useFilter(source, sp); 
		    ArffSaver saver = new ArffSaver(); 
		    saver.setInstances(newData); 
		    saver.setFile(new File(arffFile)); 
		    saver.writeBatch();    
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void csvToArff(String csvFile, String arffFile){
		
	    
	    CSVLoader loader = new CSVLoader();
	    try{
	    loader.setSource(new File(csvFile));
	    Instances data = loader.getDataSet();

	    // save ARFF
	    ArffSaver saver = new ArffSaver();
	    saver.setInstances(data);
	    saver.setFile(new File(arffFile));
	    saver.writeBatch();
	    }catch(Exception e){
	    	e.printStackTrace();
	    }
	    
	}
	
	
	//why TreeSet? fast lookup
	private static TreeSet<CIK> getCIKSFromFile(String fileName, int colNum){
		TreeSet<CIK> ciks = new TreeSet<CIK>();
		BufferedReader in = null;
		try{
		    in = new BufferedReader(new FileReader(fileName));
		    String line = "" ;
		    while((line = in.readLine()) != null){
		        String[] fields = line.split(",");
		        ciks.add(new CIK(fields[colNum]));
		    }
		}
		catch(Exception e){
		    e.printStackTrace();
		}
		finally{
		    if (in != null) {
		        try {
		            in.close();
		        } catch (IOException e) {
		        }
		    }
		}
		
		return ciks;
	}
	
	
	private static TreeSet<String> getTickersFromFile(String fileName, int colNum){
		TreeSet<String> tickers = new TreeSet<String>();
		BufferedReader in = null;
		try{
		    in = new BufferedReader(new FileReader(fileName));
		    String line = "" ;
		    while((line = in.readLine()) != null){
		        String[] fields = line.split(",");
		        tickers.add(fields[colNum]);
		    }
		}
		catch(Exception e){
		    e.printStackTrace();
		}
		finally{
		    if (in != null) {
		        try {
		            in.close();
		        } catch (IOException e) {
		        }
		    }
		}
		
		return tickers;
	}
}
