import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Minimal HTTP client for the Gemini API, with no external JSON library. */
public class GeminiClient {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent";
    private static final int MAX_ATTEMPTS = 3;

    private String apiKey;

    public GeminiClient(String apiKey) {
        this.apiKey = apiKey;
    }

    /** Sends the prompt and returns the text of the first candidate answer. */
    public String ask(String prompt) throws Exception {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new Exception("Missing AI API key. Enter it in the start screen "
                    + "or set the GEMINI_API_KEY environment variable.");
        }

        String requestBody = buildBody(prompt);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .timeout(Duration.ofSeconds(45))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return extractText(response.body());
            }

            // 503 means the model is temporarily overloaded: wait a bit and retry.
            if (response.statusCode() == 503 && attempt < MAX_ATTEMPTS) {
                Thread.sleep(2000);
                continue;
            }

            throw new Exception("The AI responded with error " + response.statusCode() + ": " + response.body());
        }

        throw new Exception("The AI responded with error " + response.statusCode() + ": " + response.body());
    }

    /** Builds the JSON request body, escaping the prompt by hand. */
    private String buildBody(String prompt) {
        String escapedPrompt = prompt.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        return "{\"contents\":[{\"parts\":[{\"text\":\"" + escapedPrompt + "\"}]}]}";
    }

    /** Extracts the first "text" field from the JSON response, decoding escape sequences. */
    private String extractText(String jsonResponse) {
        String extractedText = null;
        int start = jsonResponse.indexOf("\"text\":");

        if (start != -1) {
            start = jsonResponse.indexOf("\"", start + 7) + 1;

            StringBuilder text = new StringBuilder();
            boolean escaping = false;
            boolean finished = false;
            int i = start;

            while (i < jsonResponse.length() && !finished) {
                char c = jsonResponse.charAt(i);

                if (escaping) {
                    if (c == 'n') {
                        text.append('\n');
                    } else {
                        text.append(c);
                    }
                    escaping = false;
                } else if (c == '\\') {
                    escaping = true;
                } else if (c == '"') {
                    finished = true;
                } else {
                    text.append(c);
                }

                i++;
            }

            extractedText = text.toString();
        }

        return extractedText;
    }
}
