#version 150

in vec3 a_position;
in vec4 a_color;

out vec4 v_color;

uniform mat4 u_projViewWorldTrans;

void main() {
	v_color = a_color;

	gl_Position = u_projViewWorldTrans * vec4(a_position, 1.0);;
}
