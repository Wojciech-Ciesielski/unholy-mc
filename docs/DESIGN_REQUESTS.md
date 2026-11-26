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

## 6. Slopy i halucynacje jako moby 3D

Nowa decyzja użytkownika: rezygnujemy ze „śmiesznych” slopów w formie obrazków PNG i budujemy na ich podstawie **prawdziwe moby 3D**. To samo dla halucynacji (czarne, półprzezroczyste wersje). Nadpisuje GDD 4 („slopy korzystają z grafik PNG zamiast modeli 3D”).
- Które slopy są „śmieszne”, do potwierdzenia z użytkownikiem (w `ASSETS.md` oznaczone są `wig_guy` i `cigar_cat`). Straszne slopy (np. `shadow_watcher`, `hollow_screamer`, `locust`) prawdopodobnie zostają obrazkami, ale też do potwierdzenia.
- Proszę o **modele 3D** (wielokąty w formacie, jaki da się podłączyć jako elementy modelu: prostokątne bryły jak model Slendermana) i tekstury, oraz wersje hallucination. Potrzebne zestawy części do animacji chodu.
- Rozmiary i hitboxy z punktu 4 dotyczą też nowych modeli.

## 7. Atmosfera i kolory

- **Szarszy klimat w dzień:** kolory bardziej wyblakłe, niezbyt mocno. Jeśli macie pomysł, jak to osiągnąć z poziomu resource packa (np. wyblakłe tekstury trawy i liści, efekt post-process), proszę o propozycję.
- **Shadery:** użytkownik chce użyć gotowej, niewymagającej paczki open source (licencja MIT lub podobna) i dodać kilka efektów. Jeśli znacie dobrych kandydatów, podajcie.

## 8. Dźwięki przy niskim sanity (rozszerzenie punktu 1)

Przy sanity **poniżej 15** (≤ 14) mają być słyszalne: fałszywe kroki, krzyk „zza ściany”, fałszywe creepery (syk). Proszę o pliki i krótki opis, kiedy mają grać (częstość, głośność, kierunek).
