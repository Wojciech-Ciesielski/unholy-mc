# The Unholy Minecraft – specyfikacja assetów

Wątek designu. Ten plik jest **kontraktem między assetami a kodem**: nazwy plików, ścieżki, id i to, jak kod ma wybierać warianty.
Jeśli implementacja potrzebuje innej nazwy, zmieniamy ją tutaj i w plikach jednocześnie.

Podglądy wszystkich assetów: `assets_src/preview/*.png`. Generatory (Python + Pillow + numpy): `assets_src/tools/`.
Po zmianie generatora: `cd assets_src/tools && python3 <skrypt>.py` (skrypty `cut_slops.py` i `make_*.py`).

## 0. Konwencje

| Co | Ścieżka |
|---|---|
| Namespace | `unholymc` |
| Root resource packa | `src/main/resources/assets/unholymc/` |
| Tekstury | `textures/{item,block,entity,gui,mob_effect}/...` |
| Modele itemów / definicje itemów | `models/item/*.json` / `items/*.json` |
| Modele bloków / blockstates | `models/block/*.json` / `blockstates/*.json` |
| Dźwięki | `sounds/<kategoria>/<nazwa>.ogg` + `sounds.json` |
| Ikona moda | `icon.png` (128×128) |

Wszystkie id są w `snake_case`. Warianty itemów wybiera po stronie klienta **`minecraft:custom_model_data`**, więc nie trzeba własnych właściwości modelu w kodzie. Kod ustawia tylko komponent na stacku (sekcja 2).

---

## 1. Slopy (billboardy PNG)

`textures/entity/slop/<id>.png`: **512×512 RGBA**, tło przezroczyste, postać **wyrównana do dołu i wyśrodkowana** (stopy = dolna krawędź). Piksele w pełni przezroczyste mają RGB = 0 (brak białych obwódek przy filtrowaniu).

| Kategoria (udział) | id tekstury | Źródło | Charakter | Proponowany rozmiar billboardu (bloki) | Hitbox (szer.×wys.) |
|---|---|---|---|---|---|
| Agresywne (60%) | `shadow_watcher` | `pobierz_2` | cień ze świecącymi oczami | 2,2 | 0,7×2,0 |
| Agresywne | `love_note` | `love_note` | głowa w kominiarce z zaklejonymi ustami | 1,8 | 0,9×1,6 |
| Agresywne | `wig_guy` | `pobierz_6` | koleś w peruce (śmieszny) | 1,9 | 0,9×1,8 |
| Neutralne (30%) | `sheet_ghost` | `pobierz_3` | duszek z prześcieradła (alfa z jasności, miękki) | 1,4 (lewituje 0,3 nad ziemią) | 0,7×1,2 |
| Neutralne | `cigar_cat` | `pobierz_5` | kotka z cygarem (śmieszna) | 1,6 | 0,9×1,4 |
| Unholy (10%) | `hollow_screamer` | `the_hollow_screamer` | zgarbiona postać z twarzą-maską | 2,6 | 0,8×2,4 |
| Unholy | `locust` | `the_locust` | wychudzone monstrum z ludzką twarzą | 2,8 | 0,7×2,6 |

`pobierz_4` **nie jest slopem**: to referencja wyglądu 3D Slendermana (sekcja 1.3).

**Losowanie:** w obrębie kategorii każda tekstura ma równą szansę. Wariant wybiera serwer przy spawnie (np. `int variant` w danych encji, synchronizowany do klienta).

### 1.1 Render billboardu (wskazówki dla implementacji)
- Quad o boku = „rozmiar billboardu” z tabeli, dolna krawędź na `y = 0` encji, obracany **tylko wokół osi Y** do kamery (cylindryczny billboard, postać nie „kładzie się”, gdy patrzysz z góry).
- RenderType: `entityCutoutNoCull` dla zwykłych slopów (alfa < 0,1 odcięta). Wyjątek: `sheet_ghost` i wszystkie halucynacje mają miękką alfę, więc dostają `entityTranslucent`.
- Światło: zwykłe oświetlenie świata (slop w ciemności ma być ciemny i ledwo widoczny, a latarka go „wyłapuje”).
- Gdy slop goni gracza, lekki „jitter” pozycji quada (±0,02 bloku co kilka ticków) dodaje niepokoju.

