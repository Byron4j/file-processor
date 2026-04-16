package com.fileprocessor.intelligence;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Keyword extractor using HanLP
 */
@Component
public class KeywordExtractor {

    private static final Logger log = LoggerFactory.getLogger(KeywordExtractor.class);

    /**
     * Extract keywords using TextRank algorithm
     */
    public List<Keyword> extract(String text, int topN) {
        log.debug("Extracting keywords, topN: {}", topN);

        try {
            List<String> keywords = HanLP.extractKeyword(text, topN);
            List<Float> scores = calculateScores(text, keywords);

            List<Keyword> result = new ArrayList<>();
            for (int i = 0; i < keywords.size(); i++) {
                Keyword keyword = new Keyword();
                keyword.setWord(keywords.get(i));
                keyword.setRank(i + 1);
                keyword.setScore(scores.size() > i ? scores.get(i) : 0.5f);
                result.add(keyword);
            }

            return result;
        } catch (Exception e) {
            log.error("Failed to extract keywords", e);
            return Collections.emptyList();
        }
    }

    /**
     * Extract keywords with phrase extraction
     */
    public List<Keyword> extractWithPhrases(String text, int topN) {
        List<Keyword> keywords = extract(text, topN * 2);

        // Filter out single characters and keep meaningful phrases
        return keywords.stream()
                .filter(k -> k.getWord().length() > 1)
                .limit(topN)
                .collect(Collectors.toList());
    }

    /**
     * Extract phrases from text
     */
    public List<String> extractPhrases(String text, int topN) {
        try {
            return HanLP.extractPhrase(text, topN);
        } catch (Exception e) {
            log.error("Failed to extract phrases", e);
            return Collections.emptyList();
        }
    }

    private List<Float> calculateScores(String text, List<String> keywords) {
        // Simple scoring based on position and frequency
        List<Float> scores = new ArrayList<>();
        float baseScore = 1.0f;

        for (int i = 0; i < keywords.size(); i++) {
            scores.add(baseScore - (i * 0.05f));
        }

        return scores;
    }

    /**
     * Keyword result
     */
    public static class Keyword {
        private String word;
        private float score;
        private int rank;

        public String getWord() {
            return word;
        }

        public void setWord(String word) {
            this.word = word;
        }

        public float getScore() {
            return score;
        }

        public void setScore(float score) {
            this.score = score;
        }

        public int getRank() {
            return rank;
        }

        public void setRank(int rank) {
            this.rank = rank;
        }
    }
}
