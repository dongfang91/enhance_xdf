package edu.arizona.biosemantics.semanticmarkup.enhance.transform;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;

import edu.arizona.biosemantics.common.ling.Token;
import edu.arizona.biosemantics.common.ling.transform.ITokenizer;
import edu.arizona.biosemantics.semanticmarkup.enhance.know.KnowsPartOf;
import edu.arizona.biosemantics.semanticmarkup.enhance.know.KnowsSynonyms;
import edu.arizona.biosemantics.semanticmarkup.enhance.run.Run;

public class RemoveNonSpecificBiologicalEntitiesByForwardConnectors extends RemoveNonSpecificBiologicalEntities {

	private String connectForwardToParent = "in|on|of|at";
	private CollapseBiologicalEntityToName collapseBiologicalEntityToName;
	
	public RemoveNonSpecificBiologicalEntitiesByForwardConnectors(
			KnowsPartOf knowsPartOf, KnowsSynonyms knowsSynonyms, ITokenizer tokenizer,
			CollapseBiologicalEntityToName collapseBiologicalEntityToName) {
		super(knowsPartOf, knowsSynonyms, tokenizer);
		this.collapseBiologicalEntityToName = collapseBiologicalEntityToName;
	}
	
	@Override
	public boolean transform(List<AbstractTransformer> transformers,Element currentStatement, Element currentBiologicalEntity,List<Element> context, List<Element> wholeStatements) {
		System.out.println("---------ForwardConnectors-----------");
		String currentSentence = currentStatement.getChildText("text");
		System.out.println(currentSentence);
		String currentId = currentBiologicalEntity.getAttributeValue("id").trim();
		String currentName = currentBiologicalEntity.getAttributeValue("name").trim();
		String currentOriginalName = currentBiologicalEntity.getAttributeValue("name_original").trim();
		String currentConstraint = currentBiologicalEntity.getAttributeValue("constraint");
		currentConstraint = currentConstraint == null ? "" : currentConstraint.trim();
		String parentStructure= null;
		if (currentId.equals("o608"))
			System.out.println(currentId);

		for(Element statement : context) {
			Element parent = null;
			//parent = findParentHasRelationWith(statement,currentId);
			int nameOccurence=this.getOccurencesNum(currentBiologicalEntity, statement);
			if(nameOccurence==-1) break;
			parent = findParentConnectedByForwardKeyWords(statement, nameOccurence, currentBiologicalEntity,context);

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
				
				if(knowsPartOf.isPartOf(currentName, termNormalized)||knowsPartOf.isPartOf(currentName,nameSplited)||knowsPartOf.isPartOf(currentName,constraintName)){
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
						else return foundParent;
							
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
//					Element constraintStatement = this.getStatementWithIdFromContext(context, parent.getAttributeValue("id"));
//					Run run =new Run();
//					return run.runTransformer(transformers,constraintStatement, parent,wholeStatements);
//				}
			}
		}
		return false;
	}
	
	private Element findParentConnectedByForwardKeyWords(Element statement, int nameOccurence, Element biologicalEntity, List<Element> context) {
		
		/*if(name.equals("side") && sentence.startsWith("breast, sides, and flanks pinkish brown contrasting with white belly and sides of rump;")) {
			System.out.println();
		}*/
		Element parent=null;;
		String sentence = statement.getChild("text").getValue().toLowerCase();
		String currentOriginalName = biologicalEntity.getAttributeValue("name_original").trim();
		sentence = parenthesisRemover.remove(sentence, '(', ')');
		List<Token> terms = tokenizer.tokenize(sentence);
		int termPosition = indexOf(terms, new Token(currentOriginalName), nameOccurence) - 1;
		
		if(termPosition <= -1){
			return null;
		}
		termPosition = termPosition +2;
		for(; termPosition < terms.size() - 1; termPosition++) {
			String nextTerm = terms.get(termPosition).getContent();
			if(nextTerm.matches("\\p{Punct}")) {
				return null;
			}
			if(isBiologicalEntity(nextTerm,context)) {
				return null;
			}
			if(isConnectForwardToParentKeyWord(nextTerm)) {
				return findFollowingParentInSentence(statement, termPosition,currentOriginalName,context);
			}
		}
		return null;
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
	
	private Element findBiologicalEntityByNameInStatementWithPosition(List<Token> terms,int position, String term, Element statement) {
		int i=0;
		for(Element biologicalEntity : statement.getChildren("biological_entity")) {
			
			if(biologicalEntity.getAttributeValue("name_original").equals(term)) {
				i = i+1;
				int termPosition = indexOf(terms, new Token(term), i);
				if (termPosition == position)
					return biologicalEntity;
			}
		}
		return null;
	}

	private Element findFollowingParentInSentence(Element statement, int connectorPosition, String term, List<Element> context) {
		String sentence = statement.getChild("text").getValue().toLowerCase();
		sentence = parenthesisRemover.remove(sentence, '(', ')');
		List<Token> terms = tokenizer.tokenize(sentence);
		Element biologicalEntity = null;
		for(int termPosition = connectorPosition + 1; termPosition < terms.size(); termPosition ++) {
			String followingTerm = terms.get(termPosition).getContent();
			if(followingTerm.matches("\\p{Punct}")) {
				return null;
			}
			
			if(isBiologicalEntity(followingTerm,context)){
				biologicalEntity = findBiologicalEntityByNameInStatementWithPosition(terms,termPosition,followingTerm, statement);
				return biologicalEntity;
			}
		}
		return null;
	}

	private boolean isConnectForwardToParentKeyWord(String term) {
		return term.matches(connectForwardToParent);
	}

	@Override
	public void transformAll(Element statement, Element biologicalEntity,
			List<Element> wholeStatement) {
		// TODO Auto-generated method stub
		
	}

}
