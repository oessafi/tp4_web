package com.tp4_omaressafi.tpwebomaressafi.jsf;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.model.SelectItem;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.Part; // Important pour l'upload
import com.tp4_omaressafi.tpwebomaressafi.service.LlmClient;

// Import de LlmClientTavily supprimé
import com.tp4_omaressafi.tpwebomaressafi.service.LlmClientRag; // Import du nouveau client
import com.tp4_omaressafi.tpwebomaressafi.service.MagasinEmbeddings; // Import du service RAG

import java.io.InputStream; // Important pour l'upload
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named("bb") // Explicite le nom du bean
@ViewScoped
public class Bb implements Serializable {

    private String roleSysteme;
    private boolean roleSystemeChangeable = true;
    private List<SelectItem> listeRolesSysteme;
    private String question;
    private String reponse;
    private StringBuilder conversation = new StringBuilder();

    // --- CHAMPS POUR L'UPLOAD (Optionnel TP) ---
    @Inject
    private MagasinEmbeddings magasinEmbeddings; // Injection du service RAG

    private Part fichier; // Pour h:inputFile
    private String messagePourChargementFichier;
    // --- FIN DES CHAMPS UPLOAD ---

    @Inject
    private FacesContext facesContext;


    @Inject
    private LlmClientRag llmRag; // Par le nouveau client
    // ========================


    @Inject
    private LlmClient llmClient;

    public Bb() {}

    // ... (Getters et Setters existants) ...
    public String getRoleSysteme() { return roleSysteme; }
    public void setRoleSysteme(String roleSysteme) { this.roleSysteme = roleSysteme; }

    public boolean isRoleSystemeChangeable() { return roleSystemeChangeable; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getReponse() { return reponse; }
    public void setReponse(String reponse) { this.reponse = reponse; }

    public String getConversation() { return conversation.toString(); }
    public void setConversation(String conversation) { this.conversation = new StringBuilder(conversation); }

    // --- NOUVEAUX GETTERS/SETTERS POUR L'UPLOAD ---
    public Part getFichier() {
        return fichier;
    }

    public void setFichier(Part fichier) {
        this.fichier = fichier;
    }

    public String getMessagePourChargementFichier() {
        return messagePourChargementFichier;
    }
    // --- FIN NOUVEAUX GETTERS/SETTERS ---

    public String envoyer() {
        if (question == null || question.isBlank()) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Texte question vide", "Il manque le texte de la question");
            facesContext.addMessage(null, msg);
            return null;
        }

        try {
            if (conversation.isEmpty()) {
                llmClient.setSystemRole(roleSysteme);
                this.roleSystemeChangeable = false;
            }

            // === MODIFICATION ICI ===
            // Client RAG + Web (Test 5) qui utilise le magasin centralisé
            // this.reponse = llmTavily.ask(question); // Remplacé
            this.reponse = llmRag.ask(question); // Par le nouveau client
            // ========================


            // Pour le test 2 (Routage - Test 3)
            //this.reponse = llmRoutage.ask(question);

            afficherConversation();

        } catch (Exception e) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Erreur API", e.getMessage());
            facesContext.addMessage(null, msg);
            e.printStackTrace(); // Utile pour le débug
        }
        return null;
    }

    public String nouveauChat() {
        return "index"; // réinitialise la vue
    }

    private void afficherConversation() {
        this.conversation.append("== User:\n").append(question)
                .append("\n== Gemini:\n").append(reponse).append("\n\n");
    }

    // --- NOUVELLE MÉTHODE UPLOAD (Optionnel TP) ---
    /**
     * Téléchargement de fichier PDF qui est ajouté dans la base de données vectorielle.
     */
    public void upload() {
        if (this.fichier == null) {
            this.messagePourChargementFichier = "Erreur : Aucun fichier sélectionné.";
            return;
        }

        String nomFichier = fichier.getSubmittedFileName();

        // getSubmittedFileName() retourne le nom du fichier sur le disque du client.
        if (nomFichier != null && !nomFichier.isBlank() && nomFichier.toLowerCase().endsWith(".pdf")) {
            try (InputStream inputStream = fichier.getInputStream()) {

                // Appeler le service centralisé pour ajouter le document
                magasinEmbeddings.ajouterDocument(inputStream, nomFichier);

                this.messagePourChargementFichier = "Fichier '" + nomFichier + "' chargé avec succès dans le RAG.";

            } catch (Exception e) {
                this.messagePourChargementFichier = "Erreur lors du chargement : " + e.getMessage();
                e.printStackTrace();
            }
        } else {
            this.messagePourChargementFichier = "Erreur : Veuillez sélectionner un fichier PDF valide.";
        }
    }
    // --- FIN NOUVELLE MÉTHODE ---


    public List<SelectItem> getRolesSysteme() {
        if (this.listeRolesSysteme == null) {
            this.listeRolesSysteme = new ArrayList<>();
            String role = """
                    You are a helpful assistant. You help the user to find the information they need.
                    If the user types a question, you answer it clearly.
                    """;
            this.listeRolesSysteme.add(new SelectItem(role, "Assistant"));
        }
        return this.listeRolesSysteme;
    }
}