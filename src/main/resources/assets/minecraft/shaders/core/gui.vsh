#version 330

// The Unholy Minecraft: kopia vanilla 26.2 core/gui.vsh + ukrycie tła sidebara (tabeli wyników) po prawej stronie ekranu.
// Sidebar wypełnia tło czernią z alfą 0.3 (wiersze, 0x4C000000) i 0.4 (tytuł, 0x66000000), gdy gracz ma domyślne
// ustawienie „tło tekstu tylko na czacie”. Te dwa kolory w prawej połowie ekranu robimy przezroczystymi
// (gui.fsh odrzuca fragmenty z alfą 0). Reszta GUI bez zmian.
// Can't moj_import in things used during startup, when resource packs don't exist.
layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};
layout(std140) uniform Projection {
    mat4 ProjMat;
};

in vec3 Position;
in vec4 Color;

out vec4 vertexColor;

bool isSidebarBackground(vec4 c) {
    if (c.r > 0.001 || c.g > 0.001 || c.b > 0.001) return false;
    return abs(c.a - 76.0 / 255.0) < 0.002 || abs(c.a - 102.0 / 255.0) < 0.002;
}

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    vertexColor = Color;
    if (isSidebarBackground(Color) && gl_Position.x > 0.0) {
        vertexColor = vec4(0.0);
    }
}
