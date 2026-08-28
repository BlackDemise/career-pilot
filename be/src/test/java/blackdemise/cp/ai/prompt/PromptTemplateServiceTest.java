package blackdemise.cp.ai.prompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Test;

class PromptTemplateServiceTest {

    private final PromptTemplateService promptTemplateService = new PromptTemplateService();

    @Test
    void render_substitutesVariables_inChatSystemTemplate() {
        String rendered = promptTemplateService.render("chat-system",
                Map.of("user_profile", "Backend developer", "career_goal", "Become a tech lead"));

        assertThat(rendered)
                .contains("Backend developer")
                .contains("Become a tech lead")
                .doesNotContain("{{user_profile}}")
                .doesNotContain("{{career_goal}}");
    }

    @Test
    void load_throwsIllegalArgumentException_whenTemplateMissing() {
        assertThatThrownBy(() -> promptTemplateService.load("does-not-exist"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