### 1.2 Halucynacje
Decyzja użytkownika: **osobne PNG** `textures/entity/slop/<id>_hallucination.png`. To czarna sylwetka (RGB = 0) z alfą = 60% alfy oryginału.
- Zwykła halucynacja: losowa tekstura `_hallucination` z puli **agresywnych** slopów. RenderType `entityTranslucent`, **fullbright** (żeby była czarną plamą także w świetle), bez cienia.
- Halucynacja Slendermana: ta sama tekstura, ale `alpha` mnożona w renderze ×1,3 (wyraźniejsza). Opcjonalnie co ~2 s błysk na 2 ticki w kolorze `#7a0a1c`.
- Widoczność per gracz to logika kodu (patrz DECISIONS 22–23).

### 1.3 Slenderman (jedyny mob 3D)
Tekstura `textures/entity/slenderman/slenderman.png` (64×64, box UV) + `slenderman_glitch.png`. Kod podmienia ją na **2–4 klatki** przy teleporcie, razem z jitterem pozycji modelu ±0,15 bloku i dźwiękiem `entity.slenderman.teleport`.

Geometria (jednostki modelu = px, y w dół, ziemia = 24, jak w modelach vanilla):

| Część | Pivot (x,y,z) | Box origin | Rozmiar (w,h,d) | UV (u,v) |
|---|---|---|---|---|
| head | (0, −18, 0) | (−4, −10, −4) | 8×10×8 | (0, 0) |
| body | (0, −18, 0) | (−4, 0, −2) | 8×14×4 | (32, 0) |
| right_arm | (−5, −17, 0) | (−1, −1, −1) | 2×30×2 | (0, 18) |
| left_arm | (5, −17, 0) | (−1, −1, −1) | 2×30×2 | (8, 18) |
| right_leg | (−2, −4, 0) | (−1, 0, −1) | 2×28×2 | (16, 18) |
| left_leg | (2, −4, 0) | (−1, 0, −1) | 2×28×2 | (24, 18) |

Wysokość ok. 52 px ≈ **3,25 bloku**. Hitbox 0,7×3,2. Ręce sięgają ok. 12 px nad ziemię (nienaturalnie długie).
Animacja: powolny chód z minimalnym bujaniem rąk, głowa lekko przechylona (`zRot ≈ 0,15`), w bezruchu powolne „oddychanie” torsu.
Deadly Slenderman: ta sama tekstura, tint `0xFFB02020` (czerwonawy) i skala ×1,15.

---

## 2. Itemy (16×16, styl mroczny/brudny)

| Item id | Tekstury (`textures/item/`) | Model / wybór wariantu |
|---|---|---|
| `holy_water` | `holy_water.png` | prosty |
| `splash_holy_water` | `splash_holy_water.png` | prosty |
| `crucifix` | `crucifix.png` | prosty; glint enchantu vanilla |
| `nightmare_gem` | `nightmare_gem.png` | prosty |
| `flashlight` | `flashlight_<rdzeń>.png` + nakładka `flashlight_beam.png` / `flashlight_beam_dark.png` | `custom_model_data.strings[0]` = rdzeń, `flags[0]` = włączona |
| `unholy_sword` | `unholy_sword.png` (animowany, 8 klatek, `frametime 3`, interpolacja) / `unholy_sword_discharged.png` | `custom_model_data.flags[0]` = rozładowany |
| `thermos` | `thermos_{empty,water,holy_water,honey,milk}.png` | `custom_model_data.strings[0]` = płyn |

Rdzenie latarki (`strings[0]`): `lapis`, `redstone`, `emerald`, `diamond`, `netherite`, `nether_star`, `nightmare_gem`. Brak wartości lub nieznana wartość daje `none`. Kolor soczewki = kolor rdzenia. Przy `nightmare_gem` nakładka „włączona” to **ciemna** poświata (`flashlight_beam_dark`).
Płyny termosu (`strings[0]`): `water`, `holy_water`, `honey`, `milk`. Brak wartości = pusty.

