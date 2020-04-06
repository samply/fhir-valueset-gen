package de.samply.fhir.valueset.gen.icd;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.ValueSet;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Extractor {

    private CodeSystem codeSystem;
    private static String CATEGORY_PATTERN = "[A-Z][0-9][0-9]";
    private static String SUB_CATEGORY_PATTERN_SUFFIX = "[.][0-9]";

    public Extractor(Reader reader) {
        IParser parser = FhirContext.forR4().newJsonParser();
        codeSystem = parser.parseResource(CodeSystem.class, reader);
    }

    public List<ValueSet> generate() {
        return addSubCategories(generateEmptyValueSets());
    }


    private List<ValueSet> addSubCategories(List<ValueSet> emptyValueSets) {
        for (CodeSystem.ConceptDefinitionComponent subCategory : getSubCategories()) {
            ValueSet valueSet = findValueSet(emptyValueSets, getCategoryBySubCategory(subCategory.getCode()));
            if (valueSet == null) {
                throw new IllegalStateException("ValueSet of ICD category should exist, no one found for: " + subCategory.getCode());
            }
            valueSet.getExpansion().addContains(createContains(subCategory.getCode(), subCategory.getDisplay()));
        }
        return emptyValueSets;
    }

    public String getCategoryBySubCategory(String subCategoryCode) {
        return subCategoryCode.replaceFirst(SUB_CATEGORY_PATTERN_SUFFIX, "");
    }


    private List<ValueSet> generateEmptyValueSets() {
        List<ValueSet> generatedValueSets = new ArrayList<>();
        for (CodeSystem.ConceptDefinitionComponent category : getCategories()) {
            ValueSet valueSet = createValueSet(category.getCode(), category.getDisplay());
            generatedValueSets.add(valueSet);
        }
        return generatedValueSets;
    }


    private ValueSet findValueSet(List<ValueSet> valueSetList, String code) {
        for (ValueSet valueSet : valueSetList) {
            if (valueSet.getId().equals(code)) {
                return valueSet;
            }
        }
        return null;
    }

    private List<CodeSystem.ConceptDefinitionComponent> getCategories() {
        return codeSystem.getConcept().stream().filter(c -> Pattern.matches(CATEGORY_PATTERN, c.getCode())).collect(Collectors.toList());
    }

    private List<CodeSystem.ConceptDefinitionComponent> getSubCategories() {
        return codeSystem.getConcept().stream().filter(c -> Pattern.matches(CATEGORY_PATTERN + SUB_CATEGORY_PATTERN_SUFFIX, c.getCode())).collect(Collectors.toList());
    }


    private ValueSet createValueSet(String code, String title) {
        ValueSet valueSet = new ValueSet()
                .setTitle(title)
                .setName(code)
                .setUrl(codeSystem.getUrl() + "/" + code)
                .setStatus(Enumerations.PublicationStatus.DRAFT);
        valueSet.setId(code);

        // compose
        ValueSet.ConceptSetFilterComponent conceptSetFilterComponent = new ValueSet.ConceptSetFilterComponent()
                .setOp(ValueSet.FilterOperator.ISA)
                .setProperty("parent")
                .setValue(code);
        ValueSet.ConceptSetComponent conceptSetComponent = new ValueSet.ConceptSetComponent()
                .setSystem(codeSystem.getUrl())
                .setVersion(codeSystem.getVersion())
                .addFilter(conceptSetFilterComponent);
        ValueSet.ValueSetComposeComponent valueSetComposeComponent = new ValueSet.ValueSetComposeComponent()
                .addInclude(conceptSetComponent);
        valueSet.setCompose(valueSetComposeComponent);

        //empty expansion
        valueSet.getExpansion().addContains(new ValueSet.ValueSetExpansionContainsComponent());

        return valueSet;
    }

    private ValueSet.ValueSetExpansionContainsComponent createContains(String code, String display) {
        ValueSet.ValueSetExpansionContainsComponent valueSetExpansionContainsComponent = new ValueSet.ValueSetExpansionContainsComponent()
                .setSystem(codeSystem.getUrl())
                .setVersion(codeSystem.getVersion())
                .setCode(code)
                .setDisplay(display);
        return valueSetExpansionContainsComponent;
    }
}
