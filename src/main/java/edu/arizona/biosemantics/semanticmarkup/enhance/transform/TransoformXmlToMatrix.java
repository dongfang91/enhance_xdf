package edu.arizona.biosemantics.semanticmarkup.enhance.transform;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import au.com.bytecode.opencsv.CSVWriter;
import edu.arizona.biosemantics.common.ling.know.SingularPluralProvider;
import edu.arizona.biosemantics.common.ling.know.lib.WordNetPOSKnowledgeBase;
import edu.arizona.biosemantics.common.ling.transform.IInflector;
import edu.arizona.biosemantics.common.ling.transform.lib.SomeInflector;
import edu.arizona.biosemantics.common.log.LogLevel;
import edu.arizona.biosemantics.semanticmarkup.enhance.config.Configuration;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.objectbank.TokenizerFactory;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.trees.Tree;


public class TransoformXmlToMatrix extends AbstractTransformer {
	private static String nonSpecificParts = "apex|appendix|area|band|base|belt|body|cavity|cell|center|centre|chamber|component|content|crack|edge|element|end|"
			+ "face|groove|layer|line|margin|middle|notch|part|pore|portion|protuberance|remnant|section|"
			+ "side|stratum|surface|tip|wall|zone";
	
	private static String identifiedConstraint ="peri-marginal|distal|ovule-bearing|germ|corolla|brach|individual|hymenophoral|middle|front|stigmatic|style|"
			+ "lateral|colour|external|central|broader|ante-terminal|lamellar|inner|subcuticular|concentric|"
			+ "superior|maxillary|anterior|adaxial|abaxial|distinct|transverso-medial|externo-medial|posterior|submarginal|lower|"
			+"oil|proximal|cauline|labial|outer|lateral|universal|upper|marginal|discal|apical|terminal|median|basal";
	
//	private static String[] start = {"<sentence>","<sentence>","<sentence>","<sentence>"};
//	
//	private static String[] end = {"</sentence>","/<sentence>","/<sentence>","/<sentence>"};

	private static String[] start = {"<sentence>"};
	
	private static String[] end = {"</sentence>"};
	protected ParenthesisRemover parenthesisRemover = new ParenthesisRemover();
	
	protected CollapseBiologicalEntityToName collapseBiologicalEntityToName = new CollapseBiologicalEntityToName();
	
	private SAXBuilder saxBuilder = new SAXBuilder();
	
	static TokenizerFactory<Word> tf;
	
	
	
	
    public TransoformXmlToMatrix() {
		
	}
    
    public static List<Word> getTokens(String sentence){ 
    	if(tf == null) 
    		tf = PTBTokenizer.factory(); 

    	List<Word> tokens_words = tf.getTokenizer(new StringReader(sentence)).tokenize(); 


    	return tokens_words; 
    } 
    
