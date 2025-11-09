package com.tp4_omaressafi.tpwebomaressafi.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service centralisé (ApplicationScoped) pour gérer l'EmbeddingStore.
 * Il charge les PDF initiaux au démarrage et permet d'en ajouter
 * dynamiquement via l'upload (partie optionnelle du TP).
 */
@ApplicationScoped
public class MagasinEmbeddings {

    private EmbeddingStore<TextSegment> embeddingStore;
    private EmbeddingModel embeddingModel;
    private DocumentParser documentParser;

    @PostConstruct
    public void init() {
        // Initialiser les composants de base
        this.embeddingModel = new AllMiniLmL6V2EmbeddingModel();
        this.embeddingStore = new InMemoryEmbeddingStore<>();
        this.documentParser = new ApacheTikaDocumentParser();

        // Configurer le logging pour LangChain4j (Test 2)
        configureLogger();

        // Charger les documents initiaux depuis les ressources
        try {
            // ============ CORRECTION ICI ============
            // Le fichier s'appelle "rag.pdf", pas "rag-2.pdf"
            chargerDocumentInitial("rag.pdf");
            // ========================================

            // Changement de 'sport.pdf' à 'music.pdf' (votre demande)
            chargerDocumentInitial("music.pdf");

        } catch (Exception e) {
            Logger.getLogger(MagasinEmbeddings.class.getName())
                    .log(Level.SEVERE, "Erreur lors du chargement des PDF initiaux", e);
        }
    }

    private void chargerDocumentInitial(String nomFichier) throws URISyntaxException {
        URL resource = getClass().getClassLoader().getResource(nomFichier);
        if (resource == null) {
            throw new IllegalStateException("❌ Fichier PDF introuvable dans resources: " + nomFichier);
        }
        Path path = Paths.get(resource.toURI());
        Document document = FileSystemDocumentLoader.loadDocument(path, documentParser);
        ingesterDocument(document);
        Logger.getLogger(MagasinEmbeddings.class.getName())
                .info("Document initial chargé dans le RAG : " + nomFichier);
    }

    /**
     * Logique centrale pour splitter et ingester un document.
     */
    private void ingesterDocument(Document document) {
        // Splitter (découpage) comme dans le Test 1
        DocumentSplitter splitter = DocumentSplitters.recursive(300, 30);
        List<TextSegment> segments = splitter.split(document);
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        embeddingStore.addAll(embeddings, segments);
    }

    /**
     * Méthode publique pour ajouter un nouveau document au magasin (pour l'upload).
     * C'est la partie "..." à compléter dans le TP.
     */
    public void ajouterDocument(InputStream inputStream, String documentName) {
        try {
            Document document = documentParser.parse(inputStream);

            // Correction de la méthode .add() en .put()
            document.metadata().put("source", documentName);


            ingesterDocument(document);
            Logger.getLogger(MagasinEmbeddings.class.getName())
                    .info("Nouveau document ajouté au RAG (upload) : " + documentName);
        } catch (Exception e) {
            Logger.getLogger(MagasinEmbeddings.class.getName())
                    .log(Level.SEVERE, "Erreur lors de l'ajout du document uploadé", e);
            throw new RuntimeException("Erreur lors du parsing ou de l'ingestion du document", e);
        }
    }

    /**
     * Ajout du Logging (Test 2)
     */
    private static void configureLogger() {
        Logger packageLogger = Logger.getLogger("dev.langchain4j");
        packageLogger.setLevel(Level.FINE);
    }

    // --- Getters pour les autres services ---

    public EmbeddingStore<TextSegment> getEmbeddingStore() {
        return embeddingStore;
    }

    public EmbeddingModel getEmbeddingModel() {
        return embeddingModel;
    }
}