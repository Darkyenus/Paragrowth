#version 330

in vec4 a_letter_xywh;
in vec4 a_letter_uvu2v2;
in vec3 a_position_xyz;
in vec4 a_color;

uniform mat4 u_projViewTrans;

flat out vec3 v_color;
out vec2 v_uv;

const vec2 faceMultipliers[4] = vec2[4](vec2(0, 0), vec2(0, 1), vec2(1, 1), vec2(1, 0));

void main() {
	vec2 faceMultiplier = faceMultipliers[gl_VertexID & 3];

	vec2 xy = a_letter_xywh.xy + faceMultiplier * a_letter_xywh.zw;
	v_uv = mix(a_letter_uvu2v2.xy, a_letter_uvu2v2.zw, faceMultiplier);
	v_color = a_color.rgb;

	vec4 screenSpacePos = u_projViewTrans * vec4(a_position_xyz, 1.0);
	screenSpacePos.xy += xy * (1.0 / 40.0) * a_color.a;
	gl_Position = screenSpacePos;
}
