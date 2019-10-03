package hot.fhir.owl.server;

import ca.uhn.fhir.model.primitive.StringDt;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import hot.fhirowl.loader.impl.StatoTransformer;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.obolibrary.robot.IOHelper;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class InMemoryOWLCodeSystemResourceProvider implements IResourceProvider {
    private Map<String, CodeSystem> codeSystems = new HashMap<>();
    private final Logger logger = LoggerFactory.getLogger(InMemoryOWLCodeSystemResourceProvider.class);

    public InMemoryOWLCodeSystemResourceProvider() {
        // TODO: Change this to load dynamically
        String id = "stato";
        String url = "http://purl.obolibrary.org/obo/stato.owl";
        try {
            Instant start = Instant.now();
            OWLOntology ontology = new IOHelper().loadOntology(IRI.create(url));
            Instant step1 = Instant.now();
            logger.info("Total execution time for loading ontology: {}", Duration.between(start, step1).toMillis());
            StatoTransformer transformer = new StatoTransformer();
            CodeSystem codeSystem = transformer.transformToR4(ontology);
            codeSystem.setId(id);
            codeSystem.setUrl(url);
            codeSystems.put(id, codeSystem);
            Instant finish = Instant.now();
            logger.info("Total execution time for transforming ontology: {}", Duration.between(step1, finish).toMillis());
        } catch (IOException usex) {
            System.err.println("Failed to load ontology");
        }
    }

    @Override
    public Class<? extends IBaseResource> getResourceType() {
        return CodeSystem.class;
    }

    @Read
    public CodeSystem read(@IdParam IdType theId) {
        CodeSystem cs = codeSystems.get(theId.getIdPart());
        if (cs == null) {
            throw new ResourceNotFoundException(theId);
        }
        return cs;
    }

    @Search
    public List<CodeSystem> findCodeSystemByCode(@RequiredParam(name = "title") StringDt title) {
        LinkedList<CodeSystem> list = new LinkedList<>();
        for (CodeSystem codeSystem: codeSystems.values()) {
            if (codeSystem.getTitle().toLowerCase().contains(title.toString().toLowerCase())) {
                list.add(codeSystem);
            }
        }
        return list;
    }
}
