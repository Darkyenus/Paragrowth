#version 330

in vec3 a_position;
in vec4 a_color;
in float a_blend_offset;

flat out vec4 v_color;

uniform mat4 u_projViewTrans;

uniform float u_blend;
uniform vec3 u_blendEdgeLeft;
uniform vec3 u_blendEdgeRight;
uniform int u_blendIn;
const float MAX_EDGE_DIST_INV = 1.0 / 50.0;

float getBlend(vec2 pos) {
	float leftDist = dot(vec3(pos, 1.0), u_blendEdgeLeft);
	float rightDist = dot(vec3(pos, 1.0), u_blendEdgeRight);
	float maxDist = max(leftDist, rightDist);

	// Goes [0, 1] as u_blend goes [0, 0.5]
	float outerBlend = clamp(u_blend * 2.0, 0.0, 1.0);

	// Blend value for inside of the wedge
	// Goes [0, 1] as u_blend goes [0.5, 1]
	float wedgeBlend = clamp((u_blend - 0.5) * 2.0, 0.0, 1.0);

	// 0 inside the wedge, 1 completely outside wedge, coming closer to 0 when near the wedge
	float outWedge = clamp(maxDist * MAX_EDGE_DIST_INV, 0, 1);
	return mix(wedgeBlend, outerBlend, outWedge);
}

void main() {
	v_color = a_color;

	vec3 pos = a_position;
	float blend = getBlend(pos.xy);
	if (u_blendIn > 0) {
		blend = 1.0 - blend;
	}

	pos.z += a_blend_offset * blend;

	gl_Position = u_projViewTrans * vec4(pos, 1.0);
}
