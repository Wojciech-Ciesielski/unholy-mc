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
Tekstura `textures/entity/slenderman/slenderman.png` (64×64, box UV). Twarz jest **pusta** (bez ust i oczu, tylko ledwo widoczne zapadnięte oczodoły), ciało czarne i wychudzone, z żebrami.

**Teleport:** tekstury `slenderman_static_0..3.png` to czarno-biała statyka TV w kształcie modelu, z wypadającymi poziomymi pasami. Przebieg (ok. 10 ticków):
1. W starym miejscu model przez 4–6 ticków renderuje się na zmianę jako statyka (klatka = `tick % 4`) i normalnie, z jitterem pozycji ±0,1 bloku.
2. Znika, a w nowym miejscu pojawia się od razu jako statyka przez 3–4 ticki, potem normalnie.
3. Dźwięk `entity.slenderman.teleport` odtwarza się w obu miejscach.
Tekstury encji nie obsługują `.mcmeta`, więc klatki przełącza kod.

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

## 2. Itemy (16×16, styl vanilla)

| Item id | Tekstury (`textures/item/`) | Model / wybór wariantu |
|---|---|---|
| `holy_water` | **vanilla** `minecraft:item/potion_overlay` + `minecraft:item/potion` | butelka identyczna z vanilla; płyn = tint warstwy 0 `0x9CE4FF` (stała w `items/holy_water.json`) |
| `splash_holy_water` | **vanilla** `minecraft:item/potion_overlay` + `minecraft:item/splash_potion` | jw. |
| `crucifix` | `crucifix.png` (drewno w palecie vanilla patyka, łza ghasta w środku) | prosty; glint enchantu vanilla |
| `nightmare_gem` | `nightmare_gem.png` (kształt jak emerald, fiolet z czerwonym rdzeniem) | prosty |
| `flashlight` | `flashlight.png` (jasna: ciemny metal, radełkowany uchwyt, czerwony przycisk, srebrny bezel, ciepła soczewka) / `flashlight_dark.png` (soczewka z koszmarnego klejnotu) + nakładki `flashlight_beam.png` / `flashlight_beam_dark.png` (stożek światła) | `custom_model_data.strings[0]` = rdzeń (wygląd zmienia tylko `nightmare_gem`), `flags[0]` = włączona |
| `unholy_sword` | `unholy_sword.png` (sylwetka jak miecze vanilla, karmazynowe ostrze, kościany jelec; 12 klatek, `frametime 2`: pulsująca żyła + unoszące się żarzące drobinki) / `unholy_sword_discharged.png` (popielate, spękane, bez cząsteczek) | `custom_model_data.flags[0]` = rozładowany |
| `thermos` | `thermos_{empty,water,holy_water,honey,milk}.png` (zielona piersiówka; kolor etykiety-kropli = płyn, pusty = bez etykiety) | `custom_model_data.strings[0]` = płyn |

Latarka ma **dwa wyglądy**: jasny (bez rdzenia lub dowolny rdzeń: lapis, redstone, emerald, diament, netheryt, gwiazda Netheru) i ciemny (`strings[0] = "nightmare_gem"`). Rdzeń dalej zmienia zasięg w logice, ale nie teksturę.
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

## 4. HUD i efekty (DECISIONS #34)

Czcionka `unholymc:hud_stats` (`font/hud_stats.json`, tekstury `textures/font/hud_stats/`, generator `assets_src/tools/make_hud.py`, podgląd `assets_src/preview/hud_stats_preview.png`). Wszystko to zwykłe glify, więc działa na kliencie vanilla bez shaderów. Stare sprite'y 10 ikon (`textures/gui/sprites/hud/*`) przestają być używane.

### 4.1 Lewy górny róg: 4 statystyki

Każda statystyka to wiersz: **panel** (półprzezroczyste tło) + **ikona 16×16** w stanie zależnym od wartości + **liczba punktów** z cyfr pixel-art.

| Stan | Punkty | Kolor liczby |
|---|---|---|
| `full` | 15–20 (i więcej, np. zdrowie z efektami) | `#FFFFFF` |
| `medium` | 10–14 | `#FFE070` |
| `low` | 5–9 | `#FF9A30` |
| `critical` | 0–4 | `#FF4040`; ikona na zmianę `critical` / `critical_blink` co 10 ticków (Sanity: `critical_jitter` losowo co 3–6 ticków, „drży”) |

