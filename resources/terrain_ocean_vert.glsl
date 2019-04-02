#version 330

in vec3 a_position;
in vec4 a_color;
in vec4 a_normal;

flat out vec4 v_color;

uniform mat4 u_projViewTrans;
uniform vec2 u_worldTrans[64];
uniform vec3 u_position;
uniform float u_time;

uniform sampler2D u_displacement;
uniform sampler2D u_normal;

const vec3 lightDirection = vec3(0.2, 0.0, 0.9797958975);

void main() {
	float diffuse = dot(a_normal.xyz, lightDirection);
	v_color = vec4(a_color.rgb * diffuse, 1.0);

	vec4 pos = vec4(a_position, 1.0);
	pos.xy += u_worldTrans[gl_InstanceID];

	if (pos.z <= 0.0) {
		// General
		float mixFactor = sqrt(clamp(-pos.z, 0.0, 1.0));
		vec2 oceanSamplePos = (pos.xy + vec2(u_time)) * 0.01;
		float displacement = texture(u_displacement, oceanSamplePos).x;

		// Position
		float heightDisplacement = displacement - 1.0;
		pos.z = mix(0.0, heightDisplacement, mixFactor);

		// Color
		vec3 normal = normalize(texture(u_normal, oceanSamplePos).xyz);
		vec3 cameraToHere = normalize(u_position - pos.xyz);
		vec3 reflection = reflect(-lightDirection, normal);
		float specularBase = dot(reflection, cameraToHere);
		float specular = 0.0;
		if (specularBase > 0.0) {
			specular = pow(specularBase, 20.0);
		}

		float colorDisplacement = pow(displacement, 2.0);

		v_color.rgb += vec3(specular + colorDisplacement * 0.5);
	}

	gl_Position = u_projViewTrans * pos;
}
