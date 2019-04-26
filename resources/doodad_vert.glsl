#version 330

in vec3 a_position;
in vec4 a_color;
in float a_blend_offset;

flat out vec4 v_color;

uniform mat4 u_projViewTrans;
uniform float u_blend;

void main() {
	v_color = a_color;

	vec3 pos = a_position;
	pos.z += a_blend_offset * u_blend;

	gl_Position = u_projViewTrans * vec4(pos, 1.0);
}
