package tn.esprit.rh_rse.aspectrestau;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class PerformanceAspectRestau {

    @Around("execution(* tn.esprit.rh_rse.service.impl.CommandeServiceImpl.save(..)) || " +
            "execution(* tn.esprit.rh_rse.service.impl.PaiementPlatServiceImpl.payerCommande(..)) || " +
            "execution(* tn.esprit.rh_rse.service.impl.CommandeServiceImpl.updatePlats(..))")
    public Object mesurerTemps(ProceedingJoinPoint pjp) throws Throwable {

        long debut = System.currentTimeMillis();

        try {
            Object resultat = pjp.proceed(); // exécute la vraie méthode
            long duree = System.currentTimeMillis() - debut;

            if (duree > 500) {
                log.warn("[AOP] ⚠ LENT : {} → {} ms", pjp.getSignature().getName(), duree);
            } else {
                log.info("[AOP] ⚡ {} → {} ms", pjp.getSignature().getName(), duree);
            }

            return resultat;

        } catch (Throwable ex) {
            long duree = System.currentTimeMillis() - debut;
            log.error("[AOP] ⚡ {} échoué après {} ms", pjp.getSignature().getName(), duree);
            throw ex; //
        }
    }
}