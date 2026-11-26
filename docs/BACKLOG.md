# The Unholy Minecraft – lista błędów i zmian po testach (v0.1.1)

Zapis uwag po „intensywnych” testach z użytkownikiem i znajomymi. **Nic tu jeszcze nie jest zrobione.**
Przyczyny oznaczone „hipoteza” nie zostały zweryfikowane w kodzie ani w grze.
Rzeczy dla wątku designu są dodatkowo w `docs/DESIGN_REQUESTS.md`.

Oznaczenia: **[B]** błąd, **[Z]** zmiana specyfikacji (nadpisuje GDD/DECISIONS), **[F]** nowa funkcja, **[?]** do doprecyzowania z użytkownikiem.

## Błędy

### B1. [B] Wysokość pasków sanity i nawodnienia — zastąpione przez Z-HUD
- Uwaga użytkownika: paski są wyżej, niż powinny. Podnoszą się tylko przy dodatkowej statystyce (pancerz), a pancerz ma być nad sanity.
- Stan obecny: w v0.1.1 paski to znaki czcionki w action barze na stałej wysokości, jeden rząd nad sercami, więc bez pancerza zostaje pusty rząd.
- **Ten punkt traci znaczenie**, bo cały HUD zostaje przeprojektowany (patrz Z-HUD poniżej). Zostawiam jako zapis historii.

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
- **Z-mruganie [Z][?]:** efekt migania ekranu i ślepoty/ciemności przy odwodnieniu. Ostatnia wypowiedź użytkownika: działa gdy nawodnienie jest **mniej niż 6 ikon kropli**. Wcześniej mówił „poniżej 4”. Niejasne: 6 ikon to 12 pkt, a 6 pkt to 3 ikony. Do ustalenia jednostka i próg.
- **Z-spawn [Z]:** pojedyncze agresywne slopy mają pojawiać się też na polanach (patrz B3). Nadpisuje GDD 4.
- **Z-anomalie [Z]:** gęstość anomalii co 50–60 kratek (nadpisuje poprzednie 100–150).
- **Z-odwodnienie [Z][?]:** złagodzić efekty (nadpisuje GDD 5.1 i DECISIONS #29). Zasady użytkownika po doprecyzowaniu:
  - **Mining Fatigue i Weakness** występują dopiero, gdy **obie** statystyki, głód **i** nawodnienie, są poniżej 6.
  - **Miganie ekranu i darkness** przy nawodnieniu poniżej 6 ikon kropli (patrz Z-mruganie).
  - **Brak sprintu:** próg jak u głodu (poniżej 6).
  - Efekty pośrednie (Mining Fatigue przy 14–10, Weakness II i nudności przy 9–5) są **usuwane**.
  - **Do ustalenia jednostka „6”:** użytkownik mówi „ikon kropli”, ale w vanilla próg sprintu głodu to 6 punktów (3 ikony). Czy „6” to punkty (3 ikony), czy 6 ikon (12 pkt)? Do zapytania, zanim to zakoduję.
- **Z-slopy-3D [Z][?]:** usuwamy **wszystkie** slopy w formie obrazków PNG (nie tylko „śmieszne”, to zmiana względem poprzedniej wersji). Na ich podstawie powstają „**fotorealistyczne moby MC**”, czyli prawdziwe moby 3D. Halucynacje tak samo. Nadpisuje GDD 4 („slopy korzystają z grafik PNG zamiast modeli 3D”).
  - Wymaga modeli i tekstur od designu (patrz `DESIGN_REQUESTS.md`).
  - Rozwiązuje część problemów z hitboxami i rozmiarami (B6, B7) i animacją chodu (F1).
  - **Do ustalenia styl:** „fotorealistyczne” w grze z kwadratowymi blokami może znaczyć „bardziej realistyczne niż obrazki” albo „w stylu Minecrafta, ale realistyczne”. Dopytać.
  - **Skutek uboczny:** obrazy slopów są dziś używane jako twarze jumpscare'ów. Po usunięciu PNG jumpscare'y potrzebują własnych obrazów (F3).
- **Z-HUD [Z]:** zmiana układu HUD (nadpisuje DECISIONS #33 i część GDD 5.1/5.2):
  - **Lewy górny róg:** 4 statystki (zdrowie, głód, nawodnienie, sanity). Zamiast 10 ikon każda ma **jedną ikonę, która dynamicznie zmienia wygląd** i pokazuje **liczbę punktów**, a nie liczbę ikon.
  - **Prawy górny róg:** reszta, czyli „equiped set” (założone wyposażenie).
  - **Nad paskiem przedmiotów** zostają pozostałe statystyki, np. zdrowie zwierzaka (wierzchowca) i „nawodnienie” (do wyjaśnienia, patrz niżej).
  - **Wykonalność, niesprawdzona:** po stronie serwera dla zwykłego klienta to najtrudniejsza zmiana HUD.
    - Zastąpienie vanilla serc, głodu i pancerza: da się je schować, podmieniając ich sprite'y w paczce serwera na przezroczyste (globalnie, dla każdego gracza tego serwera), a własne rysować po swojemu.
    - Prawy górny róg ma natywne miejsce na **ikony efektów**; ikona niestandardowego efektu z własną teksturą pojawia się dokładnie tam. Liczba zmieniających się wartości (np. każda wartość zdrowia osobno) wymagałaby dużo efektów lub podmiany ikon.
    - Lewy górny róg nie ma natywnego miejsca w vanilla (nic tam nie ma, poza debugiem). Tytuł i action bar są wyśrodkowane, a przesunięcie w bok przez znaki spacji zależy od szerokości okna i skali GUI gracza, więc dokładne umieszczenie w rogu nie jest pewne.
    - Do zrobienia: krótki test wykonalności (które rogi i jak stabilnie) przed decyzją o dalszych krokach.
  - **Do wyjaśnienia:** co dokładnie pokazuje „equiped set” (tylko pancerz, czy też trzymane przedmioty), oraz co oznacza „nawodnienie” nad paskiem przedmiotów, skoro nawodnienie jest w lewym górnym rogu (może chodzi o powietrze pod wodą?).

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
  - Po usunięciu slopów PNG (Z-slopy-3D) obrazy jumpscare'ów muszą być osobnymi plikami.
  - Pomysły: przykrycie ekranu (np. ślepota/ciemność razem z obrazem), seria szybko zmieniających się klatek, drżenie i przybliżanie, szum. Ograniczenie: obraz jest znakiem czcionki w tytule, więc dynamikę trzeba robić serią tytułów lub dodatkowymi efektami. Wymaga zrobienia i sprawdzenia w grze.
- **F4. Komenda `/thm` do teleportacji do specjalnych struktur:** np. `/thm tp dark_corner|slender_forest|holy_pond|...`. Dziś jest tylko `/thm dark_corner locate` i `locate` dla lasu Slendermana.

## Pomysły od użytkownika i znajomych

- **I1. Więcej dźwięków otoczenia.** Konkretne listy i momenty do ustalenia osobno.
- **I2. Szarszy klimat w dzień.** Kolory bardziej wyblakłe, ale niezbyt mocno. Najpewniej przez efekty z paczki (patrz I7) albo inne sztuczki z resource packa.
- **I3. Dźwięki przy niskim sanity.** Przy sanity **poniżej bezpiecznego 15** (czyli ≤ 14) gracz ma słyszeć też: fałszywe kroki, krzyk dobiegający „zza ściany”, fałszywe creepery (syk). Dziś dźwięki tła pojawiają się dopiero przy ≤ 9. Do ustalenia: częstość i skalowanie z poziomem sanity. Pliki dźwięków od designu, na początek zaślepki z vanilla.
- **I4. Nowy wariant neutralnego slopa.** Atakuje **tylko wtedy, gdy gracz spojrzy mu w oczy** (jak Enderman). Wymaga sprawdzenia kierunku patrzenia po stronie serwera. Nadpisuje GDD 4 (neutralne „nie atakują”), więc to dodatkowy wariant.
- **I5. Mini-struktury: pojedyncze groby** rozsiane po mapie. Ustalenia użytkownika: ma to być **straszna** mini-struktura z **beczką z lootem, którą znajduje się po rozkopaniu** grobu. Do ustalenia: co jest w beczce (tabela lootu), głębokość, częstość, jak straszy (cząsteczki, dźwięki, zdarzenia przy rozkopaniu), jak współgra z blokiem „Krzyż nagrobny”.
- **I6. Wszystkie slopy i halucynacje jako moby 3D** (patrz Z-slopy-3D).
- **I7. Shadery z gotowej paczki.** Zamiast pisać od zera: wziąć gotową, **niewymagającą** paczkę open source na licencji **MIT** lub podobnie wolnej, i dodać kilka własnych efektów. Do zrobienia przed wyborem:
  - szukamy paczki, która działa jako zwykły resource pack (efekty z paczki zasobów), a nie jako shaderpack dla Iris, bo Iris wymaga instalacji u gracza, a serwer może dostarczyć tylko zwykłą paczkę,
  - sprawdzić licencję (wymagana atrybucja?), zgodność z 26.2, zużycie wydajności i zgodność z naszą paczką (łączenie plików),
  - pierwszy test: jeden efekt (ziarno i odbarwienie przy niskim sanity), sprawdzić działanie kamery i płynność (wcześniejsze ustalenia).
- Pozostałe pomysły: napięcie przed pojawieniem się czegoś (zapowiedzi, ciche zwierzęta, gasnące pochodnie), więcej wariantów kartek, ciemniejsze noce w lasach.

## Do doprecyzowania

- Z-odwodnienie i Z-mruganie: czy „6” to punkty (3 ikony) czy ikony (12 pkt), i czy miganie jest przy „poniżej 6” (nowa wypowiedź) czy „poniżej 4” (wcześniejsza).
- Z-slopy-3D: styl „fotorealistyczne moby MC”.
- Z-HUD: co pokazuje „equiped set”; co to za statystyka „nawodnienie” nad paskiem przedmiotów; czy ikony punktów mają pokazywać też wartość maksymalną.
- I4: czy nowy neutralny ma zastąpić któryś z obecnych, czy dojść jako czwarty wariant.
- I5: tabela lootu w beczce i sposób straszenia.