Przykład (Java, 26.x):
```java
stack.set(DataComponents.CUSTOM_MODEL_DATA,
    new CustomModelData(List.of(), List.of(isOn), List.of(coreId), List.of()));
```
Definicje w `items/flashlight.json`, `items/thermos.json`, `items/unholy_sword.json`. Latarka i miecz używają parenta `item/handheld`.

---

## 3. Bloki

| Blok | Pliki | Uwagi |
|---|---|---|
| Woda święcona (fluid) | `block/holy_water_still.png` (16×16, 32 klatki), `block/holy_water_flow.png` (32×32, 32 klatki) | Tekstury **już pokolorowane** (jasnoniebieski, alfa 200, migoczące iskierki), więc tint = `0xFFFFFFFF`. Poświata: light level 7, plus cząsteczki `minecraft:end_rod` / `glow` nad powierzchnią (rzadko). Poza pełnią bajorko to zwykła woda z cząsteczkami. |
| Kartka na drzewie (`tree_note`) | `block/tree_note_{help,he_is_here,blood,symbol}.png`, `models/block/tree_note_*.json` (parent `minecraft:block/ladder`), `blockstates/tree_note.json` | Właściwości: `facing` (north/east/south/west, jak drabina = kierunek *od* pnia) i `variant` (help/he_is_here/blood/symbol). Render layer **cutout**. Drop: 1 papier. |

---

## 4. HUD i efekty

Sprite'y GUI (atlas `gui`, id `unholymc:hud/...`), 9×9 px, rysowane jak serca/jedzenie vanilla:

| Pasek | Sprite'y | Rysowanie |
|---|---|---|
| Sanity (mózgi) **nad sercami** | `hud/sanity/container`, `container_blinking`, `full`, `half`, `full_low`, `half_low` | 10 ikon od lewej, 1 mózg = 2 pkt. `half` = wypełniona lewa połowa. Przy Sanity ≤ 4 użyj wariantów `_low` (zgniłe, szarozielone) i trzęś ikonami jak serca przy niskim HP. `container_blinking` przez 10 ticków po stracie Sanity. |
| Nawodnienie (krople) **nad głodem** | `hud/hydration/container`, `full`, `half`, `full_dehydrated`, `half_dehydrated` | 10 ikon od prawej (jak głód), 1 kropla = 2 pkt. `half` = wypełniona prawa połowa. Warianty `_dehydrated` (mętna woda) przy nawodnieniu ≤ 4. |

Kolizja z vanilla: pasek pancerza i bąbelki powietrza też siedzą nad sercami/głodem. Propozycja: rząd Sanity zajmuje miejsce pancerza, a pancerz przesuwa się o 10 px wyżej. Tak samo bąbelki powietrza idą nad rząd nawodnienia.

Efekt **Wellbeing**: `textures/mob_effect/wellbeing.png` (18×18, koniczyna z iskrą). Kolor efektu (cząsteczki): `0x8EE07A`.

---

## 5. Jumpscare – koncept

Tekstury: `gui/jumpscare/spooky_face.png` (512×512), `gui/jumpscare/blood_vignette.png` (256×256, rozciągana), sprite `jumpscare/static` (128×128, animowany 8 klatek, kafelkowany).

### 5.1 Zwykły jumpscare (screamer), ok. 1 s
1. **t = 0**: losowa tekstura slopa (pełny kolor) na środku ekranu, startowo 60% wysokości ekranu. Dźwięk `jumpscare.screamer`.
2. **0–6 ticków**: szybki zoom do 140%, trzęsienie obrazu ±12 px co tick, aberracja chromatyczna (obraz rysowany 3× z tintami R/G/B i offsetem 3–6 px), nakładka `static` z alfą 0,5.
3. **6–14 ticków**: `blood_vignette` na pełnym ekranie, obraz zanika (alfa → 0), static zanika.
4. −10 Sanity (logika). Krucyfiks w ręce blokuje cały efekt.

