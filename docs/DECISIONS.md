# The Unholy Minecraft – decyzje i doprecyzowania GDD

Ten plik **nadpisuje** `docs/GDD_original.md` wszędzie tam, gdzie się różnią.
Oryginalny GDD to tekst wyciągnięty z dokumentu .docx autora (bez zmian).

## Techniczne

| # | Decyzja |
|---|---------|
| T1 | Minecraft **26.2** (potwierdzone przez autora; nie 26.2.1), Fabric (Loader + Fabric API), Java 25. |
| T2 | Mod jest projektowany **pod serwer dedykowany multiplayer** (musi działać też w singleplayerze/LAN). Ścisły podział client/server, sieć przez Fabric Networking, dane graczy przez Data Attachments. |
| T3 | **Brak wsparcia** Sodium/Iris i innych modów. Zamknięty projekt dla znajomych, maintainer = autor. |
| T4 | Wszystkie wartości liczbowe (tempa, szanse, promienie, HP, DMG) **konfigurowalne** w pliku configu (serwer jest źródłem prawdy). |
| T5 | Grafiki slopów: surowe obrazki w `assets_src/slop_png_raw/` (część śmieszna, część straszna). Dźwięki są w produkcji – kod ma mieć gotowe `SoundEvent`y i rejestrację z placeholderami. Assety projektuje osobny wątek designu. |
| T6 | Projekt zamknięty dla znajomych – można użyć przerobionej (spowolnionej, odwróconej) płyty 11 jako własnego assetu dźwiękowego. |
| T7 | Języki: **en_us** i **pl_pl**. |
| T8 | Branch: **tylko `main`**. |
| T9 | **Mod działa tylko po stronie serwera** (od v0.1.1, biblioteka Polymer wbudowana w jar). Gracze wchodzą **zwykłym klientem vanilla**, bez Fabrica i bez modów; zasoby dostarcza serwerowa paczka zasobów hostowana automatycznie. Zastępuje podział client/server z T2: kod kliencki i własne pakiety sieciowe zostały usunięte. |
| T10 | Wszystko, co widzi gracz, musi być możliwe do zrobienia na kliencie vanilla (paczka zasobów, znaki czcionki, encje display, efekty, dźwięki, cząsteczki). Efekty niemożliwe bez klienta (np. kolor nieba, barwa wody) są odrzucane albo robione zastępczo. |

## Mechaniki

