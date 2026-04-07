# UI Revamp Implementation Plan

## Goal Description
Enhance the user interface of all HR application HTML templates in the recruitment module. The goal is to "donner envie" (make it visually appealing) and ensure the application looks extremely professional and polished for the final presentation to attract an excellent grade from the professor. This includes utilizing modern web design concepts available via TailwindCSS, such as gradient texts, hover micro-interactions, softer drop-shadows, rounded UI elements (glassmorphism in some areas), and refined spacing.

## Proposed Changes

### Module: Recrutement (HTML Templates)
The exact list of files to be modified:
#### [MODIFY] [dashboard.component.html](file:///f:/RH-APPLICATION-FRONTEND/src/app/modules/recrutement/dashboard/dashboard.component.html)
- Add gradient typography `bg-clip-text text-transparent bg-gradient-to-r from-blue-700 to-indigo-700` and `leading-tight` to headers.
- Enhance KPI cards with `transition-all duration-300 hover:-translate-y-1 hover:shadow-2xl ring-1 ring-black/5`.
- Round the cards with `rounded-3xl` instead of `rounded-2xl` for a more modern appeal.
- Enhance action buttons with soft shadows, hover transitions, and pill shapes.

#### [MODIFY] [offres-list.component.html](file:///f:/RH-APPLICATION-FRONTEND/src/app/modules/recrutement/offres/list/offres-list.component.html)
- Enhance the header with gradients.
- Add hover transitions to the job postings grid `hover:shadow-xl hover:-translate-y-1 transition-all`.
- Polish the action buttons within the job card.
- Modernize the loading spinner display.

#### [MODIFY] [offre-detail.component.html](file:///f:/RH-APPLICATION-FRONTEND/src/app/modules/recrutement/offres/detail/offre-detail.component.html)
- Re-style details sections to feel like a premium job portal (e.g., subtle backgrounds, nice dividers).
- Optimize spacing using modern typography tokens.

#### [MODIFY] [creer-offre.component.html](file:///f:/RH-APPLICATION-FRONTEND/src/app/modules/recrutement/offres/creer/creer-offre.component.html)
- Enhance form layout with softer borders, focused states (`focus:ring-2 focus:ring-blue-500`), and a prominent gradient "Créer" button.

#### [MODIFY] [pipeline.component.html](file:///f:/RH-APPLICATION-FRONTEND/src/app/modules/recrutement/pipeline/pipeline.component.html)
- Revamp the drag-and-drop columns with a modern background (like `bg-slate-50/80 backdrop-blur`) and rounded corners.
- Enhance candidate cards with `shadow-sm hover:shadow-md transition-shadow cursor-grab`.

#### [MODIFY] [mes-candidatures.component.html](file:///f:/RH-APPLICATION-FRONTEND/src/app/modules/recrutement/candidatures/mes-candidatures/mes-candidatures.component.html)
- Make the dashboard look attractive and personalized. Update lists similar to [dashboard.component.html](file:///f:/RH-APPLICATION-FRONTEND/src/app/modules/recrutement/dashboard/dashboard.component.html).

#### [MODIFY] [postuler.component.html](file:///f:/RH-APPLICATION-FRONTEND/src/app/modules/recrutement/candidatures/postuler/postuler.component.html)
- Improve the application form to feel friendly and modern, adding smooth borders and subtle highlights.

#### [MODIFY] [calendrier.component.html](file:///f:/RH-APPLICATION-FRONTEND/src/app/modules/recrutement/calendrier/calendrier.component.html)
- Add padding and shadow improvements to the calendar container.

#### [MODIFY] [feedback-entretien.component.html](file:///f:/RH-APPLICATION-FRONTEND/src/app/modules/recrutement/entretiens/feedback-entretien.component.html)
- Enhance form fields for scoring inputs with more professional spacing.

#### [MODIFY] [planifier-entretien.component.html](file:///f:/RH-APPLICATION-FRONTEND/src/app/modules/recrutement/entretiens/planifier-entretien.component.html)
- Modernize the dialogue input.

#### [MODIFY] [charte.component.html](file:///f:/RH-APPLICATION-FRONTEND/src/app/modules/recrutement/charte/charte.component.html)
- Style the HR charter document like a refined reading view, using `prose` (if available via Tailwind typography) or standard nice spacing, and readable soft gray typography for the body.

## Verification Plan
### Automated Tests
* None needed as these are strictly presentational changes to HTML tailwind classes without altering core component TS logic.
### Manual Verification
* `npm start` is already running in the background. Note the page reloading, then visually inspect the UI in the browser across these paths (Dashboard, Offres, Mes candidatures, Pipeline) to guarantee there are no visual glitches and that the aesthetics hit a high-quality standard (shadows, gradients, typography flow).
* I will present screenshots of the updated sections to the user during Walkthrough.

# Feature: Smart Matching AI Algorithm

## Goal Description
Implement a backend algorithm in Spring Boot to automatically calculate a "Matching Score" between a candidate's CV and the job offer's requirements when they apply. Display this score prominently in the Angular frontend.

## Proposed Changes

### Backend Spring Boot (`f:\RH-APPLICATION-BACKEND`)
#### [MODIFY] [CandidatureServiceImpl.java](file:///f:/RH-APPLICATION-BACKEND/src/main/java/tn/esprit/rh_rse/service/impl/CandidatureServiceImpl.java)
- Modify the [postuler](file:///f:/RH-APPLICATION-BACKEND/src/main/java/tn/esprit/rh_rse/service/impl/CandidatureServiceImpl.java#45-150) method.
- Add logic to extract skills or simulate AI extraction from the CV text.
- Retrieve the [Offre](file:///f:/RH-APPLICATION-BACKEND/src/main/java/tn/esprit/rh_rse/entity/Offre.java#12-49) to get `competencesRequises`.
- Calculate an intersection (Matching Score) between `competencesRequises` and the candidate's skills.
- Save `scoreMatching` (valeur entre 0 et 100) and `competencesExtraites` into the [Candidature](file:///f:/RH-APPLICATION-BACKEND/src/main/java/tn/esprit/rh_rse/entity/Candidature.java#11-41) entity before saving to MongoDB.

### Frontend Angular (`f:\RH-APPLICATION-FRONTEND`)
- **Note:** The frontend already has UI placeholders for `scoreMatching`! (e.g. `Score IA : {{ c.scoreMatching }}%`). We will ensure it is correctly mapped in the [pipeline.component.html](file:///f:/RH-APPLICATION-FRONTEND/src/app/modules/recrutement/pipeline/pipeline.component.html) and [mes-candidatures.component.html](file:///f:/RH-APPLICATION-FRONTEND/src/app/modules/recrutement/candidatures/mes-candidatures/mes-candidatures.component.html).

## Verification Plan
- Submit a new [Candidature](file:///f:/RH-APPLICATION-BACKEND/src/main/java/tn/esprit/rh_rse/entity/Candidature.java#11-41) via the frontend form.
- Inspect the backend logs to see the matching calculation.
- Check the [Pipeline](file:///f:/RH-APPLICATION-FRONTEND/src/app/modules/recrutement/dashboard/dashboard.component.ts#86-90) or `Mes Candidatures` views to confirm the Score is correctly loaded and displayed with the fiery gauge UI.
