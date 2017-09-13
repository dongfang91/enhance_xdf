package edu.arizona.biosemantics.semanticmarkup.enhance.transform;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import edu.arizona.biosemantics.common.ling.Token;
import edu.arizona.biosemantics.common.log.LogLevel;
import edu.arizona.biosemantics.semanticmarkup.enhance.know.KnowsSynonyms.SynonymSet;

public class SVMInput extends AbstractTransformer{
	
	private static String nonSpecificParts = "apex|appendix|area|band|base|belt|body|cavity|cell|center|centre|chamber|component|content|crack|edge|element|end|"
			+ "face|groove|layer|line|margin|middle|notch|part|pore|portion|protuberance|remnant|section|"
			+ "side|stratum|surface|tip|wall|zone";
	
	protected ParenthesisRemover parenthesisRemover = new ParenthesisRemover();
	
	protected CollapseBiologicalEntityToName collapseBiologicalEntityToName = new CollapseBiologicalEntityToName();
	
	private SAXBuilder saxBuilder = new SAXBuilder();
	
    public SVMInput() {
		
	}
	
	public static void main(String[] args) throws IOException {
		SVMInput SVMInput = new SVMInput();
		//reordeXml.run(new File("in3_original"), new File("in4"));
		//reordeXml.run(new File("in3_original"), new File("in4_reordered"));
		SVMInput.run(new File("in4_reordered"), new File("nlp_train"),new File("nlp_output"));
	}
	
	public void run(File inputXMLDirectory,File inputTextDirectory, File outputDirectory) throws IOException {
		List<Line> text= new LinkedList<Line>();
		List<String> lines = new LinkedList<String>();

		for(File file : inputTextDirectory.listFiles()) {
			if(file.isFile()) {
				System.out.println("Transforming file " + file.getName());
			}
			FileInputStream inputStream = new FileInputStream(file);
			BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, "utf-8"));
			String line = null;

