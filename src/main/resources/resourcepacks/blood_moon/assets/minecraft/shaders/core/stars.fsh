#version 330

#moj_import <minecraft:dynamictransforms.glsl>

out vec4 fragColor;

void main() {
    fragColor = vec4(ColorModulator.rgb * vec3(1.0, 0.35, 0.30), ColorModulator.a * 0.6);
}
