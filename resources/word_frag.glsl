#version 330

flat in vec3 v_color;
in vec2 v_uv;

uniform sampler2D u_font_texture;

out vec4 fragmentColor;

void main() {
	vec4 textureColor = texture(u_font_texture, v_uv);
	if (textureColor.a < 0.5) {
		discard;
	}

	fragmentColor = vec4(v_color.rgb * textureColor.rgb, 1.0);
}
