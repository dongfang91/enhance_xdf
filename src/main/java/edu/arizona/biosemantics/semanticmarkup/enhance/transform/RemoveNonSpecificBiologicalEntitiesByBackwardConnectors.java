package edu.arizona.biosemantics.semanticmarkup.enhance.transform;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom2.Document;
import org.jdom2.Element;

import edu.arizona.biosemantics.common.ling.Token;
import edu.arizona.biosemantics.common.ling.transform.IInflector;
import edu.arizona.biosemantics.common.ling.transform.ITokenizer;
import edu.arizona.biosemantics.semanticmarkup.enhance.know.KnowsPartOf;
import edu.arizona.biosemantics.semanticmarkup.enhance.know.KnowsSynonyms;
import edu.arizona.biosemantics.semanticmarkup.enhance.run.Run;

public class RemoveNonSpecificBiologicalEntitiesByBackwardConnectors extends RemoveNonSpecificBiologicalEntities {

	private String connectBackwardToParent = "has|have|with|contains|without";
	private CollapseBiologicalEntityToName collapseBiologicalEntityToName;
	private IInflector inflector;
	
	public RemoveNonSpecificBiologicalEntitiesByBackwardConnectors(
			KnowsPartOf knowsPartOf, KnowsSynonyms knowsSynonyms, ITokenizer tokenizer,
			CollapseBiologicalEntityToName collapseBiologicalEntityToName, IInflector inflector) {
		super(knowsPartOf, knowsSynonyms, tokenizer);
		this.collapseBiologicalEntityToName = collapseBiologicalEntityToName;
		this.inflector = inflector;
	}
	