| Znak | Glify |
|---|---|
| `U+E400–E408` | zdrowie: `full, medium, low, critical, critical_blink, poison, wither, frozen, absorption` |
| `U+E410–E415` | głód (udko, coraz bardziej obgryzione): `full, medium, low, critical, critical_blink, effect` (efekt Hunger) |
| `U+E420–E424` | nawodnienie (kropla opróżnia się): `full, medium, low, critical, critical_blink` |
| `U+E430–E435` | Sanity (mózg, coraz bardziej zgniły; w krytycznym oko i czarne krople): `full, medium, low, critical, critical_jitter, wellbeing` |
| `U+E480–E48B` | cyfry `0–9`, `+`, `/` (białe z obrysem, 7×9; kolor z koloru tekstu) |
| `U+E490` | panel wiersza 44×20 |
| `U+E4F0–E4F8` | spacje: −1, −2, −4, −8, −16, −21, −45, +2, +4 |

**Pierwszeństwo wariantów:** zdrowie `wither` > `poison` > `frozen` (zamrożenie w śniegu proszkowym) > stan wg punktów. Przy absorpcji za liczbą `+N` w kolorze `#F0C020`, a ikona `absorption`, gdy absorpcja ≥ zdrowie. Głód: `effect`, gdy działa efekt Hunger. Sanity: `wellbeing`, gdy działa efekt Wellbeing.

**Złożenie wiersza:** `E490` (panel) `E4F6` (−45, powrót na początek panelu) `E4F7` (+2) `<ikona>` `E4F7` `<cyfry>`. Wiersze jeden pod drugim co 22 px (osobne linie albo osobne paski bossbara, jak wygodniej). Ikony mają `height 16, ascent 12`, więc przy innym nośniku trzeba dostroić `ascent`.

### 4.2 Prawy górny róg: pancerz

4 sloty jeden pod drugim (hełm, napierśnik, spodnie, buty), co 24 px:
- ramka `U+E4A0` (`armor_slot.png`, 20×20),
- w środku przedmiot jako komponent tekstu **sprite** (26.x: `{"type":"object","atlas":"minecraft:items","sprite":"item/<id>"}`, dla zbroi skórzanej z tintem nie ma, więc wystarczy podstawowy sprite). Pusty slot pokazuje sprite vanilla z atlasu `minecraft:gui`: `container/slot/helmet`, `chestplate`, `leggings`, `boots`,
- pod ramką pasek wytrzymałości `U+E4B0–E4B8` (`durability_0` = prawie zniszczony, czerwony … `durability_8` = cały, zielony). `ascent −3`, więc rysuje się pod linią bazową.

### 4.3 Nad paskiem przedmiotów
Zdrowie wierzchowca i powietrze pod wodą zostają z vanilla, bez nowych grafik.

Efekt **Wellbeing**: `textures/mob_effect/wellbeing.png` (18×18). Kolor efektu: `0x8EE07A`.

---

## 5. Jumpscare – klient vanilla (tytuł z glifów czcionki)

Od v0.1.1 jumpscare to **tytuł** (`ClientboundSetTitleTextPacket`) złożony z glifów czcionki `unholymc:jumpscare_frames` (`font/jumpscare_frames.json`, generator `assets_src/tools/make_jumpscare_frames.py`). Obrazy **nie pochodzą już z PNG slopów**: twarze są wycięte z surowych grafik i przerobione (czerń, krwista czerwień, trupia biel, ziarno, winieta wtapiająca się w czerń). Podgląd: `assets_src/preview/jumpscare_frames_preview.png`.

### 5.1 Glify

Wszystkie klatki 248×248. W tytule vanilla skaluje tekst ×4, więc glif twarzy o `height` 96 ma ok. 384 px GUI, a tło o `height` 256 ma ok. 1024 px GUI i przykrywa cały ekran przy każdej skali GUI.

| Znak | Plik | height / ascent | Co to |
|---|---|---|---|
| `U+E300` | `backdrop.png` (8×8, czerń) | 256 / 128 | tło przykrywające ekran |
| `U+E301` | `backdrop_red.png` (8×8, `#280002`) | 256 / 128 | tło na ostatnią klatkę / Spooky |
| `U+E310`–`E315` | `screamer_1_0..5` – the_locust | 96 / 52 | klatki 0–5 |
| `U+E318`–`E31D` | `screamer_2_0..5` – love_note | 96 / 52 | |
| `U+E320`–`E325` | `screamer_3_0..5` – cień ze świecącymi oczami | 96 / 52 | |
| `U+E328`–`E32D` | `screamer_4_0..5` – blada głowa (pobierz_4) | 96 / 52 | |
| `U+E330`–`E337` | `spooky_0..7` – the_hollow_screamer, rozciągnięta paszcza | 96 / 52 | Spooky Jumpscare |
| `U+E340`–`E343` | `static_0..3` (półprzezroczysty szum) | 96 / 52 | nakładka na twarz |
| `U+E3F0` | space −257 | – | cofnięcie o szerokość tła |
| `U+E3F1` | space −97 | – | cofnięcie o szerokość twarzy |
| `U+E3F2` / `U+E3F3` | space +80 / +79 | – | wyśrodkowanie twarzy na tle |

