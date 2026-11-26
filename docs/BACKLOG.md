# The Unholy Minecraft – lista błędów i zmian po testach (v0.1.1)

Zapis uwag po „intensywnych” testach z użytkownikiem i znajomymi. **Nic tu jeszcze nie jest zrobione.**
Przyczyny oznaczone „hipoteza” nie zostały zweryfikowane w kodzie ani w grze.
Rzeczy dla wątku designu są dodatkowo w `docs/DESIGN_REQUESTS.md`.

Oznaczenia: **[B]** błąd, **[Z]** zmiana specyfikacji (nadpisuje GDD/DECISIONS), **[F]** nowa funkcja, **[?]** do doprecyzowania z użytkownikiem.

## Błędy

### B1. [B][?] Wysokość pasków sanity i nawodnienia
- Uwaga użytkownika: paski są wyżej, niż powinny. Podnoszą się tylko wtedy, gdy jest dodatkowa statystyka (pancerz), a pancerz ma być nad sanity.
- Stan obecny: w v0.1.1 paski są rysowane jako znaki czcionki w action barze, na stałej wysokości jeden rząd nad sercami. Bez pancerza zostaje więc pusty rząd między sercami a mózgami.
- Zrozumiałem to tak: bez pancerza mózgi i krople mają być tuż nad sercami i głodem, a przy pancerzu mają wędrować wyżej, nad niego.
- Ograniczenie: serwer nie może przesunąć rysowanego przez klienta pancerza ani bąbelków powietrza, więc układ „pancerz nad sanity” jest możliwy tylko w jedną stronę. Serwer zna wartość pancerza i powietrza gracza i może dynamicznie zmieniać wysokość glifów.
- Do ustalenia: czy chodzi o dynamiczne obniżanie pasków, gdy nie ma pancerza.

### B2. [B] Slopy i Slenderman „zapadają się pod ziemię” podczas chodzenia
- Wygląda komicznie.
- Hipoteza: obraz (display) jest przypięty jako pasażer niewidzialnego zombie. Skala zombie jest zmieniana atrybutem, a punkt siedzenia pasażera zależy od wysokości i od interpolacji, więc przy ruchu obraz opada względem podłoża.
- Do sprawdzenia: przesunięcie w pionie względem pozycji bytu, interpolacja i teleport w czasie chodu (`MobVisuals.java`).

### B3. [B] Slopy w ogóle nie spawnują się naturalnie
- Ani w lesie, ani na polanach. Użytkownik przyznaje, że mogło go nie być w lesie.
- Nowe wymaganie: pojedyncze agresywne slopy mają pojawiać się też na polanach i otwartym terenie (GDD 4 mówi „unikają otwartych powierzchni”, więc to zmiana, patrz Z-spawn).
- Hipoteza: reguły spawnu (`SlopSpawning`) mogą nie działać na serwerze dedykowanym albo warunki są zbyt wąskie (noc, ciemność, biom). Trzeba sprawdzić w logu i komendą, ile ich faktycznie się generuje.

### B4. [B] Anomalie jest „tragicznie mało”
- Cel: jedna co 50–60 kratek (poprzednio było 100–150).
- Stan obecny: siatka 128 kratek (`world.anomalyGridSize`), a część prób generowania się nie udaje (płaskość terenu, rośliny), więc w praktyce jest jeszcze mniej.
- Do zrobienia: zmniejszyć siatkę, zmierzyć rzeczywistą liczbę na świeżym świecie i poprawić warunki stawiania.

### B5. [B] Slopy i „anomalie” nie przemieszczają się, stoją w miejscu
- Doprecyzowanie użytkownika: chodzi o to, że **nie ruszają się z miejsca**, a nie o brak animacji kroków (animacje kroków to osobna sprawa, F1).
- Uwaga: B2 mówi, że slopy i Slenderman „zapadają się” podczas chodzenia, więc przynajmniej część z nich się porusza. Trzeba odtworzyć, kiedy stoją: może wolny AI, brak celu, albo obraz nie nadąża za bytem.
- Hipoteza: wygląd (display) może aktualizować pozycję rzadziej niż sam byt i jest widoczny jako stanie w miejscu między skokami; do sprawdzenia także AI (szukanie ścieżki, cel) po przeróbce na Polymera.
- „Anomalie”: nie jest jasne, czy chodzi o moby związane z anomaliami (np. zombie z dynią). Do potwierdzenia przy odtwarzaniu błędu.

