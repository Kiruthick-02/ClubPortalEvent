package com.example.clubeventportal;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SentimentAnalyzer {

    // Simple dictionary-based AI for demo purposes
    private static final Set<String> POSITIVE_WORDS = new HashSet<>(Arrays.asList(
            "good", "great", "excellent", "amazing", "love", "best", "awesome", "nice",
            "helpful", "fun", "enjoyed", "perfect", "fantastic", "happy", "thanks", "well"
    ));

    private static final Set<String> NEGATIVE_WORDS = new HashSet<>(Arrays.asList(
            "bad", "worst", "terrible", "boring", "hate", "awful", "waste", "poor",
            "slow", "messy", "disappointed", "sad", "angry", "useless", "confusing"
    ));

    public static String analyze(String text) {
        if (text == null || text.isEmpty()) return "NEUTRAL";

        String[] words = text.toLowerCase().split("\\s+");
        int score = 0;

        for (String word : words) {
            // Remove punctuation
            word = word.replaceAll("[^a-zA-Z]", "");
            if (POSITIVE_WORDS.contains(word)) score++;
            if (NEGATIVE_WORDS.contains(word)) score--;
        }

        if (score > 0) return "POSITIVE";
        if (score < 0) return "NEGATIVE";
        return "NEUTRAL";
    }
}