#version 330

// The Unholy Minecraft: vanilla 26.2 fog.glsl + lekka atmosfera:
//  - korekcja kolorów świata: -20% nasycenia, chłodniej (szarawy dzień),
//  - noc: głębsze cienie (jasne źródła światła zostają jasne),
//  - delikatna mgiełka zaczynająca się bliżej niż w vanilla.
// Działa tylko tam, gdzie gra rysuje mgłę (FogColor.a > 0), czyli w świecie; GUI i przedmioty w ekwipunku bez zmian.
// Podczas Krwawego Księżyca ten plik zastępuje wersja z nakładki resourcepacks/blood_moon.

layout(std140) uniform Fog {
    vec4 FogColor;
    float FogEnvironmentalStart;
    float FogEnvironmentalEnd;
    float FogRenderDistanceStart;
    float FogRenderDistanceEnd;
    float FogSkyEnd;
    float FogCloudsEnd;
};

vec3 unholy_grade(vec3 c) {
    float l = dot(c, vec3(0.299, 0.587, 0.114));
    vec3 g = mix(vec3(l), c, 0.80) * vec3(0.97, 1.0, 1.03);
    // jasność mgły ~ pora dnia: jasna mgła = dzień, ciemna = noc
    float day = clamp(dot(FogColor.rgb, vec3(0.3333)) * 1.8, 0.0, 1.0);
    vec3 night = pow(max(g, vec3(0.0)), vec3(1.35));
    return mix(night, g, 0.25 + 0.75 * day);
}

float linear_fog_value(float vertexDistance, float fogStart, float fogEnd) {
    if (vertexDistance <= fogStart) {
        return 0.0;
    } else if (vertexDistance >= fogEnd) {
        return 1.0;
    }
    return (vertexDistance - fogStart) / (fogEnd - fogStart);
}

float total_fog_value(float sphericalVertexDistance, float cylindricalVertexDistance, float environmentalStart, float environmantalEnd, float renderDistanceStart, float renderDistanceEnd) {
    float vanilla = max(linear_fog_value(sphericalVertexDistance, environmentalStart, environmantalEnd), linear_fog_value(cylindricalVertexDistance, renderDistanceStart, renderDistanceEnd));
    float haze = linear_fog_value(cylindricalVertexDistance, renderDistanceEnd * 0.35, renderDistanceEnd) * 0.35;
    return max(vanilla, haze);
}

vec4 apply_fog(vec4 inColor, float sphericalVertexDistance, float cylindricalVertexDistance, float environmentalStart, float environmantalEnd, float renderDistanceStart, float renderDistanceEnd, vec4 fogColor) {
    float fogValue = total_fog_value(sphericalVertexDistance, cylindricalVertexDistance, environmentalStart, environmantalEnd, renderDistanceStart, renderDistanceEnd);
    vec3 base = inColor.rgb;
    vec3 fogRgb = fogColor.rgb;
    if (fogColor.a > 0.0) {
        base = unholy_grade(base);
        fogRgb = mix(vec3(dot(fogRgb, vec3(0.299, 0.587, 0.114))), fogRgb, 0.75);
    }
    return vec4(mix(base, fogRgb, fogValue * fogColor.a), inColor.a);
}

float fog_spherical_distance(vec3 pos) {
    return length(pos);
}

float fog_cylindrical_distance(vec3 pos) {
    float distXZ = length(pos.xz);
    float distY = abs(pos.y);
    return max(distXZ, distY);
}
