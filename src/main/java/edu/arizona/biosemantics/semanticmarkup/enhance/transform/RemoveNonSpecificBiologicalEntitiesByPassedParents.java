package edu.arizona.biosemantics.semanticmarkup.enhance.transform;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.jdom2.Document;
import org.jdom2.Element;

import com.google.gwt.thirdparty.guava.common.collect.Lists;

import edu.arizona.biosemantics.common.ling.Token;
import edu.arizona.biosemantics.common.ling.transform.IInflector;
import edu.arizona.biosemantics.common.ling.transform.ITokenizer;
import edu.arizona.biosemantics.semanticmarkup.enhance.know.KnowsPartOf;
import edu.arizona.biosemantics.semanticmarkup.enhance.know.KnowsSynonyms;
import edu.arizona.biosemantics.semanticmarkup.enhance.run.Run;

public class RemoveNonSpecificBiologicalEntitiesByPassedParents extends RemoveNonSpecificBiologicalEntities {
	
	private CollapseBiologicalEntityToName collapseBiologicalEntityToName;
	private IInflector inflector;

	public RemoveNonSpecificBiologicalEntitiesByPassedParents(
			KnowsPartOf knowsPartOf, KnowsSynonyms knowsSynonyms, ITokenizer tokenizer,
			CollapseBiologicalEntityToName collapseBiologicalEntityToName, 
			IInflector inflector) {
		super(knowsPartOf, knowsSynonyms, tokenizer);
		this.inflector = inflector;
		this.collapseBiologicalEntityToName = collapseBiologicalEntityToName;
	}

	@Override
	public boolean transform(List<AbstractTransformer> transformers,Element currentStatement, Element currentBiologicalEntity,List<Element> context, List<Element> wholeStatements) {
		System.out.println("----------PassedParents----------");

		String currentSentence = currentStatement.getChildText("text");
		System.out.println(currentSentence);
		String currentId = currentBiologicalEntity.getAttributeValue("id").trim();
		String currentName = currentBiologicalEntity.getAttributeValue("name").trim();
		String currentOriginalName = currentBiologicalEntity.getAttributeValue("name_original").trim();
		String currentConstraint = currentBiologicalEntity.getAttributeValue("constraint");
		String currentTermNormalized = collapseBiologicalEntityToName.collapse(currentBiologicalEntity);
		String currentStatementId = currentStatement.getAttributeValue("id").trim();
		
		
		currentConstraint = currentConstraint == null ? "" : currentConstraint.trim();
		String parentStructure= null;
		
		for(Element statement : context) {
			String sentence = statement.getChildText("text");
			String statementId = statement.getAttributeValue("id").trim();
			
			
			
			System.out.println(sentence);
			//if(sentence.startsWith("the flagellum, mandibles, anterior margin of the")) {
				//System.out.println();
			//}
			
			Element parent = findParentInPassedStatements(currentBiologicalEntity, statement);
			
			
			if(parent != null) {
				String constraintName = parent.getAttributeValue("name");
				constraintName = constraintName == null ? "" : constraintName.trim();
				
				String constraintCon = parent.getAttributeValue("constraint");
				constraintCon = constraintCon == null ? "" : constraintCon.trim();
				
				String inferredConstraint = parent.getAttributeValue("inferred_constraint");
				inferredConstraint = inferredConstraint == null ? "" : inferredConstraint.trim();
				
				parentStructure = null;
				String termNormalized = collapseBiologicalEntityToName.collapse(parent);
				
				String nameSplited = termNormalized.replace(inferredConstraint,"").trim();
				
				if(knowsPartOf.isPartOf(currentName, termNormalized)||knowsPartOf.isPartOf(currentName,constraintName)||knowsPartOf.isPartOf(currentTermNormalized,constraintName)||knowsPartOf.isPartOf(currentName, nameSplited)||knowsPartOf.isPartOf(currentTermNormalized, nameSplited)||knowsPartOf.isPartOf(currentTermNormalized, termNormalized)){
					if(!inferredConstraint.equals("")||!RemoveNonSpecificBiologicalEntities.isNonSpecificPart(constraintName)||RemoveNonSpecificBiologicalEntities.isPartOfAConstraint(constraintName, constraintCon)) {
						parentStructure  = collapseBiologicalEntityToName.collapse(parent);
						if(parentStructure != null) {
							currentConstraint = (parentStructure + " " + currentConstraint).trim();								
							this.appendInferredConstraint(currentBiologicalEntity, parentStructure);
							currentBiologicalEntity.setAttribute("constraint", currentConstraint);
							System.out.println(currentConstraint);
							return true;
						}
					}
					else {
						Element constraintStatement = this.getStatementWithIdFromContext(context, parent.getAttributeValue("id"));
						Run run =new Run();
						boolean foundParent = true;
						foundParent =  run.runTransformer(transformers,constraintStatement, parent,wholeStatements);
						if(foundParent){
							parentStructure  = collapseBiologicalEntityToName.collapse(parent);
							if(parentStructure != null) {
								currentConstraint = (parentStructure + " " + currentConstraint).trim();								
								this.appendInferredConstraint(currentBiologicalEntity, parentStructure);
								currentBiologicalEntity.setAttribute("constraint", currentConstraint);
								System.out.println(currentConstraint);
								return true;
							}
						}
						else return false;
					}
				}
				
//				if(!RemoveNonSpecificBiologicalEntities.isNonSpecificPart(constraintName)||RemoveNonSpecificBiologicalEntities.isPartOfAConstraint(constraintName, constraintCon)) {
//					parentStructure = null;
//					String termNormalized = collapseBiologicalEntityToName.collapse(parent);
//
//					if(knowsPartOf.isPartOf(currentName, termNormalized)) {
//						parentStructure = collapseBiologicalEntityToName.collapse(parent);
//					} else {
//						if(knowsPartOf.isPartOf(currentName, parent.getAttributeValue("name"))) 
//							parentStructure = collapseBiologicalEntityToName.collapse(parent);
//					}
//					if(parentStructure != null) {
//						currentConstraint = (parentStructure + " " + currentConstraint).trim();								
//						this.appendInferredConstraint(currentBiologicalEntity, parentStructure);
//						currentBiologicalEntity.setAttribute("constraint", currentConstraint);
//						System.out.println(currentConstraint);
//						return true;
//					}
//				}
//				else {
//				    Element constraintStatement = this.getStatementWithIdFromContext(context, parent.getAttributeValue("id"));
//					Run run =new Run();
//					return run.runTransformer(transformers,constraintStatement, parent,wholeStatements);
//			   }
			}
			
			if(parent == null) {
				parentStructure = findParentStrInPassedStatements(currentBiologicalEntity, statement);
				if(parentStructure != null) {
					currentConstraint = (parentStructure + " " + currentConstraint).trim();								
					this.appendInferredConstraint(currentBiologicalEntity, parentStructure);
					currentBiologicalEntity.setAttribute("constraint", currentConstraint);
					System.out.println(currentConstraint);
					return true;
				}
			}
			
			
		}
		return false;
	}

