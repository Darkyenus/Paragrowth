#version 330

in vec4 v_color;
in vec2 v_texCoords;
out vec4 fragmentColor;

uniform sampler2D u_texture;

void main() {
  fragmentColor = v_color * texture(u_texture, v_texCoords);
}