			while ((line = br.readLine()) != null) {
				if(line.startsWith("<description")){
					lines = new LinkedList<String>();
				}
				lines.add(line);
				if(line.startsWith("</description")){
					text.add(new Line(lines));
				}
			}
			br.close();
		}





		for(File file : inputXMLDirectory.listFiles()) {
			if(file.isFile()) {
				File outputFile = new File(outputDirectory, file.getName());
				FileOutputStream outputStream = new FileOutputStream(outputFile);
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));

				System.out.println("Transforming file " + file.getName());
				log(LogLevel.DEBUG, "Transforming file " + file.getName());
				Document document = null;
				try(InputStreamReader inputStreamReader = new InputStreamReader(new FileInputStream(file), "UTF-8")) {
					document = saxBuilder.build(inputStreamReader);
				} catch (JDOMException | IOException e) {
					log(LogLevel.ERROR, "Can't read xml from file " + file.getAbsolutePath(), e);
				}

				if(document != null){
					int descriptionIndex = 0;
					int lineIndex;
					String singleLine = "";
					for (Element description : this.descriptionXpath.evaluate(document)){
						Line descriptionLines =  text.get(descriptionIndex);
						descriptionIndex = descriptionIndex + 1;
						lineIndex = 0;
						String forwardPart = null;
						String backwardPart = null;
						
						List<Element> statements =  description.getChildren("statement");
						int statementLength = statements.size(); 
						for(int statementIndex = 0; statementIndex <= statementLength-1;statementIndex++){
							Element statement =  statements.get(statementIndex);
							String currentSentence = statement.getChildText("text").toLowerCase();
							if(currentSentence.startsWith("plants 50–150 cm."))
								System.out.println("2asd");
							int length = currentSentence.length();
							if(length>=25){
								forwardPart = currentSentence.substring(0,25);
								backwardPart = currentSentence.substring(length-25,length);
							}
							else{
								forwardPart = currentSentence;
								backwardPart = currentSentence;
							}
							
							String line = "";

							if(lineIndex<=descriptionLines.getLine().size()-1)
								line = descriptionLines.getLine().get(lineIndex).toLowerCase();
							
							while((line.contains("<description"))&&lineIndex<=descriptionLines.getLine().size()-1){
								bw.write(line);
								bw.newLine();
								lineIndex = lineIndex + 1;
								if(lineIndex<=descriptionLines.getLine().size()-1)
									line = descriptionLines.getLine().get(lineIndex).toLowerCase();
							}


							if (line.contains(currentSentence)||line.contains(forwardPart)||line.contains(backwardPart)){
								currentSentence = parenthesisRemover.remove(currentSentence, '(', ')');
								for(Element biologicalEntities : statement.getChildren("biological_entity")) {
									String originalName = biologicalEntities.getAttributeValue("name_original").trim();
									String currentName = biologicalEntities.getAttributeValue("name").trim();
									if(!originalName.equals("")){
										String currentConstraint = biologicalEntities.getAttributeValue("constraint");
										currentConstraint = currentConstraint == null ? "" : currentConstraint.trim();
										if(!currentConstraint.equals("")){
											String completeEntity = currentConstraint+ " "+originalName;
											if(currentName.matches(nonSpecificParts))
												currentSentence = currentSentence.replaceAll(completeEntity, "<"+completeEntity +"><nbio>");
											else
												currentSentence = currentSentence.replaceAll(completeEntity, "<"+completeEntity +"><bio>");
										}

									}
								}
								for(Element biologicalEntities : statement.getChildren("biological_entity")) {
									String originalName = biologicalEntities.getAttributeValue("name_original").trim();
									String currentName = biologicalEntities.getAttributeValue("name").trim();

									if(!originalName.equals("")){
										String currentConstraint = biologicalEntities.getAttributeValue("constraint");
										currentConstraint = currentConstraint == null ? "" : currentConstraint.trim();
										if(currentConstraint.equals("")){
											if(currentName.matches(nonSpecificParts))
												currentSentence =replaceAllWith(currentSentence,originalName,"<nbio>");
											else
												currentSentence = replaceAllWith(currentSentence,originalName,"<bio>");
										}		
									}
								}
								singleLine = singleLine + currentSentence + " ";
								if(statementIndex==statementLength-1){
									
									bw.write(singleLine);
									bw.newLine();
									System.out.println(singleLine);
									singleLine = "";
									lineIndex = lineIndex+1;
									line = descriptionLines.getLine().get(lineIndex).toLowerCase();
									bw.write(line);
									bw.newLine();
									System.out.println(line);
									lineIndex = lineIndex + 1;
									
								}
							}

							else if((!line.contains(currentSentence)&&!line.contains(forwardPart)&&!line.contains(backwardPart))){
								statementIndex = statementIndex -1;
								lineIndex = lineIndex+1;
								bw.write(singleLine);
								System.out.println(singleLine);
								bw.newLine();
								singleLine = "";
							}
							
							
						}	
					}
				}
				bw.close();
			}
		}
	}


	
//	private int indexOf(String line, String k, int appearance) {
//		int found = 0;
//		for(int i=0; i<tokens.size(); i++) {
//			Token t = tokens.get(i);
//			if(t.equals(token))
//				found++;
//			if(found == appearance)
//				return i;
//		}
//		return -1;
//	}
	
	public static boolean isNonSpecificPart(String name) {
		if(name.matches(nonSpecificParts)) {
				return true;
		}
		return false;
	}
	
	public String replaceAllWith(String sentence, String originalName, String bioAnno) {
		sentence = sentence.replaceAll(originalName+ " ","<"+originalName+">"+bioAnno+" ");
		sentence = sentence.replaceAll(originalName+ ",","<"+originalName+">"+bioAnno+",");
		sentence = sentence.replaceAll(originalName+ ":","<"+originalName+">"+bioAnno+":");
		sentence = sentence.replaceAll(originalName+ ";","<"+originalName+">"+bioAnno+";");
		sentence = sentence.replaceAll(originalName+ "/.","<"+originalName+">"+bioAnno+"/.");
		sentence = sentence.replaceAll("  ", " ");
		
		return sentence;
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
