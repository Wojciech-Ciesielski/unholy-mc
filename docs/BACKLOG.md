# The Unholy Minecraft – lista błędów i zmian po testach (v0.1.1)

Zapis uwag po „intensywnych” testach z użytkownikiem i znajomymi. Stan prac: patrz sekcja „Stan realizacji” na końcu.
Przyczyny oznaczone „hipoteza” nie zostały zweryfikowane w kodzie ani w grze.
Rzeczy dla wątku designu są dodatkowo w `docs/DESIGN_REQUESTS.md`.

Oznaczenia: **[B]** błąd, **[Z]** zmiana specyfikacji (nadpisuje GDD/DECISIONS), **[F]** nowa funkcja, **[?]** do doprecyzowania z użytkownikiem.

## Błędy

### B1. [B] Wysokość pasków sanity i nawodnienia — zastąpione przez Z-HUD
- Uwaga użytkownika: paski są wyżej, niż powinny. Podnoszą się tylko przy dodatkowej statystyce (pancerz), a pancerz ma być nad sanity.
- Stan obecny: w v0.1.1 paski to znaki czcionki w action barze na stałej wysokości, jeden rząd nad sercami, więc bez pancerza zostaje pusty rząd.
- **Ten punkt traci znaczenie**, bo cały HUD zostaje przeprojektowany (patrz Z-HUD poniżej, decyzja #34). Zostawiam jako zapis historii.

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
- **Z-mruganie [Z]:** miganie ekranu i darkness przy odwodnieniu poniżej **12 pkt (6 ikon)**. Zastępuje wcześniejsze „poniżej 4”. (Decyzja #35/#45.)
- **Z-spawn [Z]:** pojedyncze agresywne slopy mają pojawiać się też na polanach (patrz B3). Nadpisuje GDD 4.
- **Z-anomalie [Z]:** gęstość anomalii co 50–60 kratek (nadpisuje poprzednie 100–150).
- **Z-odwodnienie [Z]:** złagodzić efekty (decyzja #35, nadpisuje GDD 5.1 i DECISIONS #29). Jedna wartość: **12 pkt (< 6 ikon)**.
  - **Nawodnienie < 12:** miganie, darkness i brak sprintu.
  - **Głód < 12 i nawodnienie < 12 jednocześnie:** Mining Fatigue i Weakness.
  - Efekty pośrednie (rozmycie, Mining Fatigue przy 14–10, Weakness II i nudności przy 9–5) usuwane. Obumieranie przy 0 pkt bez zmian.
  - Konfiguracja: sekcja `hydration` w configu. Dotyczy `SurvivalSystem.applyEffects`, `ServerFx.dehydration`, `ServerFx.noSprint`.
  - Zapis „brak sprintu przy nawodnieniu < 12” to moje rozumienie wypowiedzi „jak u głodu”. Głód w vanilla blokuje sprint przy ≤ 6 pkt, to się nie zmienia.
- **Z-slopy-3D [ANULOWANE]:** autor odrzucił modele 3D (2026-11-26). Slopy i halucynacje **zostają billboardami PNG**, decyzja #36 anulowana. Zostają: większe rozmiary (B7), animacja ruchu billboardów (F1), hitboxy (B6), a nowe slopy z obrazków autora czekają na pliki źródłowe.
  - Jumpscare'y mają teraz własne klatki od designu (patrz F3), więc nie zależą od PNG slopów.
- **Z-HUD [Z]:** nowy układ HUD (decyzja #34, zastępuje DECISIONS #33):
  - **Lewy górny róg:** 4 statystki (zdrowie, głód, nawodnienie, sanity) jako po **jednej ikonie dynamicznej** z liczbą punktów (zamiast 10 ikon).
  - **Prawy górny róg:** założony zestaw, **tylko pancerz** (potwierdzone).
  - **Nad paskiem przedmiotów:** zdrowie zwierzaka (wierzchowca) i **powietrze pod wodą** (potwierdzone, że „nawodnienie” w tym zdaniu znaczyło powietrze). To są vanilla elementy, które dziś są już w tym miejscu, więc wystarczy ich nie ruszać.
  - **Ukrycie vanilla serc, głodu i pancerza:** podmiana ich sprite'ów w paczce serwera na przezroczyste (dla wszystkich graczy tego serwera). Stare paski mózgów/kropli z action baru (`ServerHud`) są wycofywane.
  - **Wykonalność, niesprawdzona:**
    - Prawy górny róg ma natywne miejsce na **ikony efektów**: niestandardowy efekt z własną teksturą pojawia się dokładnie tam. Liczba zmieniających się wartości (np. każda wartość zdrowia) wymagałaby wielu efektów lub podmiany ikon.
    - Lewy górny róg nie ma natywnego miejsca w vanilla. Tytuł i action bar są wyśrodkowane, a przesunięcie w bok znakami spacji zależy od szerokości okna i skali GUI gracza, więc dokładne umieszczenie w rogu jest niepewne.
    - **Zrobić krótki test wykonalności** (które rogi i jak stabilnie) przed przeróbką.
  - Mniejsze luki do uzupełnienia: jak pokazać dodatkowe serca (absorpcja), efekty zdrowia (trucizna, wither) i hardcore na pojedynczej ikonie zdrowia.

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

- Z-HUD: czy pojedyncze ikony mają pokazywać też wartość maksymalną; jak pokazać absorpcję, trucizny i hardcore.
- Z-odwodnienie: potwierdzić, że brak sprintu przy nawodnieniu < 12 pkt to właściwy próg.
- I4: czy nowy neutralny slop ma zastąpić któryś z obecnych, czy dojść jako czwarty wariant.
- I5: tabela lootu w beczce w grobie i sposób straszenia.
- B5: co znaczy „anomalie nie chodzą” (moby anomalii, np. zombie z dynią?).

## Stan realizacji

Zrobione i sprawdzone w grze (serwer dedykowany + czysty klient vanilla), commity `4c53576` i `ac9f042`:
- **Z-szybkość deszczu** (pogoda: deszcz częsty i długi, burze częstsze; mnożniki w `world.*`). Kod gotowy, ale rzeczywisty rytm pogody nie był obserwowany przez dłuższy czas.
- **B4/Z-anomalie:** siatka 56 kratek, komórki w blokach (nie w chunkach). Rzeczywista gęstość na świeżym świecie jeszcze niezmierzona.
- **Z-odwodnienie, Z-mruganie:** jeden próg 12 pkt, Mining Fatigue i Weakness tylko przy głodzie i nawodnieniu poniżej 12. Przetestowane tylko przez kompilację i wcześniejsze efekty, bez oglądania w grze.
- **B2:** zapadanie się. Przeprojektowałem wygląd (zwykłe elementy zamiast pasażerów zombie), slop chodzi po ziemi i stoi na niej. Błędu nie udało mi się odtworzyć przed zmianą, więc nie mam pewności, że to była ta przyczyna.
- **B6:** hitbox przez element interakcji o dokładnym rozmiarze. Trafienia działają (życie 20 → 17 po trzech ciosach).
- **B7:** slopy większe o 30% (`slops.sizeMultiplier`).
- **B8:** cząsteczki trafienia (kod gotowy; nie obejrzałem ich na ekranie).
- **Z-chód Slendermana:** dłuższy krok, ręce przeciw nogom, pochylenie, skręt i podskok przy kroku. Nie obejrzany w grze.
- **B3:** własny spawner slopów w nocy (las: wszystkie kategorie, polany: pojedyncze agresywne). W lesie w ciągu 50 s pojawiły się 2 agresywne i 1 neutralny. Na otwartym terenie nie zdążyłem potwierdzić.
- **F4:** `/thm tp <dark_corner|slender_forest|holy_pond>`. Działa dla ciemnego zakątka. Bajorka tylko w załadowanych chunkach.
- **F1 (część):** animacja picia z termosu (termos unosi się do twarzy).

Do zrobienia: B5 (ruch moby/anomalii), B9 (niebo Krwawego Księżyca), F1 reszta (animacje slopów), F2 (dźwięki), F3 (jumpscare'y), I2–I7, Z-slopy-3D, Z-HUD (patrz niżej).

### Wyniki testu wykonalności Z-HUD

Sprawdzone na serwerze dedykowanym i czystym kliencie vanilla:
- **Ukrycie serc, głodu i pancerza: działa.** Podmiana sprite'ów w `assets/minecraft/textures/gui/sprites/hud/` (`heart/*`, `food_*`, `armor_*`, z wyjątkiem `vehicle_*`) na przezroczyste. Dotyczy wszystkich graczy serwera. Nie zostało zacommitowane, bo bez nowego HUD zdrowie byłoby niewidoczne.
- **Prawy górny róg: działa.** Niestandardowy efekt z własną ikoną pojawia się jako ikona w prawym górnym rogu (sprawdzone na przykładzie efektu oporności).
- **Lewy górny róg: nie da się natywnie.** Nie ma w vanilla elementu zakotwiczonego w lewym górnym rogu. Tytuł i action bar są wyśrodkowane, a ich przesunięcie zależy od szerokości okna i skali GUI gracza, więc narożnik nie jest pewny.
- **Tabela wyników po prawej (sidebar scoreboard): działa.** Zakotwiczona przy prawej krawędzi, wyśrodkowana w pionie, skaluje się z GUI. Wiersze mogą zawierać glify naszej czcionki (ikonę) i liczbę. Wymaga doklejenia do czcionki zwykłych cyfr (dziś nasza czcionka ich nie ma i cyfry wyświetlają się jako ramki).
- **Alternatywy dla lewego górnego rogu:** (A) panel statystyk po prawej stronie w sidebarze (4 wiersze: ikona + liczba) i pancerz jako ikony efektów w prawym górnym rogu; (B) kompaktowy rząd ikon z liczbami nad paskiem przedmiotów (jak dziś, ale jedna ikona na statystykę); (C) szacowane przesunięcie w lewo od środka, niepewne.
- Decyzja użytkownika potrzebna: który wariant.
