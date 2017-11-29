#version 150

in vec4 v_color;

void main() {
	vec4 diffuse = v_color;

	gl_FragColor = vec4(diffuse.rgb, 1.0);
}
