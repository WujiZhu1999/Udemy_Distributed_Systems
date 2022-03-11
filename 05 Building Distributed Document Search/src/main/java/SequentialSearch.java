import model.DocumentData;
import search.TFIDF;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SequentialSearch {
    public static final String BOOKS_DIRECTORY = "./src/main/resources/books";
    public static final String SEARCH_QUERY_1 = "The best detective that catches many criminals using his deductive methods";
    public static final String SEARCH_QUERY_2 = "The girl that falls through a rabbit hole into a fantasy wonderland";
    public static final String SEARCH_QUERY_3 = "A war between Russia and France in the cold winter";

    public static void main(String [] args) throws FileNotFoundException {
        File documentsDirectory = new File(BOOKS_DIRECTORY);
        List<String> documents = Arrays.asList(documentsDirectory.list())
                .stream()
                .map(documentName -> BOOKS_DIRECTORY + "/" + documentName)
                .collect(Collectors.toList());

        System.out.println("-------------------------------Query 1---------------------------------");
        List<String> terms1 = TFIDF.getWordsFromLine(SEARCH_QUERY_1);
        findMostRelevantDocuments(documents, terms1);

        System.out.println("-------------------------------Query 2---------------------------------");
        List<String> terms2 = TFIDF.getWordsFromLine(SEARCH_QUERY_2);
        findMostRelevantDocuments(documents, terms2);

        System.out.println("-------------------------------Query 3---------------------------------");
        List<String> term3 = TFIDF.getWordsFromLine(SEARCH_QUERY_3);
        findMostRelevantDocuments(documents, term3);

    }

    private static void findMostRelevantDocuments(List<String> documents, List<String> terms) throws FileNotFoundException {
        /**
         * Stage 1, read all books and generate frequency table for each book
         * */
        Map<String, DocumentData> documentResults = new HashMap<>();

        for (String document : documents) {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(document));
            List<String> lines = bufferedReader.lines().collect(Collectors.toList());
            List<String> words = TFIDF.getWordsFromDocument(lines);
            DocumentData documentData = TFIDF.createDocumentData(words, terms);

            documentResults.put(document, documentData);
        }

        Map<Double, List<String>> documentsByScore = TFIDF.getDocumentsSortedByScore(terms, documentResults);
        printResults(documentsByScore);
    }

    private static void printResults(Map<Double, List<String>> documentsByScore) {
        for(Map.Entry<Double, List<String>> docScorePair : documentsByScore.entrySet()){
            double score = docScorePair.getKey();
            for (String document : docScorePair.getValue()) {
                System.out.println(String.format("Book : %s - score: %f", document.split("/")[5], score));
            }
        }
    }


}
