package net.pkhapps.idispatch.server.ui;

import net.pkhapps.idispatch.server.entity.*;
import net.pkhapps.idispatch.server.entity.repository.*;
import org.springframework.boot.test.context.TestComponent;

import java.util.UUID;

/**
 * Creates test fixtures directly through the repositories, bypassing the secured services. All services use
 * {@code REQUIRES_NEW} transactions, so tests cannot roll back their changes; instead, every fixture gets a unique
 * name so that tests sharing the same application context do not interfere with each other.
 */
@TestComponent
public class TestData {

    private final MunicipalityRepository municipalityRepository;
    private final AssignmentTypeRepository assignmentTypeRepository;
    private final ResourceTypeRepository resourceTypeRepository;
    private final ResourceRepository resourceRepository;
    private final ResourceStatusRepository resourceStatusRepository;

    TestData(MunicipalityRepository municipalityRepository,
             AssignmentTypeRepository assignmentTypeRepository,
             ResourceTypeRepository resourceTypeRepository,
             ResourceRepository resourceRepository,
             ResourceStatusRepository resourceStatusRepository) {
        this.municipalityRepository = municipalityRepository;
        this.assignmentTypeRepository = assignmentTypeRepository;
        this.resourceTypeRepository = resourceTypeRepository;
        this.resourceRepository = resourceRepository;
        this.resourceStatusRepository = resourceStatusRepository;
    }

    /**
     * Returns a short string that is unique within the test run, prefixed with the given prefix.
     */
    public static String unique(String prefix) {
        return prefix + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public Municipality municipality(String name) {
        var municipality = new Municipality();
        municipality.setNameFi(name);
        return municipalityRepository.saveAndFlush(municipality);
    }

    public Municipality municipality(String nameFi, String nameSv) {
        var municipality = new Municipality();
        municipality.setNameFi(nameFi);
        municipality.setNameSv(nameSv);
        return municipalityRepository.saveAndFlush(municipality);
    }

    public Municipality deletedMunicipality(String name) {
        var municipality = new Municipality();
        municipality.setNameFi(name);
        municipality.setActive(false);
        return municipalityRepository.saveAndFlush(municipality);
    }

    public AssignmentType assignmentType(String code, String description) {
        var type = new AssignmentType();
        type.setCode(code);
        type.setDescriptionFi(description);
        return assignmentTypeRepository.saveAndFlush(type);
    }

    public AssignmentType assignmentType(String code, String descriptionFi, String descriptionSv) {
        var type = new AssignmentType();
        type.setCode(code);
        type.setDescriptionFi(descriptionFi);
        type.setDescriptionSv(descriptionSv);
        return assignmentTypeRepository.saveAndFlush(type);
    }

    public ResourceType resourceType(String code, String description) {
        var type = new ResourceType();
        type.setCode(code);
        type.setDescriptionFi(description);
        return resourceTypeRepository.saveAndFlush(type);
    }

    public ResourceType deletedResourceType(String code, String description) {
        var type = new ResourceType();
        type.setCode(code);
        type.setDescriptionFi(description);
        type.setActive(false);
        return resourceTypeRepository.saveAndFlush(type);
    }

    public Resource resource(String callSign, ResourceState initialState) {
        var resource = new Resource();
        resource.setCallSign(callSign);
        resource.setType(resourceType(unique("RT"), "Test resource type"));
        resource = resourceRepository.saveAndFlush(resource);
        resourceStatusRepository.saveAndFlush(new ResourceStatus(resource, initialState));
        return resource;
    }

    public ResourceStatus statusOf(Resource resource) {
        return resourceStatusRepository.findByResource(resource);
    }
}
