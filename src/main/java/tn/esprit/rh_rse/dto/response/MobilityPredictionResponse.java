package tn.esprit.rh_rse.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class MobilityPredictionResponse {

    private String recommended_job;
    private double confidence_score;
    private List<TopJob> top_3_jobs;
    private List<SkillGapItem> skills_gap;
    private List<String> evolution_plan;

    @Data
    public static class TopJob {
        private String job;
        private double score;
    }

    @Data
    public static class SkillGapItem {
        private String skill;
        private double current;
        private double required;
        private double gap;
    }
}