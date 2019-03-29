#version 330

in vec3 a_position;
in vec4 a_color;

flat out vec4 v_color;

uniform mat4 u_projViewTrans;

void main() {
	v_color = a_color;

	gl_Position = u_projViewTrans * vec4(a_position, 1.0);
}
