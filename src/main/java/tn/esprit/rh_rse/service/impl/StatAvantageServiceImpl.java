package tn.esprit.rh_rse.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.dto.response.StatAvantageDTOs.*;
import tn.esprit.rh_rse.entity.AvantageReservation;
import tn.esprit.rh_rse.entity.Offre;
import tn.esprit.rh_rse.entity.Partenaire;
import tn.esprit.rh_rse.entity.enums.StatutReservation;
import tn.esprit.rh_rse.service.StatAvantageService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatAvantageServiceImpl implements StatAvantageService {

    private final MongoTemplate mongoTemplate;

    @Override
    public KpiDto getKpis() {
        long totalReservations = mongoTemplate.count(new Query(), AvantageReservation.class);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime firstDayOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime lastDayOfMonth = now.plusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);

        long reservationsCeMois = mongoTemplate.count(
                new Query(Criteria.where("dateReservation").gte(firstDayOfMonth).lt(lastDayOfMonth)),
                AvantageReservation.class
        );

        long offresActives = mongoTemplate.count(
                new Query(Criteria.where("statut").is("ACTIVE")),
                Offre.class
        );

        long partenairesActifs = mongoTemplate.count(
                new Query(Criteria.where("actif").is(true)),
                Partenaire.class
        );

        return KpiDto.builder()
                .totalReservations(totalReservations)
                .reservationsCeMois(reservationsCeMois)
                .offresActives(offresActives)
                .partenairesActifs(partenairesActifs)
                .build();
    }

    @Override
    public List<StatCategorieDto> getStatParCategorie() {
        // En utilisant String pour 'idOffre' on doit s'assurer que c'est bien relié à _id.
        // Puisque la DB MongoDB peut stocker '_id' en tant que ObjectId, une agrégation avec $lookup depuis
        // un champ String peut être complexe sans conversion ObjectId spécifique à Spring.
        // L'alternative propre avec MongoTemplate c'est d'utiliser l'agrégation, mais avec l'opérateur $toObjectId
        // si nécessaire. Cependant, on peut aussi grouper par ID puis compléter en mémoire si c'est plus sûr, mais 
        // l'utilisateur veut utiliser Aggregation.
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("statut").is(StatutReservation.CONFIRMEE)),
                Aggregation.addFields().addField("idOffreObj").withValueOfExpression("{ $toObjectId: '$idOffre' }").build(),
                Aggregation.lookup("offres", "idOffreObj", "_id", "offre_docs"),
                Aggregation.unwind("offre_docs"),
                Aggregation.group("offre_docs.categorie").count().as("count"),
                Aggregation.project("count").and("_id").as("categorie")
        );

        AggregationResults<StatCategorieDto> results = mongoTemplate.aggregate(aggregation, "avantage_reservation", StatCategorieDto.class);
        List<StatCategorieDto> stats = results.getMappedResults();

        long total = stats.stream().mapToLong(StatCategorieDto::getCount).sum();
        for (StatCategorieDto stat : stats) {
            stat.setPourcentage(total == 0 ? 0 : Math.round((stat.getCount() * 100.0 / total) * 100.0) / 100.0);
        }

        return stats;
    }

    @Override
    public List<StatTopOffreDto> getTop5Offres() {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("statut").is(StatutReservation.CONFIRMEE)),
                Aggregation.group("idOffre").count().as("count"),
                Aggregation.sort(Sort.Direction.DESC, "count"),
                Aggregation.limit(5),
                Aggregation.project("count").and("_id").as("idOffre")
        );

        AggregationResults<StatTopOffreDto> results = mongoTemplate.aggregate(aggregation, "avantage_reservation", StatTopOffreDto.class);
        List<StatTopOffreDto> stats = results.getMappedResults();

        // Récupérer les titres des offres
        List<String> ids = stats.stream().map(StatTopOffreDto::getIdOffre).collect(Collectors.toList());
        List<Offre> offres = mongoTemplate.find(new Query(Criteria.where("id").in(ids)), Offre.class);
        Map<String, String> titreMap = offres.stream().collect(Collectors.toMap(Offre::getId, Offre::getTitre));

        for (StatTopOffreDto dto : stats) {
            dto.setTitreOffre(titreMap.getOrDefault(dto.getIdOffre(), "Offre inconnue"));
        }

        return stats;
    }

    @Override
    public List<StatMensuelleDto> getStatParMois(Integer annee) {
        int year = (annee != null) ? annee : LocalDate.now().getYear();
        LocalDateTime debutAnnee = LocalDateTime.of(year, 1, 1, 0, 0);
        LocalDateTime finAnnee = LocalDateTime.of(year + 1, 1, 1, 0, 0);

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("dateReservation").gte(debutAnnee).lt(finAnnee)),
                Aggregation.project()
                        .andExpression("month(dateReservation)").as("mois"),
                Aggregation.group("mois").count().as("count"),
                Aggregation.project("count").and("_id").as("mois"),
                Aggregation.sort(Sort.Direction.ASC, "mois")
        );

        AggregationResults<StatMensuelleDto> results = mongoTemplate.aggregate(aggregation, "avantage_reservation", StatMensuelleDto.class);
        List<StatMensuelleDto> statsFromDb = results.getMappedResults();

        // Remplir les données pour avoir 12 mois complets
        Map<Integer, Long> monthlyCounts = statsFromDb.stream()
                .collect(Collectors.toMap(StatMensuelleDto::getMois, StatMensuelleDto::getCount));

        return java.util.stream.IntStream.rangeClosed(1, 12).mapToObj(m -> {
            StatMensuelleDto dto = new StatMensuelleDto();
            dto.setMois(m);
            dto.setCount(monthlyCounts.getOrDefault(m, 0L));
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public List<StatStatutDto> getStatStatuts() {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.group("statut").count().as("count"),
                Aggregation.project("count").and("_id").as("statut")
        );

        AggregationResults<StatStatutDto> results = mongoTemplate.aggregate(aggregation, "avantage_reservation", StatStatutDto.class);
        List<StatStatutDto> stats = results.getMappedResults();

        long total = stats.stream().mapToLong(StatStatutDto::getCount).sum();
        for (StatStatutDto stat : stats) {
            stat.setPourcentage(total == 0 ? 0 : Math.round((stat.getCount() * 100.0 / total) * 100.0) / 100.0);
        }

        return stats;
    }
}
