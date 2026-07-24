package com.lifestylehomecorp.app;

import com.commercetools.api.models.project.Project;
import com.lifestylehomecorp.catalog.application.ProductRepository;
import com.lifestylehomecorp.platform.errors.TaskNotImplementedException;
import com.lifestylehomecorp.project.application.ProjectRepository;
import com.lifestylehomecorp.training.config.ParticipantIdentity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Wiring smoke test for the full BFF. Runs fully offline (no SDK credentials): the worked slice is
 * verified with a mocked ProjectRepository (which returns a mocked SDK Project — the service maps
 * it), and the 501 mechanism is verified by mocking a repository to throw
 * {@link TaskNotImplementedException} and asserting the platform error advice maps it to 501. This
 * keeps the test independent of which tasks are actually implemented.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SmokeTest {

    @Autowired
    private MockMvc mockMvc;

    // Replace the SDK gateway so the worked slice runs offline. It now returns the raw SDK Project;
    // ProjectService maps it to the domain.
    @MockitoBean
    private ProjectRepository projectRepository;

    // Don't hit the live api-clients endpoint during tests; identity is fixed.
    @MockitoBean
    private ParticipantIdentity participantIdentity;

    // Stand in for the catalog SDK gateway so the 501 mechanism can be exercised offline: with the
    // repository stubbed to throw, GET /api/products surfaces 501 regardless of real credentials.
    @MockitoBean
    private ProductRepository productRepository;

    /** Simulate the day-1 stubbed state for the catalog reads. */
    private void stubUnimplementedCatalog() {
        given(productRepository.findAll(any())).willThrow(new TaskNotImplementedException("2.1"));
    }

    private void stubProject() {
        given(participantIdentity.id()).willReturn("test-participant");
        given(participantIdentity.name()).willReturn("Test Participant");
        Project project = mock(Project.class);
        given(project.getKey()).willReturn("lhc-project");
        given(project.getName()).willReturn("Lifestyle & Home Corp");
        given(project.getCurrencies()).willReturn(List.of("EUR"));
        given(project.getLanguages()).willReturn(List.of("en"));
        given(project.getCountries()).willReturn(List.of("DE"));
        given(projectRepository.fetch()).willReturn(project);
    }

    @Test
    void projectEndpointReturns200WithProjectKey() throws Exception {
        stubProject();
        mockMvc.perform(get("/api/project"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value("lhc-project"));
    }

    @Test
    void unimplementedCatalogEndpointReturns501() throws Exception {
        stubUnimplementedCatalog();
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    void taskTrackingReflectsImplementedVsStubbed() throws Exception {
        stubProject();
        stubUnimplementedCatalog();

        mockMvc.perform(get("/api/training/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.module=='project')]").exists())
                .andExpect(jsonPath("$[?(@.module=='catalog')]").exists());

        mockMvc.perform(get("/api/project")).andExpect(status().isOk());
        mockMvc.perform(get("/api/products")).andExpect(status().isNotImplemented());

        mockMvc.perform(get("/api/training/progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perTask['project.Session 1.1']").value(true))
                .andExpect(jsonPath("$.perTask['catalog.Session 2.1']").value(false));

        mockMvc.perform(get("/api/training/capabilities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unlocked").value(org.hamcrest.Matchers.hasItem("project.info")))
                .andExpect(jsonPath("$.unlocked").value(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("catalog.plp"))));
    }
}