### 5.2 Spooky Jumpscare (klątwa 10 przespanych nocy), ok. 6 s, kończy się śmiercią
1. **Przebudzenie (0–1 s)**: gracz budzi się normalnie, ale wszystkie dźwięki gry cichną (master → 0, oprócz naszych).
2. **Cisza (1–4 s)**: ekran powoli ciemnieje do 85% czerni. `jumpscare.spooky_buildup` (zwalniające serce + szept). Na środku miga 3× czerwony napis: EN „you slept too long” / PL „spałeś za długo” (klucz `jumpscare.unholymc.spooky_text`).
3. **Uderzenie (4–5,5 s)**: `spooky_face` na pełnym ekranie (cover, bez marginesów), zoom 100% → 120%, bardzo mocne trzęsienie, static z alfą 0,8, co 2 ticki inwersja kolorów na 1 tick. Dźwięk `jumpscare.spooky`.
4. **Śmierć (5,5 s)**: natychmiastowy czarny ekran i zabicie gracza. Komunikat śmierci `death.attack.unholymc.spooky_jumpscare`: EN „%1$s never woke up”, PL „%1$s już się nie obudził(a)”.

Na serwerze dedykowanym kod wysyła pakiet start → klient odgrywa sekwencję, a śmierć zadaje serwer po 110 tickach (niezależnie od klienta).

---

## 6. Dźwięki

**Format:** OGG Vorbis, 44,1/48 kHz, jakość ~q5. **Mono** dla wszystkiego, co ma pozycję w świecie (inaczej MC nie zrobi panoramy ani tłumienia z dystansu). **Stereo** tylko dla `music.eleven_reversed`, `jumpscare.*` i `ambient.blood_moon.rise` (dźwięki „w głowie”). Normalizacja do ok. −14 LUFS; jumpscare'y mogą być głośniejsze (−9 LUFS), żeby naprawdę straszyły. Brak ciszy na początku pliku (opóźnia jumpscare). Pętle (`music.eleven_reversed`, `ambient.dark_corner`, `entity.slenderman.proximity`, `ambient.heartbeat`) muszą łączyć się bez kliku.

Ścieżka pliku = `src/main/resources/assets/unholymc/sounds/<kolumna „Pliki”>`. `sounds.json` jest już wygenerowany (`assets_src/tools/make_sounds.py`). Brakujące pliki dają tylko ostrzeżenie w logu, nie crash.
Napisy (subtitles): klucz `subtitles.unholymc.<event>`. Teksty EN/PL są w ostatniej kolumnie, do wpisania w `lang/en_us.json` i `lang/pl_pl.json` (pliki lang prowadzi wątek implementacji).

