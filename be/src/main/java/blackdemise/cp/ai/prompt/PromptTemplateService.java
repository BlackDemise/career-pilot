package blackdemise.cp.ai.prompt;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

// Loads prompt templates from classpath resources (prompts/<name>.txt), one file per workflow,
// and substitutes {{variable}} placeholders. Never hardcode prompt text in Java code.
@Service
public class PromptTemplateService {

    private static final String TEMPLATE_PATH_FORMAT = "prompts/%s.txt";

    public String render(String templateName, Map<String, String> variables) {
        String rendered = load(templateName);
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return rendered;
    }

    public String load(String templateName) {
        ClassPathResource resource = new ClassPathResource(TEMPLATE_PATH_FORMAT.formatted(templateName));
        if (!resource.exists()) {
            throw new IllegalArgumentException("Prompt template not found: " + templateName);
        }
        try (InputStream in = resource.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to load prompt template: " + templateName, ex);
        }
    }
}
