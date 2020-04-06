package de.samply.icd2fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.CodeSystem;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    private static String sourceFile, outputPath;

    /**
     * @param args source output
     */
    public static void main(String[] args) throws FileNotFoundException {
        try {
            sourceFile = args[0];
            outputPath = args[1];
        } catch (ArrayIndexOutOfBoundsException e) {
            Logger.getAnonymousLogger().log(Level.SEVERE, "source file or output path not provided");
        }
        System.out.println("source: " + sourceFile);
        System.out.println("output: " + outputPath);


        // https://hapifhir.io/hapi-fhir/docs/model/parsers.html

        IParser parser = FhirContext.forR4().newJsonParser();
        CodeSystem parsed = parser.parseResource(CodeSystem.class, new FileReader(sourceFile));

        System.out.println(parsed.getConcept().get(4).getCode());

    }


}