### B6. [B] Hitboxy slopów i Slendermana
- Slopy i Slenderman są budowane na niewidzialnym zombie, więc hitbox jest kształtu zombie, skalowany atrybutem, a nie własny.
- Do sprawdzenia: czy dodatkowy element `Interaction` o właściwych wymiarach pozwoli na dokładniejsze trafianie, oraz zgodność wymiarów z `ASSETS.md`.

### B7. [B] Slopy są za małe
- Zwiększyć nieco rozmiary billboardów (w `ASSETS.md` są rozmiary bazowe) i dopasować hitboxy.

### B8. [B] Brak cząsteczek uderzenia
- Trafienie slopa, Slendermana i halucynacji nie daje żadnych cząsteczek.

### B9. [B] Krwawy Księżyc nie zmienia wyglądu nieba ani księżyca
- Jest komunikat, czerwona winieta na krawędziach i dźwięk, ale niebo i księżyc wyglądają normalnie.
- Ograniczenie vanilla klienta (wspomniane wcześniej), kod po stronie klienta został usunięty. Możliwe drogi: efekty post-process/shadery z paczki (patrz „Pomysły”) albo inne sztuczki. Wymaga badania.

## Zmiany specyfikacji

- **Z-szybkość deszczu [Z]:** odwrócić proporcje. Teraz deszcz jest tak częsty jak słońce, a słońce tak częste jak deszcz. Ma być: **deszcz często, słońce rzadko**. **Burze częściej.** Nadpisuje GDD 5.4 („+50% deszczu”). Parametry: `world.rainFrequencyMultiplier`, `world.guaranteedRainEveryDays`.
- **Z-mruganie [Z]:** efekt migania ekranu i ślepoty przy **odwodnieniu** ma działać tylko poniżej 4 pkt (3–0), a nie przy 4–0 jak teraz. (Użytkownik doprecyzował: chodziło o odwodnienie, nie o sanity.)
- **Z-spawn [Z]:** pojedyncze agresywne slopy mają pojawiać się też na polanach (patrz B3). Nadpisuje GDD 4.
- **Z-anomalie [Z]:** gęstość anomalii co 50–60 kratek (nadpisuje poprzednie 100–150).
- **Z-odwodnienie [Z][?]:** złagodzić efekty odwodnienia i niedojedzenia:
  - Mining Fatigue i Weakness występują **tylko na najniższym progu**.
  - „Bezpieczny” próg odwodnienia ma być taki jak u głodu (brak sprintu). W vanilla sprint blokuje głód 6 i mniej. Do potwierdzenia, że to samo ma dotyczyć nawodnienia (dziś brak sprintu przy 9–5).
  - Efekty pośrednie (Mining Fatigue przy 14–10, Weakness II i nudności przy 9–5) są **usuwane**.
  - Nadpisuje GDD 5.1 i DECISIONS #29.
- **Z-slopy-3D [Z]:** zrezygnować ze „śmiesznych” slopów w formie obrazków PNG i na ich podstawie zrobić **prawdziwe moby 3D**. To samo dla halucynacji. Nadpisuje GDD 4 („slopy korzystają z grafik PNG zamiast modeli 3D”). Które slopy uznać za śmieszne (w `ASSETS.md` oznaczone są `wig_guy` i `cigar_cat`), do potwierdzenia. Wymaga modeli i tekstur od designu (patrz `DESIGN_REQUESTS.md`). Przy okazji rozwiązuje część problemów z hitboxami i rozmiarami (B6, B7).

## Nowe funkcje

- **F1. Animacje:** „w ogóle jest mało animacji”. Konkretnie:
  - Termos nie ma animacji picia. Przedmiot pokazywany klientowi jest zastępnikiem, więc animacja picia musi pochodzić z komponentu przedmiotu (picie/jedzenie) i być ustawiona na zastępniku. Do sprawdzenia.
  - Slopy: animacja chodu (kołysanie, podskakiwanie, kroki).
  - Slenderman: chód ma być bardziej „ludzki”, a nie sztywny.