`ascent` to punkt wyjścia: wątek implementacji niech go dostroi, aż twarz będzie na środku ekranu (tytuł vanilla rysuje się trochę nad środkiem).

**Złożenie jednej klatki** (warstwy nakładają się dzięki ujemnym spacjom, całość ma szerokość tła, więc tytuł jest wyśrodkowany):
`E300 E3F0 E3F2 <twarz> E3F1 <static> E3F3`. Statykę można pominąć (wtedy `E3F2 <twarz> E3F3`, a po twarzy `+1` do wyrównania nie jest potrzebne).

### 5.2 Zwykły jumpscare (screamer), ok. 12 ticków
- `jumpscare.screamer<n>` (dźwięk) i twarz `n` (1–4) losowane razem, żeby wrzask pasował do twarzy.
- Tytuł: `fadeIn 0, stay 2, fadeOut 0`, **nowy pakiet co 2 ticki** z kolejną klatką `screamer_n_0 … _5`, statyka `static_(tick % 4)` na klatkach 0, 2, 4 i 5.
- Klatka 3 to błysk negatywu, a 5 to rozpad w szum. Po ostatniej klatce `ClientboundClearTitlesPacket`.
- Drżenie: kamerę ruszyć się nie da, więc drżenie i najazd są **wypalone w klatkach**. Na serwerze można dodać krótkie `Nausea` (20 ticków) i `Darkness` 10 ticków po wszystkim.
- −10 Sanity (logika bez zmian). Krucyfiks w ręce blokuje całość.

### 5.3 Spooky Jumpscare, ok. 6 s, kończy się śmiercią
1. **0–1 s:** cisza (`stopSound` wszystkiego), `Blindness` 3 s.
2. **1–4 s:** dźwięk `jumpscare.spooky_buildup`, tytuł z tłem `E301` i czerwonym tekstem `jumpscare.unholymc.spooky_text` migającym 3×.
3. **4–5,5 s:** dźwięk `jumpscare.spooky`, klatki `spooky_0 … _7` co 2 ticki (16 ticków), na wszystkich statyka, tło `E300`, na klatkach 3 i 5 tło `E301`.
4. **5,5 s:** czarne tło `E300` (tytuł z samym tłem) i zabicie gracza przez serwer. Komunikat śmierci `death.attack.unholymc.spooky_jumpscare`.

Stare `gui/jumpscare/*` (`spooky_face`, `blood_vignette`, sprite `static`) były dla klienta z modem i nie są potrzebne na kliencie vanilla. Zostają w repo na wypadek powrotu do wersji z klientem.

---

## 5a. Krwawy Księżyc – klient vanilla

Klient vanilla nie zmieni nieba z poziomu serwera, ale **może przyjąć drugą paczkę zasobów w trakcie gry**: `ClientboundResourcePackPushPacket` dokłada paczkę na wierzch, `ClientboundResourcePackPopPacket` ją zdejmuje (od 1.20.3). Dlatego Krwawy Księżyc to osobna, mała **paczka-nakładka** `src/main/resources/resourcepacks/blood_moon/` (format 88 = 26.2, generator `assets_src/tools/make_packs.py`):

| Plik w nakładce | Efekt |
|---|---|
| `textures/environment/celestial/moon/full_moon.png` | krwisty księżyc z większą tarczą (12×12 zamiast 8×8) i szeroką czerwoną łuną. Krwawy Księżyc wypada zawsze w pełnię, więc wystarczy podmienić pełnię |
| `shaders/core/sky.fsh` | niebo w odcieniach krwi |
| `shaders/core/stars.fsh` | gwiazdy czerwone i przygaszone |
| `shaders/core/rendertype_clouds.fsh` | chmury krwiste |
| `shaders/include/fog.glsl` | krwista mgła (horyzont, dal) i lekki czerwony odcień całego świata, mobów i cząsteczek |
| `textures/colormap/{grass,foliage,dry_foliage}.png` | trawa i liście w kolorze rdzawej czerwieni |

