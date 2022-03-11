package model;

import java.util.HashMap;
import java.util.Map;

/**
 * Class used to store term<-->frequency for a specific document
 * */
public class DocumentData {
    private Map<String, Double> termToFrequency = new HashMap<>();

    public void putTermFrequency(String term, double frequency) {
        termToFrequency.put(term, frequency);
    }

    public double getFrequency(String term) {
        if (!termToFrequency.containsKey(term)) {
            return 0;
        }
        return termToFrequency.get(term);
    }
}
