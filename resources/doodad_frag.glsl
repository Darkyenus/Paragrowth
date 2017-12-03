#version 330

flat in vec4 v_color;
out vec4 fragmentColor;

void main() {
	vec4 diffuse = v_color;

	fragmentColor = vec4(diffuse.rgb, 1.0);
}