**Przebieg po stronie serwera:**
1. Start eventu (zmierzch): push nakładki do graczy w Overworldzie (wymagana, `required = true`, żeby nikt jej nie odrzucił), tytuł i dźwięk `ambient.blood_moon.rise`.
2. Gracz wchodzi do Overworldu w trakcie eventu: push.
3. Świt albo wyjście z Overworldu: pop.

Klient przeładowuje zasoby przy pushu i popie: to ok. 1–3 s „zawieszenia” z ekranem ładowania, dwa razy na event. Traktujemy to jako część dramaturgii (ekran gaśnie i wraca czerwony). Hosting: Polymer autohost serwuje główną paczkę; nakładkę trzeba spakować (zip) przy starcie serwera i podać pod drugim URL-em (albo osobnym `id` paczki, jeśli autohost to obsługuje). Do sprawdzenia po stronie implementacji.

**Niezweryfikowane:** nie mam tu GPU ani walidatora GLSL. Shadery to małe zmiany plików vanilla 26.2 (te same uniformy i wejścia), ale trzeba je raz uruchomić. Jeśli coś się nie skompiluje, klient vanilla pokaże błąd ładowania paczki, a wtedy wystarczy usunąć plik `.fsh`/`.glsl` z nakładki (księżyc i kolormapy działają bez shaderów).

Tytuły i komunikaty (bez zmian):

| klucz | en_us | pl_pl |
|---|---|---|
| `title.unholymc.blood_moon` | The Blood Moon rises | Wschodzi Krwawy Księżyc |
| `title.unholymc.blood_moon.sub` | No one sleeps tonight | Tej nocy nikt nie zaśnie |
| `message.unholymc.blood_moon.no_sleep` | You can't sleep under the Blood Moon | Nie zaśniesz pod Krwawym Księżycem |

## 5b. Atmosfera i kolory (DECISIONS #43)

- **Szarszy dzień:** główna paczka nadpisuje `minecraft:textures/colormap/{grass,foliage,dry_foliage}.png` wersjami −30% nasycenia i −6% jasności (pochodne vanilla 26.2, generator `make_packs.py`). Trawa i liście we wszystkich biomach są lekko wyblakłe, a niebo i woda zostają vanilla. To bezpieczna podstawa i działa bez shaderów.
- **Shadery:** propozycje gotowych paczek są w sekcji 8. Własne efekty (np. lekka globalna desaturacja, winieta) dopiszemy do wybranej paczki, żeby dwie paczki nie nadpisywały tych samych plików `shaders/core`.

---

## 6. Dźwięki

**Stan (v0.1.1+):** wszystkie pliki z `sounds.json` (68 zdarzeń, 130 plików, ok. 4,6 MB) są wygenerowane proceduralnie przez `assets_src/tools/make_audio.py` (synteza w numpy + `ffmpeg`/libvorbis). Muzyka to przerobiona płyta 11 (odwrócona, spowolniona ×0,6, stereo, pogłos, bezszwowa pętla 115 s). Oryginał `11.ogg` leży lokalnie w `assets_src/audio_src/`, który jest w `.gitignore`. To **wersje robocze do odsłuchu**: każdy plik można podmienić nagraniem o tej samej nazwie, a przepis w `make_audio.py` poprawić i wygenerować ponownie (`python3 make_audio.py <filtr>`). Kontrola poziomów: `python3 assets_src/tools/check_audio.py`.

**Format:** OGG Vorbis, 44,1 kHz, q5 (muzyka q3). **Mono** wszystko, co ma pozycję w świecie. **Stereo** tylko `music.eleven_reversed`, `jumpscare.*`, `ambient.blood_moon.rise` (grane „w głowie”, bez pozycji). Głośność ok. −18 dB RMS, jumpscare'y −11/−12 dB (naprawdę głośne), ambient tła −20…−22 dB. Szczyt ≤ −3 dBFS, bez ciszy na początku pliku. Pętle (`music.eleven_reversed`, `entity.slenderman.proximity`, `ambient.dark_corner`, `ambient.heartbeat`, `entity.slop.float`) łączą się bez kliku.

### 6.1 Kiedy grają nowe dźwięki (wyzwalacze dla kodu)

