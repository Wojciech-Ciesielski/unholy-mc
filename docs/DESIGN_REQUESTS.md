# Prośby do wątku designu (po testach v0.1.1)

Dla wątku designu. Od wersji 0.1.1 mod działa tylko po stronie serwera (Polymer), a wszystko, co widzi gracz, idzie przez **resource pack serwera**. Pliki `.ogg`, tekstury i modele nadal wasze, ścieżki zgodnie z `docs/ASSETS.md`.
Techniczne szczegóły i pozostałe błędy: `docs/BACKLOG.md`.

## 1. Dźwięki (najważniejsze)

Użytkownik: dźwięku „jest za mało”, a klimat horroru bez niego nie działa. **Żadne pliki `.ogg` z `sounds.json` nie istnieją** (klient loguje „file … does not exist”), więc gra jest w ciszy.

Prośba:
- Dostarczyć pliki z `sounds.json` (jumpscare'y, slopy, halucynacje, Slenderman, muzyka `eleven_reversed`, ambient, przedmioty).
- Dodać **znacznie więcej dźwięków otoczenia i używania**, np.:
  - otoczenie: szepty, odległe kroki, stukanie, trzask gałęzi, cichy oddech, skrzypienie drzew, zagłuszenie ptaków przed pojawieniem się slopa;
  - używanie przedmiotów: picie z termosu, otwieranie i zamykanie termosu, kliknięcie latarki, uderzenie krucyfiksu, rozbicie wody święconej, uderzenie Unholy Sworda;
  - kroki i ruch slopów oraz Slendermana (obecnie bezgłośne);
  - dźwięki trafienia slopów, jeśli mają się różnić od vanilla;
  - deszcz, burza i wiatr w klimacie (jeśli mają różnić się od vanilla).
- Nowe zdarzenia dodajcie do `sounds.json`, a ja podepnę wyzwalacze w kodzie. Podajcie nazwy i krótki opis, kiedy mają grać.

## 2. Jumpscare'y

Użytkownik: „tragedia”. Powody: **tło jest dalej widać** (obraz nie przykrywa ekranu), **nie są dynamiczne** (statyczny obraz), brak dźwięku (osobny punkt 1). Obecnie są dwa:
- screamer: pełnoekranowy obraz slopa przez ~0,7 s w trybie tytułu (obraz 248×248 w czcionce, rozmiar ograniczony atlasem czcionek),
- Spooky Jumpscare: ciemność, czerwony napis „you slept too long”, twarz.

Prośba: przegląd i propozycje poprawy. Mile widziane: seria kilku klatek zamiast jednej (z różnymi wariantami lub przesunięciem obrazu), obraz przykrywający cały ekran (z przyciemnieniem), zmiany skali i drżenie, szum. Proszę o dostarczenie klatek w rozmiarze, który zmieści się w czcionce (obecnie 248×248 na znak), jeśli potrzebna jest większa rozdzielczość, dajcie znać.

## 3. Animacje

Użytkownik: „w ogóle jest mało animacji”.
- **Termos:** brak animacji picia. Proszę o model/animację używania (po mojej stronie: sposób włączenia animacji picia na zastępniku przedmiotu).
- **Slopy (billboardy PNG):** stoją i płyną nad ziemią bez kroków. Potrzebne wersje klatek albo wskazówki, jak je animować (np. 2–4 klatki chodu, kołysanie, pochylenie przy goniącym slopie).
- **Slenderman:** chód ma być bardziej „ludzki”, a nie sztywny. Model 3D jest wasz (`ASSETS.md` §1.3), proszę o wytyczne ruchu (długość kroku, kołysanie ramion, pochylenie, przechył głowy).

## 4. Rozmiary slopów i Slendermana

Użytkownik: slopy są za małe, trzeba je „troszkę powiększyć”. Rozmiary bazowe są w `ASSETS.md` §1 (tabela billboardów i hitboxów). Proszę o nową propozycję rozmiarów i hitboxów (szerokość × wysokość) dla każdego slopa. Hitboxy są obecnie budowane na niewidzialnym zombie i nie pasują do obrazka, więc proszę o wymiary, które da się odwzorować dokładniej.

## 5. Krwawy Księżyc

Mamy teksturę `blood_moon.png`, ale na kliencie vanilla niebo i księżyc nie zmieniają wyglądu (czerwona winieta, tytuł i dźwięk są jedyną zmianą). Jeśli macie pomysł na wizualne wzmocnienie dostępne z resource packa (np. efekty post-process, zmiana tekstury księżyca w paczce), proszę o propozycję. Wykonalność sprawdzę po mojej stronie.

## 6. Wszystkie slopy i halucynacje jako moby 3D

Zmieniona decyzja użytkownika: **usuwamy wszystkie slopy w formie obrazków PNG** (nie tylko „śmieszne”) i budujemy na ich podstawie „**fotorealistyczne moby MC**”, czyli prawdziwe moby 3D. To samo dla halucynacji (czarne, półprzezroczyste wersje). Nadpisuje GDD 4 („slopy korzystają z grafik PNG zamiast modeli 3D”).
- Proszę o **modele 3D** (prostokątne bryły składane jak model Slendermana, bo tak da się je pokazać na kliencie vanilla), tekstury, wersje hallucination i zestawy części do animacji chodu dla **wszystkich siedmiu** slopów.
- Styl (potwierdzony przez użytkownika): **realistyczniejszy niż obrazki, ale dalej w stylu Minecrafta**: więcej detalu na teksturach, realistyczne proporcje.
- Rozmiary i hitboxy z punktu 4 dotyczą też nowych modeli.
- **Jumpscare'y używają dziś obrazów slopów.** Po usunięciu PNG potrzebne są osobne obrazy do jumpscare'ów (patrz punkt 2).

## 7. Atmosfera i kolory

- **Szarszy klimat w dzień:** kolory bardziej wyblakłe, niezbyt mocno. Jeśli macie pomysł, jak to osiągnąć z poziomu resource packa (np. wyblakłe tekstury trawy i liści, efekt post-process), proszę o propozycję.
- **Shadery:** użytkownik chce użyć gotowej, niewymagającej paczki open source (licencja MIT lub podobna) i dodać kilka efektów. Jeśli znacie dobrych kandydatów, podajcie.

## 8. Dźwięki przy niskim sanity (rozszerzenie punktu 1)

Przy sanity **poniżej 15** (≤ 14) mają być słyszalne: fałszywe kroki, krzyk „zza ściany”, fałszywe creepery (syk). Proszę o pliki i krótki opis, kiedy mają grać (częstość, głośność, kierunek).

## 9. Nowy HUD (ikony dynamiczne)

Nowa decyzja użytkownika (decyzja #34 w `DECISIONS.md`):
- **Lewy górny róg:** 4 statystki (zdrowie, głód, nawodnienie, sanity). Każda to **jedna ikona, która dynamicznie zmienia wygląd** i pokazuje **liczbę punktów** (nie liczbę ikon, jak dziś 10 sztuk).
- **Prawy górny róg:** założony zestaw, **tylko pancerz** (hełm, napierśnik, spodnie, buty).
- **Nad paskiem przedmiotów:** zdrowie zwierzaka i powietrze pod wodą. To są vanilla elementy, więc nie wymagają projektu.
- Vanilla serca, głód i pancerz są chowane przez podmianę ich sprite'ów w paczce na przezroczyste.

Proszę o projekt: wygląd ikon dla każdej z czterech statystyk w kilku stanach (np. pełny, średni, niski, krytyczny), sposób pokazania liczby punktów i układ pancerza. Dostępne tylko to, co da się zrobić zwykłą paczką zasobów i znakami czcionki (przezroczyste PNG), bez własnych shaderów. Wykonalność w rogach sprawdzę po mojej stronie, więc projekt powinien przetrwać różne skale GUI. Ikony mózgu i kropli, które już mamy (`textures/gui/sprites/hud/`), można wykorzystać jako punkt wyjścia.

## 10. Sekcje `ASSETS.md` do aktualizacji przez design

Po zmianach z tego pliku następujące sekcje `ASSETS.md` są nieaktualne i proszę o ich przepisanie:
- **§1 Slopy (billboardy PNG)** → moby 3D (punkt 6).
- **§4 HUD i efekty** → nowy HUD (punkt 9). Stare sprite'y 10 ikon przestają być używane.
- **§5 Jumpscare** → obrazy nie mogą już pochodzić z PNG slopów (punkt 2).
- **§5a Krwawy Księżyc** → ograniczenia klienta vanilla (punkt 5).
- **§6 Dźwięki** → nowe dźwięki (punkty 1 i 8).

---

## Status po stronie designu (2026-11-26)

| Punkt | Stan | Gdzie |
|---|---|---|
| 1, 8 Dźwięki | **gotowe** (wersje robocze, synteza): 68 zdarzeń / 130 plików, w tym nowe otoczenie, kroki, przedmioty, niskie Sanity; fałszywy creeper = vanilla `entity.creeper.primed` | `sounds.json`, `ASSETS.md` §6 (tabela wyzwalaczy §6.1), `assets_src/tools/make_audio.py` |
| 2 Jumpscare'y | **gotowe**: 4 twarze × 6 klatek, Spooky 8 klatek, nakładka szumu, tło na cały ekran, nowa czcionka `unholymc:jumpscare_frames` | `ASSETS.md` §5 |
| 5 Krwawy Księżyc | **gotowe do sprawdzenia**: paczka-nakładka (push/pop w trakcie gry), shadery niezweryfikowane na GPU | `resourcepacks/blood_moon/`, `ASSETS.md` §5a |
| 7 Atmosfera | szarszy dzień = wyblakłe kolormapy w głównej paczce; shadery: kandydaci w `ASSETS.md` §8 | `assets/minecraft/textures/colormap/` |
| 9 HUD | **gotowe, wariant autora**: 4 statystyki w sidebarze po prawej, bez ciemnego tła (shader `gui.vsh`), ikony wyrównane; pancerz w rogu i panel **usunięte** | `ASSETS.md` §4 |
| 6 Moby 3D | **anulowane przez autora (2026-11-26)**: modele wyglądały źle. Slopy i halucynacje **zostają billboardami PNG** (nadpisuje DECISIONS #36). Wersja robocza modeli została wycofana (revert) | – |
| 3, 4 Animacje, rozmiary | do zrobienia dla billboardów PNG; nowe slopy z obrazków autora czekają na pliki źródłowe | – |
