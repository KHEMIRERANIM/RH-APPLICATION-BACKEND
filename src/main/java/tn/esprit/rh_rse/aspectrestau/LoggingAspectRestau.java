package tn.esprit.rh_rse.aspectrestau;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class LoggingAspectRestau {

    @Before("execution(* tn.esprit.rh_rse.service.impl.*.*(..))")
    public void logAvant(JoinPoint joinPoint) {
        log.info("[AOP] ▶ Début : {}", joinPoint.getSignature().getName());
    }

    @After("execution(* tn.esprit.rh_rse.service.impl.*.*(..))")
    public void logApres(JoinPoint joinPoint) {
        log.info("[AOP] ✔ Fin   : {}", joinPoint.getSignature().getName());
    }

    // ── Throwable au lieu de Exception pour capturer tout ──────
    @AfterThrowing(
        pointcut = "execution(* tn.esprit.rh_rse.service.impl.*.*(..))",
        throwing  = "ex"
    )
    public void logErreur(JoinPoint joinPoint, Throwable ex) {
        log.error("[AOP] ✘ ERREUR dans {} → {}",
            joinPoint.getSignature().getName(),
            ex.getMessage());
    }
}