	@Override
	public boolean transform(List<AbstractTransformer> transformers,Element currentStatement, Element currentBiologicalEntity,List<Element> context, List<Element> wholeStatements) {
		System.out.println("--------ByBackwardConnectors------------");
		String currentSentence = currentStatement.getChildText("text");
		System.out.println(currentSentence);
		String currentId = currentBiologicalEntity.getAttributeValue("id").trim();
		String currentName = currentBiologicalEntity.getAttributeValue("name").trim();
		String currentOriginalName = currentBiologicalEntity.getAttributeValue("name_original").trim();
		String currentConstraint = currentBiologicalEntity.getAttributeValue("constraint");
		currentConstraint = currentConstraint == null ? "" : currentConstraint.trim();
		String parentStructure= null;
		if (currentId.equals("o644"))
			System.out.println(currentId);
		
		for(Element statement : context) {
			Element parent = null;
			//parent = findParentHasRelationWith(statement,currentId);
			parent= findParentHasConstraintWith(statement,currentId);
			int nameOccurence=this.getOccurencesNum(currentBiologicalEntity, statement);
			if(nameOccurence==-1) break;
			
			if(parent == null) {
				parentStructure = findParentConnectedByBackwardKeyWords(statement, nameOccurence, currentBiologicalEntity,context);
				if (parentStructure==null){
					break;
				}
				else if (parentStructure.matches("^o\\d+_?\\d?$"))
					parent = getBiologicalEntityWithIdFromContext(context,parentStructure);
				else{
					currentConstraint = (parentStructure + " " + currentConstraint).trim();								
					if(knowsPartOf.isPartOf(currentName, currentConstraint)){
						this.appendInferredConstraint(currentBiologicalEntity, parentStructure);
						currentBiologicalEntity.setAttribute("constraint", currentConstraint);
						System.out.println(currentConstraint);
						return true;
					}
					
				}
					
			}


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
				
				
				if(knowsPartOf.isPartOf(currentName, termNormalized)||knowsPartOf.isPartOf(currentName, nameSplited)||knowsPartOf.isPartOf(currentName, constraintName)){
					if(!inferredConstraint.equals("")||!RemoveNonSpecificBiologicalEntities.isNonSpecificPart(constraintName)||RemoveNonSpecificBiologicalEntities.isPartOfAConstraint(constraintName, constraintCon)) {
						parentStructure  = collapseBiologicalEntityToName.collapse(parent);
						if(parentStructure != null) {
							currentConstraint = (parentStructure + " " + currentConstraint).trim();								
//							this.appendInferredConstraint(currentBiologicalEntity, parentStructure);
							currentBiologicalEntity.setAttribute("constraint", currentConstraint);
							System.out.println(currentConstraint);
							return true;
						}
					}
					else {
						Element constraintStatement = this.getStatementWithIdFromContext(context, parent.getAttributeValue("id"));
						Run run =new Run();
						boolean foundParent = true;
						foundParent = run.runTransformer(transformers,constraintStatement, parent,wholeStatements);
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
		}
		return false;
	}
	
		
	
	
	
	private Element findParentHasConstraintWith(Element statement,
			String currentId) {
		Element parent = null;
		for (Element biologicalEntity: statement.getChildren("biological_entity")){
			for (Element character: biologicalEntity.getChildren("character")){
				String constraint = character.getAttributeValue("constraint");
				constraint = constraint == null ? "" : constraint.trim();
				if(constraint.startsWith("with")){
					if(currentId.equals(character.getAttributeValue("constraintid"))){
						parent=biologicalEntity;
								//collapseBiologicalEntityToName.collapse(biologicalEntity);
						return parent;
					}
				}

			}

		}
		return null;
	}

	private String findParentConnectedByBackwardKeyWords(Element statement, int nameOccurence, Element biologicalEntity,List<Element> context) {
		
		
		
		/*if(name.equals("margin") && sentence.startsWith(("annulus superous, thin, membranous, simple, up to 1.8 cm broad, supramedian").toLowerCase())) {
			System.out.println();
		}*/
		String parent=null;;
		String sentence = statement.getChild("text").getValue().toLowerCase();
		String currentOriginalName = biologicalEntity.getAttributeValue("name_original").trim();
		sentence = parenthesisRemover.remove(sentence, '(', ')');
		List<Token> terms = tokenizer.tokenize(sentence);
		int termPosition = indexOf(terms, new Token(currentOriginalName), nameOccurence) - 1;
		if(termPosition == -1){
			return null;
		}
		else{
			int k =0;
			int j =0; 
			for(; termPosition >= 0; termPosition--) {
				String previousTerm = terms.get(termPosition).getContent();
				String secondPreviousTerm = null;
				k++;
				j++;

				if(previousTerm.matches("\\p{Punct}")) {
					k++;
					if(k==3) return null;
				}
				// compound terms appearing before the non-specific structure terms 
				if(isBiologicalEntity(previousTerm,context)) {
					if (termPosition>=1 && RemoveNonSpecificBiologicalEntities.isNonSpecificPart(previousTerm)){
					     secondPreviousTerm = terms.get(termPosition-1).getContent();
					     if(!RemoveNonSpecificBiologicalEntities.isNonSpecificPart(secondPreviousTerm)){
					    	 if(RemoveNonSpecificBiologicalEntities.isPartOfAConstraint(previousTerm, secondPreviousTerm)){
					    		 return parent = inflector.getSingular(secondPreviousTerm);
					    	 }
					     }
					}
					else{
						if(j<=2){
							biologicalEntity = findBiologicalEntityByNameInStatement(previousTerm, statement);
							if(biologicalEntity!=null){
								String id = biologicalEntity.getAttributeValue("id").trim();
								return id;
							}
						}
					}
					return null;
				}
				
				if(isConnectBackwardToParentKeyWord(previousTerm)) {
					return findPreceedingParentInSentence(statement, termPosition,currentOriginalName,context);
				}
			}
		    return null;
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

	private String findPreceedingParentInSentence(Element statement, int connectorPosition, String term, List<Element> context) {
		String sentence = statement.getChild("text").getValue().toLowerCase();
		sentence = parenthesisRemover.remove(sentence, '(', ')');
		List<Token> terms = tokenizer.tokenize(sentence);
		Element biologicalEntity = null;
		
		int k=0;
		
		for(; connectorPosition >= 0; connectorPosition--) {
			String previousTerm = terms.get(connectorPosition).getContent();
			if(previousTerm.matches("\\p{Punct}")) {
				return null;
			}
			if(isBiologicalEntity(previousTerm,context)){
				biologicalEntity = findBiologicalEntityByNameInStatement(previousTerm, statement);
				String id = biologicalEntity.getAttributeValue("id").trim();
				return id;
			}
		}
		return null;
	}
	
	private boolean isConnectBackwardToParentKeyWord(String term) {
		return term.matches(connectBackwardToParent);
	}

	@Override
	public void transformAll(Element statement, Element biologicalEntity,
			List<Element> wholeStatement) {
		// TODO Auto-generated method stub
		
	}

}