    public static void main(String[] args) throws IOException {
//    	String input = "If the returnDelims  flag is true, then the delimiter characters are also returned as tokens.";
//
//        
//    	
//    	List<Word> tokens_words = getTokens(input);
//    	for (Word t: tokens_words){
//    		System.out.println(t.word());
//    	}
    	WordNetPOSKnowledgeBase wordNetPOSKnowledgeBase = new WordNetPOSKnowledgeBase(Configuration.wordNetDirectory, false);
    	SingularPluralProvider singularPluralProvider = new SingularPluralProvider();
    	
    	IInflector inflectorIInflector  = new SomeInflector(wordNetPOSKnowledgeBase, singularPluralProvider.getSingulars(), singularPluralProvider.getPlurals());
    	TransoformXmlToMatrix test = new TransoformXmlToMatrix();
    	
    	//test.run(new File("in5_reordered"), new File("gold5_reordered"),inflectorIInflector);
    	test.run(new File("in_mixedtaxon_reordered"), new File("gold_mixed_taxon_reordered"),inflectorIInflector);
    }
    
    
    
    
    public void run(File inputXMLDirectory,File goldXMLDirectory,IInflector inflectorIInflector) throws IOException {
    	CSVWriter csvWriter= new CSVWriter(new FileWriter("evaluation/inputdata_mixed_taxon.csv"));
    	
    	
    	for(File file : inputXMLDirectory.listFiles()) {
			if(file.isFile()) {
				System.out.println("Transforming file " + file.getName());
				log(LogLevel.DEBUG, "Transforming file " + file.getName());
				Document inputDocument = null;
				Document goldDocument = null;
				try(InputStreamReader inputStreamReader = new InputStreamReader(new FileInputStream(file), "UTF-8")) {
					inputDocument = saxBuilder.build(inputStreamReader);
				} catch (JDOMException | IOException e) {
					log(LogLevel.ERROR, "Can't read xml from file " + file.getAbsolutePath(), e);
				}
				
				try(InputStreamReader inputStreamReader2 = new InputStreamReader(new FileInputStream(file), "UTF-8")) {
					goldDocument = saxBuilder.build(inputStreamReader2);
				} catch (JDOMException | IOException e) {
					log(LogLevel.ERROR, "Can't read xml from file " + file.getAbsolutePath(), e);
				}
				
				if(inputDocument != null &&goldDocument != null){
					//List<String> entityList =  new LinkedList<String>();
					List<Element> inputDescriptions = this.descriptionXpath.evaluate(inputDocument);
					List<Element> goldDescriptions = this.descriptionXpath.evaluate(inputDocument);

					int dIndex = 0;
					while(dIndex<inputDescriptions.size()){
						Element inputDescription = inputDescriptions.get(dIndex);
						Element goldDescription = goldDescriptions.get(dIndex);
						
						List<Element> inputContext =  inputDescription.getChildren("statement");
						List<Element> goldContext =  inputDescription.getChildren("statement");
						
						List<String> tokens = new LinkedList<String>();
						List <String []> sentence = new LinkedList<String []>();
						List <String []> sentence2 = new LinkedList<String []>();
						//String [] senteneTokens = new String [4];
						String line ="";
						for(Element statement: inputDescription.getChildren("statement")){
							String currentSentence = statement.getChildText("text").toLowerCase();
							currentSentence = parenthesisRemover.remove(currentSentence, '(', ')');
							line = line +" "+ currentSentence;
						}
						line = line.trim();
						List<Word> tokens_words = getTokens(line);
//						
						
						int index = 0;
						for (Word t: tokens_words){
							tokens.add(t.word());
							String [] senteneTokens = {String.valueOf(index),t.word(),"",""};
							String [] tokens1 = {t.word()};
							sentence.add( senteneTokens);
							sentence2.add( tokens1);
							index++;
//							csvWriter.writeNext(senteneTokens);
						}
						index = 0;
						for(String token: tokens){
							
							if(inflectorIInflector.getSingular(token).matches(nonSpecificParts)){
								if(index>=1&&tokens.get(index-1).matches(identifiedConstraint)){
									if(index>=2&&tokens.get(index-2).matches(identifiedConstraint)){
										sentence.get(index-2)[2]="B_nBio";
										sentence.get(index-1)[2]="I_nBio";
										sentence.get(index)[2]="I_nBio";
									}
									
									if(index>=3&&tokens.get(index-2).matches("and")&&tokens.get(index-3).matches(identifiedConstraint)){
										sentence.get(index-3)[2]="B_nBio";
										sentence.get(index-2)[2]="I_nBio";
										sentence.get(index-1)[2]="I_nBio";
										sentence.get(index)[2]="I_nBio";
									}
									
									else {
										sentence.get(index-1)[2]="B_nBio";
										sentence.get(index)[2]="I_nBio";
									}
										
								}
								
								else if(index>=1&&isBiologicalEntity(tokens.get(index-1),inputContext)){
									sentence.get(index-1)[2]="B_Bio";
									sentence.get(index)[2]="I_Bio";
								}
								
								else {
									
									sentence.get(index)[2]="B_nBio";
								}
								
							}
							
							if(isBiologicalEntity(token,inputContext)&&!inflectorIInflector.getSingular(token).matches(nonSpecificParts)){
								if(index>=1&&tokens.get(index-1).matches(identifiedConstraint)){
									if(index>=2&&tokens.get(index-2).matches(identifiedConstraint)){
										sentence.get(index-2)[2]="B_Bio";
										sentence.get(index-1)[2]="I_Bio";
										sentence.get(index)[2]="I_Bio";
									}
									
									if(index>=3&&tokens.get(index-2).matches("and")&&tokens.get(index-3).matches(identifiedConstraint)){
										sentence.get(index-3)[2]="B_Bio";
										sentence.get(index-2)[2]="I_Bio";
										sentence.get(index-1)[2]="I_Bio";
										sentence.get(index)[2]="I_Bio";
									}
									
									else {
										sentence.get(index-1)[2]="B_Bio";
										sentence.get(index)[2]="I_Bio";
									}
										
								}
								
								else{
									sentence.get(index)[2]="B_Bio";
								}
							}	
							index ++;	
						}
						
						sentence2.add(0,start);
						sentence2.add(sentence.size()+1,end);
						csvWriter.writeAll(sentence2);
						
						dIndex++;
					}
					csvWriter.close();
					
				}
				
			}
    	}
    }

    
    
    
    
    
    
    
	@Override
	public boolean transform(List<AbstractTransformer> transformers,
			Element statement, Element biologicalEntity, List<Element> context,
			List<Element> wholeStatements) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void transformAll(Element statement, Element biologicalEntity,
			List<Element> wholeStatement) {
		// TODO Auto-generated method stub
		
	}
    }
    
    
    
    

