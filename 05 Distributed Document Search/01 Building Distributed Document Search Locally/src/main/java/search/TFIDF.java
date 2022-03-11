package search;

import model.DocumentData;

import java.util.*;

public class TFIDF {

    /**
     * Calculate frequency of a term in a set of words
     * */
    public static double calculateTermFrequency(List<String> words, String term){

        long count = 0;
        for(String word : words) {
            if (term.equalsIgnoreCase(word)){
                count ++ ;
            }
        }

        double termFrequency = (double) count /(words.size() + 1.0);
        return termFrequency;
    }

    /**
     * A book and a vocabulary list, store frequency for all words in the data structure DocumentData
     * */
    public static DocumentData createDocumentData(List<String> words, List<String> terms) {
        DocumentData documentData = new DocumentData();

        for (String term : terms) {
            double termFreq = calculateTermFrequency(words, term);
            documentData.putTermFrequency(term, termFreq);
        }

        return documentData;
    }

    /**
     * Given the term frequency count for all documents, and a term, get IDF
     */
    private static double getInverseDocumentFrequency(String term, Map<String, DocumentData> documentResults) {
        double nt = 0;
        for(String document: documentResults.keySet()){
            DocumentData documentData = documentResults.get(document);
            double termFrequency = documentData.getFrequency(term);
            if (termFrequency > 0.0) {
                nt ++;
            }
        }
        return nt == 0 ? 0 : Math.log10(documentResults.size() / nt);
    }

    /**
     * Get IDF for a bunch of terms
     * */
    private static Map<String, Double> getTermToInverseDocumentFrequencyMap(
            List<String> terms,
            Map<String, DocumentData> documentResults
    ){
        Map<String, Double> termToIDF = new HashMap<>();
        for (String term : terms) {
            double idf = getInverseDocumentFrequency(term, documentResults);
            termToIDF.put(term, idf);
        }
        return termToIDF;
    }

    /**
     * TF-IDF calculation and score a document
     * */
    private static double calculateDocumentScore(
            List<String> terms,
            DocumentData documentData,
            Map<String, Double> termToInverseDocumentFrequency) {
        double score = 0;
        for (String term : terms) {
            double termFrequency = documentData.getFrequency(term);
            double inverseTermFrequency = termToInverseDocumentFrequency.get(term);
            score += termFrequency * inverseTermFrequency;
        }

        return score;
    }

    /**
     * Score a bunch of documents, each represented with a string, its List<String> because
     * multiple documents may share same score
     * */
    public static Map<Double, List<String>> getDocumentsSortedByScore(
            List<String> terms,
            Map<String, DocumentData> documentResults
    ) {
        //use tree map to make sure its sorted as we go
        TreeMap<Double, List<String>> scoreToDocuments = new TreeMap<>();

        //calculate IDF
        Map<String, Double> termToInverseDocumentFrequency = getTermToInverseDocumentFrequencyMap(terms, documentResults);

        for(String document : documentResults.keySet()) {
            DocumentData documentData = documentResults.get(document);

            double score = calculateDocumentScore(terms, documentData, termToInverseDocumentFrequency);

            addDocumentScoreToTreeMap(scoreToDocuments, score, document);

        }

        return scoreToDocuments.descendingMap();
    }

    private static void addDocumentScoreToTreeMap(TreeMap<Double, List<String>> scoreToDoc, double score, String document) {
        List<String> documentsWithCurrentScore = scoreToDoc.get(score);
        if(documentsWithCurrentScore == null) {
            documentsWithCurrentScore = new ArrayList<>();
        }

        documentsWithCurrentScore.add(document);
        scoreToDoc.put(score, documentsWithCurrentScore);
    }

    //helper function for search
    public static List<String> getWordsFromLine(String line) {
        return Arrays.asList(line.split("(\\.)+|(,)+|( )+|(-)+|(\\?)+|(!)+|(;)+|(:)+|(/d)+|(/n)+"));
    }

    public static List<String> getWordsFromDocument(List<String> lines) {
        List<String> words = new ArrayList<>();
        for (String line : lines) {
            words.addAll(getWordsFromLine(line));
        }
        return words;
    }
}