| Zdarzenie | Kiedy | Częstość | Skąd / głośność |
|---|---|---|---|
| `ambient.forest.whisper` | noc, las | co 40–120 s | 3–6 kratek od gracza, losowy kierunek, vol 0,6 |
| `ambient.forest.distant_steps` | noc, las | co 60–150 s | 12–24 kratki, vol 0,8 |
| `ambient.forest.knock` | noc, las | co 2–5 min | 16–32 kratki, vol 1,0 (słychać daleko) |
| `ambient.forest.branch_snap` | noc, las; także gdy slop jest w promieniu 24 kratek | co 30–90 s | 8–16 kratek, częściej **za plecami** (kąt 120–240° od kierunku patrzenia) |
| `ambient.forest.breath` | noc, Sanity ≤ 14 | co 3–6 min | 2–3 kratki **za plecami**, vol 0,5 |
| `ambient.forest.tree_creak` | las, zawsze; w deszczu ×2 | co 20–60 s | najbliższy pień 4–12 kratek |
| `ambient.forest.birds` | dzień, las, brak deszczu | co 8–20 s | korona drzewa 6–16 kratek, vol 0,5 |
| `ambient.forest.birds_silence` | slop pojawia się w promieniu 32 kratek gracza w dzień w lesie | raz na pojawienie | najpierw `stopSound(ambient.forest.birds)`, potem ten dźwięk; przez kolejne 60 s ptaki nie grają |
| `ambient.wind` | zawsze na powierzchni; w deszczu i burzy częściej | dzień co 60–120 s, deszcz co 15–40 s | z losowego kierunku poziomo, 10 kratek, vol 0,7 (burza 1,0) |
| `ambient.sanity.fake_steps` | Sanity ≤ 14 | co 60–180 s (≤ 9: co 30–90 s) | 2–4 kratki **za plecami**; gdy gracz się odwróci (zmiana yaw > 90°), kod przerywa (`stopSound`) |
| `ambient.sanity.wall_scream` | Sanity ≤ 14 | co 3–6 min (≤ 9: co 1,5–3 min) | z **najbliższego bloku stałego** 3–8 kratek (za ścianą); jeśli brak – 10 kratek pod ziemią |
| fałszywy creeper | Sanity ≤ 14 | co 4–8 min (≤ 9: co 2–4 min) | **vanilla `minecraft:entity.creeper.primed`** (najbardziej przekonujący, bez nowego pliku), 1,5–2,5 kratki za plecami, tylko do tego gracza; bez wybuchu |
| `entity.slop.step` | slop agresywny/neutralny idzie | co ok. 0,4 s ruchu (jak vanilla `playStepSound`) | z moba, vol 0,5 |
| `entity.slop.unholy.step` | unholy slop idzie | co ok. 0,45 s | z moba, vol 0,7 |
| `entity.slop.float` | duszek (`sheet_ghost`) się porusza | pętla co 2 s | z moba, vol 0,4 |
| `entity.slenderman.step` | Slenderman idzie | co ok. 0,8 s | z moba, vol 1,0 (słychać z 24 kratek) |
| `item.thermos.open` / `close` | początek / koniec picia z termosu i napełniania | – | gracz |
| `item.crucifix.hit` | aktywne użycie krucyfiksu trafia moba (Holiness) | – | trafiony mob |
| `item.holy_water.shatter` | splash wody święconej uderza | – | punkt uderzenia (zamiast vanilla `splash_potion.break`) |
| `item.unholy_sword.hit` | trafienie Unholy Swordem | – | trafiony mob |

Szepty, kroki i oddech **za plecami** liczymy względem kierunku patrzenia gracza w chwili odtworzenia. Dźwięki z sekcji „niskie Sanity” odtwarzamy **tylko dla tego gracza** (`ServerPlayer.connection.send(ClientboundSoundPacket)`), żeby inni ich nie słyszeli.

### 6.2 Pełna lista

