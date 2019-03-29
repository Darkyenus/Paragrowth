#version 330

flat in vec4 v_color;
out vec4 fragmentColor;

void main() {
	fragmentColor = vec4(v_color.rgb, 1.0);
}
