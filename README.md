# CropHeads (Paper 1.21+, Java 21)

Compilar:  mvn clean package   ->  target/CropHeads-1.0.0.jar  -> carpeta plugins/

## Uso
- /cropheads            -> menu principal (lista de regiones)
- /cropheads give [jugador] [textura]
- /cropheads list | remove <id> | regen <id> | reload
- Coloca la cabeza donde quieras (spawn). Click derecho sobre ella (sin shift) abre su menu.
- Rompe la cabeza (admin) para recogerla; conserva la textura.

## Permisos
- cropheads.admin  : colocar/romper cabezas, menu y comandos
- cropheads.bypass : ignora regiones (puede romper todo)
- Permiso de cosecha: configurable POR CABEZA desde el menu (vacio = todos).

## Notas
- Funciona por encima de WorldGuard: la cosecha se gestiona a prioridad HIGHEST, asi los cultivos
  se pueden picar aunque el spawn este protegido.
- Cultivos con edad: se resetean a edad 0 y vuelven maduros tras el tiempo configurado (persistente
  tras reinicios). MELON/PUMPKIN desaparecen y reaparecen.
- Holograma = TextDisplay (sombra, fondo, escala, billboard). Texto en MiniMessage.
