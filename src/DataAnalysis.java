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
import java.util.Iterator;
import java.util.TreeSet;

import weka.attributeSelection.ASSearch;
import weka.attributeSelection.BestFirst;
import weka.attributeSelection.CfsSubsetEval;
import weka.attributeSelection.GainRatioAttributeEval;
import weka.classifiers.*;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.bayes.NaiveBayesUpdateable;
import weka.classifiers.functions.Logistic;
import weka.classifiers.functions.SimpleLogistic;
import weka.classifiers.meta.AttributeSelectedClassifier;
import weka.classifiers.trees.*;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;

import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.filters.unsupervised.instance.NonSparseToSparse;
 

public class DataAnalysis {

	public DataAnalysis(){
	}
	
	public void compareClassifiers() throws Exception{

		Instances train  = new Instances(new BufferedReader(new FileReader("designMatrixTrain.arff"))); 
		train.setClassIndex(train.numAttributes() - 1);
	    
	    Instances test = new Instances(new BufferedReader(new FileReader("designMatrixTest.arff")));
		test.setClassIndex(test.numAttributes() - 1);

//		AttributeSelectedClassifier classifier = new AttributeSelectedClassifier();
//		CfsSubsetEval eval = new CfsSubsetEval();		
//		BestFirst search =  new BestFirst();
//		
//		SimpleLogistic base = new SimpleLogistic();
//		
//		classifier.setClassifier(base);
//		classifier.setEvaluator(eval);
//		classifier.setSearch(search);
		
	    
	    
//		SimpleLogistic classifier = new SimpleLogistic();
//		Classifier classifier = new RandomForest();
		Classifier classifier = new NaiveBayes();
		
		System.out.println("Build Train");

		classifier.buildClassifier(train);
			 // evaluate classifier and print some statistics
			System.out.println("Evaluating");
			Evaluation evaluation = new Evaluation(test);
			evaluation.evaluateModel(classifier, test);
//			 eval.evaluateModel(classify, test);
			 System.out.println(evaluation.toSummaryString("\nResults\n======\n", false));
		
		int correct = 0;
		int numInstances = 0;
		try {
		
		Instance currentTest  = null;
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
		String tempTrainFile = "temp/tempTrainDesignMatrix.csv";
		String tempTestFile  = "temp/tempTestDesignMatrix.csv";
		String designMatrixTrain = "designMatrixTrain"+ predictionQuarter.toString().replace('/', '_') + "_" + numTrainingQuarters + ".arff";
		String designMatrixTest = "designMatrixTest"+ predictionQuarter.toString().replace('/', '_') + "_" + numTrainingQuarters + ".arff";
		//set allParts of writeable to true
		new File(System.getProperty("user.dir")+"/temp/").setWritable(true);
		
		Quarter startQuarter = predictionQuarter;
		for(int i = numTrainingQuarters; i > 0; i--)
			startQuarter = startQuarter.getPreviousQuarter();
		
		System.out.println("Starting Quarter " +  startQuarter);
		
		boolean isTrainingDesignMatrix = true;
		
		System.out.println("Storing Database values into csv file...");
		if(! new File(designMatrixTrain).exists()){
			new File(tempTrainFile).delete();
			DB.makeDesignTemp(System.getProperty("user.dir")+"/" + tempTrainFile, startQuarter.getPreviousQuarter(), predictionQuarter);
			System.out.println("Training Design Matrix...");
			writeDesignMatrixARFF(tempTrainFile, tempTestFile, designMatrixTrain, isTrainingDesignMatrix, startQuarter, numTrainingQuarters );
		}
		if(! new File(designMatrixTest).exists()){
			new File(tempTestFile).delete();
			DB.makeDesignTemp(System.getProperty("user.dir")+"/" + tempTestFile, predictionQuarter.getPreviousQuarter(), predictionQuarter.getNextQuarter());	

			System.out.println("Test Design Matrix...");
			writeDesignMatrixARFF(tempTrainFile, tempTestFile, designMatrixTest, !isTrainingDesignMatrix, startQuarter, numTrainingQuarters);
		}
		
	}
	
	private ArrayList<Double> zeroedOutArrayList(int size) {
		ArrayList<Double> x = new ArrayList<Double>(size);
		for(int i = 0; i < size; i++){
			x.add(0.0); 
		}
		return x;
	}

	
	//write design Matrix in sparse arff notation
	private void writeDesignMatrixARFF(String tempTrainFile, String tempTestFile, String designMatrixFile, boolean isTrainingDesignMatrix, Quarter startQuarter, int numQuarters){

		DB.turnMultipleQueriesOn();

		
		int colNumTicker = 0; 
		int colNumQuarter = 1;
		int colNumCIK = 2; 
		int colNumPOF = 3; 
		int colNumReturn = 4; 
		
		System.out.println("getting CIKS FROM FILE...");
		
		//we only want ciks that can be used in the final model
		TreeSet<CIK> ciks = getCIKSFromFile(tempTestFile, colNumCIK);
//		TreeSet<CIK> ciks = getCIKSFromFile(tempTrainFile, colNumCIK);	

		//Get returns
		Hashtable<CIK, Double> cikToReturns = computeReturns(ciks, startQuarter, numQuarters);

		//Get spy return for each quarter and over the whole period
		Hashtable<Quarter, Double> spyReturns = new Hashtable<Quarter, Double>();				
		Cusip spyCusip = DB.getCusipFromTicker("SPY", startQuarter);
		Quarter q = startQuarter;
		double totalSpyReturn = 1;
		for(int i = 0; i < numQuarters; i++){
			spyReturns.put(q, DB.getThreeMonthStockReturn(spyCusip, q));
			totalSpyReturn *= (1 + spyReturns.get(q));
			q = q.getNextQuarter();
		}
		spyReturns.put(q, DB.getThreeMonthStockReturn(spyCusip, q));
		totalSpyReturn--;
		
		System.out.println(ciks.size());
		System.out.println(totalSpyReturn);
		System.out.println(cikToReturns);
		Iterator<CIK> iter = ciks.iterator();
		while(iter.hasNext()){
			if(cikToReturns.get(iter.next()) <= totalSpyReturn)
				iter.remove();
		}
		
		System.out.println(ciks.size());

		TreeSet<String> tickers;
		if(isTrainingDesignMatrix)
			tickers = getTickersFromFile(tempTrainFile, colNumTicker);
		else
			tickers = getTickersFromFile(tempTestFile, colNumTicker);
				
		
		
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
						

			if(isTrainingDesignMatrix)
				out.write("@relation designMatrixTrain\n");
			else
				out.write("@relation designMatrixTest\n");
			
			out.write("@attribute Ticker ");
			out.write("{"+ tickers.toString().replaceAll("[\\[\\]]", "")+ "}");
			out.write("\n");
			
			Hashtable<CIK, Integer> cikToIndex = new Hashtable<CIK, Integer>();
			int index = 0;

			for(CIK c : ciks){

				cikToIndex.put(c, index);
				
				out.write("@attribute \'POF"+c.toString()+"\'  numeric\n");
				index++;
			}
			
			//The predicted variable
			out.write("@attribute Class  {true,false}\n");
//			
			out.write("@data\n");
			while((line = in.readLine()) != null){

				newTicker = ReadCSVFile.getValue(line, colNumTicker);
				
				newQuarter = new Quarter(ReadCSVFile.getValue(line, colNumQuarter));
				ticker = ticker == null ? newTicker : ticker;
				quarter = quarter == null ? newQuarter : quarter;

				
				if(!ticker.equals(newTicker) || !quarter.equals(newQuarter)){ //is new Quarter || is new Ticker
					System.out.println(spyReturns.get(quarter));
					ret = new Double(ReadCSVFile.getValue(line, colNumReturn)) - spyReturns.get(quarter) > 0 ? true : false;
					
					if(portionOfFunds.size()>0){					
					out.write("{");
					out.write("0 " + ticker + ",");

					if(portionOfFunds.size()>0){
						ArrayList<Integer> keys = new ArrayList<Integer>();
						for(Enumeration<Integer> e= portionOfFunds.keys(); e.hasMoreElements();){
							keys.add(e.nextElement());
						}
						Collections.sort(keys);
						for(int key: keys){
							out.write((key+1) + " " + portionOfFunds.get(key)  +",");
						}
					}
					
					
					
//					out.write(" " + portionOfFunds.size()+ ",");
//					out.write(""+(new Double(ReadCSVFile.getValue(line, colNumReturn)) - spyReturns.get(quarter)));
					out.write((cikToIndex.size()+1)+ " " + ret + "}");
					out.write(" \n");
					}
					portionOfFunds = new Hashtable<Integer, String>();
					ticker = newTicker;
					quarter = newQuarter;
					cik = new CIK(ReadCSVFile.getValue(line, colNumCIK));
					portionOfFund = new Double(ReadCSVFile.getValue(line, colNumPOF));
					if(cikToIndex.get(cik) != null){
						if(portionOfFund > .05)
							portionOfFunds.put((cikToIndex.get(cik)), ""+ 1);

//							portionOfFunds.put((cikToIndex.get(cik)), ""+ portionOfFund);
					}
				}else{
					cik = new CIK(ReadCSVFile.getValue(line, colNumCIK));
					portionOfFund = new Double(ReadCSVFile.getValue(line, colNumPOF));
					if(cikToIndex.get(cik) != null){
						if(portionOfFund > .05) 
							portionOfFunds.put((cikToIndex.get(cik)), ""+ 1);

//							portionOfFunds.put((cikToIndex.get(cik)), ""+ portionOfFund);
					}
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
		
		DB.turnMultipleQueriesOff();
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
	
	
	//why TreeSet? no duplicates
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
	
	//for an ArrayList of CIKS, start date and numQuarters
	//return a Hashtable of CIKS to returns, if the CIK does not have data for numQuarters the return value is -1.
	
	private static Hashtable<CIK, Double> computeReturns(TreeSet<CIK> ciks, Quarter startQuarter, int numQuarters){
		Lib.assertTrue(numQuarters%4 ==  0);
		
		Hashtable<CIK, Double> cikToReturn = new Hashtable<CIK, Double>();
		
		DB.turnMultipleQueriesOn();
		
		Quarter endQuarter = startQuarter;
		for(int i = 0; i < numQuarters; i++)
			endQuarter = endQuarter.getNextQuarter();
		
		double totalRet  = 1;
		double tempRet = 0.0;
		for(CIK cik: ciks){
			for(Quarter q = startQuarter; q.compareTo(endQuarter) <= 0; q = q.getNextQuarter()){
				tempRet = DB.getFundReturn(cik, q);
				if(tempRet == -999){ // if there is no return for that quarter
					totalRet = -999;
					break;
				}
				totalRet *= (tempRet+1);
			}
			cikToReturn.put(cik, totalRet-1);
			totalRet = 1;
		}
		
		DB.turnMultipleQueriesOff();
		return cikToReturn;
	}
	
}
