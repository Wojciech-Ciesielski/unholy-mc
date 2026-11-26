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

### B5. [B][?] Slopy i anomalie nie chodzą, stoją w miejscu
- Slopy to obrazki przesuwające się bez animacji kroku, więc brakuje wrażenia chodzenia.
- „Anomalie”: nie wiem, o co chodzi. Czy o statyczne anomalie świata (krzyże, czaszki), czy o moby związane z anomaliami (np. zombie z dynią)? Do doprecyzowania.

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
- **Z-mruganie [Z][?]:** efekt migania ekranu i ślepoty ma działać tylko poniżej 4 pkt. Użytkownik napisał „sanity”, ale mruganie i ślepotę mamy dziś przy **odwodnieniu 4–0**. Do wyjaśnienia, który pasek ma na myśli i czy próg ma być „< 4” (a nie ≤ 4).
- **Z-spawn [Z]:** pojedyncze agresywne slopy mają pojawiać się też na polanach (patrz B3). Nadpisuje GDD 4.
- **Z-anomalie [Z]:** gęstość anomalii co 50–60 kratek (nadpisuje poprzednie 100–150).

## Nowe funkcje

- **F1. Animacje:** „w ogóle jest mało animacji”. Konkretnie:
  - Termos nie ma animacji picia. Przedmiot pokazywany klientowi jest zastępnikiem, więc animacja picia musi pochodzić z komponentu przedmiotu (picie/jedzenie) i być ustawiona na zastępniku. Do sprawdzenia.
  - Slopy: animacja chodu (kołysanie, podskakiwanie, kroki).
  - Slenderman: chód ma być bardziej „ludzki”, a nie sztywny.
- **F2. Dźwięki:** dużo więcej dźwięków otoczenia i używania przedmiotów. Pliki `.ogg` należą do designu (`DESIGN_REQUESTS.md`). Po stronie kodu: dodać wyzwalacze, np. dla krótkich dźwięków przy użyciu przedmiotów, odgłosów otoczenia i kroków. Do tego czasu można dać zaślepki z dźwięków vanilla.
- **F3. Jumpscare'y:** użytkownik ocenia je jako „tragedia”. Co konkretnie nie działa, trzeba dopytać. Podejrzenia: nie ma dźwięku (brak plików `.ogg`), zbyt krótki obraz, brak ruchu kamery, wygląd. Część jest po stronie designu.
- **F4. Komenda `/thm` do teleportacji do specjalnych struktur:** np. `/thm tp dark_corner|slender_forest|holy_pond|...`. Dziś jest tylko `/thm dark_corner locate` i `locate` dla lasu Slendermana.

## Pomysły (czekają na listę użytkownika)

- Shadery (efekty post-process z paczki): ziarno, odbarwienie i drżenie przy niskim sanity, wygląd Krwawego Księżyca, chromatyczna aberracja przy jumpscare'ach. Wykonalność dla 26.2 niesprawdzona. Proponowany test: jeden efekt (ziarno przy sanity ≤ 9) i sprawdzenie, jak zachowuje się kamera.
- Napięcie przed pojawieniem się czegoś (zapowiedzi, ciche zwierzęta, gasnące pochodnie), więcej wariantów kartek, ciemniejsze noce w lasach.
- Pozostałe pomysły znajomych: do dopisania.