Ścieżka pliku = `src/main/resources/assets/unholymc/sounds/<kolumna „Pliki”>`. Napisy (subtitles): klucz `subtitles.unholymc.<event>`, teksty EN/PL w ostatniej kolumnie.

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
| `ambient.forest.whisper` | `ambient/forest/whisper1.ogg` … `ambient/forest/whisper3.ogg` | ambient | Pojedynczy, niezrozumiały szept tuż przy uchu. | Something whispers / Coś szepcze |
| `ambient.forest.distant_steps` | `ambient/forest/distant_steps1.ogg` … `ambient/forest/distant_steps3.ogg` | ambient | Kilka kroków po liściach w oddali, urywają się nagle. | Distant footsteps / Kroki w oddali |
| `ambient.forest.knock` | `ambient/forest/knock1.ogg`, `ambient/forest/knock2.ogg` | ambient | 3–5 uderzeń w drewno, daleko w lesie. | Knocking / Stukanie |
| `ambient.forest.branch_snap` | `ambient/forest/branch_snap1.ogg` … `ambient/forest/branch_snap3.ogg` | ambient | Trzask łamanej gałęzi. | Branch snaps / Trzask gałęzi |
| `ambient.forest.breath` | `ambient/forest/breath1.ogg`, `ambient/forest/breath2.ogg` | ambient | Cichy, wolny oddech (wdech + wydech). | Something breathes / Coś oddycha |
| `ambient.forest.tree_creak` | `ambient/forest/tree_creak1.ogg` … `ambient/forest/tree_creak3.ogg` | ambient | Skrzypienie pnia na wietrze. | Tree creaks / Drzewo skrzypi |
| `ambient.forest.birds` | `ambient/forest/birds1.ogg` … `ambient/forest/birds3.ogg` | ambient | Ćwierkanie ptaków w lesie (dzień), ok. 5 s. | Birds chirp / Ptaki ćwierkają |
| `ambient.forest.birds_silence` | `ambient/forest/birds_silence.ogg` | ambient | Trzepot skrzydeł, potem nagła cisza z cichym piskiem w uszach – slop w pobliżu. | Birds fall silent / Ptaki milkną |
| `ambient.wind` | `ambient/wind1.ogg` … `ambient/wind3.ogg` | weather | Poryw wiatru z wyciem, 4–6 s. | Wind howls / Wyje wiatr |
| `ambient.sanity.fake_steps` | `ambient/sanity/fake_steps1.ogg` … `ambient/sanity/fake_steps4.ogg` | ambient | 3–6 kroków tuż za plecami gracza. | Footsteps behind you / Kroki za tobą |
| `ambient.sanity.wall_scream` | `ambient/sanity/wall_scream1.ogg` … `ambient/sanity/wall_scream3.ogg` | ambient | Stłumiony krzyk „zza ściany” (przefiltrowany jak przez kamień). | Muffled scream / Stłumiony krzyk |
| `entity.slop.step` | `entity/slop/step1.ogg` … `entity/slop/step4.ogg` | hostile | Kroki slopów: miękkie, mokre szuranie. | Slop shuffles / Slop szura |
| `entity.slop.unholy.step` | `entity/slop/unholy/step1.ogg` … `entity/slop/unholy/step3.ogg` | hostile | Kroki unholy slopa: ciężkie, z chrzęstem kości. | Bones crunch / Chrzęst kości |
| `entity.slop.float` | `entity/slop/float.ogg` | neutral | Duszek z prześcieradła: cichy, falujący świst (pętla ok. 2 s). | Cloth flutters / Łopot materiału |
| `entity.slenderman.step` | `entity/slenderman/step1.ogg` … `entity/slenderman/step3.ogg` | hostile | Kroki Slendermana: powolne, ciężkie, z echem. | Heavy footsteps / Ciężkie kroki |
| `item.thermos.open` | `item/thermos/open.ogg` | players | Odkręcenie zakrętki (gwint + pyknięcie). | Thermos opens / Termos się otwiera |
| `item.thermos.close` | `item/thermos/close.ogg` | players | Zakręcenie (gwint + stuk). | Thermos closes / Termos się zamyka |
| `item.crucifix.hit` | `item/crucifix/hit1.ogg`, `item/crucifix/hit2.ogg` | players | Trafienie krucyfiksem: tępe uderzenie drewna + krótki dzwonek. | Crucifix strikes / Krucyfiks uderza |
| `item.holy_water.shatter` | `item/holy_water/shatter1.ogg`, `item/holy_water/shatter2.ogg` | players | Rozbicie miotanej wody święconej: szkło + plusk + krótki chór. | Holy water shatters / Woda święcona się rozbija |
| `item.unholy_sword.hit` | `item/unholy_sword/hit1.ogg` … `item/unholy_sword/hit3.ogg` | players | Trafienie Unholy Swordem: mięsiste cięcie + niski, zniekształcony puls. | Unholy Sword cuts / Bluźnierczy miecz tnie |


Klasyczne dźwięki straszenia z vanilla (wyższa szansa) to nadal `minecraft:ambient.cave`.

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
