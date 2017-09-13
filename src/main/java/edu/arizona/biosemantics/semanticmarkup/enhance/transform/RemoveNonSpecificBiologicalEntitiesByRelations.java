package edu.arizona.biosemantics.semanticmarkup.enhance.transform;

import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;

import edu.arizona.biosemantics.common.ling.transform.ITokenizer;
import edu.arizona.biosemantics.semanticmarkup.enhance.know.KnowsPartOf;
import edu.arizona.biosemantics.semanticmarkup.enhance.know.KnowsSynonyms;
import edu.arizona.biosemantics.semanticmarkup.enhance.run.Run;

public class RemoveNonSpecificBiologicalEntitiesByRelations extends RemoveNonSpecificBiologicalEntities {

	private CollapseBiologicalEntityToName collapseBiologicalEntityToName;

	public RemoveNonSpecificBiologicalEntitiesByRelations(
			KnowsPartOf knowsPartOf, KnowsSynonyms knowsSynonyms, ITokenizer tokenizer,
			CollapseBiologicalEntityToName collapseBiologicalEntityToName) {
		super(knowsPartOf, knowsSynonyms, tokenizer);
		this.collapseBiologicalEntityToName = collapseBiologicalEntityToName;
	}

	@Override
	public boolean transform(List<AbstractTransformer> transformers,Element currentStatement, Element biologicalEntity,List<Element> context, List<Element> wholeStatements) {
		System.out.println("---------Relation-----------");
		String sentence = currentStatement.getChildText("text");
		System.out.println(sentence);
		String currentId = biologicalEntity.getAttributeValue("id").trim();
		String currentName = biologicalEntity.getAttributeValue("name").trim();

		String currentConstraint = biologicalEntity.getAttributeValue("constraint");
		currentConstraint = currentConstraint == null ? "" : currentConstraint.trim();
		if (currentId.equals("o17"))
			System.out.println(currentId);

		for (Element statement : context){
			for(Element relation : statement.getChildren("relation")) {
				if(relation.getAttributeValue("name").equals("part_of")) { //||relation.getAttributeValue("name").equals("with")
					Element toBiologicalEntity = this.getBiologicalEntityWithIdFromContext(context, relation.getAttributeValue("to"));
					Element fromBiologicalEntity = this.getBiologicalEntityWithIdFromContext(context,relation.getAttributeValue("from"));
					if(toBiologicalEntity!=null&&fromBiologicalEntity!=null){

						String name = fromBiologicalEntity.getAttributeValue("name");
						String id = fromBiologicalEntity.getAttributeValue("id").trim();
						name = name == null ? "" : name.trim();

						String constraintName = toBiologicalEntity.getAttributeValue("name");
						constraintName = constraintName == null ? "" : constraintName.trim();

						String constraintCon = toBiologicalEntity.getAttributeValue("constraint");
						constraintCon = constraintCon == null ? "" : constraintCon.trim();
						
						String inferredConstraint = toBiologicalEntity.getAttributeValue("inferred_constraint");
						inferredConstraint = inferredConstraint == null ? "" : inferredConstraint.trim();


						if (id.equals(currentId)){
							System.out.println(id);
							if (id.equals("o549"))
								System.out.println(id);

							String parent = null;
							String termNormalized = collapseBiologicalEntityToName.collapse(toBiologicalEntity);
							String nameNormalized = collapseBiologicalEntityToName.collapse(fromBiologicalEntity);
							String nameSplited = termNormalized.replace(inferredConstraint,"").trim();
							
							if(!inferredConstraint.equals("")||knowsPartOf.isPartOf(name, constraintName)||knowsPartOf.isPartOf(name, termNormalized)||knowsPartOf.isPartOf(nameNormalized, constraintName)||knowsPartOf.isPartOf(nameNormalized, termNormalized)||knowsPartOf.isPartOf(name, nameSplited)){
								if(!RemoveNonSpecificBiologicalEntities.isNonSpecificPart(constraintName)||RemoveNonSpecificBiologicalEntities.isPartOfAConstraint(constraintName, constraintCon)) {
									parent = collapseBiologicalEntityToName.collapse(toBiologicalEntity);
									if(parent != null) {
										currentConstraint = (parent + " " + currentConstraint).trim();								
										this.appendInferredConstraint(biologicalEntity, parent);
										biologicalEntity.setAttribute("constraint", currentConstraint);
										System.out.println(currentConstraint);
										return true;
									}
								}
								else {
									Element constraintStatement = this.getStatementWithIdFromContext(context, toBiologicalEntity.getAttributeValue("id"));
									Run run =new Run();
									boolean foundParent = true;
									foundParent = run.runTransformer(transformers,constraintStatement, toBiologicalEntity,wholeStatements);
									if(foundParent){
										parent = collapseBiologicalEntityToName.collapse(toBiologicalEntity);
										if(parent != null) {
											currentConstraint = (parent + " " + currentConstraint).trim();								
											this.appendInferredConstraint(biologicalEntity, parent);
											biologicalEntity.setAttribute("constraint", currentConstraint);
											System.out.println(currentConstraint);
											return true;
										}
									}
									else return false;
									
								}
							}
						}
					}
				}
			}
		}
		return false;
	}	
						
						
							
						
					
//						if(!isPartOfAConstraint(name, constraint)) {		
//							if(name.equals("margin") && sentence.startsWith("the flagellum, mandibles, anterior margin of the face, and the head".toLowerCase())) {
//								System.out.println();
//							}
							


			
		
	



	@Override
	public void transformAll(Element statement, Element biologicalEntity,
			List<Element> wholeStatement) {
		// TODO Auto-generated method stub
		
	}


}
