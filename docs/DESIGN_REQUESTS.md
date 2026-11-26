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

Użytkownik: „tragedia”. Szczegółów nie podał, dopytam go. Obecnie są dwa:
- screamer: pełnoekranowy obraz slopa przez ~0,7 s, w trybie tytułu (obraz 248×248 w czcionce, rozmiar ograniczony atlasem czcionek),
- Spooky Jumpscare: ciemność, czerwony napis „you slept too long”, twarz.

Proszę o przegląd i propozycje poprawy: przeprojektowanie obrazu, krótka seria klatek zamiast jednej, dźwięk wchodzący przed obrazem, ewentualnie inne podejście, jeśli obecne ograniczenia (obraz w czcionce, bez ruchu kamery) są powodem słabego efektu.

## 3. Animacje

Użytkownik: „w ogóle jest mało animacji”.
- **Termos:** brak animacji picia. Proszę o model/animację używania (po mojej stronie: sposób włączenia animacji picia na zastępniku przedmiotu).
- **Slopy (billboardy PNG):** stoją i płyną nad ziemią bez kroków. Potrzebne wersje klatek albo wskazówki, jak je animować (np. 2–4 klatki chodu, kołysanie, pochylenie przy goniącym slopie).
- **Slenderman:** chód ma być bardziej „ludzki”, a nie sztywny. Model 3D jest wasz (`ASSETS.md` §1.3), proszę o wytyczne ruchu (długość kroku, kołysanie ramion, pochylenie, przechył głowy).

## 4. Rozmiary slopów i Slendermana

Użytkownik: slopy są za małe, trzeba je „troszkę powiększyć”. Rozmiary bazowe są w `ASSETS.md` §1 (tabela billboardów i hitboxów). Proszę o nową propozycję rozmiarów i hitboxów (szerokość × wysokość) dla każdego slopa. Hitboxy są obecnie budowane na niewidzialnym zombie i nie pasują do obrazka, więc proszę o wymiary, które da się odwzorować dokładniej.

## 5. Krwawy Księżyc

Mamy teksturę `blood_moon.png`, ale na kliencie vanilla niebo i księżyc nie zmieniają wyglądu (czerwona winieta, tytuł i dźwięk są jedyną zmianą). Jeśli macie pomysł na wizualne wzmocnienie dostępne z resource packa (np. efekty post-process, zmiana tekstury księżyca w paczce), proszę o propozycję. Wykonalność sprawdzę po mojej stronie.