1. **Światło latarki i krucyfiksu jest prawdziwe** – działa jak pochodnia/glowstone (dynamiczne źródło światła w świecie, np. niewidzialne bloki `light` podążające za graczem).
2. **Latarka**: tylko świeci + kontruje aurę ciemności. Bez wytrzymałości i baterii. Rdzeń wkłada/wyjmuje się kliknięciem w ekwipunku jak przy **bundle**.
3. **Koszmarny klejnot jako rdzeń latarki**: zasięg jak lapis/emerald (6 kratek, stożek), ale świeci „ciemnym światłem” (wizualnie ciemne). Agresywne slopy w stożku stają się neutralne. Nie działa na Unholy Slopy.
4. **Halucynacje – spawn wg pkt 5.3**: co 15 s w promieniu 20 kratek od gracza z niskim Sanity. Procentowe szanse na halucynacje z 5.2 nie obowiązują. *Do potwierdzenia: próg „niskiego” Sanity – przyjęto ≤ 9 (konfigurowalne).*
5. **Progi Sanity**: najniższy próg to **≤ 4** (nie „< 4”). Ma te same interwały co próg wyżej (jumpscare co 60 ticków), tylko wyższe szanse.
6. **Super agresywne halucynacje – usunięte.**
7. **Nawodnienie**: biom mokry + sprint = **−3 pkt / min**. Nether: biom suchy ×2 = −8 pkt / 2 min, ze sprintem **−12 pkt / 2 min** (8 + 4).
8. **Wszystkie tempa „X pkt / 2 min” naliczane płynnie** (ułamkowo co tick).
9. **Aury nie kumulują się w obrębie jednego rodzaju**: 5 slopów obok = jedno −8, 3 krowy = jedno +4. Różne rodzaje aur sumują się. Wyjątek z GDD: bliskość graczy (+4 za gracza, max +20).
10. **Mnożnik Unholy Sword ×1,5** dotyczy tylko **aur ciągłych**, nie jednorazowych strat (jumpscare, surowe jedzenie).
11. **Święte obrażenia na Unholy Slopy = 150%**: splash 3 serca (już po mnożniku). Krucyfiks: Holiness I = 4,5, II = 6, **III = 7,5 serca**.
12. **Unholy Slopy są odporne na wszystko** (broń, strzały, ogień, lawa, TNT, upadek…) **poza** komendami (`/kill`, `/thm`) i świętymi przedmiotami.
13. **Deadly Slenderman**: +200% **tylko do HP** (450 HP); obrażenia **24 HP** (12 serc).
14. **Rozdział 6 GDD nie istnieje** (usunięty element).
15. **Stół alchemiczny = vanilla brewing stand**, jak każda mikstura (woda święcona + proch → splash).
16. **Bajorka wody święconej**: małe, kilkukratkowe. Na co dzień niczym się nie wyróżniają poza cząsteczkami nad sobą. W pełnię (bez Krwawego Księżyca) woda staje się święcona: lekko jasnoniebieska, z lekką poświatą. Bajorko daje aurę **+20 pkt Sanity / 2 min w promieniu 10 kratek**, a stanie w nim (taplanie się) **+1 pkt Sanity / s**. *Do potwierdzenia: czy aura i taplanie działają tylko gdy woda jest święcona – przyjęto, że tak.*
17. **Pełnia co 8 dni** – zgodnie z vanilla fazami księżyca (nie 10).
18. **Usunięte**: wydłużona noc (+20%), skrócony dzień oraz wydłużenie nocy Krwawego Księżyca (+40%). Cykl dobowy pozostaje waniliowy.
19. **Slopy spawnują się we wszystkich lasach z drewnem + dżungla** (+ jaskinie wg tabeli), nie tylko w ciemnych zakątkach. Reset bloków/rudy węgla w ciemnym zakątku **nadpisuje bloki postawione przez graczy**.
20. **Slenderman „jeden las”** = jeden spójny obszar biomu Dark Forest (oddzielony od innych innymi biomami). Każdy taki obszar ma 10% szansy na bycie lasem Slendermana; w nim dokładnie 1 Slenderman.
21. **Kartki** na drzewach po zniszczeniu dropią 1 papier.
22. **Halucynacje**: zadają prawdziwe obrażenia graczowi, który je widzi. Cios gracza w halucynację trafia realny byt znajdujący się w jej hitboxie (np. innego gracza).
23. **Halucynacje Slendermana**: zwykłe byty widoczne dla wszystkich, atakują dowolnych graczy.
24. **Krucyfiks**: działa w main hand (i w drugiej ręce wg autora – *do potwierdzenia: efekty pasywne w obu rękach?*). **Funkcja totemu nie działa w offhand.** Jedna butelka wody święconej w kowadle naprawia **10 pkt**. Enchant **Holiness**: stół zaklęć, księgi, oraz loot **tylko** w: Woodland Mansion, End City, Mineshaft.
25. **Unholy Sword przy 0 wytrzymałości nie pęka**: obrażenia spadają do **2 serc (4 HP)**, staje się „rozładowany” i ma inną teksturę – bez pulsujących, animowanych cząsteczek.
26. **Termos**: tylko jeden rodzaj płynu naraz. Mleko z termosu leczy efekty jak vanilla mleko. Napełnianie: PPM na źródło (lub krowę dla mleka). Bez przelewania z butelek.
27. **Picie bezpośrednio ze źródła wody (PPM)** – możliwe (jak butelka wody).
28. **Seria przespanych nocy jest per gracz**: przerywa ją noc, w której *ten gracz* nie spał. Funkcja „leżenie w łóżku bez przespania +30 / 2 min” – **usunięta**.
29. *(zmienione w #35)* **Efekty niskiego nawodnienia**: wolniejsze kopanie = **Mining Fatigue**; zmniejszone obrażenia = **Weakness I**, a przy jeszcze większym odwodnieniu (i wygłodzeniu) **Weakness II**.
30. **Obumieranie z pragnienia** (0,5 serca/s) nie zależy od poziomu trudności – picie jest ważniejsze od jedzenia.
31. **Peaceful / Creative / Spectator**: wyłączone Sanity i nawodnienie; slopy nie reagują na gracza (Creative/Spectator) lub nie istnieją (Peaceful).
32. **Anomalie świata** (często): liście bez drzew, czaszki szkieletów, redstone jako „krew”, abominacje (zły kolor liści / zły gatunek drzewa w danym biomie), zombie z dynią na głowie, krzyże.
33. *(zmienione w #34)* **HUD**: pasek Sanity (mózgi) **nad sercami**, pasek nawodnienia (krople) **nad głodem**.

## Zmiany po testach v0.1.1 (nadpisują GDD i powyższe punkty)

34. **Nowy HUD.** Lewy górny róg: 4 statystki (zdrowie, głód, nawodnienie, sanity). Każda to **jedna ikona, która dynamicznie zmienia wygląd** i pokazuje **liczbę punktów**, a nie 10 ikon. Prawy górny róg: **założony zestaw** (tylko pancerz). Nad paskiem przedmiotów zostają pozostałe statystyki: **zdrowie wierzchowca/zwierzaka** i **powietrze pod wodą**. Vanilla serca, głód i pancerz są ukrywane (podmiana sprite'ów w paczce serwera). Zastępuje #33.
35. **Złagodzone efekty głodu i nawodnienia.** Zastępuje #29 i GDD 5.1. Wszystko według jednej wartości: **12 pkt (mniej niż 6 ikon)**.
    - **Nawodnienie < 12 pkt:** miganie ekranu i darkness, brak sprintu.
    - **Głód < 12 pkt i nawodnienie < 12 pkt jednocześnie:** Mining Fatigue i Weakness.
    - Efekty pośrednie (rozmycie, Mining Fatigue przy 14–10, Weakness II i nudności przy 9–5) **usunięte**.
    - Obumieranie przy 0 pkt (#30) bez zmian.
36. ~~**Wszystkie slopy to moby 3D.**~~ **ANULOWANE przez autora (2026-11-26):** modele wyglądały źle. Slopy i halucynacje **zostają billboardami PNG** (GDD 4 obowiązuje). Nowe slopy z obrazków autora czekają na pliki źródłowe.
37. **Pojedyncze agresywne slopy pojawiają się także na polanach** i otwartym terenie (nadpisuje GDD 4: „unikają otwartych powierzchni”).
38. **Nowy neutralny slop atakuje tylko wtedy, gdy gracz spojrzy mu w oczy** (jak Enderman). Dodatek do neutralnych (czy zastępuje któryś, do ustalenia).
39. **Pogoda odwrócona:** deszcz jest częsty, słońce rzadkie, burze częstsze. Nadpisuje GDD 5.4 („+50% deszczu”).
40. **Anomalie świata co 50–60 kratek** (zmiana względem „bardzo często”, wcześniej ustalone 100–150).
41. **Dźwięki przy niskim sanity:** przy sanity **poniżej 15** (≤ 14) oprócz dotychczasowych: fałszywe kroki, krzyk „zza ściany”, fałszywe creepery. Rozszerza GDD 5.2.
42. **Mini-struktura: pojedynczy grób.** Straszna, z **beczką z lootem do rozkopania**.
43. **Szary klimat w dzień:** kolory lekko wyblakłe. Efekty graficzne z gotowej, lekkiej, wolnej paczki (licencja MIT lub podobna) działającej jako zwykły resource pack (bez Iris), z dodatkiem kilku własnych efektów.
44. **Komenda `/thm` do teleportacji do specjalnych struktur** (ciemny zakątek, las Slendermana, bajorko, grób itd.).
45. **Mruganie ekranu i darkness przy odwodnieniu** są zawarte w #35 (próg 12 pkt). Zastępują wcześniejszy zapis „< 4”.

## Kolejność prac (MVP)

1. Fundament: nawodnienie + Sanity + HUD + config + komendy `/thm`.
2. Przedmioty.
3. Slopy i halucynacje.
4. Świat (pogoda, Krwawy Księżyc, dźwięk, anomalie).
5. Struktury i Slenderman.
6. Po testach v0.1.1: lista zmian w `docs/BACKLOG.md` (HUD, moby 3D, dźwięki, shadery, groby, balans efektów).