| SoundEvent (`unholymc:`) | Pliki `.ogg` | Kanał | Opis | Napis EN / PL |
|---|---|---|---|---|
| `jumpscare.screamer` | `jumpscare/screamer1.ogg` … `jumpscare/screamer4.ogg` | master/UI | Zwykły jumpscare: ostry krzyk + trzask szumu, 0,8–1,2 s, bardzo głośny początek (bez narastania). | Something screams / Coś krzyczy |
| `jumpscare.spooky_buildup` | `jumpscare/spooky_buildup.ogg` | master/UI | Spooky Jumpscare, faza 1 (ok. 3 s): wszystko cichnie, zwalniające bicie serca, szept „you slept too long”. | Heartbeat slows / Serce zwalnia |
| `jumpscare.spooky` | `jumpscare/spooky.ogg` | master/UI | Spooky Jumpscare, faza 2 (ok. 1,5 s): przesterowany wrzask + zgrzyt metalu + przester TV, kończy się ucięciem do ciszy. | You never woke up / Już się nie obudzisz |
| `entity.slop.aggressive.ambient` | `entity/slop/aggressive/ambient1.ogg` … `entity/slop/aggressive/ambient3.ogg` | hostile | Niskie charczenie / mamrotanie, zniekształcone (pitch −30%). | Slop mutters / Slop mamrocze |
| `entity.slop.aggressive.chase` | `entity/slop/aggressive/chase1.ogg`, `entity/slop/aggressive/chase2.ogg` | hostile | Stinger przy rozpoczęciu pościgu: nagły smyczkowy pisk + dyszenie. | Slop gives chase / Slop rusza w pogoń |
| `entity.slop.aggressive.hurt` | `entity/slop/aggressive/hurt1.ogg`, `entity/slop/aggressive/hurt2.ogg` | hostile | Zgrzyt papieru/płótna + jęk. | Slop hurts / Slop obrywa |
| `entity.slop.aggressive.death` | `entity/slop/aggressive/death.ogg` | hostile | Darcie papieru + gasnący wrzask. | Slop dies / Slop ginie |
| `entity.slop.neutral.ambient` | `entity/slop/neutral/ambient1.ogg` … `entity/slop/neutral/ambient3.ogg` | neutral | Chichot / szept z daleka, lekko odwrócony. | Slop giggles / Slop chichocze |
| `entity.slop.neutral.scare` | `entity/slop/neutral/scare1.ogg` … `entity/slop/neutral/scare3.ogg` | neutral | Głośne „BU!”/wrzask przy straszeniu gracza (moment przed ucieczką). | Slop shrieks / Slop wrzeszczy |
| `entity.slop.neutral.flee` | `entity/slop/neutral/flee1.ogg`, `entity/slop/neutral/flee2.ogg` | neutral | Szybkie tupanie / szuranie oddalające się. | Slop scurries away / Slop ucieka |
| `entity.slop.neutral.hurt` | `entity/slop/neutral/hurt1.ogg`, `entity/slop/neutral/hurt2.ogg` | neutral | Piskliwe jęknięcie. | Slop squeals / Slop piszczy |
| `entity.slop.neutral.death` | `entity/slop/neutral/death.ogg` | neutral | Pisk + pyknięcie (jak pęknięty balon). | Slop pops / Slop pęka |
| `entity.slop.unholy.ambient` | `entity/slop/unholy/ambient1.ogg` … `entity/slop/unholy/ambient3.ogg` | hostile | Chóralny, odwrócony szept w nieznanym języku + niski dron. | Unholy whispers / Bluźniercze szepty |
| `entity.slop.unholy.attack` | `entity/slop/unholy/attack1.ogg`, `entity/slop/unholy/attack2.ogg` | hostile | Uderzenie + zasysający dźwięk (nałożenie Darkness). | Unholy Slop strikes / Bluźnierczy slop uderza |
| `entity.slop.unholy.hurt` | `entity/slop/unholy/hurt1.ogg`, `entity/slop/unholy/hurt2.ogg` | hostile | Skwierczenie (święte obrażenia) + syk. | Unholy Slop sizzles / Bluźnierczy slop skwierczy |
| `entity.slop.unholy.immune` | `entity/slop/unholy/immune.ogg` | hostile | Głuchy, metaliczny „tunk” – cios nie robi wrażenia (zwykła broń). | Attack has no effect / Cios nie działa |
| `entity.slop.unholy.death` | `entity/slop/unholy/death.ogg` | hostile | Wielogłosowy wrzask zanikający w pogłosie kościelnym. | Unholy Slop perishes / Bluźnierczy slop ginie |
| `entity.hallucination.spawn` | `entity/hallucination/spawn1.ogg`, `entity/hallucination/spawn2.ogg` | hostile | Cichy szum + wdech tuż przy uchu. | Something appears / Coś się pojawia |
| `entity.hallucination.ambient` | `entity/hallucination/ambient1.ogg` … `entity/hallucination/ambient3.ogg` | hostile | Szepty, zniekształcone imię „...hey...”, mocny pogłos. | Whispers / Szepty |
| `entity.hallucination.hurt` | `entity/hallucination/hurt.ogg` | hostile | Rozmazany, „wodny” jęk. | Hallucination flickers / Halucynacja migocze |
| `entity.hallucination.dissolve` | `entity/hallucination/dissolve.ogg` | hostile | Rozpad w szum + ulga (opadający ton). | Hallucination fades / Halucynacja znika |
| `entity.slenderman.ambient` | `entity/slenderman/ambient1.ogg`, `entity/slenderman/ambient2.ogg` | hostile | Niski szum statyczny + odległy dźwięk dzwonu. | Static hums / Szumi statyka |
| `entity.slenderman.proximity` | `entity/slenderman/proximity.ogg` | hostile | Pętla (ok. 8 s, bez kliku na łączeniu) narastającego szumu; kod skaluje głośność wg dystansu. | Static intensifies / Statyka narasta |
| `entity.slenderman.teleport` | `entity/slenderman/teleport1.ogg` … `entity/slenderman/teleport3.ogg` | hostile | Glitch: cyfrowe zacięcie + bitcrush + krótki trzask, 0,3–0,5 s. | Slenderman glitches / Slenderman się zacina |
| `entity.slenderman.summon` | `entity/slenderman/summon.ogg` | hostile | Przyzwanie halucynacji: niski ryk przechodzący w chór szeptów. | Slenderman summons / Slenderman przyzywa |
| `entity.slenderman.hurt` | `entity/slenderman/hurt1.ogg`, `entity/slenderman/hurt2.ogg` | hostile | Trzask szumu + metaliczny zgrzyt. | Slenderman hurts / Slenderman obrywa |
| `entity.slenderman.death` | `entity/slenderman/death.ogg` | hostile | Długi (3 s) rozpad szumu w ciszę, na końcu jeden dzwon. | Slenderman dies / Slenderman ginie |
| `entity.slenderman.vanish` | `entity/slenderman/vanish.ogg` | hostile | Odejście o świcie: glitch + odwrócony dzwon. | Slenderman vanishes / Slenderman znika |
| `entity.slenderman.deadly_roar` | `entity/slenderman/deadly_roar.ogg` | hostile | Krwawy Księżyc: przemiana w Deadly Slendermana, ryk ze zniekształceniem. | Deadly Slenderman roars / Śmiertelny Slenderman ryczy |
| `music.eleven_reversed` | `music/eleven_reversed.ogg` | music | Pętla: przerobiona płyta 11 – spowolniona (ok. 0,6×) i odwrócona, stereo, płynna pętla. Kod gra ją ciągle (noc +10%, jaskinie +15%). | Eerie music plays / Gra niepokojąca muzyka |
| `ambient.low_sanity` | `ambient/low_sanity1.ogg` … `ambient/low_sanity4.ogg` | ambient | „Głośne, niepokojące dźwięki w tle” (Sanity 9–5): kroki za plecami, pukanie, drapanie, oddech. | Something is nearby / Coś jest w pobliżu |
| `ambient.heartbeat` | `ambient/heartbeat.ogg` | ambient | Bicie serca (Sanity ≤4), pętla 2 s. | Heartbeat / Bicie serca |
| `ambient.dark_corner` | `ambient/dark_corner.ogg` | ambient | Pętla w ciemnym zakątku: dron + roje owadów + trzaski. | Darkness hums / Ciemność brzęczy |
| `ambient.blood_moon.rise` | `ambient/blood_moon/rise.ogg` | ambient | Start Krwawego Księżyca: dzwon pogrzebowy + chór, ok. 6 s. | The Blood Moon rises / Wschodzi Krwawy Księżyc |
| `ambient.full_moon.bless` | `ambient/full_moon/bless.ogg` | ambient | Bajorko staje się święcone (pełnia): delikatny chór + dzwoneczki. | Water is blessed / Woda zostaje pobłogosławiona |
| `item.holy_water.sizzle` | `item/holy_water/sizzle1.ogg`, `item/holy_water/sizzle2.ogg` | players | Święta woda pali moba: skwierczenie + syk pary. | Holy water burns / Święcona woda parzy |
| `item.crucifix.use` | `item/crucifix/use1.ogg`, `item/crucifix/use2.ogg` | players | Aktywne użycie: jasny dzwonek + chór „aah”, krótki. | Crucifix glows / Krucyfiks rozbłyska |
| `item.crucifix.save` | `item/crucifix/save.ogg` | players | Funkcja totemu: rozbicie drewna + chór + dzwon. | Crucifix shatters / Krucyfiks pęka |
| `item.flashlight.toggle_on` | `item/flashlight/toggle_on.ogg` | players | Klik włącznika + brzęczenie żarówki. | Flashlight clicks on / Latarka się włącza |
| `item.flashlight.toggle_off` | `item/flashlight/toggle_off.ogg` | players | Klik włącznika (niższy). | Flashlight clicks off / Latarka się wyłącza |
| `item.flashlight.insert_core` | `item/flashlight/insert_core.ogg` | players | Wsunięcie rdzenia: metaliczny zatrzask. | Core inserted / Rdzeń włożony |
| `item.flashlight.remove_core` | `item/flashlight/remove_core.ogg` | players | Wysunięcie rdzenia: zatrzask + szuranie. | Core removed / Rdzeń wyjęty |
| `item.unholy_sword.charge` | `item/unholy_sword/charge.ogg` | players | Ładowanie klejnotem w kowadle: pulsujący, niski „wdech”. | Unholy Sword feeds / Bluźnierczy miecz się karmi |
| `item.unholy_sword.discharged` | `item/unholy_sword/discharged.ogg` | players | Wytrzymałość spada do 0: gasnące, opadające buczenie. | Unholy Sword goes dormant / Bluźnierczy miecz gaśnie |
| `item.thermos.drink` | `item/thermos/drink1.ogg`, `item/thermos/drink2.ogg` | players | Picie z termosu (łyk z metalowym pogłosem). | Drinking from thermos / Picie z termosu |
| `item.thermos.fill` | `item/thermos/fill.ogg` | players | Napełnianie: bulgotanie w metalowym naczyniu. | Thermos fills / Termos się napełnia |
| `block.tree_note.rip` | `block/tree_note/rip1.ogg`, `block/tree_note/rip2.ogg` | blocks | Zerwanie kartki z drzewa: darcie papieru. | Note torn / Kartka zerwana |
| `player.dehydrated` | `player/dehydrated1.ogg`, `player/dehydrated2.ogg` | players | Suchy kaszel (nawodnienie 0). | Player coughs / Gracz kaszle |


