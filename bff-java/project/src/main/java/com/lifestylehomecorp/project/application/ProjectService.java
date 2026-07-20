package com.lifestylehomecorp.project.application;

import com.commercetools.api.models.project.Project;
import com.lifestylehomecorp.project.domain.ProjectSummary;
import org.springframework.stereotype.Service;

/**
 * Application use-case for the Project. The repository returns the raw SDK {@link Project}; this
 * service maps it to the domain {@link ProjectSummary} — SDK types stop here and never reach the
 * api layer.
 */
@Service
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public ProjectSummary getProjectSummary() {
        Project project = projectRepository.fetch();
        return new ProjectSummary(
                project.getKey(),
                project.getName(),
                project.getCurrencies(),
                project.getLanguages(),
                project.getCountries());
    }
}
