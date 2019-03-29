#version 330

in vec2 a_position_xy;
in float a_position_z;
in vec4 a_color;
in vec3 a_normal;

flat out vec4 v_color;

uniform mat4 u_projViewTrans;
uniform mat4 u_worldTrans;
uniform float u_time;

const vec3 lightDirection = vec3(0.2, 0.0, 0.9797958975);

void main() {
	vec3 a_position = vec3(a_position_xy, a_position_z);

	float diffuse = dot(a_normal, lightDirection);
	v_color = vec4(a_color.rgb * diffuse, 1.0);

	vec4 pos = u_worldTrans * vec4(a_position, 1.0);

	if (pos.z <= 0.0) {
		float mixFactor = sqrt(clamp(-pos.z, 0.0, 1.0));
		float heightDisplacement = sin(pos.x + u_time) * sin(pos.y + u_time) * 0.1 + sin(pos.x*2.0 - u_time) * sin(pos.y*2.0 + u_time) * 0.2;
		float colorDisplacement = sin(pos.x + u_time*0.1 + 2.0) * cos(pos.y + u_time) * sin(pos.y + u_time) * 0.1 + sin(pos.x*0.08 - u_time) * sin(pos.y*0.08 + u_time) * 0.07;

		pos.z = mix(0.0, heightDisplacement, mixFactor);
		v_color.rgb += vec3(mix(0.0, colorDisplacement, mixFactor));
	}

	gl_Position = u_projViewTrans * pos;
}
