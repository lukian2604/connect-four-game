import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Minimal HTTP client for the Gemini API, with no external JSON library. */
public class ClienteAI {

    private static final String URL_BASE = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent";
    private static final int TENTATIVI_MASSIMI = 3;

    private String apiKey;

    public ClienteAI(String apiKey) {
        this.apiKey = apiKey;
    }

    /** Sends the prompt and returns the text of the first candidate answer. */
    public String chiedi(String prompt) throws Exception {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new Exception("Chiave API dell'AI mancante. Inseriscila nella schermata iniziale "
                    + "oppure nella variabile d'ambiente GEMINI_API_KEY.");
        }

        String corpoRichiesta = costruisciCorpo(prompt);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        HttpRequest richiesta = HttpRequest.newBuilder()
                .uri(URI.create(URL_BASE))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .timeout(Duration.ofSeconds(45))
                .POST(HttpRequest.BodyPublishers.ofString(corpoRichiesta))
                .build();

        HttpResponse<String> risposta = null;

        for (int tentativo = 1; tentativo <= TENTATIVI_MASSIMI; tentativo++) {
            risposta = client.send(richiesta, HttpResponse.BodyHandlers.ofString());

            if (risposta.statusCode() == 200) {
                return estraiTesto(risposta.body());
            }

            // 503 means the model is temporarily overloaded: wait a bit and retry.
            if (risposta.statusCode() == 503 && tentativo < TENTATIVI_MASSIMI) {
                Thread.sleep(2000);
                continue;
            }

            throw new Exception("L'AI ha risposto con errore " + risposta.statusCode() + ": " + risposta.body());
        }

        throw new Exception("L'AI ha risposto con errore " + risposta.statusCode() + ": " + risposta.body());
    }

    /** Builds the JSON request body, escaping the prompt by hand. */
    private String costruisciCorpo(String prompt) {
        String promptEscapato = prompt.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        return "{\"contents\":[{\"parts\":[{\"text\":\"" + promptEscapato + "\"}]}]}";
    }

    /** Extracts the first "text" field from the JSON response, decoding escape sequences. */
    private String estraiTesto(String jsonRisposta) {
        String testoEstratto = null;
        int inizio = jsonRisposta.indexOf("\"text\":");

        if (inizio != -1) {
            inizio = jsonRisposta.indexOf("\"", inizio + 7) + 1;

            StringBuilder testo = new StringBuilder();
            boolean carattereEscape = false;
            boolean terminato = false;
            int i = inizio;

            while (i < jsonRisposta.length() && !terminato) {
                char c = jsonRisposta.charAt(i);

                if (carattereEscape) {
                    if (c == 'n') {
                        testo.append('\n');
                    } else {
                        testo.append(c);
                    }
                    carattereEscape = false;
                } else if (c == '\\') {
                    carattereEscape = true;
                } else if (c == '"') {
                    terminato = true;
                } else {
                    testo.append(c);
                }

                i++;
            }

            testoEstratto = testo.toString();
        }

        return testoEstratto;
    }
}
