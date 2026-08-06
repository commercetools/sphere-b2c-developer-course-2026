package com.lifestylehomecorp.project.api;

import com.lifestylehomecorp.platform.annotations.TaskDescription;
import com.lifestylehomecorp.project.api.dto.ProjectView;
import com.lifestylehomecorp.project.application.ProjectService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The worked reference endpoint. {@code GET /api/project} returns the Project's key, name,
 * currencies, languages, and countries — the result of a real commercetools call carried up
 * through application and domain to this view.
 */
@RestController
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @TaskDescription(
            module = "project", session = "Session 1", taskNumber = 1,
            title = "Project settings", tier = "T1", capability = "project.info",
            description = "Implement one repository method — CtProjectRepository.fetch() in the project "
                    + "module's infrastructure layer — returning the raw SDK Project. "
                    + "ProjectService maps it to the domain so GET /api/project exposes the project's key, "
                    + "name, currencies, languages and countries — the settings the storefront reads to "
                    + "drive its locale and currency switchers.",
            hint = "Docs: Project settings — the currencies / languages / countries a project supports "
                    + "(docs.commercetools.com/api/projects/project).")
    @GetMapping("/api/project")
    public ProjectView getProject() {
        return ProjectViewMapper.toView(projectService.getProjectSummary());
    }
}
