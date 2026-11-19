# Flujo de especificación → implementación

Metodología **spec-anchored**: la especificación se mantiene viva y manda sobre el código.

## Antes de desarrollar (una vez por proyecto)

- [ ] Entrevista inicial (`docs/entrevista-inicial.md`)
- [ ] Constitución aprobada (`docs/constitution.md`)
- [ ] Stack y alcance fijados en la constitución

## Por cada funcionalidad

1. [ ] **Spec** — `specs/NNN-nombre/spec.md` (qué / por qué, requisitos en EARS, estado: borrador)
2. [ ] **Clarificación** — revisión tipo QA (ambigüedades, contradicciones, límites, constitución)
3. [ ] **Aprobación de spec** — estado: aprobada
4. [ ] **Plan** — `plan.md` (archivos, lógica, decisiones, tests; marca cada RF)
5. [ ] **Tareas** — `tasks.md` (pequeñas, verificables, con criterio «hecho cuando»)
6. [ ] **Aprobación de plan y tareas**
7. [ ] **Implementación** — una tarea por vez; tests primero en lógica de núcleo
8. [ ] **Validación** — RF por RF; veredicto aprobado o cambios necesarios
9. [ ] **Cambio** — si hay requisito nuevo: primero la spec, luego plan/tareas, luego código

## Regla de oro

No implementar fuera de la spec activa. Si falta una decisión, parar y resolverla antes de seguir.
