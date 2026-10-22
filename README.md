# Black Maltese Puppy (Fabric 1.20.1)

Mod que añade a **Frida**, una perrita Maltese negra.

## Cómo compilar (paso a paso)
1. Instala **JDK 17** (https://adoptium.net) y comprueba con `java -version`.
2. Instala **Gradle 8.5** (https://gradle.org/install). Dentro de esta carpeta ejecuta una sola vez:
       gradle wrapper --gradle-version 8.5
3. Compila:
       Windows:   gradlew.bat build
       Mac/Linux: ./gradlew build
4. El mod queda en `build/libs/maltesemod-1.0.0.jar` (NO uses el `-sources.jar`).
5. Copia ese .jar a la carpeta `mods` de tu instalación de Minecraft 1.20.1 con **Fabric Loader** y **Fabric API 0.92.2+1.20.1** (descárgalo de Modrinth/CurseForge y ponlo también en `mods`).

Para probar sin instalar nada: `gradlew runClient`.

## Qué hace cada archivo
| Archivo | Para qué sirve |
|---|---|
| `MalteseMod.java` | Registra la entidad, el huevo "Black Maltese Puppy", el spawn en llanuras/bosques y los comandos de chat `sit` / `up`. |
| `entity/BlackMalteseEntity.java` | La perrita: atributos, "cerebro" (goals), domesticación con huesos, baile, sonidos y nombre "Frida". |
| `client/MalteseClient.java` | Conecta modelo y renderer (solo cliente). |
| `client/BlackMalteseRenderer.java` | Dibuja la entidad con su textura. |
| `client/BlackMalteseModel.java` | Modelo 3D (cajas) y TODAS las animaciones (caminar, correr, sentarse, cola, lamerse, estirarse, baile). |
| `assets/.../textures/entity/black_maltese.png` | Textura 64x64 (pelaje negro, ojos, nariz, lengua). |
| `assets/.../lang/*.json` | Nombres en inglés y español. |
| `fabric.mod.json` | Datos del mod. |

## Cómo se usa en el juego
- Dale un **hueso** (clic derecho): 50 % de domesticarla; baila y salen corazones. Ya domesticada, cada hueso la cura y la hace bailar.
- Domesticada, te sigue (corre si estás lejos).
- Escribe **sit** en el chat para sentarla y **up** para levantarla (también `sientate` / `levanta`). Clic derecho con la mano vacía hace lo mismo.
- Huevo: pestaña "Huevos de spawn" → **Black Maltese Puppy**.
- Spawn natural raro (peso 3) en llanuras, bosques, bosque de abedules, bosque de flores y bosque oscuro.

## Ajustes fáciles
- Rareza del spawn: el `3` en `BiomeModifications.addSpawn(...)` de `MalteseMod.java`.
- Probabilidad de domesticar: `random.nextInt(2)` en `interactMob`.
- Velocidad: `GENERIC_MOVEMENT_SPEED` (0.3).
- Posturas: los números de `poseSit()`, `poseDance()`, etc. en el modelo.
- Textura: edítala con cualquier editor de píxeles (respetando el diseño de 64x64).

## Sobre el modelo
El modelo está definido en código Java (`getTexturedModelData`) en vez de un `.json`/`.bbmodel`: es lo más estable en Fabric 1.20.1, no necesita librerías extra y permite animaciones procedimentales suaves. Si más adelante quieres rehacerlo en Blockbench, puedes usar las mismas medidas.
