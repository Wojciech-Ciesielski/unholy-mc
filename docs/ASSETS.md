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

## 5a. Krwawy Księżyc – wygląd

Tekstura: `textures/environment/celestial/blood_moon.png` (32×32). Format jest taki sam jak vanilla `environment/celestial/moon/*.png`: obraz nieprzezroczysty, czarne tło = brak światła (niebo rysuje księżyc addytywnie), tarcza 8×8 w środku i poświata dookoła. Krwawy Księżyc jest zawsze w pełni, bo wypada w pełnię, więc wystarczy jedna klatka. Podgląd: `assets_src/preview/blood_moon_preview.png`.

Co kod podmienia na kliencie, gdy serwer zsynchronizuje „Krwawy Księżyc aktywny”:

| Element | Wartość | Uwagi |
|---|---|---|
| Tekstura księżyca | `unholymc:textures/environment/celestial/blood_moon.png` | zamiast `full_moon.png` (mixin w renderze nieba) |
| Rozmiar księżyca | ×1,6 względem vanilla | ma wisieć nad graczem, wyraźnie większy |
| Kolor nieba (noc) | mnożony / mieszany do `#3A0608` | zmiana stopniowo przez **200 ticków** o zmierzchu i z powrotem o świcie |
| Mgła | `#2A0505`, zasięg ×0,8 | nocą świat robi się ciaśniejszy |
| Chmury | tint `#5A1010` | |
| Gwiazdy | kolor `#FF5A50`, jasność ×0,6 | |
| Światło świata | lekko czerwony odcień oświetlenia nocnego (lightmap, ok. +15% R) | opcjonalnie, jeśli łatwo |
| Start eventu | tytuł `title.unholymc.blood_moon` + podtytuł `title.unholymc.blood_moon.sub` (kolor `#C8281C`), dźwięk `ambient.blood_moon.rise` | tylko dla graczy w Overworldzie |
| Próba snu | komunikat na pasku akcji `message.unholymc.blood_moon.no_sleep` | łóżko się nie aktywuje |

Teksty:

| klucz | en_us | pl_pl |
|---|---|---|
| `title.unholymc.blood_moon` | The Blood Moon rises | Wschodzi Krwawy Księżyc |
| `title.unholymc.blood_moon.sub` | No one sleeps tonight | Tej nocy nikt nie zaśnie |
| `message.unholymc.blood_moon.no_sleep` | You can't sleep under the Blood Moon | Nie zaśniesz pod Krwawym Księżycem |

Bajorka nie stają się święcone (patrz DECISIONS 16), więc nie mają cząsteczek ani poświaty.

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