- **F2. Dźwięki:** dużo więcej dźwięków otoczenia i używania przedmiotów. Pliki `.ogg` należą do designu (`DESIGN_REQUESTS.md`). Po stronie kodu: dodać wyzwalacze, np. dla krótkich dźwięków przy użyciu przedmiotów, odgłosów otoczenia i kroków. Do tego czasu można dać zaślepki z dźwięków vanilla.
- **F3. Jumpscare'y:** użytkownik ocenia je jako „tragedia”. Powody podane przez niego:
  - **tło jest dalej widać** (obraz nie przykrywa całego ekranu),
  - **nie są dynamiczne** (statyczny obraz),
  - brak dźwięku (to dodamy później, pliki od designu).
  - Pomysły: przykrycie ekranu (np. ślepota/ciemność razem z obrazem), seria szybko zmieniających się klatek, drżenie i przybliżanie, szum. Ograniczenie: obraz jest znakiem czcionki w tytule, więc dynamikę trzeba robić serią tytułów lub dodatkowymi efektami. Wymaga zrobienia i sprawdzenia w grze.
- **F4. Komenda `/thm` do teleportacji do specjalnych struktur:** np. `/thm tp dark_corner|slender_forest|holy_pond|...`. Dziś jest tylko `/thm dark_corner locate` i `locate` dla lasu Slendermana.

## Pomysły od użytkownika i znajomych

- **I1. Więcej dźwięków otoczenia.** Konkretne listy i momenty do ustalenia osobno.
- **I2. Szarszy klimat w dzień.** Kolory bardziej wyblakłe, ale niezbyt mocno. Najpewniej przez efekty z paczki (patrz I7) albo inne sztuczki z resource packa.
- **I3. Dźwięki przy niskim sanity.** Przy sanity **poniżej bezpiecznego 15** (czyli ≤ 14) gracz ma słyszeć też: fałszywe kroki, krzyk dobiegający „zza ściany”, fałszywe creepery (syk). Dziś dźwięki tła pojawiają się dopiero przy ≤ 9. Do ustalenia: częstość i skalowanie z poziomem sanity. Pliki dźwięków od designu, na początek zaślepki z vanilla.
- **I4. Nowy wariant neutralnego slopa.** Atakuje **tylko wtedy, gdy gracz spojrzy mu w oczy** (jak Enderman). Wymaga sprawdzenia kierunku patrzenia po stronie serwera. Nadpisuje GDD 4 (neutralne „nie atakują”), więc to dodatkowy wariant.
- **I5. Mini-struktury: pojedyncze groby** rozsiane po mapie. Szczegóły (wygląd, zawartość, częstość, czy mają loot) do ustalenia. Pasują do bloku „Krzyż nagrobny”.
- **I6. Slopy i halucynacje jako moby 3D** (patrz Z-slopy-3D).
- **I7. Shadery z gotowej paczki.** Zamiast pisać od zera: wziąć gotową, **niewymagającą** paczkę open source na licencji **MIT** lub podobnie wolnej, i dodać kilka własnych efektów. Do zrobienia przed wyborem:
  - szukamy paczki, która działa jako zwykły resource pack (efekty z paczki zasobów), a nie jako shaderpack dla Iris, bo Iris wymaga instalacji u gracza, a serwer może dostarczyć tylko zwykłą paczkę,
  - sprawdzić licencję (wymagana atrybucja?), zgodność z 26.2, zużycie wydajności i zgodność z naszą paczką (łączenie plików),
  - pierwszy test: jeden efekt (ziarno i odbarwienie przy niskim sanity), sprawdzić działanie kamery i płynność (wcześniejsze ustalenia).
- Pozostałe pomysły: napięcie przed pojawieniem się czegoś (zapowiedzi, ciche zwierzęta, gasnące pochodnie), więcej wariantów kartek, ciemniejsze noce w lasach.

## Do doprecyzowania

- B1: czy paski mają być dynamicznie obniżane, gdy nie ma pancerza (patrz B1).
- Z-odwodnienie: dokładne progi (czy sprint blokowany przy ≤ 6 jak u głodu, i który próg to „najniższy” dla Mining Fatigue i Weakness).
- Z-slopy-3D: które slopy usuwamy (czy tylko `wig_guy` i `cigar_cat`?).
- I4: czy nowy neutralny ma zastąpić któryś z obecnych, czy dojść jako czwarty wariant.
- I5: co mają zawierać groby.