	private String findParentStrInPassedStatements(
			Element currentBiologicalEntity, Element statement) {
		Element biologicalEntity = null;
		String name = currentBiologicalEntity.getAttributeValue("name");
		String constraintName = collapseBiologicalEntityToName.collapse(currentBiologicalEntity);
		constraintName = constraintName == null ? "" : constraintName.trim();
		name = name == null ? "" : name.trim();
		String sentence = statement.getChild("text").getValue().toLowerCase();
		sentence = parenthesisRemover.remove(sentence, '(', ')');
		List<Token> tokens = tokenizer.tokenize(sentence);
		for(Token term : tokens) {
			String termNorm = inflector.getSingular(term.getContent());
			if(knowsPartOf.isPartOf(name, termNorm)||knowsPartOf.isPartOf(constraintName,termNorm)){
				biologicalEntity = findBiologicalEntityByNameInStatement(term.getContent(),statement);
				if(biologicalEntity==null)
					return inflector.getSingular(term.getContent());
				else{
					return collapseBiologicalEntityToName.collapse(biologicalEntity);
				}
					
			}
		}
		return null;
	}

	private Element findParentInPassedStatements(Element currentBiologicalEntity, Element statement) {
		Element parent = null;
		String constraintName = collapseBiologicalEntityToName.collapse(currentBiologicalEntity);
		String name = currentBiologicalEntity.getAttributeValue("name");
		String id = currentBiologicalEntity.getAttributeValue("id");
		constraintName = constraintName == null ? "" : constraintName.trim();
		
		id = id.replace("o", "");
		id = id.replaceAll("_[0-9]$", "");
		
		for (Element biologicalEntity: statement.getChildren("biological_entity")){
			String termNormalized = collapseBiologicalEntityToName.collapse(biologicalEntity);
			String parentId =  biologicalEntity.getAttributeValue("id");
			String inferredConstraint = biologicalEntity.getAttributeValue("inferred_constraint");
			inferredConstraint = inferredConstraint == null ? "" : inferredConstraint.trim();
			String nameSplited = termNormalized.replace(inferredConstraint,"").trim();
			
			parentId = parentId.replace("o", "");
                                                                                                                                                                 			parentId = parentId.replaceAll("_\\S$", "");
			//if(Integer.valueOf(parentId) >= Integer.valueOf(id)) break;
			if(knowsPartOf.isPartOf(constraintName, termNormalized)||knowsPartOf.isPartOf(name, termNormalized)||knowsPartOf.isPartOf(name, nameSplited)||knowsPartOf.isPartOf(constraintName, nameSplited)||knowsPartOf.isPartOf(name, biologicalEntity.getAttributeValue("name"))||knowsPartOf.isPartOf(constraintName, biologicalEntity.getAttributeValue("name"))) {
				parent = biologicalEntity;
				return parent;
			} 
		}
		return null;
	}
		
		
//		String sentence = statement.getChild("text").getValue().toLowerCase();
//		sentence = parenthesisRemover.remove(sentence, '(', ')');
//		List<Token> tokens = tokenizer.tokenize(sentence);
		//tokens = Lists.reverse(tokens);
//		for(Token term : tokens) {
//			//if (term.getContent().equals("pileus"))
//				//System.out.println("12312312");
//			Element termsBiologicalEntity = getBiologicalEntity(term.getContent(), searchStatements);
//			
//			if(termsBiologicalEntity != null) {
//				String termNormalized = collapseBiologicalEntityToName.collapse(termsBiologicalEntity);
//				if(knowsPartOf.isPartOf(name, termNormalized)) {
//					return collapseBiologicalEntityToName.collapse(termsBiologicalEntity);
//				} else {
//					if(knowsPartOf.isPartOf(name, termsBiologicalEntity.getAttributeValue("name"))) 
//						return collapseBiologicalEntityToName.collapse(termsBiologicalEntity);
//				}
//			} else {
//				if(knowsPartOf.isPartOf(name, term.getContent())) 
//					return inflector.getSingular(term.getContent());
//			}
//		}
	

	@Override
	public void transformAll(Element statement, Element biologicalEntity,
			List<Element> wholeStatement) {
		// TODO Auto-generated method stub
		
	}
	

}