Klasyczne ambientowe dźwięki straszenia (wyższa szansa) to vanilla `minecraft:ambient.cave`, więc nie potrzebują plików.

---

## 7. Proponowane nazwy (lang)

| id | en_us | pl_pl |
|---|---|---|
| `item.unholymc.holy_water` | Holy Water | Woda święcona |
| `item.unholymc.splash_holy_water` | Splash Holy Water | Miotana woda święcona |
| `item.unholymc.crucifix` | Crucifix | Krucyfiks |
| `item.unholymc.flashlight` | Flashlight | Latarka |
| `item.unholymc.unholy_sword` | Unholy Sword | Bluźnierczy Miecz |
| `item.unholymc.thermos` | Thermos | Termos |
| `item.unholymc.nightmare_gem` | Nightmare Gem | Koszmarny klejnot |
| `effect.unholymc.wellbeing` | Wellbeing | Dobre samopoczucie |
| `enchantment.unholymc.holiness` | Holiness | Świętość |
| `entity.unholymc.slenderman` | Slenderman | Slenderman |
| `block.unholymc.tree_note` | Note | Kartka |

---

## 8. Do zrobienia / otwarte
- [ ] Pliki `.ogg` (produkcja u autora) według tabeli z sekcji 6.
- [ ] Ewentualne tekstury anomalii świata (np. „krzyże”), jeśli będą to nowe bloki, a nie tylko vanilla.
- [ ] Partikle (czarne cząsteczki ciemnego zakątka): na razie proponuję vanilla `minecraft:squid_ink` / `smoke`; własna tekstura tylko gdyby to nie wystarczało.
