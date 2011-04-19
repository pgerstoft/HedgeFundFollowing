import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.TreeSet;

import weka.classifiers.functions.SimpleLogistic;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.instance.NonSparseToSparse;
 

public class DataAnalysis {

	public DataAnalysis(){
	}
	
	public void compareClassifiers(){
		csvToArff("designMatrixTrain.csv", "designMatrixTrain.arff");
		arffToSparse("designMatrixTrain.arff");
		csvToArff("designMatrixTest.csv", "designMatrixTest.arff");
		arffToSparse("designMatrixTest.arff");
		
		ArffLoader loaderTrain = new ArffLoader();
		Instances train = null;
	    try {
			loaderTrain.setFile(new File("designMatrixTrain.arff"));
			train = loaderTrain.getStructure();
		} catch (IOException e) {
			e.printStackTrace();
		}
	    
	    ArffLoader loaderTest = new ArffLoader();
	    Instances test = null;
	    try {
			loaderTest.setFile(new File("designMatrixTest.arff"));
		    test = loaderTest.getStructure();
		} catch (IOException e) {
			e.printStackTrace();
		}
	    
		SimpleLogistic logit = new SimpleLogistic();
		try {
			logit.buildClassifier(train);
		} catch (Exception e) {
			e.printStackTrace();
		}

		for( int i =  0; i < test.numInstances(); i++){
			try {
				 System.out.print("ID: " + test.instance(i).value(0) + " ");
				System.out.print(logit.distributionForInstance(test.instance(i)));
				System.out.println("Classifi " +  test.classAttribute().value( (int) logit.classifyInstance(test.instance(i))));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}
	
	
	public void makeDesignMatrixCSV(Quarter predictionQuarter, int numTrainingQuarters){
		
		//TODO move file before you write, input file to makeDesigntemp
		String tempTrainFile = "temp/tempTrainDesignMatrix.csv";
		String tempTestFile  = "temp/tempTestDesignMatrix.csv";
		String designMatrixTrain = "designMatrixTrain.csv";
		String designMatrixTest = "designMatrixTest.csv";
		//set allParts of writeable to true
		new File(System.getProperty("user.dir")+"/temp/").setWritable(true);
		
		Quarter startQuarter = predictionQuarter;
		for(int i = numTrainingQuarters; i > 0; i--)
			startQuarter = startQuarter.getPreviousQuarter();
		
		System.out.println("Storing Database values into csv file...");
//		new File(tempTrainFile).delete();
//		DB.makeDesignTemp(System.getProperty("user.dir")+"/" + tempTrainFile, startQuarter.getPreviousQuarter(), predictionQuarter);
//		new File(tempTestFile).delete();
//		DB.makeDesignTemp(System.getProperty("user.dir")+"/" + tempTestFile, predictionQuarter.getPreviousQuarter(), predictionQuarter.getNextQuarter());		
		System.out.println("Pivoting table...");
		
		writeDesignMatrix(tempTrainFile, tempTestFile, designMatrixTrain, true);
		writeDesignMatrix(tempTrainFile, tempTestFile, designMatrixTest, false);
		
		
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

	private void writeDesignMatrix(String tempTrainFile, String tempTestFile, String designMatrixFile, boolean isTrainingDesignMatrix){
		int colNumTicker = 0; //ReadCSVFile.getColNum(line, "TICKER");
		int colNumQuarter = 1;
		int colNumCIK = 2; //ReadCSVFile.getColNum(line, "CIK");
		int colNumPOF = 3; //ReadCSVFile.getColNum(line, "PortionOfFund");
		int colNumReturn = 4; //ReadCSVFile.getColNum(line,"RET"); //TODO
		
		System.out.println("getting CIKS FROM FILE...");
		//we only want ciks that can be used in the final model
		TreeSet<CIK> ciks = getCIKSFromFile(tempTestFile, colNumCIK);
		
		Hashtable<CIK, Integer> cikToIndex = new Hashtable<CIK, Integer>();
		
		int index = 0;
		for(Object c: ciks.toArray())
			cikToIndex.put((CIK) c, index++);
		
		ArrayList<Double> portionOfFunds = zeroedOutArrayList(cikToIndex.size());

		String ticker = null, newTicker = null;
		Quarter quarter = null, newQuarter = null;
		double portionOfFund;
		CIK cik;
		double ret;
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
			System.out.println(ciks.size());
			for(Object c : ciks.toArray()){
				tempCIK = (CIK) c;
				out.write("POF"+c.toString()+", ");
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
					ret = 0.0; //new Double(ReadCSVFile.getValue(line, colNumReturn));
					//ret = getValue - SPY > 0 ? 1 : 0;
					//System.out.println(ticker + ","  + quarter + "," + portionOfFunds.toString().replaceAll("[\\[\\]]", "") + "," + ret);
					
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
		String designMatrixFileArff = designMatrixFile.replace(".csv", ".arff");
		csvToArff(designMatrixFile, designMatrixFileArff);
		arffToSparse(designMatrixFileArff);
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
	
}
