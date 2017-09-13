package edu.arizona.biosemantics.semanticmarkup.enhance.transform;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import com.google.gwt.dev.util.collect.HashMap;

import edu.arizona.biosemantics.common.ling.Token;
import edu.arizona.biosemantics.common.ling.transform.ITokenizer;
import edu.arizona.biosemantics.common.ling.transform.lib.WhitespaceTokenizer;
import edu.arizona.biosemantics.common.log.LogLevel;



public class ReordeXml extends AbstractTransformer{
	private SAXBuilder saxBuilder = new SAXBuilder();
	protected ParenthesisRemover parenthesisRemover = new ParenthesisRemover();
	ITokenizer tokenizer = new WhitespaceTokenizer();
	public ReordeXml() {
		
	}
	
	public static void main(String[] args) throws IOException {
		ReordeXml reordeXml = new ReordeXml();
		reordeXml.run(new File("in3"), new File("in3_reordered"));
		reordeXml.run(new File("gold"), new File("gold2_reordered"));
//		reordeXml.run(new File("in_mixedtaxon"), new File("in_mixedtaxon_reordered"));
//		reordeXml.run(new File("gold_mixed_taxon"), new File("gold_mixed_taxon_reordered"));
	}
	
	public void run(File inputDirectory, File outputDirectory) {
		

		for(File file : inputDirectory.listFiles()) {
			if(file.isFile()) {
				System.out.println("Transforming file " + file.getName());
				log(LogLevel.DEBUG, "Transforming file " + file.getName());
				Document document = null;
				try(InputStreamReader inputStreamReader = new InputStreamReader(new FileInputStream(file), "UTF8")) {
					document = saxBuilder.build(inputStreamReader);
				} catch (JDOMException | IOException e) {
					log(LogLevel.ERROR, "Can't read xml from file " + file.getAbsolutePath(), e);
				}
				
				if(document != null){
					for (Element description : this.descriptionXpath.evaluate(document)){
						List<Element> wholeStatements = new LinkedList<Element>();
						for(Element statement : description.getChildren("statement")) {
							wholeStatements.add(statement);
							for(Element biologicalEntity : statement.getChildren("biological_entity")) {
								
							}

						}

						for(Element statement : description.getChildren("statement")) {
							String sentence = statement.getChild("text").getValue().toLowerCase();
							//sentence = parenthesisRemover.remove(sentence, '(', ')');
							sentence = sentence.replaceAll("-", " ");
							sentence = sentence.replaceAll("[–()]", " ");
							List<Token> terms = tokenizer.tokenize(sentence);
							
							Map<String, Integer> entityOccurences = new HashMap<String, Integer>();
							Map<Token, Integer> characterOccurences = new HashMap<Token, Integer>();
							boolean hasBiologicalentity = true;
							List<Element> removingBiologicalEntity = new LinkedList<Element>();
							List<Integer> statementIdList = new LinkedList<Integer>();
							statementIdList = getStatementIdList(statement);
							
							for(Element biologicalEntity : statement.getChildren("biological_entity")) {
								String name = biologicalEntity.getAttributeValue("name_original").trim().replaceAll("-"," ").replaceAll("–", " ");
								String nameR = biologicalEntity.getAttributeValue("name").trim();
								String constraint = biologicalEntity.getAttributeValue("constraint");
								String id = biologicalEntity.getAttributeValue("id");
								constraint = constraint == null ? "" : constraint.trim();
								
								if(id.equals("o129"))
								   System.out.println("asdadwfsaf");
								
								if(!entityOccurences.containsKey(name))
									entityOccurences.put(name, 0);
								entityOccurences.put(name, entityOccurences.get(name) + 1);
								int appearance = entityOccurences.get(name);
								List<Token> entityTokens = tokenizer.tokenize(name);
								boolean entityTokenExist = true;
								for (Token entityToken:entityTokens){
									entityTokenExist = isTokenExist(terms, entityToken, appearance) &entityTokenExist;
									//&&isIdStartOrEnd(id,statementIdList)
								}
								
								if(!entityTokenExist&&!nameR.equals("whole_organism")&&!isTokenExist(terms, new Token(constraint), 1)&&isIdStartOrEnd(id,statementIdList))
									{removingBiologicalEntity.add(biologicalEntity);
									System.out.println(id+"  "+ name );}
								
								else {
									boolean characterTokenExist = true;
									float k=0;
									float j=0;
									for (Element character: biologicalEntity.getChildren("character"))
									{
									
										if(character.getAttributeValue("char_type")!=null){
											k++;
											if(character.getAttributeValue("char_type").equals("range_value")){
												String from = character.getAttributeValue("from").toLowerCase().trim().replaceAll("-", " ").replaceAll("–", " ");
												String to = character.getAttributeValue("to").toLowerCase().trim().replaceAll("-", " ").replaceAll("–", " ");
												List<Token> characterTokens1 = tokenizer.tokenize(from);
												List<Token> characterTokens2 = tokenizer.tokenize(to);
												boolean characterRangeTokenExist1 = true;
												boolean characterRangeTokenExist2 = true;
												for (Token characterToken : characterTokens1){
													characterRangeTokenExist1 =isTokenExist(terms, characterToken, 1)&&characterRangeTokenExist1;
												}
												
												for (Token characterToken : characterTokens2){
													characterRangeTokenExist2 =isTokenExist(terms, characterToken, 1)&&characterRangeTokenExist2;
												}
												if(characterRangeTokenExist1||characterRangeTokenExist2)
													j++;
												
												characterTokenExist = characterTokenExist& (characterRangeTokenExist1||characterRangeTokenExist2);
												
												
											}	
										}
											
										
										else if(character.getAttributeValue("value")!=null&&!character.getAttributeValue("name").equals("quantity")){
											k++;
											String characterValue = character.getAttributeValue("value").toLowerCase().trim().replaceAll("-"," ").replaceAll("–", " ");
											boolean characterValueTokenExist = true;
//											if(character.getAttributeValue("value").equals("surrounding"))
//												System.out.println("asdadwfsaf");
											List<Token> characterTokens = tokenizer.tokenize(characterValue);
											if(characterTokens.isEmpty()) break;
											for (Token characterToken : characterTokens){
//												if(!characterOccurences.containsKey(characterToken))
//													characterOccurences.put(characterToken, 0);
//												characterOccurences.put(characterToken, characterOccurences.get(characterToken) + 1);
//												int characterAppearance = characterOccurences.get(characterToken);
												characterValueTokenExist =isTokenExist(terms, characterToken, 1)&&characterValueTokenExist;
											}
											if (characterValueTokenExist)
												j++;
											characterTokenExist = characterTokenExist&characterValueTokenExist;
										}
										
									}
									if((!characterTokenExist&&isIdStartOrEnd(id,statementIdList))){
										if(!nameR.equals("whole_organism")&&k>0&&j/k<0.333333){
											removingBiologicalEntity.add(biologicalEntity);
											//System.out.println(j/k);
										    System.out.println(id+"  "+ name );
										}
									}
									else continue;
									
								}
								
							}
							if(wholeStatements.indexOf(statement)>0 &&!removingBiologicalEntity.isEmpty()){
								Element preStatement=wholeStatements.get(wholeStatements.indexOf(statement)-1);
								for(Element biologicalEntity : removingBiologicalEntity) {
									if(statement.indexOf(biologicalEntity)!=-1)
										statement.removeContent(biologicalEntity);
									if(preStatement.indexOf(biologicalEntity)==-1)
										addBiologicalEntity(preStatement,biologicalEntity);
								}
							}
							
						}	
					}
				}
				File outputFile = new File(outputDirectory, file.getName());
				try {
					outputFile.getParentFile().mkdirs();
					outputFile.createNewFile();
				} catch (IOException e) {
					log(LogLevel.ERROR, "Can't create xml file " + file.getAbsolutePath(), e);
				}
				try(OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(outputFile), "UTF8")) {
					XMLOutputter xmlOutput = new XMLOutputter(Format.getPrettyFormat());
					xmlOutput.output(document, outputStreamWriter);
				} catch (IOException e) {
					log(LogLevel.ERROR, "Can't write xml to file " + file.getAbsolutePath(), e);
				}
			}
		}
	
	}
	
	private boolean isIdStartOrEnd(String id, List<Integer> statementIdList) {
		int size = statementIdList.size();
		id=id.replaceAll("o", "");
		if(size>=1){
			int a=statementIdList.get(0);
			int b = statementIdList.get(size-1);
			if(Integer.valueOf(id)==a||Integer.valueOf(id)==b)
				return true;
		}
		return false;
	}

	private void addBiologicalEntity(Element preStatement,
			Element biologicalEntity) {
		int i=0;
		String id = biologicalEntity.getAttributeValue("id").trim();
		id = id.replace("o", "");
		int index=0;
		List<Element> preBiologicalEntitys = new LinkedList<Element>();
		preBiologicalEntitys = preStatement.getChildren("biological_entity");
		if(preStatement.indexOf(biologicalEntity)!=-1)
			return;
		for (Element preBiologicalEntity: preBiologicalEntitys ){
			String preId = preBiologicalEntity.getAttributeValue("id").trim();
			preId = preId.replace("o", "");
			index = preStatement.indexOf(preBiologicalEntity);
			
			if (Integer.valueOf(id)< Integer.valueOf(preId)){
				    i=index;
				    preStatement.addContent(i-1, biologicalEntity);
				    return;
				    
			}
			
		}
		
		if(i==0){
				preStatement.addContent(index+2, biologicalEntity);
		}
			
	}

	private int indexOf(List<Token> tokens, Token token, int appearance) {
		int found = 0;
		for(int i=0; i<tokens.size(); i++) {
			Token t = tokens.get(i);
			if(t.equals(token))
				found++;
			if(found == appearance)
				return i;
		}
		return -1;
	}
	
	private boolean isTokenExist(List<Token> tokens, Token token, int appearance){
		int k = indexOf(tokens, token, appearance);
		if (k==-1)
			return false;
		else 
			return true;
		
	}
	

	@Override
	public boolean transform(List<AbstractTransformer> transformers,
			Element statement, Element biologicalEntity, List<Element> context,
			List<Element> wholeStatements) {
				return false;
		// TODO Auto-generated method stub
		
	}

	@Override
	public void transformAll(Element statement, Element biologicalEntity,
			List<Element> wholeStatement) {
		// TODO Auto-generated method stub
		
	}
	
	
	
	
}
