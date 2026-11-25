"""Jedno źródło prawdy dla dźwięków: generuje assets/unholymc/sounds.json i tabelę markdown (assets_src/sounds_table.md)."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
A = ROOT / "src/main/resources/assets/unholymc"

# (event id, liczba wariantów, stream, kanał, opis PL, napis EN, napis PL)
SOUNDS = [
    # --- jumpscare'y
    ("jumpscare.screamer", 4, False, "master/UI", "Zwykły jumpscare: ostry krzyk + trzask szumu, 0,8–1,2 s, bardzo głośny początek (bez narastania).", "Something screams", "Coś krzyczy"),
    ("jumpscare.spooky_buildup", 1, False, "master/UI", "Spooky Jumpscare, faza 1 (ok. 3 s): wszystko cichnie, zwalniające bicie serca, szept „you slept too long”.", "Heartbeat slows", "Serce zwalnia"),
    ("jumpscare.spooky", 1, False, "master/UI", "Spooky Jumpscare, faza 2 (ok. 1,5 s): przesterowany wrzask + zgrzyt metalu + przester TV, kończy się ucięciem do ciszy.", "You never woke up", "Już się nie obudzisz"),
    # --- slopy agresywne
    ("entity.slop.aggressive.ambient", 3, False, "hostile", "Niskie charczenie / mamrotanie, zniekształcone (pitch −30%).", "Slop mutters", "Slop mamrocze"),
    ("entity.slop.aggressive.chase", 2, False, "hostile", "Stinger przy rozpoczęciu pościgu: nagły smyczkowy pisk + dyszenie.", "Slop gives chase", "Slop rusza w pogoń"),
    ("entity.slop.aggressive.hurt", 2, False, "hostile", "Zgrzyt papieru/płótna + jęk.", "Slop hurts", "Slop obrywa"),
    ("entity.slop.aggressive.death", 1, False, "hostile", "Darcie papieru + gasnący wrzask.", "Slop dies", "Slop ginie"),
    # --- slopy neutralne
    ("entity.slop.neutral.ambient", 3, False, "neutral", "Chichot / szept z daleka, lekko odwrócony.", "Slop giggles", "Slop chichocze"),
    ("entity.slop.neutral.scare", 3, False, "neutral", "Głośne „BU!”/wrzask przy straszeniu gracza (moment przed ucieczką).", "Slop shrieks", "Slop wrzeszczy"),
    ("entity.slop.neutral.flee", 2, False, "neutral", "Szybkie tupanie / szuranie oddalające się.", "Slop scurries away", "Slop ucieka"),
    ("entity.slop.neutral.hurt", 2, False, "neutral", "Piskliwe jęknięcie.", "Slop squeals", "Slop piszczy"),
    ("entity.slop.neutral.death", 1, False, "neutral", "Pisk + pyknięcie (jak pęknięty balon).", "Slop pops", "Slop pęka"),
    # --- unholy slopy
    ("entity.slop.unholy.ambient", 3, False, "hostile", "Chóralny, odwrócony szept w nieznanym języku + niski dron.", "Unholy whispers", "Bluźniercze szepty"),
    ("entity.slop.unholy.attack", 2, False, "hostile", "Uderzenie + zasysający dźwięk (nałożenie Darkness).", "Unholy Slop strikes", "Bluźnierczy slop uderza"),
    ("entity.slop.unholy.hurt", 2, False, "hostile", "Skwierczenie (święte obrażenia) + syk.", "Unholy Slop sizzles", "Bluźnierczy slop skwierczy"),
    ("entity.slop.unholy.immune", 1, False, "hostile", "Głuchy, metaliczny „tunk” – cios nie robi wrażenia (zwykła broń).", "Attack has no effect", "Cios nie działa"),
    ("entity.slop.unholy.death", 1, False, "hostile", "Wielogłosowy wrzask zanikający w pogłosie kościelnym.", "Unholy Slop perishes", "Bluźnierczy slop ginie"),
    # --- halucynacje
    ("entity.hallucination.spawn", 2, False, "hostile", "Cichy szum + wdech tuż przy uchu.", "Something appears", "Coś się pojawia"),
    ("entity.hallucination.ambient", 3, False, "hostile", "Szepty, zniekształcone imię „...hey...”, mocny pogłos.", "Whispers", "Szepty"),
    ("entity.hallucination.hurt", 1, False, "hostile", "Rozmazany, „wodny” jęk.", "Hallucination flickers", "Halucynacja migocze"),
    ("entity.hallucination.dissolve", 1, False, "hostile", "Rozpad w szum + ulga (opadający ton).", "Hallucination fades", "Halucynacja znika"),
    # --- Slenderman
    ("entity.slenderman.ambient", 2, False, "hostile", "Niski szum statyczny + odległy dźwięk dzwonu.", "Static hums", "Szumi statyka"),
    ("entity.slenderman.proximity", 1, False, "hostile", "Pętla (ok. 8 s, bez kliku na łączeniu) narastającego szumu; kod skaluje głośność wg dystansu.", "Static intensifies", "Statyka narasta"),
    ("entity.slenderman.teleport", 3, False, "hostile", "Glitch: cyfrowe zacięcie + bitcrush + krótki trzask, 0,3–0,5 s.", "Slenderman glitches", "Slenderman się zacina"),
    ("entity.slenderman.summon", 1, False, "hostile", "Przyzwanie halucynacji: niski ryk przechodzący w chór szeptów.", "Slenderman summons", "Slenderman przyzywa"),
    ("entity.slenderman.hurt", 2, False, "hostile", "Trzask szumu + metaliczny zgrzyt.", "Slenderman hurts", "Slenderman obrywa"),
    ("entity.slenderman.death", 1, False, "hostile", "Długi (3 s) rozpad szumu w ciszę, na końcu jeden dzwon.", "Slenderman dies", "Slenderman ginie"),
    ("entity.slenderman.vanish", 1, False, "hostile", "Odejście o świcie: glitch + odwrócony dzwon.", "Slenderman vanishes", "Slenderman znika"),
    ("entity.slenderman.deadly_roar", 1, False, "hostile", "Krwawy Księżyc: przemiana w Deadly Slendermana, ryk ze zniekształceniem.", "Deadly Slenderman roars", "Śmiertelny Slenderman ryczy"),
    # --- ambient / muzyka
    ("music.eleven_reversed", 1, True, "music", "Pętla: przerobiona płyta 11 – spowolniona (ok. 0,6×) i odwrócona, stereo, płynna pętla. Kod gra ją ciągle (noc +10%, jaskinie +15%).", "Eerie music plays", "Gra niepokojąca muzyka"),
    ("ambient.low_sanity", 4, False, "ambient", "„Głośne, niepokojące dźwięki w tle” (Sanity 9–5): kroki za plecami, pukanie, drapanie, oddech.", "Something is nearby", "Coś jest w pobliżu"),
    ("ambient.heartbeat", 1, False, "ambient", "Bicie serca (Sanity ≤4), pętla 2 s.", "Heartbeat", "Bicie serca"),
    ("ambient.dark_corner", 1, True, "ambient", "Pętla w ciemnym zakątku: dron + roje owadów + trzaski.", "Darkness hums", "Ciemność brzęczy"),
    ("ambient.blood_moon.rise", 1, False, "ambient", "Start Krwawego Księżyca: dzwon pogrzebowy + chór, ok. 6 s.", "The Blood Moon rises", "Wschodzi Krwawy Księżyc"),
    ("ambient.full_moon.bless", 1, False, "ambient", "Bajorko staje się święcone (pełnia): delikatny chór + dzwoneczki.", "Water is blessed", "Woda zostaje pobłogosławiona"),
    # --- itemy / bloki
    ("item.holy_water.sizzle", 2, False, "players", "Święta woda pali moba: skwierczenie + syk pary.", "Holy water burns", "Święcona woda parzy"),
    ("item.crucifix.use", 2, False, "players", "Aktywne użycie: jasny dzwonek + chór „aah”, krótki.", "Crucifix glows", "Krucyfiks rozbłyska"),
    ("item.crucifix.save", 1, False, "players", "Funkcja totemu: rozbicie drewna + chór + dzwon.", "Crucifix shatters", "Krucyfiks pęka"),
    ("item.flashlight.toggle_on", 1, False, "players", "Klik włącznika + brzęczenie żarówki.", "Flashlight clicks on", "Latarka się włącza"),
    ("item.flashlight.toggle_off", 1, False, "players", "Klik włącznika (niższy).", "Flashlight clicks off", "Latarka się wyłącza"),
    ("item.flashlight.insert_core", 1, False, "players", "Wsunięcie rdzenia: metaliczny zatrzask.", "Core inserted", "Rdzeń włożony"),
    ("item.flashlight.remove_core", 1, False, "players", "Wysunięcie rdzenia: zatrzask + szuranie.", "Core removed", "Rdzeń wyjęty"),
    ("item.unholy_sword.charge", 1, False, "players", "Ładowanie klejnotem w kowadle: pulsujący, niski „wdech”.", "Unholy Sword feeds", "Bluźnierczy miecz się karmi"),
    ("item.unholy_sword.discharged", 1, False, "players", "Wytrzymałość spada do 0: gasnące, opadające buczenie.", "Unholy Sword goes dormant", "Bluźnierczy miecz gaśnie"),
    ("item.thermos.drink", 2, False, "players", "Picie z termosu (łyk z metalowym pogłosem).", "Drinking from thermos", "Picie z termosu"),
    ("item.thermos.fill", 1, False, "players", "Napełnianie: bulgotanie w metalowym naczyniu.", "Thermos fills", "Termos się napełnia"),
    ("block.tree_note.rip", 2, False, "blocks", "Zerwanie kartki z drzewa: darcie papieru.", "Note torn", "Kartka zerwana"),
    ("player.dehydrated", 2, False, "players", "Suchy kaszel (nawodnienie 0).", "Player coughs", "Gracz kaszle"),
]

def main():
    data = {}
    rows = ["| SoundEvent (`unholymc:`) | Pliki `.ogg` | Kanał | Opis | Napis EN / PL |", "|---|---|---|---|---|"]
    for sid, n, stream, cat, desc, en, pl in SOUNDS:
        base = sid.replace(".", "/")
        files = [f"{base}{i + 1}" if n > 1 else base for i in range(n)]
        entry = {"subtitle": f"subtitles.unholymc.{sid}", "sounds": []}
        for f in files:
            s = {"name": f"unholymc:{f}"}
            if stream: s["stream"] = True
            entry["sounds"].append(s if len(s) > 1 else s["name"])
        data[sid] = entry
        flist = ", ".join(f"`{f}.ogg`" for f in files) if n <= 2 else f"`{files[0]}.ogg` … `{files[-1]}.ogg`"
        rows.append(f"| `{sid}` | {flist} | {cat} | {desc} | {en} / {pl} |")
    (A / "sounds.json").write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n")
    (ROOT / "assets_src/sounds_table.md").write_text("\n".join(rows) + "\n")
    print(len(SOUNDS), "events,", sum(s[1] for s in SOUNDS), "files")

if __name__ == "__main__":
    main()
